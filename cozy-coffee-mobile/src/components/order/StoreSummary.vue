<template>
  <view class="store-summary">
    <!-- 外送：外送 tag + 地址 + 联系人 + 预计送达 -->
    <template v-if="isDelivery">
      <view class="store-main" @click="$emit('tap')">
        <view class="store-copy">
          <view class="store-title-row">
            <text class="pickup-tag">外送</text>
            <text v-if="deliveryAddress && deliveryAddress.label" class="addr-label">{{ labelText(deliveryAddress.label) }}</text>
            <text class="store-address" :class="{ placeholder: !deliveryAddress }">{{ addressText }}</text>
          </view>
          <view v-if="deliveryAddress" class="contact-row">
            <text class="contact-name">{{ contactName }}</text>
            <text class="contact-phone">{{ deliveryAddress.phone }}</text>
          </view>
        </view>
        <text class="store-arrow">›</text>
      </view>
      <text class="delivery-eta">{{ deliveryEta }}</text>
    </template>

    <!-- 自提：门店名 + 地址 + 取餐时间 -->
    <template v-else>
      <view class="store-main" @click="$emit('tap')">
        <view class="store-copy">
          <view class="store-title-row">
            <text class="store-name">{{ name }}</text>
            <text class="pickup-tag">自提</text>
          </view>
          <text class="store-address">{{ address }}</text>
        </view>
        <text class="store-arrow">›</text>
      </view>
      <view class="pickup-row">
        <text class="pickup-label">取餐时间</text>
        <text class="pickup-value">{{ pickupLabel }}</text>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { FIXED_STORE } from '@/config/store'

const props = defineProps({
  name: { type: String, default: FIXED_STORE.name },
  address: { type: String, default: FIXED_STORE.address },
  pickupLabel: { type: String, default: FIXED_STORE.pickupLabel },
  mode: { type: String, default: 'pickup' }, // pickup / delivery
  deliveryAddress: { type: Object, default: null },
  deliveryEta: { type: String, default: '现在下单，预计 30-40 分钟送达' }
})
defineEmits(['tap'])

const isDelivery = computed(() => props.mode === 'delivery')

const addressText = computed(() => {
  if (!props.deliveryAddress) return '请添加收货地址'
  return [props.deliveryAddress.region, props.deliveryAddress.detail].filter(Boolean).join(' ')
})

const contactName = computed(() => {
  const a = props.deliveryAddress || {}
  const honorific = String(a.gender || '').toUpperCase() === 'FEMALE' ? '女士' : '先生'
  return `${a.name || ''}（${honorific}）`
})

const labelText = (label) => ({ HOME: '家', COMPANY: '公司', SCHOOL: '学校' }[label] || label || '')
</script>

<style lang="scss" scoped>
.store-summary {
  padding: 32rpx;
  border-radius: 28rpx;
  background: #fff;
}
.store-main { display: flex; align-items: flex-start; gap: 20rpx; }
.store-copy { min-width: 0; flex: 1; }
.store-title-row { display: flex; align-items: center; gap: 16rpx; }
.store-name {
  overflow: hidden;
  font-family: $font-display;
  font-size: 34rpx;
  font-weight: 600;
  color: $cozy-ink;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.pickup-tag {
  flex: none;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  border: 1rpx solid $cozy-ink;
  color: $cozy-ink;
  font-size: 20rpx;
  font-weight: 650;
}
.store-address {
  min-width: 0;
  flex: 1;
  font-size: 26rpx;
  line-height: 1.5;
  color: $cozy-ink;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.store-address.placeholder { color: $cozy-placeholder; }
.addr-label {
  flex: none;
  padding: 0 10rpx;
  border: 1rpx solid $cozy-primary;
  border-radius: 4rpx;
  color: $cozy-primary;
  font-size: 18rpx;
  line-height: 1.6;
}
.store-arrow {
  flex: none;
  color: $cozy-muted;
  font-size: 44rpx;
  font-weight: 300;
  line-height: 1;
  margin-top: 4rpx;
}
.contact-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 16rpx;
}
.contact-name { color: $cozy-ink; font-size: 26rpx; font-weight: 650; }
.contact-phone { color: $cozy-muted; font-size: 24rpx; }
.delivery-eta {
  display: block;
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid $cozy-border;
  color: $cozy-ink;
  font-size: 24rpx;
}
.pickup-row {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid $cozy-border;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.pickup-label { color: $cozy-ink; font-size: 24rpx; }
.pickup-value { color: $cozy-ink; font-size: 24rpx; font-weight: 650; }
</style>
