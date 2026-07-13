<template>
  <div class="cart-items">
    <div v-for="(item, index) in cartItems" :key="index" class="cart-item">
      <img :src="getImageUrl(item.productImage)" :alt="item.productName" @error="handleImageError">
      <div class="item-details">
        <h4>{{ item.productName }}</h4>
        <div v-if="item.category !== 'bakery' && (item.cupSize || item.sugarLevel || item.temperature || item.coffeeStrength || (item.milkType && item.milkType !== 'WHOLE'))" class="item-specs">
          <span v-if="item.cupSize">{{ getCupSizeLabel(item.cupSize) }}</span>
          <span v-if="item.sugarLevel">{{ getSugarLabel(item.sugarLevel) }}</span>
          <span v-if="item.temperature">{{ getTempLabel(item.temperature) }}</span>
          <span v-if="item.coffeeStrength">{{ getStrengthLabel(item.coffeeStrength) }}</span>
          <span v-if="item.milkType && item.milkType !== 'WHOLE'">{{ getMilkLabel(item.milkType) }}</span>
        </div>
        <div class="item-price">
          <span v-if="item.discountAmount > 0" class="original-price">¥{{ item.unitPrice }}</span>
          <span :class="{ 'discounted-price': item.discountAmount > 0, 'normal-price': item.discountAmount === 0 }">
            ¥{{ (item.discountedPrice ?? item.unitPrice).toFixed(0) }}
          </span>
        </div>
      </div>
      <div class="item-actions">
        <div class="quantity-ctrl">
          <button @click="$emit('update-quantity', index, item.quantity - 1)">
            <Minus :size="14" :stroke-width="2.5" />
          </button>
          <span>{{ item.quantity }}</span>
          <button @click="$emit('update-quantity', index, item.quantity + 1)">
            <Plus :size="14" :stroke-width="2.5" />
          </button>
        </div>
        <button class="remove-btn" @click="$emit('remove-item', index)">
          <Trash2 :size="14" :stroke-width="2" /> 删除
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Minus, Plus, Trash2 } from 'lucide-vue-next'

defineProps({
  cartItems: { type: Array, required: true },
  getImageUrl: { type: Function, required: true },
  handleImageError: { type: Function, required: true }
})

defineEmits(['update-quantity', 'remove-item'])

const getCupSizeLabel = (size) => {
  const map = { 'STANDARD': '标准杯', 'LARGE': '大杯' }
  return map[size] || size
}

const getSugarLabel = (level) => {
  const map = {
    'NONE': '不加糖', 'none': '不加糖',
    'LESS': '少糖', 'less': '少糖',
    'HALF': '半糖', 'half': '半糖',
    'LIGHT': '微甜', 'light': '微甜',
    'MEDIUM': '少甜', 'medium': '少甜',
    'STANDARD': '标准甜', 'standard': '标准甜'
  }
  return map[level] || level
}

const getTempLabel = (temp) => {
  const map = { 'COLD': '冰', 'HOT': '热', 'WARM': '温' }
  return map[temp] || temp
}

const getStrengthLabel = (strength) => {
  const map = { 'NORMAL': '标准浓度', 'STRONG': '加浓' }
  return map[strength] || strength
}

const getMilkLabel = (milk) => {
  const map = {
    'WHOLE': '标准牛乳',
    'OAT': '换燕麦奶',
    'COCONUT': '换椰奶'
  }
  return map[milk] || milk
}
</script>

<style scoped>
.cart-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cart-item {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: #FFFFFF;
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(141, 110, 99, 0.06);
  transition: all 0.3s ease;
}

.cart-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(141, 110, 99, 0.12);
}

.cart-item img {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  object-fit: cover;
  background: #F5F5F5;
}

.item-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.item-details h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #3E2723;
}

.item-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.item-specs span {
  font-size: 11px;
  color: #8D6E63;
  background: #FDF8F3;
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid rgba(198, 156, 109, 0.15);
}

.item-price {
  font-size: 16px;
  font-weight: 600;
  color: #5D4037;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.original-price {
  text-decoration: line-through;
  color: #BDBDBD;
  font-size: 13px;
  font-weight: 400;
}

.discounted-price {
  color: #E65100;
  font-weight: 700;
  font-size: 16px;
}

.normal-price {
  color: #5D4037;
  font-weight: 600;
}

.item-actions {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
}

.quantity-ctrl {
  display: flex;
  align-items: center;
  background: #F5F5F5;
  border-radius: 20px;
  padding: 2px;
}

.quantity-ctrl button {
  width: 28px;
  height: 28px;
  border: none;
  background: #FFFFFF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #5D4037;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
  transition: all 0.2s;
}

.quantity-ctrl button:hover {
  background: #5D4037;
  color: #FFF;
}

.quantity-ctrl button:active {
  transform: scale(0.9);
}

.quantity-ctrl span {
  width: 30px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #3E2723;
}

.remove-btn {
  background: transparent;
  border: none;
  color: #BCAAA4;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.remove-btn:hover {
  background: #FEEBEB;
  color: #EF5350;
}
</style>
