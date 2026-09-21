#!/usr/bin/env bash
#
# 本地全栈 E2E：USER_EVENTS 全链路冒烟（隔离环境）
#
# 覆盖《CHANGELOG》未完成清单第 13 条的 ①–⑭：
#   ① 独立 Compose project / volume（不污染日常开发库）
#   ② 起 MySQL / Redis / Nacos / RocketMQ + 4 个 Provider + Gateway
#   ③ 等健康检查（--wait / 轮询），而不是固定 sleep
#   ④ 显式创建 cozy-user-events 与 cozy-order-events
#   ⑤ 断言两个 topic 均 4 队列 + 6 个 consumer group 在线且订阅 tag 正确
#      （含 user 侧第 7 步新增的 cozy-user-invite-reward，它订阅 cozy-order-events 上的 order_completed）
#   ⑥ 真实 API 注册邀请人 / 被邀请人 / 管理员（3 个用户，因此有 3 条新人礼事件）
#   ⑦ 真实 API 下单 → 接单 → 完成 → 确认取餐（触发 ORDER_COMPLETED）
#   ⑧ 断言一律【按 tag + unique_key 精确定位】，不用"全表恰好一行"——
#      注册本身会产生多条 welcome_gift_eligible，全表计数一加生产者就误报。
#      邀请：资格已认领、事件恰好一行且 SENT、邀请券恰好一张、首单积分恰好一次
#      新人礼：3 个用户各一行且 SENT、各一张券，且券内容与旧同步路径逐字段一致
#   ⑨ 从订单 outbox 读原始载荷重放 ORDER_COMPLETED，等 member 首单与 user 邀请认领【两个消费组】
#      都处理完，再断言【该链路】的积分/outbox/券都不增
#   ⑩ 重放 welcome_gift_eligible，断言新人礼的 outbox 与券都不增（消费侧幂等）
#   ⑪ 禁用用户必须立即撤销会话：禁用前 token 可用 → 禁用后 401 → 恢复 active 仍 401 →
#      重新登录才恢复（守 JwtAuthInterceptor 只认 Redis session 键这个前提）
#   ⑫ 生日：真实 API 设置生日 → `birthday_set` 一行且 SENT → member 领取记录 + mall 券各一次
#      （券内容按模板 BIRTHDAY_BASIC_DISCOUNT 钉住）→ 重放该事件不增（年度由生产者盖章，C8）
#   ⑬ 完善资料：补齐邮箱 → `profile_completed` 一行且 SENT → member 批次（20 分）+ 流水各一次
#      （键↔列映射按 C7）→ 重放不增
#   ⑭ 用户建档：注册即落 `user_created` 一行且 SENT → member 恰好一条会员行 → 重放不增（C3 唯一键吸收）
#   ⑮ 放行名单回归门禁（无效 token 下的期望矩阵）：公开入口 login / admin-login / register /
#      wechat-session / reset-dev 与 SSE /events 必须【非 401】；受保护入口 /api/auth/me、
#      SSE /ticket 与 /disconnect 必须【401】。注册点只有 cozy-gateway/config/WebConfig 一处。
#   失败留诊断、成功清环境
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
  # 日常 dev 栈的容器名是 `cozy-mysql` / `cozy-nacos` …（compose project = cozycoffee，但容器名是自定义前缀），
  # 不是 `cozycoffee-*` —— 只匹配后者会漏掉整个 dev 栈，所以两种前缀都要抓，并且剔除本项目自己的容器。
  dev_containers="$(docker ps --format '{{.Names}}' | grep -E '^(cozy-|cozycoffee-)' | grep -v '^cozycoffee-e2e-' || true)"
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
    # 再在 IDE 里停掉 4 个 Provider / Gateway 的运行配置
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

banner "④ 显式创建 cozy-user-events 与 cozy-order-events（不依赖「首条消息自动创建」）"
docker exec "$BROKER" sh -c "$MQADMIN updateTopic -n $NS -c DefaultCluster -t cozy-user-events -r 4 -w 4" > /dev/null
# order 事件 topic：user 侧新增的「邀请资格认领」消费者订阅它（ADR §8 第 7 步）。
# 不显式创建的话，消费者注册会发生在首条消息之后 → CONSUME_FROM_LAST_OFFSET 直接跳过它（手册 §8.2 同款坑）。
docker exec "$BROKER" sh -c "$MQADMIN updateTopic -n $NS -c DefaultCluster -t cozy-order-events -r 4 -w 4" > /dev/null
echo "  ✓ created"

