#!/usr/bin/env bash
#
# 本地全栈 E2E：USER_EVENTS 全链路冒烟（隔离环境）
#
# 覆盖《CHANGELOG》未完成清单第 13 条的 ①–⑨：
#   ① 独立 Compose project / volume（不污染日常开发库）
#   ② 起 MySQL / Redis / Nacos / RocketMQ + 5 个 provider + gateway
#   ③ 等健康检查（--wait / 轮询），而不是固定 sleep
#   ④ 显式创建 cozy-user-events
#   ⑤ 断言 topic 4 队列 + 5 个 consumer group 在线且订阅 tag 正确
#   ⑥ 真实 API 注册邀请人 / 被邀请人，并用邀请码绑定邀请关系
#   ⑦ 真实 API 下单 → 接单 → 完成 → 确认取餐（触发 ORDER_COMPLETED）
#   ⑧ 等收敛后断言：资格已认领、outbox 恰好一行且 SENT、邀请券恰好一张、首单积分恰好一次
#   ⑨ 从订单 outbox 读原始载荷重放同一事件，断言积分/outbox/券都不增；失败留诊断、成功清环境
#
# 为什么值得有：HTTP → Order MQ → Member → Dubbo → User outbox → User MQ → Mall 券落库
# 这条编排，单测与 CI 结构上都覆盖不到（CI 排除了需要 MySQL/Redis/Nacos 的集成测试）——
# 2026-09-20 线上踩到的「消费者先上线 → topic 不存在 → group 根本不注册」正属此类。
#
# 用法：
#   bash scripts/e2e-user-events.sh            # 检测到日常 dev 栈在跑时会拒绝执行
#   bash scripts/e2e-user-events.sh --force    # 明确知道要并行时使用

set -euo pipefail
cd "$(dirname "$0")/.."

PROJECT=cozycoffee-e2e
TAG=local
GW_PORT=18080
GW="http://127.0.0.1:$GW_PORT"
COMPOSE=(docker compose -p "$PROJECT" -f docker-compose.prod.yml -f docker-compose.e2e.yml)
BROKER="$PROJECT-rocketmq-broker-1"
MQADMIN='cd /home/rocketmq/rocketmq-5.3.0/bin && ./mqadmin'
NS=rocketmq-namesrv:9876

# base 文件里 8 个变量用 ${VAR:?...} 声明为必填（Compose 解析时会校验存在性）。
# 本脚本不做 ACR 拉取、也不对公网暴露，所以全部给本地/一次性值：
#   ACR_REGISTRY/ACR_NAMESPACE/IMAGE_TAG —— 镜像已被覆盖层换成本地 tag，这里只为过校验
#   JWT_SECRET —— 仅供隔离环境签发 token，长度需满足 HS512（≥64 字节更稳）
export ACR_REGISTRY=dummy ACR_NAMESPACE=dummy IMAGE_TAG="$TAG"
export MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-e2e_root_pw}"
export MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-e2e_minio_pw_local}"
export JWT_SECRET="${JWT_SECRET:-e2e_only_jwt_secret_do_not_reuse_0123456789abcdef0123456789abcdef}"
export CORS_ALLOWED_ORIGINS="http://127.0.0.1:$GW_PORT"
export STORAGE_PUBLIC_BASE_URL="http://127.0.0.1:$GW_PORT/media"

# ------------------------------------------------- 运行前：资源冲突守卫
# 本脚本会起 11 个容器 + 跑一次 mvn package，与日常 dev 栈同时跑会抢内存/CPU。
# 只【停】不停删：docker compose stop（不要 down，更不要 -v）。
if [ "${1:-}" != "--force" ]; then
  conflicted=0
  dev_containers="$(docker ps --format '{{.Names}}' | grep -E '^cozycoffee-' | grep -v '^cozycoffee-e2e-' || true)"
  if [ -n "$dev_containers" ]; then
    echo "⚠️ 检测到日常 dev compose 栈仍在运行："
    echo "$dev_containers" | sed 's/^/     /'
    conflicted=1
  fi
  if netstat -ano 2>/dev/null | grep -qE ':(8080|8081)[^0-9].*LISTENING'; then
    echo "⚠️ 8080/8081 被占用（很可能是 IDE 里的 5 个 JVM）"
    conflicted=1
  fi
  if [ "$conflicted" = 1 ]; then
    cat <<'EOT'

