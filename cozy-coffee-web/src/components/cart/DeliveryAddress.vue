<template>
  <div v-if="visible" class="delivery-address-section">
    <label class="section-label">配送地址</label>
    <div v-if="userAddresses.length === 0" class="no-address-hint">
      <span>暂无收货地址，请先添加</span>
      <button class="add-addr-btn" @click="$emit('open-address-dialog')">去添加</button>
    </div>
    <select v-else v-model="localAddressId" class="address-select">
      <option value="">请选择配送地址</option>
      <option v-for="addr in userAddresses" :key="addr.id" :value="addr.id">
        {{ addr.contactName }} {{ addr.phone }} - {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
      </option>
    </select>
    <div v-if="selectedAddress" class="selected-address-preview">
      <span v-if="selectedAddress.isDefault" class="address-tag">默认</span>
      {{ selectedAddress.contactName }} {{ selectedAddress.phone }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  userAddresses: { type: Array, default: () => [] },
  selectedAddressId: { type: [String, Number], default: '' }
})

const emit = defineEmits(['update:selectedAddressId', 'open-address-dialog'])

const localAddressId = computed({
  get: () => props.selectedAddressId,
  set: (val) => emit('update:selectedAddressId', val)
})

const selectedAddress = computed(() => {
  if (!localAddressId.value) return null
  return props.userAddresses.find(addr => addr.id === localAddressId.value)
})
</script>

<style scoped>
.delivery-address-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #F5F5F5;
  border-radius: 12px;
  border: 1px dashed #E0E0E0;
}

.section-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #5D4037;
  margin-bottom: 12px;
}

.address-select {
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

.address-select:focus {
  border-color: #C69C6D;
}

.no-address-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 1rem;
  border-radius: 8px;
  border: 1px dashed #d9d9d9;
  color: #999;
  font-size: 0.9rem;
}

.add-addr-btn {
  background: #8B4513;
  color: #fff;
  border: none;
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: background 0.2s;
}

.add-addr-btn:hover {
  background: #6F3709;
}

.selected-address-preview {
  margin-top: 8px;
  font-size: 12px;
  color: #5D4037;
  display: flex;
  align-items: center;
  gap: 6px;
}

.address-tag {
  background: #C69C6D;
  color: #FFF;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
