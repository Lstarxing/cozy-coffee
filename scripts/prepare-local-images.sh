#!/usr/bin/env bash
# OSS 到期临时方案：把本地商品图/静态图复制到网关本地存储目录（默认 cozy-coffee-backend/uploads，即网关以该目录为工作目录时 /uploads/** 生效）
# 用法：bash scripts/prepare-local-images.sh [目标目录，默认 cozy-coffee-backend/uploads]
set -euo pipefail
cd "$(dirname "$0")/.."
TARGET="${1:-cozy-coffee-backend/uploads}"
mkdir -p "$TARGET/images/seed" "$TARGET/images/mobile"

echo "→ 复制商品图 → $TARGET/images/seed"
cp -r "CozyCoffee商品图/点单商品" "$TARGET/images/seed/"
cp -r "CozyCoffee商品图/兑换商品" "$TARGET/images/seed/"

echo "→ 复制静态营销图 → $TARGET/images/mobile"
cp cozy-coffee-mobile/src/static/images/home/hero-coffee-photo.jpg "$TARGET/images/mobile/" 2>/dev/null || true
for f in about-store about-beans about-cupping about-roastery about-pour-over; do
  [ -f "cozy-coffee-mobile/prototype/assets/$f.jpg" ] && cp "cozy-coffee-mobile/prototype/assets/$f.jpg" "$TARGET/images/mobile/" || true
done

echo "✓ 完成。网关工作目录需为该目标目录（或设 LOCAL_UPLOAD_DIR 指向它），/uploads/** 即生效。"
echo "  若网关 CWD 不同，例如在 cozy-coffee-backend/cozy-gateway 启动，则改为：bash scripts/prepare-local-images.sh cozy-coffee-backend/cozy-gateway/uploads"
