<template>
  <div class="create-order-panel">
    <h3>☕ 模拟消费 (下单)</h3>
    <p class="desc">模拟线下或小程序下单流程，支持多商品与优惠券。</p>

    <div class="form-container">
      <!-- 用餐方式选择 v5.0 -->
      <div class="form-item">
        <label>用餐方式</label>
        <div class="dining-method-selector">
          <label class="method-option" :class="{ active: diningMethod === 'TAKEOUT' }">
            <input v-model="diningMethod" type="radio" value="TAKEOUT" />
            <span class="icon">🥤</span>
            <span>自提</span>
          </label>
          <label class="method-option" :class="{ active: diningMethod === 'DELIVERY' }">
            <input v-model="diningMethod" type="radio" value="DELIVERY" />
            <span class="icon">🛵</span>
            <span>外卖</span>
          </label>
        </div>
      </div>

      <!-- MVP: 暂时只支持输入总金额 (后续升级为商品选择) -->
      <div class="form-item">
        <label>订单总额 (¥)</label>
        <input 
          v-model.number="orderAmount" 
          type="number" 
          min="1" 
          placeholder="请输入金额" 
          @input="validateAmount"
        >
      </div>

      <!-- 优惠券选择 -->
      <div class="form-item coupon-selector" @click="showCouponPicker = true">
        <label>优惠券</label>
        <div class="selector-value" :class="{ 'has-coupon': selectedCoupon }">
          <template v-if="selectedCoupon">
            <span class="coupon-tag">{{ getCouponText(selectedCoupon) }}</span>
            <span class="discount-amount">-¥{{ discountAmount }}</span>
          </template>
          <span v-else class="placeholder">选择优惠券 ></span>
        </div>
      </div>

      <!-- 结算明细 -->
      <div class="summary-box">
        <div class="row">
          <span>商品总额</span>
          <span>¥{{ orderAmount || 0 }}</span>
        </div>
        <div v-if="discountAmount > 0" class="row discount">
          <span>优惠抵扣</span>
          <span>-¥{{ discountAmount }}</span>
        </div>
        <div class="divider"></div>
        <div class="row total">
          <span>实付金额</span>
          <span class="price">¥{{ finalPayAmount }}</span>
        </div>
        <div class="row points">
          <span>预计获得积分</span>
          <span class="bonus">+{{ estimatedPoints }}</span>
        </div>
      </div>

      <button 
        class="submit-btn" 
        :disabled="!isValidOrder || submitting" 
        @click="handleSubmit"
      >
        {{ submitting ? '提交中...' : '确认下单' }}
      </button>
    </div>

    <!-- 优惠券弹窗 -->
    <CouponPickerDialog 
      v-model:visible="showCouponPicker"
      :order-amount="orderAmount || 0"
      :items="mockItems"
      :current-coupon-id="selectedCoupon?.id"
      @select="handleCouponSelect"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { createOrder } from '@/api/order'
import CouponPickerDialog from './CouponPickerDialog.vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['order-success'])

const orderAmount = ref(30) // 默认值
const diningMethod = ref('TAKEOUT') // v5.0: 用餐方式 (TAKEOUT/DELIVERY)
const showCouponPicker = ref(false)
const selectedCoupon = ref(null)
const submitting = ref(false)

// 计算属性
const discountAmount = computed(() => {
  if (!selectedCoupon.value || !orderAmount.value) return 0
  const c = selectedCoupon.value
  
  if (c.couponType === 'FULL_REDUCE') {
    return (orderAmount.value >= c.minAmount) ? c.value : 0
  } else if (c.couponType === 'DISCOUNT') {
    // value 是 85 表示 8.5折
    const discountRate = c.value / 100
    // 优惠金额 = 原价 * (1 - 折扣率)
    return Math.floor(orderAmount.value * (1 - discountRate) * 100) / 100
  } else if (c.couponType === 'EXCHANGE') {
    // 兑换券通常抵扣特定商品，MVP如果是纯金额模拟，假设抵扣全部或固定值?
    // 简单起见，假设兑换券抵扣全部金额（若金额匹配），或者需要具体逻辑
    // MVP: 假设兑换券无金额抵扣逻辑显示，或抵扣该商品价格。
    // 这里简单处理：如果金额小于等于30，抵扣全部(模拟)
    return Math.min(orderAmount.value, 30) 
  }
  return 0
})

const finalPayAmount = computed(() => {
  return Math.max(0, (orderAmount.value || 0) - discountAmount.value)
})

const estimatedPoints = computed(() => {
  // 简单估算：1元=1积分
  return Math.floor(finalPayAmount.value)
})

const isValidOrder = computed(() => {
  return orderAmount.value > 0
})

