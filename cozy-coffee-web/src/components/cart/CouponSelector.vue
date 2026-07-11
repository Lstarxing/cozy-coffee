<template>
  <div class="cart-section coupon-section-wrapper">
    <!-- Main coupon dropdown -->
    <div class="coupon-section">
      <label class="section-label">优惠券</label>
      <select
        v-model="localCouponCode"
        class="coupon-select"
      >
        <option value="">不使用优惠券</option>
        <option
          v-for="coupon in filteredMainCoupons"
          :key="coupon.id"
          :value="coupon.meetsThreshold ? coupon.couponCode : ''"
          :disabled="!coupon.meetsThreshold"
        >
          {{ coupon.displayName }}
          {{ coupon.meetsThreshold ? `-¥${coupon.estimatedDiscount.toFixed(2)}` : `(${coupon.unavailableReason})` }}
        </option>
      </select>
    </div>

    <!-- Addon coupons -->
    <div v-if="addonCoupons.length > 0" class="addon-coupons-section">
      <label class="section-label">附加券（可与主券叠加）</label>
      <div class="addon-coupon-list">
        <label
          v-for="coupon in addonCoupons"
          :key="coupon.id"
          class="addon-coupon-item"
          :class="{
            'delivery-fee': coupon.couponType === 'DELIVERY_FEE',
            'shot-coupon': coupon.couponType === 'SHOT',
            selected: localAddonCoupons.includes(coupon.couponCode),
            disabled: isAddonDisabled(coupon)
          }"
        >
          <input
            v-model="localAddonCoupons"
            type="checkbox"
            :value="coupon.couponCode"
            :disabled="isAddonDisabled(coupon)"
          />
          <span class="coupon-icon">
            <Truck v-if="coupon.couponType === 'DELIVERY_FEE'" :size="18" />
            <Coffee v-else-if="coupon.couponType === 'SHOT'" :size="18" />
            <Gift v-else :size="18" />
          </span>
          <span class="coupon-name">{{ coupon.displayName }}</span>
          <span class="coupon-desc">{{ addonDesc(coupon) }}</span>
          <span v-if="addonTip(coupon)" class="coupon-tip">{{ addonTip(coupon) }}</span>
        </label>
      </div>
    </div>

    <!-- Remark -->
    <div class="remark-section">
      <input v-model="localRemark" type="text" placeholder="备注（选填，最多200字）" maxlength="200" class="remark-input">
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Truck, Coffee, Gift } from 'lucide-vue-next'

const props = defineProps({
  filteredMainCoupons: { type: Array, default: () => [] },
  addonCoupons: { type: Array, default: () => [] },
  selectedCouponCode: { type: String, default: '' },
  selectedAddonCoupons: { type: Array, default: () => [] },
  remark: { type: String, default: '' },
  isAddonCouponDisabled: { type: Function, required: true },
  getAddonCouponDesc: { type: Function, required: true },
  getAddonCouponTip: { type: Function, required: true }
})

const emit = defineEmits(['update:selectedCouponCode', 'update:selectedAddonCoupons', 'update:remark'])

const localCouponCode = computed({
  get: () => props.selectedCouponCode,
  set: (val) => emit('update:selectedCouponCode', val)
})

const localAddonCoupons = computed({
  get: () => props.selectedAddonCoupons,
  set: (val) => emit('update:selectedAddonCoupons', val)
})

const localRemark = computed({
  get: () => props.remark,
  set: (val) => emit('update:remark', val)
})

const isAddonDisabled = (coupon) => props.isAddonCouponDisabled(coupon)
const addonDesc = (coupon) => props.getAddonCouponDesc(coupon)
const addonTip = (coupon) => props.getAddonCouponTip(coupon)
</script>

<style scoped>
.cart-section {
  border-top: 1px solid rgba(0,0,0,0.03);
  padding-top: 20px;
}

.section-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #5D4037;
  margin-bottom: 12px;
}

.coupon-section {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  margin-bottom: 20px;
}

.coupon-select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #E0E0E0;
  border-radius: 10px;
  background: #FFF;
  color: #5D4037;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%238D6E63' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 10px center;
  background-size: 16px;
}

.coupon-select:focus {
  border-color: #C69C6D;
}

.coupon-select option:disabled {
  color: #BDBDBD;
}

.addon-coupons-section {
  margin-bottom: 16px;
}

.addon-coupon-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.addon-coupon-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #E0E0E0;
  border-radius: 10px;
  background: #FAFAFA;
  cursor: pointer;
  transition: all 0.2s;
}

.addon-coupon-item input { display: none; }

.addon-coupon-item.selected {
  border-color: #C69C6D;
  background: #FFF8E1;
}

.addon-coupon-item .coupon-name {
  font-weight: 600;
  color: #5D4037;
  font-size: 13px;
}

.addon-coupon-item .coupon-desc {
  font-size: 12px;
  color: #9E9E9E;
  margin-left: auto;
}

.addon-coupon-item.delivery-fee {
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
  border: 1px dashed #2196F3;
  position: relative;
}

.addon-coupon-item.delivery-fee .coupon-icon {
  font-size: 18px;
  margin-right: 4px;
}

.addon-coupon-item.delivery-fee.selected {
  background: linear-gradient(135deg, #BBDEFB 0%, #90CAF9 100%);
  border-color: #1976D2;
  border-style: solid;
}

.addon-coupon-item.delivery-fee.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(0.5);
}

.addon-coupon-item.delivery-fee .coupon-name {
  color: #1565C0;
}

.addon-coupon-item.delivery-fee .coupon-tip {
  font-size: 10px;
  color: #F57C00;
  margin-left: 8px;
  padding: 2px 6px;
  background: #FFF3E0;
  border-radius: 4px;
}

.addon-coupon-item.shot-coupon {
  background: linear-gradient(135deg, #EFEBE9 0%, #D7CCC8 100%);
  border: 1px dashed #5D4037;
}

.addon-coupon-item.shot-coupon .coupon-icon {
  font-size: 18px;
  color: #3E2723;
}

.addon-coupon-item.shot-coupon.selected {
  background: linear-gradient(135deg, #D7CCC8 0%, #BCAAA4 100%);
  border-color: #3E2723;
  border-style: solid;
}

.addon-coupon-item.shot-coupon.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(0.8);
}

.addon-coupon-item.shot-coupon .coupon-name {
  color: #3E2723;
  font-weight: 700;
}

.addon-coupon-item.shot-coupon .coupon-tip {
  font-size: 10px;
  color: #FFF;
  margin-left: 8px;
  padding: 2px 6px;
  background: #8D6E63;
  border-radius: 4px;
  font-weight: 600;
}

.remark-section {
  width: 100%;
}

.remark-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #E0E0E0;
  border-radius: 10px;
  background: #FFF;
  color: #5D4037;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.remark-input:focus {
  border-color: #C69C6D;
}
</style>
