#!/usr/bin/env bash
# CozyCoffee 生产发布脚本（在服务器 /opt/cozycoffee 下执行）
#
# 用法：./deploy.sh <git-sha>
#
# 语义：
# - 镜像来自阿里云 ACR（CI 构建，tag = git SHA）。本脚本只 pull + up -d --wait。
# - current-version  = 最近一次「发布成功」的版本（last known good）
# - previous-version = current 之前的那个成功版本（供 rollback.sh 主动回退）
# - 发布失败时**不改**版本记录；此时容器可能已切到失败版本，
#   恢复办法 = 用 deploy.sh 重新发布 current-version（last known good）。
# - 若发布目标恰好等于 current-version（即"恢复当前版本"），成功后不旋转 previous。
# - ⚠️ 回滚是「镜像级」的：Flyway 已执行的迁移不会回退。
#   因此迁移必须**向后兼容**（只加列/加表，不删改旧代码仍在用的列），否则旧镜像可能启动失败。
set -euo pipefail

TAG="${1:?用法: ./deploy.sh <git-sha>}"

BASE_DIR="/opt/cozycoffee"
ENV_FILE="${BASE_DIR}/env/.env.prod"
COMPOSE_FILE="${BASE_DIR}/docker-compose.prod.yml"
STATE_DIR="${BASE_DIR}/deploy"
CURRENT_FILE="${STATE_DIR}/current-version"
PREVIOUS_FILE="${STATE_DIR}/previous-version"

mkdir -p "${STATE_DIR}"
export IMAGE_TAG="${TAG}"

COMPOSE=(docker compose -p cozycoffee --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}")

cur="$(cat "${CURRENT_FILE}" 2>/dev/null || true)"

echo "==> 发布 CozyCoffee：IMAGE_TAG=${IMAGE_TAG}（当前成功版本：${cur:-无}）"
"${COMPOSE[@]}" pull

# 2C8G 上 5 个 JVM 同时冷启动约需 7~8 分钟（实测），故 wait 超时给到 600s
if ! "${COMPOSE[@]}" up -d --remove-orphans --wait --wait-timeout 600; then
  echo "" >&2
  echo "❌ 发布失败：服务未在超时内就绪（容器可能已切到 ${IMAGE_TAG}）。" >&2
  if [ -n "${cur}" ]; then
    echo "   最后成功版本 = ${cur}" >&2
    echo "   恢复命令：./deploy.sh ${cur}" >&2
  fi
  echo "   排查：docker compose -p cozycoffee -f ${COMPOSE_FILE} ps / logs <svc>" >&2
  exit 1
fi

if [ "${TAG}" = "${cur}" ]; then
  echo "==> 已恢复到当前版本 ${TAG}；版本记录不变。"
else
  if [ -n "${cur}" ]; then
    echo "${cur}" > "${PREVIOUS_FILE}"
  fi
  echo "${TAG}" > "${CURRENT_FILE}"
  echo "==> 发布成功。当前版本：${TAG}"
  if [ -n "${cur}" ]; then
    echo "    上一版本：${cur}（可用 ./rollback.sh 主动回退）"
  fi
fi
"${COMPOSE[@]}" ps