// 模拟下单场景下的商品明细（由于该面板是纯金额模拟，我们假定它是一个饮品订单，以便可以使用优惠券测试）
const mockItems = computed(() => {
  return [
    {
      productId: 1,
      price: orderAmount.value,
      category: 'espresso', // 默认作为饮品，确保测试时可用券
      quantity: 1
    }
  ]
})

// 方法
const validateAmount = () => {
  if (orderAmount.value < 0) orderAmount.value = 0
  // 金额变动，如果优惠券不再满足门槛，应自动移除?
  // 简单起见，CouponPicker 打开时会显示 unavailable，这里暂不自动清除，
  // 提交时后端会校验。或者前端做简单校验。
  if (selectedCoupon.value && selectedCoupon.value.minAmount > orderAmount.value) {
    selectedCoupon.value = null
  }
}

const handleCouponSelect = (coupon) => {
  selectedCoupon.value = coupon
}

const getCouponText = (c) => {
  if (c.couponType === 'FULL_REDUCE') return `满${c.minAmount}减${c.value}`
  if (c.couponType === 'DISCOUNT') return `${c.value/10}折`
  return '兑换券'
}

const handleSubmit = async () => {
  if (!isValidOrder.value) return
  
  submitting.value = true
  try {
    // 构造 multi-item 结构 (MVP: 1个虚拟商品)
    // 后端需要 items[]
    const payload = {
      items: [
        {
          productId: 1, // 假设 ID 1 是通用咖啡/占位
          productName: "拿铁 (模拟)",
          quantity: 1,
          unitPrice: orderAmount.value
        }
      ],
      couponCode: selectedCoupon.value?.couponCode,
      diningMethod: diningMethod.value, // v5.0: 用餐方式
      remark: "Frontend Simulation Order"
    }

    const res = await createOrder(payload)
    if (res.code === 200 || res.success) {
      ElMessage.success('下单成功！')
      emit('order-success', res.data)
      // 重置
      selectedCoupon.value = null
      // orderAmount.value = 30
    } else {
      ElMessage.error(res.message || res.msg || '下单失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(e.message || '系统异常')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.create-order-panel {
  background: #fff;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  
  h3 { margin-top: 0; margin-bottom: 0.5rem; color: #333; }
  .desc { font-size: 0.9rem; color: #888; margin-bottom: 1.5rem; }
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  
  label { font-weight: 500; font-size: 0.95rem; color: #555; }
  
  input[type="number"] {
    padding: 0.8rem;
    border: 1px solid #ddd;
    border-radius: 8px;
    font-size: 1rem;
    transition: all 0.2s;
    
    &:focus {
      border-color: #8B4513; 
      outline: none;
    }
  }
}

/* v5.0 用餐方式选择器 */
.dining-method-selector {
  display: flex;
  gap: 10px;
  
  .method-option {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 12px 8px;
    border: 2px solid #e5e7eb;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s;
    background: #fff;
    
    input[type="radio"] { display: none; }
    
    .icon { font-size: 20px; }
    span:last-child { font-size: 13px; color: #666; }
    
    &.active {
      border-color: #8B4513;
      background: #fdf8f3;
      span:last-child { color: #8B4513; font-weight: 600; }
    }
    
    &:hover:not(.active) {
      border-color: #d4a574;
    }
  }
}

.coupon-selector {
  cursor: pointer;
  
  .selector-value {
    padding: 0.8rem;
    border: 1px solid #ddd;
    border-radius: 8px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #f9f9f9;
    
    &.has-coupon {
      border-color: #ff9800;
      background: #fff8e1;
    }
    
    .placeholder { color: #999; }
    
    .coupon-tag {
      background: #ff9800;
      color: #fff;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.8rem;
    }
    
    .discount-amount {
      color: #f56c6c;
      font-weight: bold;
    }
  }
}

.summary-box {
  background: #fafafa;
  padding: 1rem;
  border-radius: 8px;
  
  .row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 0.5rem;
    font-size: 0.9rem;
    color: #666;
    
    &.discount { color: #f56c6c; }
    
    &.total {
      font-size: 1.1rem;
      color: #333;
      font-weight: bold;
      .price { color: #8B4513; }
    }
    
    &.points {
      margin-top: 0.5rem;
      font-size: 0.85rem;
      .bonus { color: #52c41a; font-weight: bold; }
    }
  }
  
  .divider {
    height: 1px;
    background: #eee;
    margin: 0.8rem 0;
  }
}

.submit-btn {
  width: 100%;
  padding: 1rem;
  background: linear-gradient(135deg, #8B4513, #A0522D);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
  
  &:disabled {
    background: #ccc;
    cursor: not-allowed;
  }
  
  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(139, 69, 19, 0.3);
  }
}
</style>