banner "⑤ 断言 topic 路由（readQueueNums=4 / writeQueueNums=4）"
for t in cozy-user-events cozy-order-events; do
  route="$(docker exec "$BROKER" sh -c "$MQADMIN topicRoute -n $NS -t $t")"
  grep -q '"readQueueNums":4' <<<"$route"  || { echo "✗ $t readQueueNums != 4"; echo "$route"; exit 1; }
  grep -q '"writeQueueNums":4' <<<"$route" || { echo "✗ $t writeQueueNums != 4"; echo "$route"; exit 1; }
done
echo "  ✓ 2 个 topic 均 4/4"

banner "⑤ 断言 6 个 consumer group 在线且订阅 tag 正确（等心跳，最多 90s）"
# 关键：**先等再断言**。刚建完 topic 时消费者可能还没注册上（这正是线上踩过的坑）
# 值形如 <topic>:<tag> —— 第 7 步后 user 侧的邀请认领消费者订阅的是 cozy-order-events
declare -A CONSUMER_SUBS=(
  [cozy-member-user-created]="cozy-user-events:user_created"
  [cozy-member-profile-completed]="cozy-user-events:profile_completed"
  [cozy-member-birthday-set]="cozy-user-events:birthday_set"
  [cozy-mall-welcome-gift]="cozy-user-events:welcome_gift_eligible"
  [cozy-mall-invite-reward]="cozy-user-events:invite_reward_earned"
  [cozy-user-invite-reward]="cozy-order-events:order_completed"
)
for attempt in $(seq 1 18); do
  all_ok=1
  for g in "${!CONSUMER_SUBS[@]}"; do
    spec="${CONSUMER_SUBS[$g]}"; topic="${spec%%:*}"; tag="${spec##*:}"
    out="$(docker exec "$BROKER" sh -c "$MQADMIN consumerConnection -g $g -n $NS" 2>&1 || true)"
    grep -qE "^${topic}[[:space:]]+${tag}$" <<<"$out" || all_ok=0
  done
  [ "$all_ok" = 1 ] && break
  sleep 5
done
for g in "${!CONSUMER_SUBS[@]}"; do
  spec="${CONSUMER_SUBS[$g]}"; topic="${spec%%:*}"; tag="${spec##*:}"
  out="$(docker exec "$BROKER" sh -c "$MQADMIN consumerConnection -g $g -n $NS" 2>&1 || true)"
  if grep -qE "^${topic}[[:space:]]+${tag}$" <<<"$out"; then
    echo "  ✓ $g → $topic:$tag"
  else
    echo "  ✗ $g 未在线或订阅 tag 不符（期望 $topic:$tag）"
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
ADMIN_ID="$(mysql_q "SELECT id FROM cozy_user.users WHERE username='$ADMIN_PHONE'")"
[ -n "$ADMIN_ID" ] || { echo "  ✗ 取不到管理员 id"; exit 1; }
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

banner "⑧ 等异步链路收敛后断言（按 tag + unique_key 精确定位，不用全表计数）"

assert_eq() { # <说明> <sql> <期望>：断言【SQL 查询结果】
  local v; v="$(mysql_q "$2" | tr -d '[:space:]')"
  if [ "$v" = "$3" ]; then echo "  ✓ $1 = $v"; else echo "  ✗ $1 期望 $3 实得 $v"; exit 1; fi
}

# 比两个【字面值】（HTTP 状态码 / redis-cli 输出 …）。
# ⚠️ 别用 assert_eq 比这些：它会把第 2 个参数当 SQL 扔给 mysql，
# 例如 `assert_eq "..." 200 200` 会报 `ERROR 1064 ... near '200'`（本脚本真踩过）。
assert_val() { # <说明> <实得> <期望>
  if [ "$2" = "$3" ]; then echo "  ✓ $1 = $2"; else echo "  ✗ $1 期望 $3 实得 $2"; exit 1; fi
}

