<!--
  金额明细块（确认订单页 / 订单详情页公用）：
  商品小计 → 可折叠「配送费 / 优惠合计」（展开券明细）→ 实付（待支付/实付）
  数据全部由外部传入，组件不感知来源（preview 或 order）。
-->
<template>
  <view class="price-breakdown">
    <view class="pb-row"><text>商品小计</text><text>¥{{ money(subtotal) }}</text></view>

    <!-- 可折叠：配送费 / 优惠合计 → 展开券明细 -->
    <view v-if="hasDiscountOrFee" class="pb-collapse" @click="expanded = !expanded">
      <view v-if="Number(deliveryFee)" class="pb-row"><text>配送费</text><text>¥{{ money(deliveryFee) }}</text></view>
      <view v-if="Number(discount)" class="pb-row discount">
        <view class="pb-collapse-left">
          <text>优惠合计</text>
          <text class="pb-arrow" :class="{ open: expanded }">›</text>
        </view>
        <text>-¥{{ money(discount) }}</text>
      </view>
    </view>
    <view class="pb-collapse-body" :class="{ open: expanded }">
      <view v-for="(item, idx) in couponItems" :key="idx" class="pb-coupon-line">
        <text class="pb-coupon-name">{{ item.title || '优惠券' }}</text>
        <text :class="{ muted: !Number(item.discount) }">{{ formatAmount(item.discount) }}</text>
      </view>
    </view>

    <view class="pb-total-row">
      <text class="pb-total-label">{{ payableLabel }}</text>
      <text class="pb-total-value">¥{{ money(payable) }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { money } from '@/utils/format'

const props = defineProps({
  subtotal: { type: [Number, String], default: 0 },
  discount: { type: [Number, String], default: 0 },
  deliveryFee: { type: [Number, String], default: 0 },
  payable: { type: [Number, String], default: 0 },
  payableLabel: { type: String, default: '实付' },
  couponItems: { type: Array, default: () => [] } // [{ title, discount }]
})

const expanded = ref(false)
const hasDiscountOrFee = computed(() => Number(props.deliveryFee) > 0 || Number(props.discount) > 0)

function formatAmount(value) {
  const n = Number(value || 0)
  return n > 0 ? `-¥${n.toFixed(2)}` : '--'
}
</script>

<style lang="scss" scoped>
.price-breakdown { width: 100%; }

.pb-row { min-height: 60rpx; display: flex; align-items: center; justify-content: space-between; gap: 16rpx; color: $cozy-muted; font-size: 25rpx; }
.pb-row.discount { color: $cozy-primary; }

.pb-collapse { width: 100%; }
.pb-collapse-left { display: flex; align-items: center; gap: 8rpx; }
.pb-arrow { font-size: 30rpx; color: $cozy-placeholder; transition: transform $cozy-duration $cozy-ease-out; }
.pb-arrow.open { transform: rotate(90deg); color: $cozy-ink; }

.pb-collapse-body {
  overflow: hidden;
  max-height: 0;
  transition: max-height $cozy-duration $cozy-ease-out;

  &.open { max-height: 400rpx; }
}
.pb-coupon-line {
  min-height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  color: $cozy-muted;
  font-size: 24rpx;
}
.pb-coupon-name { min-width: 0; flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.pb-coupon-line .muted { color: $cozy-placeholder; }

.pb-total-row {
  min-height: 64rpx;
  margin-top: 8rpx;
  padding-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pb-total-label { color: $cozy-ink; font-size: 26rpx; font-weight: 650; }
.pb-total-value { color: $cozy-primary; font-size: 34rpx; font-weight: 750; }
</style>
