<template>
  <div class="cart-footer">
    <div class="price-summary">
      <div v-if="showPriceDetails" class="summary-details">
        <div class="summary-row">
          <span>商品金额</span>
          <span>¥{{ subtotal.toFixed(2) }}</span>
        </div>
        <div v-if="diningMethod === 'DELIVERY'" class="summary-row">
          <span>配送费</span>
          <span v-if="isBlackGoldMember" class="free-delivery">¥0 <small>(黑金免运费)</small></span>
          <span v-else>¥{{ deliveryFee.toFixed(2) }}</span>
        </div>
        <div v-if="memberDiscount > 0" class="summary-row discount">
          <span>黑金会员 SOE 8.5折</span>
          <span>-¥{{ memberDiscount.toFixed(2) }}</span>
        </div>
        <div v-if="discount > 0" class="summary-row discount">
          <span>优惠</span>
          <span>-¥{{ discount.toFixed(2) }}</span>
        </div>
        <div v-if="addonDiscount > 0" class="summary-row discount">
          <span>附加券优惠</span>
          <span>-¥{{ addonDiscount.toFixed(2) }}</span>
        </div>
      </div>

      <div class="summary-main">
        <div class="price-left">
          <span class="toggle-details" @click="showPriceDetails = !showPriceDetails">
            {{ showPriceDetails ? '收起明细' : '明细' }}
            <i :class="showPriceDetails ? 'arrow-down' : 'arrow-up'"></i>
          </span>
          <div class="total-price-block">
            <span class="label">实付</span>
            <strong class="value">¥{{ finalTotal.toFixed(2) }}</strong>
          </div>
        </div>
        <button class="checkout-btn" :disabled="isSubmitting" @click="$emit('checkout')">
          {{ isSubmitting ? '提交中...' : '提交订单' }}
        </button>
      </div>

      <div class="cart-points-earn">
        预计获得 <strong>{{ estimatedPoints }}</strong> 积分
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  subtotal: { type: Number, default: 0 },
  diningMethod: { type: String, default: 'TAKEOUT' },
  isBlackGoldMember: { type: Boolean, default: false },
  deliveryFee: { type: Number, default: 3 },
  memberDiscount: { type: Number, default: 0 },
  discount: { type: Number, default: 0 },
  addonDiscount: { type: Number, default: 0 },
  finalTotal: { type: Number, default: 0 },
  estimatedPoints: { type: Number, default: 0 },
  isSubmitting: { type: Boolean, default: false }
})

defineEmits(['checkout'])

const showPriceDetails = ref(false)
</script>

<style scoped>
.cart-footer {
  position: relative;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid rgba(0,0,0,0.05);
  box-shadow: 0 -4px 20px rgba(0,0,0,0.05);
  z-index: 100;
}

.price-summary {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin: 0;
  border: none;
  padding: 0;
}

.summary-details {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: #fff;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  box-shadow: 0 -4px 12px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: -1;
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUp {
  from { transform: translateY(10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.summary-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.price-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toggle-details {
  font-size: 12px;
  color: #8D6E63;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.toggle-details:hover {
  text-decoration: underline;
}

.arrow-up, .arrow-down {
  border: solid #8D6E63;
  border-width: 0 1.5px 1.5px 0;
  display: inline-block;
  padding: 2px;
  margin-bottom: 2px;
}

.arrow-up { transform: rotate(-135deg); }
.arrow-down { transform: rotate(45deg); }

.total-price-block {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.total-price-block .label {
  font-size: 13px;
  color: #3E2723;
}

.total-price-block .value {
  font-size: 24px;
  font-weight: 700;
  color: #D84315;
  line-height: 1;
}

.checkout-btn {
  width: auto;
  flex: 1;
  padding: 12px 24px;
  border-radius: 24px;
  font-size: 16px;
}

.cart-points-earn {
  margin-top: 8px;
  justify-content: flex-start;
  padding: 0;
  background: none;
  border: none;
  font-size: 11px;
}

.free-delivery {
  color: #C69C6D;
  font-weight: 600;
}

.free-delivery small {
  color: #999;
  font-weight: normal;
  font-size: 11px;
}
</style>
