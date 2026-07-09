<template>
  <div v-if="visible" class="coupon-picker-mask" @click.self="handleClose">
    <div class="picker-panel">
      <div class="picker-header">
        <h3>选择优惠券</h3>
        <span class="close-btn" @click="handleClose">×</span>
      </div>

      <div v-loading="loading" class="picker-body">
        <!-- 不使用优惠券选项 -->
        <div 
          class="coupon-item no-use"
          :class="{ selected: !selectedId }"
          @click="selectCoupon(null)"
        >
          <div class="item-main">不使用优惠券</div>
          <div v-if="!selectedId" class="item-check">✓</div>
        </div>

        <div v-if="coupons.length === 0" class="empty-tip">
          暂无可用优惠券
        </div>

        <!-- 优惠券列表 -->
        <div 
          v-for="coupon in coupons"
          v-else
          :key="coupon.id"
          class="coupon-item"
          :class="{ 
            disabled: !coupon.available,
            selected: selectedId === coupon.id 
          }"
          @click="selectCoupon(coupon)"
        >
          <div class="coupon-left">
             <span v-if="coupon.couponType === 'DISCOUNT'" class="amount">{{ getDiscountDisplay(coupon) }}折</span>
             <span v-else-if="coupon.couponType === 'FULL_REDUCE'" class="amount">¥{{ coupon.value }}</span>
             <span v-else class="amount">兑</span>
             <span class="type-name">{{ getTypeName(coupon.couponType) }}</span>
          </div>
          <div class="coupon-center">
            <div class="title">{{ getCouponTitle(coupon) }}</div>
            <div class="time">有效期至 {{ formatDate(coupon.expiresAt) }}</div>
            <div v-if="!coupon.available" class="reason">{{ coupon.unavailableReason }}</div>
          </div>
          <div class="coupon-right">
             <div v-if="selectedId === coupon.id" class="check-mark">✓</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getAvailableCoupons } from '@/api/mall'

const props = defineProps({
  visible: Boolean,
  orderAmount: {
    type: Number,
    required: true
  },
  items: {
    type: Array,
    default: () => []
  },
  currentCouponId: Number
})

const emit = defineEmits(['update:visible', 'select'])

const loading = ref(false)
const coupons = ref([])
const selectedId = ref(props.currentCouponId)

watch(() => props.visible, (val) => {
  if (val) {
    loadCoupons()
    selectedId.value = props.currentCouponId
  }
})

const loadCoupons = async () => {
  loading.value = true
  try {
    const res = await getAvailableCoupons({
      orderAmount: props.orderAmount,
      items: props.items
    })
    if (res.code === 200 || res.success) {
      coupons.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const selectCoupon = (coupon) => {
  if (coupon && !coupon.available) return
  emit('select', coupon)
  handleClose()
}

const handleClose = () => {
  emit('update:visible', false)
}

// Helpers
const getTypeName = (type) => {
  const map = { 'EXCHANGE': '兑换', 'DISCOUNT': '折扣', 'FULL_REDUCE': '满减' }
  return map[type] || '优惠'
}
const getCouponTitle = (c) => c.productName || (c.couponType==='FULL_REDUCE' ? `满${c.minAmount}减${c.value}` : '全场通用')

const getDiscountDisplay = (c) => {
  // v5.3: 优先使用后端计算好的 displayTitle (e.g. "8.8折" -> "8.8")
  if (c.displayTitle && c.displayTitle.includes('折')) {
    return c.displayTitle.replace('折', '')
  }

  // 旧逻辑兜底（并修复 88 -> 880 的问题）
  if (c.value && c.value > 0) {
     return c.value >= 10 ? +(c.value / 10).toFixed(1) : c.value
  }
  
  try {
     const r = JSON.parse(c.ruleJson || '{}')
     if (r.discountRate) {
        // 如果是小数 0.x -> *10
        if (r.discountRate < 1) return +(r.discountRate * 10).toFixed(1)
        // 如果是整数 88 -> /10
        if (r.discountRate >= 10) return +(r.discountRate / 10).toFixed(1)
        return r.discountRate
     }
  } catch(e) {}
  return 0
}

const formatDate = (s) => s ? s.split('T')[0] : ''

</script>

<style scoped lang="scss">
.coupon-picker-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 2000;
  display: flex;
  align-items: flex-end; // Bottom sheet style usually better for mobile, picking center for now
  justify-content: center;
  align-items: center;
}

.picker-panel {
  width: 90%;
  max-width: 480px;
  background: #fff;
  border-radius: 16px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0,0,0,0.2);
}

.picker-header {
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  
  h3 { margin: 0; font-size: 1.1rem; }
  .close-btn { font-size: 1.5rem; cursor: pointer; color: #999; }
}

.picker-body {
  padding: 1rem;
  overflow-y: auto;
  background: #f8f8f8;
}

.coupon-item {
  background: #fff;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 0.8rem;
  display: flex;
  align-items: center;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
  
  &.selected {
    border-color: #8B4513;
    background: #fffcf5;
  }
  
  &.disabled {
    opacity: 0.6;
    cursor: not-allowed;
    background: #eee;
  }
  
  &.no-use {
    justify-content: space-between;
    font-weight: 500;
  }
}

.coupon-left {
  width: 80px;
  text-align: center;
  color: #E65100;
  display: flex;
  flex-direction: column;
  
  .amount { font-size: 1.4rem; font-weight: bold; }
  .type-name { font-size: 0.8rem; opacity: 0.8; }
}

.coupon-center {
  flex: 1;
  padding: 0 1rem;
  
  .title { font-weight: bold; font-size: 1rem; margin-bottom: 0.2rem; }
  .time { font-size: 0.8rem; color: #999; }
  .reason { color: #f56c6c; font-size: 0.8rem; margin-top: 0.2rem; }
}

.coupon-right {
  width: 30px;
  display: flex;
  justify-content: center;
  
  .check-mark {
    color: #8B4513;
    font-weight: bold;
    font-size: 1.2rem;
  }
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 2rem;
}
</style>