本次 E2E 需要一份相对空闲的本机资源。建议先【停掉】（不要删卷）：
    docker compose stop                 # 在仓库根，日常 dev 栈
    # 再在 IDE 里停掉 5 个 provider / gateway 的运行配置
然后重跑本脚本。确认要继续（例如你本就打算并行）可加 --force。
EOT
    exit 1
  fi
fi

LOG_DIR="$(mktemp -d)"
KEEP_ON_FAIL=1

cleanup() {
  local code=$?
  echo
  echo "--- 收集诊断（→ $LOG_DIR）---"
  "${COMPOSE[@]}" ps > "$LOG_DIR/ps.txt" 2>&1 || true
  for svc in user-provider member-provider order-provider mall-provider gateway; do
    docker logs "$PROJECT-$svc-1" --tail 300 > "$LOG_DIR/$svc.log" 2>&1 || true
  done
  "${COMPOSE[@]}" logs --tail 150 mysql rocketmq-broker nacos > "$LOG_DIR/infra.log" 2>&1 || true

  if [ $code -eq 0 ]; then
    echo "--- 清理隔离环境（down -v）---"
    "${COMPOSE[@]}" down -v --remove-orphans > /dev/null 2>&1 || true
    rm -rf "$LOG_DIR"
    echo "✓ 已清理"
  elif [ "$KEEP_ON_FAIL" = 1 ]; then
    echo "!! 失败：保留环境便于排查。手工清理："
    echo "   docker compose -p $PROJECT -f docker-compose.prod.yml -f docker-compose.e2e.yml down -v"
  fi
}
trap cleanup EXIT

banner() { echo; echo "=== $* ==="; }

# ---------------------------------------------------------------- 构建与启动

banner "① 构建工作区代码（-DskipTests：单测由 CI 跑，这里只产可执行 jar）"
( cd cozy-coffee-backend && mvn -B -q -DskipTests package )

banner "② 构建 5 个运行时镜像（Dockerfile.runtime 只 COPY jar，秒级）"
for pair in \
  "cozy-gateway:gateway" \
  "cozy-provider/cozy-user-provider:user-provider" \
  "cozy-provider/cozy-member-provider:member-provider" \
  "cozy-provider/cozy-order-provider:order-provider" \
  "cozy-provider/cozy-mall-provider:mall-provider" ; do
  dir="${pair%%:*}"; svc="${pair##*:}"
  docker build -q -f cozy-coffee-backend/Dockerfile.runtime \
    -t "cozycoffee-e2e/$svc:$TAG" "cozy-coffee-backend/$dir" > /dev/null
  echo "  ✓ $svc"
done

banner "③ 起全栈并【等健康检查】（--wait，不是固定 sleep）"
"${COMPOSE[@]}" up -d --wait --wait-timeout 900
echo "  ✓ 全部 healthy"

# -------------------------------------------------- ④⑤ 事件拓扑（本阶段重点）

banner "④ 显式创建 cozy-user-events（4 读 4 写；不依赖「首条消息自动创建」）"
docker exec "$BROKER" sh -c "$MQADMIN updateTopic -n $NS -c DefaultCluster -t cozy-user-events -r 4 -w 4" > /dev/null
echo "  ✓ created"

banner "⑤ 断言 topic 路由（readQueueNums=4 / writeQueueNums=4）"
route="$(docker exec "$BROKER" sh -c "$MQADMIN topicRoute -n $NS -t cozy-user-events")"
grep -q '"readQueueNums":4' <<<"$route"  || { echo "✗ readQueueNums != 4"; echo "$route"; exit 1; }
grep -q '"writeQueueNums":4' <<<"$route" || { echo "✗ writeQueueNums != 4"; echo "$route"; exit 1; }
echo "  ✓ 4/4"