# ---- 邀请链路 ----
# 定位键用 unique_key 单独一列就够；tag / status 是真断言，不是同义反复（没拿 tag 当过滤条件）。
INVITE_KEY="invite_firstorder_${INVITEE_ID}_${INVITER_ID}"
wait_for "SELECT invite_reward_granted FROM cozy_user.users WHERE id=$INVITEE_ID" 1 || exit 1
wait_for "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$INVITE_KEY' AND status='SENT'" 1 || exit 1
echo "  ✓ 资格已认领 + 邀请事件已投递"

assert_eq "邀请事件行数"   "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$INVITE_KEY'" 1
assert_eq "邀请事件 tag"    "SELECT tag FROM cozy_user.user_event_outbox WHERE unique_key='$INVITE_KEY'" invite_reward_earned
assert_eq "邀请事件 status" "SELECT status FROM cozy_user.user_event_outbox WHERE unique_key='$INVITE_KEY'" SENT
assert_eq "邀请券张数"      "SELECT COUNT(*) FROM cozy_mall.user_coupons WHERE user_id=$INVITER_ID AND coupon_code='$INVITE_KEY'" 1
assert_eq "首单积分批次数"   "SELECT COUNT(*) FROM cozy_member.points_lots WHERE user_id=$INVITEE_ID AND source_type='first_order_bonus' AND source_id=$ORDER_ID" 1

# ---- 新人礼链路 ----
# 新人券改事件投递后，issueNewUserCoupon 的【产物】必须与旧同步路径逐字段一致 ——
# 不变量是"发的是同一张券"，只是触发方式从 RPC 换成了事件。所以这里把 rule_json 的关键字段
# 与展示文案一并钉住（照着 buildNewUserCouponConfig 的取值）。
assert_welcome_gift() { # <userId> <标签>
  local u="$1" label="$2" code="NEW_USER_COUPON_$1"
  wait_for "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$code' AND status='SENT'" 1 || exit 1
  assert_eq "$label 事件行数"  "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$code'" 1
  assert_eq "$label 事件 tag"   "SELECT tag FROM cozy_user.user_event_outbox WHERE unique_key='$code'" welcome_gift_eligible
  assert_eq "$label 券张数"     "SELECT COUNT(*) FROM cozy_mall.user_coupons WHERE user_id=$u AND coupon_code='$code'" 1
  assert_eq "$label 券类型"     "SELECT coupon_type FROM cozy_mall.user_coupons WHERE coupon_code='$code'" DISCOUNT
  assert_eq "$label 券状态"     "SELECT status FROM cozy_mall.user_coupons WHERE coupon_code='$code'" ISSUED
  assert_eq "$label 折扣值"     "SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_json,'\$.value')) FROM cozy_mall.user_coupons WHERE coupon_code='$code'" 50
  assert_eq "$label 适用范围"   "SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_json,'\$.scope')) FROM cozy_mall.user_coupons WHERE coupon_code='$code'" DRINK_ONLY
  assert_eq "$label 单杯限制"   "SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_json,'\$.limit')) FROM cozy_mall.user_coupons WHERE coupon_code='$code'" SINGLE_ITEM
  assert_eq "$label 封顶金额"   "SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_json,'\$.maxDiscountAmount')) FROM cozy_mall.user_coupons WHERE coupon_code='$code'" 20
  assert_eq "$label 业务标签"   "SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_json,'\$.tag')) FROM cozy_mall.user_coupons WHERE coupon_code='$code'" NEW_USER_GIFT
  assert_eq "$label 互斥级别"   "SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_json,'\$.mutex')) FROM cozy_mall.user_coupons WHERE coupon_code='$code'" L1_EXCLUSIVE
  assert_eq "$label 有效期天数" "SELECT TIMESTAMPDIFF(DAY, issued_at, expires_at) FROM cozy_mall.user_coupons WHERE coupon_code='$code'" 7
  assert_eq "$label 显示标题"   "SELECT display_title FROM cozy_mall.user_coupons WHERE coupon_code='$code'" 新用户首单5折
}
assert_welcome_gift "$INVITER_ID" "邀请人"
assert_welcome_gift "$INVITEE_ID" "被邀请人"
assert_welcome_gift "$ADMIN_ID"   "管理员"

