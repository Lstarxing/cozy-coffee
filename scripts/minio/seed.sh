#!/usr/bin/env bash
# ============================================================
# CozyCoffee MinIO 一键种子脚本（可重复执行，幂等）
# 用途：新电脑 / 服务器首次部署后，把商品图种子包灌进 MinIO。
#   docker/minio-seed/images/   ->  桶 cozycoffee/images/{v2,seed,mobile,marketing}
# 还负责：建桶、设匿名读策略(仅 GetObject)、建 Gateway 上传专用账号。
#
# 用法：
#   本地(Docker Desktop)：  bash scripts/minio/seed.sh
#   服务器 compose 网络：    MC_ENDPOINT=minio:9000 bash scripts/minio/seed.sh
# 可用环境变量覆盖（默认值=本地演示）：
#   MC_ENDPOINT    MinIO S3 地址，默认 host.docker.internal:9000（mc 经容器访问宿主机）
#   MINIO_ROOT_USER / MINIO_ROOT_PASSWORD   默认 minioadmin / minioadmin123
#   GATEWAY_SECRET Gateway 专用账号密码     默认 gateway-secret-123
#   MINIO_BUCKET   默认 cozycoffee
# ============================================================
set -euo pipefail
export MSYS_NO_PATHCONV=1   # Windows git-bash 下防止把容器内路径 /seedroot 转成宿主路径

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"        # 仓库根
SEED_ROOT="$DIR/docker/minio-seed"                                # 含 images/ 与 policy-anon.json
MC_ENDPOINT="${MC_ENDPOINT:-host.docker.internal:9000}"
ROOT_USER="${MINIO_ROOT_USER:-minioadmin}"
ROOT_PASS="${MINIO_ROOT_PASSWORD:-minioadmin123}"
GW_SECRET="${GATEWAY_SECRET:-gateway-secret-123}"
BUCKET="${MINIO_BUCKET:-cozycoffee}"
GW_USER="cozy-gateway"

# mc 统一走 docker（无需宿主机安装 mc）；通过 MC_HOST_cozy 免配置 alias
mc() { docker run --rm -e "MC_HOST_cozy=http://${ROOT_USER}:${ROOT_PASS}@${MC_ENDPOINT}" \
       -v "$SEED_ROOT:/seedroot:ro" minio/mc "$@"; }

echo "[1/5] 等待 MinIO 就绪 (${MC_ENDPOINT}) ..."
for i in $(seq 1 15); do
  if docker run --rm minio/mc alias list >/dev/null 2>&1; then :; fi
  if mc mb --ignore-existing "cozy/$BUCKET" >/dev/null 2>&1; then break; fi
  sleep 2
done

echo "[2/5] 建桶 $BUCKET"
mc mb --ignore-existing "cozy/$BUCKET" >/dev/null 2>&1 || true

echo "[3/5] 匿名读策略（仅 GetObject，禁上传/列目录）"
mc anonymous set-json /seedroot/policy-anon.json "cozy/$BUCKET" >/dev/null

echo "[4/5] Gateway 上传专用账号 $GW_USER"
(mc admin user add "cozy" "$GW_USER" "$GW_SECRET" >/dev/null 2>&1 \
  && echo "  新建账号") || echo "  账号已存在，沿用"
mc admin policy attach "cozy" readwrite --user "$GW_USER" >/dev/null

echo "[5/5] 上传种子图 images/{v2,seed,mobile,marketing}"
for top in v2 seed mobile marketing; do
  mc cp --recursive "/seedroot/images/$top/" "cozy/$BUCKET/images/$top/" >/dev/null
  echo "  $top ok"
done

echo "完成：$(mc ls --recursive "cozy/$BUCKET" | wc -l) 个对象"