banner "⑤ 断言 5 个 consumer group 在线且订阅 tag 正确（等心跳，最多 90s）"
# 关键：**先等再断言**。刚建完 topic 时消费者可能还没注册上（这正是线上踩过的坑）
declare -A CONSUMER_TAGS=(
  [cozy-member-user-created]=user_created
  [cozy-member-profile-completed]=profile_completed
  [cozy-member-birthday-set]=birthday_set
  [cozy-mall-welcome-gift]=welcome_gift_eligible
  [cozy-mall-invite-reward]=invite_reward_earned
)
for attempt in $(seq 1 18); do
  all_ok=1
  for g in "${!CONSUMER_TAGS[@]}"; do
    out="$(docker exec "$BROKER" sh -c "$MQADMIN consumerConnection -g $g -n $NS" 2>&1 || true)"
    grep -qE "^cozy-user-events[[:space:]]+${CONSUMER_TAGS[$g]}$" <<<"$out" || all_ok=0
  done
  [ "$all_ok" = 1 ] && break
  sleep 5
done
for g in "${!CONSUMER_TAGS[@]}"; do
  out="$(docker exec "$BROKER" sh -c "$MQADMIN consumerConnection -g $g -n $NS" 2>&1 || true)"
  if grep -qE "^cozy-user-events[[:space:]]+${CONSUMER_TAGS[$g]}$" <<<"$out"; then
    echo "  ✓ $g → ${CONSUMER_TAGS[$g]}"
  else
    echo "  ✗ $g 未在线或订阅 tag 不符（期望 ${CONSUMER_TAGS[$g]}）"
    # CODE:17 "No topic route info … %RETRY%<group>" 就是 topic 不存在的典型体征
    grep -q "No topic route info" <<<"$out" && echo "    → 报 CODE:17：topic 不存在（不是消费端配置问题）"
    exit 1
  fi
done

banner "⑤ 断言三类指标已在网关侧 provider 暴露"
docker exec "$PROJECT-user-provider-1" curl -fsS localhost:8081/actuator/prometheus 2>/dev/null \
  | grep -E '^cozy_user_event_outbox_(pending|dead|oldest_pending_age_seconds) ' || {
      echo "✗ 三个指标未暴露"; exit 1; }
echo "  ✓ pending / dead / oldest_pending_age_seconds"

# -------------------------------------------- ⑥⑦⑧ 业务链路（真实 API，不用 SQL 造状态）

MYSQL="$PROJECT-mysql-1"
mysql_q() {
  docker exec -i "$MYSQL" sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 -N -B' <<< "$1"
}

# 轮询等待（不是固定 sleep）：等异步链路（outbox 投递 → mall 消费发券）收敛
wait_for() { # <sql> <期望值> [次数] [间隔秒]
  local sql="$1" want="$2" tries="${3:-30}" d="${4:-2}" i=0 v=""
  while [ "$i" -lt "$tries" ]; do
    i=$((i+1))
    v="$(mysql_q "$sql" 2>/dev/null | tr -d '[:space:]')"
    [ "$v" = "$want" ] && return 0
    sleep "$d"
  done
  echo "  ✗ 等待超时：期望 [$want]，实得 [$v]"; echo "    SQL: $sql"; return 1
}

api() { # <method> <path> [token] [body] [idempotency-key]
  local m="$1" p="$2" t="${3:-}" b="${4:-}" idem="${5:-}"
  local args=(-sS -X "$m" "$GW$p" -H 'Content-Type: application/json')
  [ -n "$t" ] && args+=(-H "Authorization: Bearer $t")
  [ -n "$b" ] && args+=(-d "$b")
  [ -n "$idem" ] && args+=(-H "Idempotency-Key: $idem")
  curl "${args[@]}"
}
jget() { grep -o "\"$1\":[^,}]*" | head -1 | sed -E "s/\"$1\":\"?([^\"]*)\"?/\1/"; }
STAMP="$(date +%s)"
PW="e2e_pass_2026"