# 重放验证的两个必要条件（缺一不可）：
#   (a) 消息【确实被消费】—— 轮询该事件【所有】订阅 group 在 topic 上的"已消费总量"
#       （各队列 Consumer Offset 之和）增长，而不是固定 sleep；
#   (b) 再消费一次【不产生新数据】。
# 只比计数的话，消息压根没被消费也会"通过"，结论就是空的。
sum_consumed() { # <group> <topic>
  docker exec "$BROKER" sh -c "$MQADMIN consumerProgress -g $1 -n $NS" \
    | awk -v t="$2" '$1 == t {s += $5} END {print s + 0}'
}
# 位点必须能解析成整数，且读取命令本身要成功。
# 空值会让 `[ "" -gt "" ]` 报 integer expression expected —— 而它在 if / && 里只表现为"条件不成立"，
# 于是「位点没读到」会被静默当成「没有增长」，再被当成「消费侧幂等通过」。这种"验证本身失效"的
# 假通过比断言失败更难发现，所以宁可当场停下。
require_offset() { # <组名> <值>
  case "$2" in
    '' | *[!0-9]*)
      echo "  ✗ [$1] 消费位点读不到有效整数（实得「$2」）——consumerProgress 失败或输出格式变了" >&2
      exit 1 ;;
  esac
}
replay_and_assert() { # <说明> <topic> <tag> <key> <载荷> <group[,group...]> <断言SQL>
  local label="$1" topic="$2" tag="$3" key="$4" payload="$5" groups="$6" sql="$7"
  local g before after pending=1 waited=0
  declare -A off_before off_after
  # 一条事件可能被【多个】消费组订阅 —— 例如 ORDER_COMPLETED 同时被 member 首单与 user 邀请认领消费。
  # 只等其中一个，就没法断言"另一个也处理过了"；那样「新消费者重放幂等」就是没验证过的空话。
  for g in ${groups//,/ }; do
    # 赋值放进 if 条件里，这样 set -e 不会在读取失败时直接静默中止，而是走到下面这句给得出原因
    if ! off_before[$g]="$(sum_consumed "$g" "$topic")"; then
      echo "  ✗ [$label] consumerProgress 执行失败（group=$g topic=$topic）——docker 没跑 / group 不存在？" >&2
      exit 1
    fi
    require_offset "$g" "${off_before[$g]}"
  done
  before="$(mysql_q "$sql" | tr -d '[:space:]')"
  docker exec -e PAYLOAD="$payload" "$BROKER" sh -c \
    "$MQADMIN sendMessage -n $NS -t $topic -c $tag -k $key -p \"\$PAYLOAD\"" > /dev/null
  while [ "$pending" = 1 ] && [ "$waited" -lt 20 ]; do
    pending=0
    for g in ${groups//,/ }; do
      if ! off_after[$g]="$(sum_consumed "$g" "$topic")"; then
        echo "  ✗ [$label] consumerProgress 执行失败（group=$g topic=$topic）" >&2
        exit 1
      fi
      require_offset "$g" "${off_after[$g]}"
      [ "${off_after[$g]}" -gt "${off_before[$g]}" ] || pending=1
    done
    [ "$pending" = 1 ] && { sleep 2; waited=$((waited + 1)); }
  done
  if [ "$pending" = 1 ]; then
    for g in ${groups//,/ }; do
      [ "${off_after[$g]}" -gt "${off_before[$g]}" ] || {
        echo "  ✗ [$label] 重放消息未被 [$g] 消费（位点仍为 ${off_before[$g]}）——本次「不增」验证无意义"; exit 1; }
    done
  fi
  after="$(mysql_q "$sql" | tr -d '[:space:]')"
  [ "$before" = "$after" ] || { echo "  ✗ [$label] 重放后有增加：$before → $after"; exit 1; }
  local detail=""
  for g in ${groups//,/ }; do detail="$detail $g(${off_before[$g]}→${off_after[$g]})"; done
  echo "  ✓ [$label] 重放已被消费（$detail），且计数未变（$before）"
}

banner "⑨ 重放同一 ORDER_COMPLETED（读订单 outbox 原始载荷）→ 该链路的积分/outbox/券都不增"
PAYLOAD="$(mysql_q "SELECT payload FROM cozy_order.message_outbox WHERE aggregate_id=$ORDER_ID ORDER BY id DESC LIMIT 1")"
[ -n "$PAYLOAD" ] || { echo "  ✗ 取不到原始载荷"; exit 1; }
# 断言范围必须限定在【这条链路】：全局计数会被新人礼事件干扰（一加生产者就会变）
# 两个消费组都要等：cozy-member-first-order（首单积分）与 cozy-user-invite-reward（邀请资格认领，
# ADR §8 第 7 步新增）。只等 member 的话，「新消费者重放幂等」这句就是没验证过的。
replay_and_assert "ORDER_COMPLETED" cozy-order-events order_completed "$ORDER_ID" "$PAYLOAD" \
  cozy-member-first-order,cozy-user-invite-reward \
  "SELECT CONCAT((SELECT COUNT(*) FROM cozy_member.points_lots WHERE user_id=$INVITEE_ID AND source_type='first_order_bonus'),'/',(SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$INVITE_KEY'),'/',(SELECT COUNT(*) FROM cozy_mall.user_coupons WHERE user_id=$INVITER_ID AND coupon_code='$INVITE_KEY'))"

banner "⑩ 重放 welcome_gift_eligible（读 user outbox 原始载荷）→ 新人礼 outbox 与券都不增"
# 这条守的是消费侧幂等（ADR C3）：issueNewUserCoupon 靠 uk_coupon_code 吸收重复投递。
# 生产者改成"事务内写 outbox + 至少一次投递"之后，重复投递不再由 Dubbo 调用方挡住。
GIFT_KEY="NEW_USER_COUPON_${INVITEE_ID}"
GIFT_PAYLOAD="$(mysql_q "SELECT payload FROM cozy_user.user_event_outbox WHERE unique_key='$GIFT_KEY'")"
[ -n "$GIFT_PAYLOAD" ] || { echo "  ✗ 取不到新人礼原始载荷"; exit 1; }
replay_and_assert "WELCOME_GIFT_ELIGIBLE" cozy-user-events welcome_gift_eligible "$GIFT_KEY" "$GIFT_PAYLOAD" cozy-mall-welcome-gift \
  "SELECT CONCAT((SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$GIFT_KEY'),'/',(SELECT COUNT(*) FROM cozy_mall.user_coupons WHERE user_id=$INVITEE_ID AND coupon_code='$GIFT_KEY'))"

banner "⑪ 禁用用户必须立即撤销会话（P1：鉴权只认 Redis 里的 session 键）"
# 探针用独立账号，避免禁用动作影响上面的订单链路
PROBE_PHONE="136${STAMP: -8}"
api POST /api/auth/register "" "{\"username\":\"$PROBE_PHONE\",\"password\":\"$PW\",\"nickname\":\"e2e-probe\"}" > /dev/null
PROBE_ID="$(mysql_q "SELECT id FROM cozy_user.users WHERE username='$PROBE_PHONE'")"
[ -n "$PROBE_ID" ] || { echo "  ✗ 取不到探针账号 id"; exit 1; }
assert_welcome_gift "$PROBE_ID" "探针"
PROBE_TOKEN="$(api POST /api/auth/login "" "{\"username\":\"$PROBE_PHONE\",\"password\":\"$PW\"}" | jget token)"
[ -n "$PROBE_TOKEN" ] || { echo "  ✗ 探针账号登录失败"; exit 1; }

REDIS="$PROJECT-redis-1"
redis_get() { docker exec "$REDIS" redis-cli get "$1" | tr -d '\r'; }
redis_exists() { docker exec "$REDIS" redis-cli exists "$1" | tr -d '\r'; }
# 该用户当前名下的 session 键个数（值为 userId）；无匹配时 grep -c 退出码为 1，故补 || true
probe_sessions() {
  local n=0 k
  for k in $(docker exec "$REDIS" redis-cli --scan --pattern 'cozy:auth:session:*' | tr -d '\r'); do
    [ "$(redis_get "$k")" = "$PROBE_ID" ] && n=$((n + 1))
  done
  echo "$n"
}
# 受保护接口：/api/member/addresses 不在 WebConfig 的放行名单里（/api/auth/me 才是放行的）
token_status() { curl -sS -o /dev/null -w '%{http_code}' -X GET "$GW/api/member/addresses" -H "Authorization: Bearer $1"; }

assert_val "禁用前 原 token 可用"      "$(token_status "$PROBE_TOKEN")" 200
assert_val "禁用前 session 键存在"     "$(probe_sessions)" 1

api PUT "/api/admin/users/$PROBE_ID/status?status=disabled" "$ADMIN_TOKEN" > /dev/null
assert_val "禁用后 原 token 已 401"    "$(token_status "$PROBE_TOKEN")" 401
assert_val "禁用后 session 键已清空"    "$(probe_sessions)" 0
assert_val "禁用后 token 指针已删"      "$(redis_exists "cozy:auth:user:token:$PROBE_ID")" 0
assert_eq  "禁用后 tokenVersion 已递增" "SELECT token_version FROM cozy_user.users WHERE id=$PROBE_ID" 1

# 恢复 active 不恢复旧会话：必须重新登录
api PUT "/api/admin/users/$PROBE_ID/status?status=active" "$ADMIN_TOKEN" > /dev/null
assert_val "恢复 active 后 旧 token 仍 401" "$(token_status "$PROBE_TOKEN")" 401
NEW_TOKEN="$(api POST /api/auth/login "" "{\"username\":\"$PROBE_PHONE\",\"password\":\"$PW\"}" | jget token)"
[ -n "$NEW_TOKEN" ] || { echo "  ✗ 恢复后重新登录失败"; exit 1; }
assert_val "重新登录后 新 token 可用"   "$(token_status "$NEW_TOKEN")" 200
echo "  ✓ 禁用即踢下线：401 → 恢复后仍 401 → 重新登录才恢复"

banner "⑫ 生日事件：设置生日 → member 权益 + mall 券各落一次；重放不增"
# 年度由【生产者】盖章（ADR C8），所以这里用同一个时区的年份去对（全栈 TZ=Asia/Shanghai）
BIRTH_YEAR="$(date +%Y)"
BIRTH_KEY="birthday_${PROBE_ID}_${BIRTH_YEAR}"
BIRTH_COUPON="${BIRTH_KEY}_gift1"
assert_eq "设置前 无该生日事件" "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$BIRTH_KEY'" 0

api PUT /api/auth/profile "$NEW_TOKEN" "{\"birthday\":\"1990-05-20\"}" > /dev/null

wait_for "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$BIRTH_KEY' AND status='SENT'" 1 || exit 1
assert_eq "生日事件行数"  "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$BIRTH_KEY'" 1
assert_eq "生日事件 tag"   "SELECT tag FROM cozy_user.user_event_outbox WHERE unique_key='$BIRTH_KEY'" birthday_set

# member 侧：basic 等级生日积分是 0，但**必有一条领取记录** —— 幂等判据正是靠它（source_id = abs(hash(key))）
wait_for "SELECT COUNT(*) FROM cozy_member.points_transactions WHERE user_id=$PROBE_ID AND source_type='birthday_gift'" 1 || exit 1
assert_eq "生日领取记录"   "SELECT COUNT(*) FROM cozy_member.points_transactions WHERE user_id=$PROBE_ID AND source_type='birthday_gift'" 1

# mall 侧：券码 = 事件键 + _gift1；模板 BIRTHDAY_BASIC_DISCOUNT 的字段一并钉住。
# ⚠️ 字段名与新人券不同：模板驱动券落的是 discountRate / skuLimit（新人券是手工构造的 value），
#    照抄新人券那套键名会取到 NULL —— 本脚本真踩过。
#    数值用 `JSON_EXTRACT(...) + 0` 比，避免 20 与 20.0 这类表示差异造成假失败。
wait_for "SELECT COUNT(*) FROM cozy_mall.user_coupons WHERE coupon_code='$BIRTH_COUPON'" 1 || exit 1
assert_eq "生日券类型"     "SELECT coupon_type FROM cozy_mall.user_coupons WHERE coupon_code='$BIRTH_COUPON'" DISCOUNT
assert_eq "生日券状态"     "SELECT status FROM cozy_mall.user_coupons WHERE coupon_code='$BIRTH_COUPON'" ISSUED
assert_eq "生日券折扣率"   "SELECT JSON_EXTRACT(rule_json,'\$.discountRate') + 0 FROM cozy_mall.user_coupons WHERE coupon_code='$BIRTH_COUPON'" 0.5
assert_eq "生日券封顶"     "SELECT JSON_EXTRACT(rule_json,'\$.maxDiscountAmount') + 0 FROM cozy_mall.user_coupons WHERE coupon_code='$BIRTH_COUPON'" 20
assert_eq "生日券适用范围" "SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_json,'\$.scope')) FROM cozy_mall.user_coupons WHERE coupon_code='$BIRTH_COUPON'" DRINK_ONLY
assert_eq "生日券杯型限制" "SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_json,'\$.skuLimit')) FROM cozy_mall.user_coupons WHERE coupon_code='$BIRTH_COUPON'" STANDARD_ONLY
assert_eq "生日券单杯限制" "SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_json,'\$.limit')) FROM cozy_mall.user_coupons WHERE coupon_code='$BIRTH_COUPON'" SINGLE_ITEM

# 重放同一条 birthday_set：消费者用「键 → source_id」判重，不得再发券/再记一笔
BIRTH_PAYLOAD="$(mysql_q "SELECT payload FROM cozy_user.user_event_outbox WHERE unique_key='$BIRTH_KEY'")"
[ -n "$BIRTH_PAYLOAD" ] || { echo "  ✗ 取不到生日事件原始载荷"; exit 1; }
replay_and_assert "BIRTHDAY_SET" cozy-user-events birthday_set "$BIRTH_KEY" "$BIRTH_PAYLOAD" cozy-member-birthday-set \
  "SELECT CONCAT((SELECT COUNT(*) FROM cozy_member.points_transactions WHERE user_id=$PROBE_ID AND source_type='birthday_gift'),'/',(SELECT COUNT(*) FROM cozy_mall.user_coupons WHERE coupon_code='$BIRTH_COUPON'))"

banner "⑬ 完善资料事件：补齐邮箱 → member 加一次分；重放不增"
PROFILE_KEY="profile_completed_${PROBE_ID}"
assert_eq "设置前 无完善资料事件" "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$PROFILE_KEY'" 0

api PUT /api/auth/profile "$NEW_TOKEN" "{\"email\":\"e2e-probe@cozycoffee.test\"}" > /dev/null

wait_for "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$PROFILE_KEY' AND status='SENT'" 1 || exit 1
assert_eq "完善资料事件 tag" "SELECT tag FROM cozy_user.user_event_outbox WHERE unique_key='$PROFILE_KEY'" profile_completed

# C7 的键映射：事件键 profile_completed_{uid} ↔ 落库 source_type=profile + source_id={uid}
# 奖励值（20 分）由**消费端**配置持有 —— 这里钉住它，因为传输切换不该改变奖励值
wait_for "SELECT COUNT(*) FROM cozy_member.points_lots WHERE user_id=$PROBE_ID AND source_type='profile' AND source_id=$PROBE_ID" 1 || exit 1
assert_eq "完善资料批次数"  "SELECT COUNT(*) FROM cozy_member.points_lots WHERE user_id=$PROBE_ID AND source_type='profile' AND source_id=$PROBE_ID" 1
assert_eq "完善资料批次数额" "SELECT initial_amount FROM cozy_member.points_lots WHERE user_id=$PROBE_ID AND source_type='profile'" 20
assert_eq "完善资料积分流水" "SELECT COUNT(*) FROM cozy_member.points_transactions WHERE user_id=$PROBE_ID AND source_type='profile' AND source_id=$PROBE_ID" 1

# 重放：消费侧靠 uk_reward(user_id,source_type,source_id) + 行锁串行化，不得再加一次分
PROFILE_PAYLOAD="$(mysql_q "SELECT payload FROM cozy_user.user_event_outbox WHERE unique_key='$PROFILE_KEY'")"
[ -n "$PROFILE_PAYLOAD" ] || { echo "  ✗ 取不到完善资料事件原始载荷"; exit 1; }
replay_and_assert "PROFILE_COMPLETED" cozy-user-events profile_completed "$PROFILE_KEY" "$PROFILE_PAYLOAD" cozy-member-profile-completed \
  "SELECT CONCAT((SELECT COUNT(*) FROM cozy_member.points_lots WHERE user_id=$PROBE_ID AND source_type='profile'),'/',(SELECT COUNT(*) FROM cozy_member.points_transactions WHERE user_id=$PROBE_ID AND source_type='profile'))"

banner "⑭ 用户建档事件：注册即落 user_created；重放不增"
UC_KEY="user_created_${PROBE_ID}"
wait_for "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$UC_KEY' AND status='SENT'" 1 || exit 1
assert_eq "建档事件行数" "SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$UC_KEY'" 1
assert_eq "建档事件 tag"  "SELECT tag FROM cozy_user.user_event_outbox WHERE unique_key='$UC_KEY'" user_created
# member 侧：恰好一条会员行（createMember 落库；它原先是注册事务后的同步 Dubbo 调用）
assert_eq "会员行数"     "SELECT COUNT(*) FROM cozy_member.member_info WHERE user_id=$PROBE_ID" 1

# 重放同一 user_created：createMember 的 check-then-insert 靠唯一键吸收（ADR C3）
UC_PAYLOAD="$(mysql_q "SELECT payload FROM cozy_user.user_event_outbox WHERE unique_key='$UC_KEY'")"
[ -n "$UC_PAYLOAD" ] || { echo "  ✗ 取不到建档事件原始载荷"; exit 1; }
replay_and_assert "USER_CREATED" cozy-user-events user_created "$UC_KEY" "$UC_PAYLOAD" cozy-member-user-created \
  "SELECT CONCAT((SELECT COUNT(*) FROM cozy_member.member_info WHERE user_id=$PROBE_ID),'/',(SELECT COUNT(*) FROM cozy_user.user_event_outbox WHERE unique_key='$UC_KEY'))"

banner "⑮ 放行名单回归门禁（无效 token 下的期望矩阵）"
# 背景：JwtAuthInterceptor 对「带了 token 但无效」是【直接 401】，不看该路径是否本就公开。
# 于是公开入口一旦漏在放行名单外，客户端手里一个失效 token 就把自己锁死 ——
# 2026-09-21 真实踩到：/api/auth/admin/login 漏了，表现是「管理端密码正确也永远登不进」。
# 这类回归正常 CI / 手测都碰不到（要恰好持有一个失效 token），只有这条门禁能拦住。
# 注册点只有一处：cozy-gateway/config/WebConfig#addInterceptors（2026-09-21 合并）。
# 此前 cozy-common 也注册一遍，两份名单【取交集】生效 —— 那正是漏掉的根源。
BAD_TOKEN="e2e.garbage.token"

_public_code() { # <方法> <路径>：打印 HTTP 状态码
  local m="$1" p="$2"
  local args=(-sS -o /dev/null -w '%{http_code}' -X "$m" "$GW$p" -H "Authorization: Bearer $BAD_TOKEN")
  [ "$m" = POST ] && args+=(-H 'Content-Type: application/json' -d '{}')
  curl "${args[@]}" 2>/dev/null
}
# 期望【非 401】：真正公开的入口必须穿过鉴权层（业务层随后可回 400/403）
assert_public_reachable() { # <方法> <路径>
  local code; code="$(_public_code "$1" "$2")"
  [ "$code" = 401 ] && { echo "  ✗ $1 $2 被失效 token 拦成 401 —— 公开入口漏在放行名单外"; exit 1; }
  echo "  ✓ $1 $2 → $code（非 401，已越过鉴权层）"
}
# 期望【必须 401】：受保护入口，失效 token 就该被拦
assert_rejected() { # <方法> <路径>
  local code; code="$(_public_code "$1" "$2")"
  [ "$code" = 401 ] || { echo "  ✗ $1 $2 期望 401，实得 $code —— 受保护入口被误放行"; exit 1; }
  echo "  ✓ $1 $2 → 401（受保护，符合预期）"
}

echo "  -- 公开入口：非 401 --"
assert_public_reachable POST /api/auth/login
assert_public_reachable POST /api/auth/admin/login
assert_public_reachable POST /api/auth/register
assert_public_reachable POST /api/auth/wechat/session
assert_public_reachable POST /api/auth/password/reset-dev
echo "  -- SSE /events：建连凭一次性 ticket，不走 JWT（非 401 即证明已放行）--"
assert_public_reachable GET  /api/member/sse/events
assert_public_reachable GET  /api/admin/sse/events
echo "  -- 受保护入口：必须 401 --"
assert_rejected GET  /api/auth/me
assert_rejected POST /api/member/sse/ticket
assert_rejected POST /api/member/sse/disconnect
assert_rejected POST /api/admin/sse/ticket

banner "✅ USER_EVENTS 全链路 E2E 通过（HTTP → Order MQ → Member → Dubbo → User outbox → User MQ → Mall 券落库；五条链路重放均幂等；禁用即时撤销会话）"
