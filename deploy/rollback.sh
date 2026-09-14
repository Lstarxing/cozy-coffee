#!/usr/bin/env bash
# CozyCoffee 主动回退脚本（在服务器 /opt/cozycoffee 下执行）
#
# 用法：
#   ./rollback.sh           # 回退到「上一个成功版本」(previous-version)
#   ./rollback.sh <git-sha> # 回退到指定版本
#
# 注意区分两种场景：
#   - 发布失败后恢复（回到 last known good）→ 用 ./deploy.sh <current-version>
#     （deploy.sh 失败时会把该命令打印出来）
#   - 正常运营中主动回退上一版 → 用本脚本（读 previous-version）
#
# 回滚 = 用旧 SHA 重新发布（镜像级）。Flyway 已执行的迁移不会回退，
# 所以能否回滚取决于迁移是否**向后兼容**。
set -euo pipefail

BASE_DIR="/opt/cozycoffee"
STATE_DIR="${BASE_DIR}/deploy"
PREVIOUS_FILE="${STATE_DIR}/previous-version"

TAG="${1:-$(cat "${PREVIOUS_FILE}" 2>/dev/null || true)}"
if [ -z "${TAG}" ]; then
  echo "错误：没有可回退的版本（${PREVIOUS_FILE} 为空，且未指定 SHA）" >&2
  exit 1
fi

echo "==> 主动回退到：${TAG}"
exec "$(dirname "$0")/deploy.sh" "${TAG}"