banner "⑥ 注册邀请人 / 被邀请人，并用邀请码建立邀请关系（真实注册 API）"
INVITER_PHONE="139${STAMP: -8}"
INVITEE_PHONE="138${STAMP: -8}"
ADMIN_PHONE="137${STAMP: -8}"
api POST /api/auth/register "" "{\"username\":\"$INVITER_PHONE\",\"password\":\"$PW\",\"nickname\":\"e2e-inviter\"}" > /dev/null
INVITE_CODE="$(mysql_q "SELECT invite_code FROM cozy_user.users WHERE username='$INVITER_PHONE'")"
[ -n "$INVITE_CODE" ] || { echo "  ✗ 取不到邀请码"; exit 1; }
INVITER_ID="$(mysql_q "SELECT id FROM cozy_user.users WHERE username='$INVITER_PHONE'")"
api POST /api/auth/register "" "{\"username\":\"$INVITEE_PHONE\",\"password\":\"$PW\",\"nickname\":\"e2e-invitee\",\"inviterCode\":\"$INVITE_CODE\"}" > /dev/null
INVITEE_ID="$(mysql_q "SELECT id FROM cozy_user.users WHERE username='$INVITEE_PHONE'")"
[ "$(mysql_q "SELECT invited_by FROM cozy_user.users WHERE id=$INVITEE_ID")" = "$INVITER_ID" ] \
  || { echo "  ✗ 邀请关系未绑定"; exit 1; }
echo "  ✓ inviter=$INVITER_ID(code=$INVITE_CODE) invitee=$INVITEE_ID"

# admin 角色：本项目无种子管理员（见 cozy-coffee-admin/README.md），引导方式就是注册后改 role。
# 这不是绕过业务流程 —— 订单的推进仍然全部走真实 API，此 SQL 仅用于环境准备。
api POST /api/auth/register "" "{\"username\":\"$ADMIN_PHONE\",\"password\":\"$PW\",\"nickname\":\"e2e-admin\"}" > /dev/null
mysql_q "UPDATE cozy_user.users SET role='admin' WHERE username='$ADMIN_PHONE'" > /dev/null
ADMIN_TOKEN="$(api POST /api/auth/login "" "{\"username\":\"$ADMIN_PHONE\",\"password\":\"$PW\"}" | jget token)"
INVITEE_TOKEN="$(api POST /api/auth/login "" "{\"username\":\"$INVITEE_PHONE\",\"password\":\"$PW\"}" | jget token)"
[ -n "$INVITEE_TOKEN" ] && [ -n "$ADMIN_TOKEN" ] || { echo "  ✗ 登录失败"; exit 1; }
echo "  ✓ admin / invitee 已登录"

banner "⑦ 真实接口下单 → 接单 → 完成 → 确认取餐（触发 ORDER_COMPLETED）"
PRODUCT_ID="$(mysql_q "SELECT id FROM cozy_order.coffee_products WHERE name='Cozy 美式' AND status='active' LIMIT 1")"
[ -n "$PRODUCT_ID" ] || PRODUCT_ID="$(mysql_q "SELECT id FROM cozy_order.coffee_products WHERE status='active' ORDER BY id LIMIT 1")"
ORDER_ID="$(api POST /api/order/create "$INVITEE_TOKEN" "{\"diningMethod\":\"TAKEOUT\",\"storeId\":1,\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":1,\"cupSize\":\"MEDIUM\",\"sugarLevel\":\"NO_ADDED_SUGAR\",\"temperature\":\"HOT\",\"coffeeStrength\":\"NORMAL\",\"optionsJson\":\"{\\\"skuId\\\": null, \\\"milkType\\\": \\\"WHOLE\\\"}\",\"addonsJson\":\"[]\"}]}" "e2e-order-$STAMP" | jget id)"
[ -n "$ORDER_ID" ] || { echo "  ✗ 下单失败"; exit 1; }
api POST "/api/admin/orders/$ORDER_ID/accept"  "$ADMIN_TOKEN"   > /dev/null
api POST "/api/admin/orders/$ORDER_ID/complete" "$ADMIN_TOKEN"  > /dev/null
api POST "/api/order/$ORDER_ID/confirm"        "$INVITEE_TOKEN" > /dev/null
echo "  ✓ order=$ORDER_ID 已推进到 completed 并确认取餐"

banner "⑧ 等异步链路收敛后断言"
wait_for "SELECT invite_reward_granted FROM cozy_user.users WHERE id=$INVITEE_ID" 1 || exit 1
wait_for "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE status='SENT'" 1 || exit 1
echo "  ✓ 资格已认领 + outbox 已投递"

