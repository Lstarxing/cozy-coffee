#!/usr/bin/env bash
# CozyCoffee 健康检查（在服务器 /opt/cozycoffee 下执行）
#
# 用法：bash deploy/check-health.sh
# 退出码：0 = 全部通过；1 = 有异常（输出会指出是哪一项）
#
# 为什么有它：事件化重构把「发券 / 积分 / 建档」从同步 RPC 改成了 outbox + MQ（最终一致）。
# 代价是失败形态变了 —— 从"调用方当场报错"变成"消息在库里静默堆积"。所以需要一个人能看的兜底检查：
#   * DEAD > 0        ⇒ 重试已耗尽 = 用户券/积分**静默没发**（必须人工重放）
#   * PENDING 滞留超时 ⇒ relay 投不出去（broker / 网络问题）
# 个人项目规模不值得上告警平台，先做成"部署后 + 随时手动跑一次"的固定检查；
# 有真实用户与持续流量后再接正式通知渠道。
#
# 判据依据：relay 退避 10/30/60/120/300s（最大 300s），所以滞留超过 600s 就不是"正常等重试"了。
set -euo pipefail

BASE_DIR="/opt/cozycoffee"
ENV_FILE="${BASE_DIR}/env/.env.prod"
MYSQL_CONTAINER="cozycoffee-mysql-1"
MAX_PENDING_AGE_SECONDS=600

CONTAINERS="cozycoffee-mysql-1 cozycoffee-nacos-1 cozycoffee-redis-1
cozycoffee-rocketmq-namesrv-1 cozycoffee-rocketmq-broker-1 cozycoffee-minio-1
cozycoffee-gateway-1 cozycoffee-user-provider-1 cozycoffee-member-provider-1
cozycoffee-order-provider-1 cozycoffee-mall-provider-1 cozycoffee-web-1 cozycoffee-admin-1"

fail=0
ok()   { printf '  ✅ %s\n' "$*"; }
bad()  { printf '  ❌ %s\n' "$*"; fail=1; }
note() { printf '     %s\n' "$*"; }

[ -r "$ENV_FILE" ] || { echo "❌ 读不到 $ENV_FILE" >&2; exit 2; }
MYSQL_PWD_VALUE="$(grep -E '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"
q() { docker exec -e MYSQL_PWD="$MYSQL_PWD_VALUE" "$MYSQL_CONTAINER" \
        mysql -uroot -N -B --default-character-set=utf8mb4 -e "$1"; }

echo "=== 1/3 容器 ==="
running="$(docker ps --format '{{.Names}}')"
for c in $CONTAINERS; do
  if ! grep -qx "$c" <<<"$running"; then bad "$c 未运行"; continue; fi
  st="$(docker ps --filter "name=^/${c}$" --format '{{.Status}}')"
  case "$st" in
    *"(healthy)"*) ok "$c  healthy" ;;
    *health*)      bad "$c 健康检查未通过：$st" ;;
    *)             note "$c 运行中（无健康检查）：$st" ;;
  esac
done

echo
echo "=== 2/3 outbox 堆积（DEAD 必须为 0）==="
check_outbox() { # <库.表> <说明>
  local target="$1" label="$2" dead pending oldest
  if ! dead="$(q "SELECT COUNT(*) FROM $target WHERE status='DEAD'" 2>/dev/null)"; then
    bad "$label：查询失败（表/列名变了？）→ $target"; return
  fi
  pending="$(q "SELECT COUNT(*) FROM $target WHERE status='PENDING'")"
  oldest="$(q "SELECT COALESCE(TIMESTAMPDIFF(SECOND, MIN(created_at), NOW()), 0) FROM $target WHERE status='PENDING'")"
  if [ "$dead" != "0" ]; then
    bad "$label：DEAD=$dead（重试耗尽的券/积分，需人工重放）"
  elif [ "$oldest" -gt "$MAX_PENDING_AGE_SECONDS" ]; then
    bad "$label：最老 PENDING 已滞留 ${oldest}s（> ${MAX_PENDING_AGE_SECONDS}s）—— relay 投不出去"
  else
    ok "$label：DEAD=0, PENDING=$pending（最老 ${oldest}s）"
  fi
}
check_outbox "cozy_user.user_event_outbox"     "用户事件 outbox（新人券/邀请/生日/完善资料/建档）"
check_outbox "cozy_member.coupon_grant_outbox" "member 发券 outbox"
check_outbox "cozy_order.message_outbox"       "订单事件 outbox"

echo
echo "=== 3/3 指标（仅展示，与上面 SQL 互为佐证）==="
for pair in "cozycoffee-user-provider-1:8081:cozy_user_event_outbox" \
            "cozycoffee-member-provider-1:8082:cozy_member_coupon_grant_outbox"; do
  c="${pair%%:*}"; rest="${pair#*:}"; port="${rest%%:*}"; metric="${rest#*:}"
  echo "  --- $c ---"
  docker exec "$c" curl -fsS "localhost:$port/actuator/prometheus" 2>/dev/null \
    | grep -E "^${metric}_(pending|dead|oldest_pending_age_seconds) " | sed 's/^/     /' \
    || note "（取不到指标：$metric）"
done

echo
if [ "$fail" = 0 ]; then
  echo "✅ 健康检查通过"
else
  echo "❌ 健康检查发现异常（见上面的 ❌）"
fi
exit "$fail"
