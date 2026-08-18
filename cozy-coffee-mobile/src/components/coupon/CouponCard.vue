<template>
  <view class="coupon-card" :class="[status, themeClass, { disabled }]">
    <!-- 左侧：价值锚点 -->
    <view class="card-left">
      <text class="value-symbol">{{ valueSymbol }}</text>
      <text class="value-type">{{ valueType }}</text>
    </view>

    <!-- 中间：信息与限制 -->
    <view class="card-center">
      <view class="header">
        <text class="title" :class="{ premium }">{{ premiumIcon }}{{ name }}</text>
        <text v-if="sourceTag" class="source-tag">{{ sourceTag }}</text>
      </view>
      <text class="condition" :class="{ disabled }">{{ displayCondition }}</text>
      <view class="date-row">
        <text class="date" :class="{ urgent: isUrgent }">{{ expireText }}</text>
        <text v-if="showDaysLeft" class="days-left">剩{{ daysLeft }}天</text>
      </view>
    </view>

    <!-- 右侧：行动区 -->
    <view class="card-right">
      <text v-if="isUrgent" class="urgent-tip">即将过期</text>
      <view v-if="status === 'available'" class="use-btn" :class="{ disabled }" @click.stop="!disabled && $emit('use', coupon)">{{ disabled ? '不可用' : (selectable ? '选择' : '去使用') }}</view>
      <text v-else class="status-tag">{{ statusText }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  coupon: { type: Object, required: true },
  selectable: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  reason: { type: String, default: '' }
})
defineEmits(['use'])

const TYPE_LABEL = {
  EXCHANGE: '免单',
  BOGO: '买一送一',
  DISCOUNT: '折扣',
  FULL_REDUCE: '代金',
  DELIVERY_FEE: '配送费',
  SHOT: '加浓'
}

const themeClass = computed(() => `theme-${theme.value}`)
const theme = computed(() => {
  const code = String(props.coupon.couponCode || '').toUpperCase()
  const name = String(props.coupon.name || props.coupon.productName || '').toLowerCase()
  const type = props.coupon.couponType
  if (code.includes('BLACK') || name.includes('黑金') || name.includes('尊享')) return 'black-gold'
  if (code.includes('DIAMOND') || name.includes('钻石')) return 'diamond'
  if (code.includes('GOLD') || name.includes('黄金')) return 'gold'
  if (code.includes('SILVER') || name.includes('白银')) return 'silver'
  if (name.includes('新品')) return 'promo'
  if (code.includes('DELIVERY') || type === 'DELIVERY_FEE') return 'utility'
  return 'default'
})

const premium = computed(() => theme.value === 'black-gold')
const premiumIcon = computed(() => premium.value ? '👑 ' : '')

const valueSymbol = computed(() => {
  const type = props.coupon.couponType
  if (type === 'EXCHANGE') return premium.value ? '尊享' : '免单'
  if (type === 'BOGO') return '1+1'
  if (type === 'DISCOUNT') return props.coupon.displayTitle || '折扣'
  if (type === 'FULL_REDUCE') return `¥${props.coupon.value || 0}`
  if (type === 'DELIVERY_FEE') return '免运'
  if (type === 'SHOT') return '+Shot'
  return props.coupon.displayTitle || '券'
})

const valueType = computed(() => TYPE_LABEL[props.coupon.couponType] || '优惠券')
const name = computed(() => props.coupon.name || props.coupon.productName || props.coupon.displayTitle || '优惠券')
const condition = computed(() => props.coupon.displaySubTitle || props.coupon.scope || '全场通用')
const displayCondition = computed(() => (props.disabled && props.reason) ? props.reason : condition.value)
const sourceTag = computed(() => {
  const code = props.coupon.couponCode || ''
  if (code.includes('BIRTHDAY')) return '生日礼'
  if (code.includes('MONTHLY')) return '月权益'
  if (code.includes('UPGRADE')) return '晋升礼'
  if (code.includes('NEW_USER')) return '新人礼'
  return ''
})

const daysLeft = computed(() => {
  if (!props.coupon.expiresAt) return 999
  const diff = new Date(props.coupon.expiresAt).getTime() - Date.now()
  return Math.ceil(diff / (1000 * 60 * 60 * 24))
})
const showDaysLeft = computed(() => daysLeft.value > 0 && daysLeft.value <= 7)
const isUrgent = computed(() => daysLeft.value > 0 && daysLeft.value <= 3)
const expireText = computed(() => {
  if (!props.coupon.expiresAt) return '永久有效'
  const d = new Date(props.coupon.expiresAt)
  if (Number.isNaN(d.getTime())) return props.coupon.expireText || ''
  return `有效期至 ${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
})

const status = computed(() => props.coupon.status || 'available')
const statusText = computed(() => ({ available: '可使用', frozen: '冻结中', used: '已使用', expired: '已过期' })[status.value] || status.value)
</script>

<style lang="scss" scoped>
.coupon-card {
  display: flex;
  align-items: stretch;
  border-radius: 28rpx;
  background: $bg-white;
  border: 1rpx solid $cozy-border;
  overflow: hidden;

  &.used,
  &.expired,
  &.frozen {
    background: $cozy-surface;
    border-color: $cozy-border;
    opacity: .85;
  }

  &.disabled { opacity: .55; }
}
.condition.disabled { color: $cozy-muted; }

/* ── 左侧价值区（主题色） ── */
.card-left {
  flex: none;
  width: 176rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  background: $cozy-primary;
  color: #fff;
}
.value-symbol {
  font-family: $font-display;
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1;
  color: #fff;
}
.value-type { font-size: 20rpx; opacity: .92; }

.theme-black-gold .card-left { background: #171411; }
.theme-black-gold .value-symbol,
.theme-black-gold .value-type { color: #E6C97A; }
.theme-gold .card-left { background: #C9A45C; }
.theme-silver .card-left { background: #8C857D; }
.theme-diamond .card-left { background: #5B7A8C; }
.theme-promo .card-left { background: #B3562B; }
.theme-utility .card-left { background: #6E6A64; }

/* ── 中间信息区 ── */
.card-center {
  flex: 1;
  min-width: 0;
  padding: 28rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.header {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.title {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 28rpx;
  font-weight: 650;
  color: $cozy-ink;
}
.title.premium { color: $cozy-primary; }
.source-tag {
  flex: none;
  padding: 2rpx 12rpx;
  border-radius: 999rpx;
  background: $cozy-surface;
  color: $cozy-muted;
  font-size: 18rpx;
  font-weight: 600;
}
.condition {
  font-size: 22rpx;
  color: $cozy-muted;
  line-height: 1.5;
}
.date-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.date { font-size: 20rpx; color: $cozy-placeholder; }
.date.urgent { color: $error-color; font-weight: 650; }
.days-left {
  padding: 2rpx 12rpx;
  border-radius: 999rpx;
  background: $cozy-warning-soft;
  color: #B3562B;
  font-size: 18rpx;
  font-weight: 650;
}

/* ── 右侧行动区 ── */
.card-right {
  flex: none;
  width: 128rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 0 12rpx;
}
.urgent-tip {
  color: $error-color;
  font-size: 20rpx;
  font-weight: 650;
}
.use-btn {
  width: 100%;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: $cozy-ink;
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;

  &:active { opacity: .85; }
  &.disabled { background: $cozy-border; color: $cozy-muted; }
}
.status-tag {
  font-size: 22rpx;
  font-weight: 600;
  color: $cozy-placeholder;
}
</style>