assert_eq() { # <说明> <sql> <期望>
  local v; v="$(mysql_q "$2" | tr -d '[:space:]')"
  if [ "$v" = "$3" ]; then echo "  ✓ $1 = $v"; else echo "  ✗ $1 期望 $3 实得 $v"; exit 1; fi
}
assert_eq "outbox 行数"      "SELECT COUNT(*) FROM cozy_user.user_event_outbox" 1
assert_eq "outbox tag"       "SELECT tag FROM cozy_user.user_event_outbox LIMIT 1" invite_reward_earned
assert_eq "outbox uniqueKey" "SELECT unique_key FROM cozy_user.user_event_outbox LIMIT 1" "invite_firstorder_${INVITEE_ID}_${INVITER_ID}"
assert_eq "邀请券张数"        "SELECT COUNT(*) FROM cozy_mall.user_coupons WHERE user_id=$INVITER_ID AND coupon_code='invite_firstorder_${INVITEE_ID}_${INVITER_ID}'" 1
assert_eq "首单积分批次数"     "SELECT COUNT(*) FROM cozy_member.points_lots WHERE user_id=$INVITEE_ID AND source_type='first_order_bonus' AND source_id=$ORDER_ID" 1

banner "⑨ 重放同一条 ORDER_COMPLETED（读订单 outbox 的原始载荷）→ 断言三者都不增"
PAYLOAD="$(mysql_q "SELECT payload FROM cozy_order.message_outbox WHERE aggregate_id=$ORDER_ID ORDER BY id DESC LIMIT 1")"
[ -n "$PAYLOAD" ] || { echo "  ✗ 取不到原始载荷"; exit 1; }
sum_consumed() { # 该 group 在 cozy-order-events 上的"已消费总量"= 各队列 Consumer Offset 之和
  docker exec "$BROKER" sh -c "$MQADMIN consumerProgress -g cozy-member-first-order -n $NS" \
    | awk '/^cozy-order-events/{s+=$5} END{print s+0}'
}
OFF_BEFORE="$(sum_consumed)"
BEFORE="$(mysql_q "SELECT CONCAT((SELECT COUNT(*) FROM cozy_member.points_lots WHERE user_id=$INVITEE_ID),'/',(SELECT COUNT(*) FROM cozy_user.user_event_outbox),'/',(SELECT COUNT(*) FROM cozy_mall.user_coupons WHERE user_id=$INVITER_ID))")"
docker exec -e PAYLOAD="$PAYLOAD" "$BROKER" sh -c \
  "$MQADMIN sendMessage -n $NS -t cozy-order-events -c order_completed -k $ORDER_ID -p \"\$PAYLOAD\"" > /dev/null
# 等重放【确实被消费】：轮询"已消费总量"增长，而不是固定 sleep
for _ in $(seq 1 20); do
  [ "$(sum_consumed)" -gt "$OFF_BEFORE" ] && break
  sleep 2
done
if [ "$(sum_consumed)" -le "$OFF_BEFORE" ]; then
  echo "  ✗ 重放消息未被消费（消费总量仍为 $OFF_BEFORE）——本次「不增」验证无意义"; exit 1
fi
AFTER="$(mysql_q "SELECT CONCAT((SELECT COUNT(*) FROM cozy_member.points_lots WHERE user_id=$INVITEE_ID),'/',(SELECT COUNT(*) FROM cozy_user.user_event_outbox),'/',(SELECT COUNT(*) FROM cozy_mall.user_coupons WHERE user_id=$INVITER_ID))")"
[ "$BEFORE" = "$AFTER" ] || { echo "  ✗ 重放后有增加：$BEFORE → $AFTER"; exit 1; }
echo "  ✓ 重放已被消费（消费总量 $OFF_BEFORE → $(sum_consumed)），且积分/outbox/券 均未增加（$BEFORE）"

banner "✅ USER_EVENTS 全链路 E2E 通过（HTTP → Order MQ → Member → Dubbo → User outbox → User MQ → Mall 券落库；重放幂等）"
