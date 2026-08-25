<!--
  商品明细行（确认订单页 / 订单详情页公用）：
  左侧商品图 + 中名称/规格 + 右侧价格与数量（或兑换积分）
  数据由外部规范化为 { key, image, name, spec, quantity, priceText, pointsText? }
-->
<template>
  <view v-for="item in items" :key="item.key" class="pl-row">
    <image v-if="item.image" :src="item.image" class="pl-img" mode="aspectFill" />
    <view class="pl-main">
      <text class="pl-name">{{ item.name }}</text>
      <text class="pl-spec">{{ item.spec }}</text>
    </view>
    <view class="pl-right">
      <template v-if="item.pointsText">
        <text class="pl-points">{{ item.pointsText }}</text>
      </template>
      <template v-else>
        <text class="pl-price">¥{{ item.priceText }}</text>
        <text class="pl-qty">x{{ item.quantity }}</text>
      </template>
    </view>
  </view>
</template>

<script setup>
defineProps({
  // [{ key, image, name, spec, quantity, priceText, pointsText? }]
  items: { type: Array, default: () => [] }
})
</script>

<style lang="scss" scoped>
.pl-row { display: flex; align-items: center; gap: 24rpx; padding: 24rpx 0; }
.pl-row + .pl-row { border-top: 1rpx solid $cozy-border; }

.pl-img { flex: none; width: 150rpx; height: 150rpx; border-radius: 20rpx; background: $cozy-surface; }
.pl-main { min-width: 0; flex: 1; }
.pl-name { display: block; color: $cozy-ink; font-family: $font-display; font-size: 30rpx; font-weight: 600; line-height: 1.3; }
.pl-spec { display: block; margin-top: 6rpx; color: $cozy-muted; font-size: 21rpx; line-height: 1.5; }

.pl-right { flex: none; display: flex; align-items: baseline; gap: 12rpx; }
.pl-price { color: $cozy-ink; font-size: 28rpx; font-weight: 700; }
.pl-qty { color: $cozy-muted; font-size: 22rpx; }
.pl-points { color: $cozy-primary; font-size: 28rpx; font-weight: 650; }
</style>
