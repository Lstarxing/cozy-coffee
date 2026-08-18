<template>
  <div class="dining-method-section">
    <label class="section-label">用餐方式</label>
    <div class="dining-options">
      <label class="dining-option" :class="{ active: modelValue === 'TAKEOUT' }">
        <input type="radio" value="TAKEOUT" :checked="modelValue === 'TAKEOUT'" @change="$emit('update:modelValue', 'TAKEOUT')" />
        <CupSoda :size="18" :stroke-width="1.8" />
        <span>自提</span>
      </label>
      <label class="dining-option" :class="{ active: modelValue === 'DELIVERY' }">
        <input type="radio" value="DELIVERY" :checked="modelValue === 'DELIVERY'" @change="$emit('update:modelValue', 'DELIVERY')" />
        <Truck :size="18" :stroke-width="1.8" />
        <span v-if="isBlackGoldMember">外卖 <b class="free-tag">免运费</b></span>
        <span v-else>外卖 +¥{{ deliveryFee }}</span>
      </label>
    </div>
  </div>
</template>

<script setup>
import { CupSoda, Truck } from 'lucide-vue-next'

defineProps({
  modelValue: { type: String, required: true },
  deliveryFee: { type: Number, default: 3 },
  isBlackGoldMember: { type: Boolean, default: false }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.dining-method-section {
  margin-top: 10px;
  margin-bottom: 24px;
}

.section-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #5D4037;
  margin-bottom: 12px;
}

.dining-options {
  display: flex;
  gap: 12px;
}

.dining-option {
  flex: 1;
  position: relative;
  cursor: pointer;
}

.dining-option input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.dining-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border: 1px solid #E0E0E0;
  border-radius: 12px;
  background: #FFF;
  color: #9E9E9E;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.dining-option:hover {
  border-color: #C69C6D;
  color: #8D6E63;
  background: #FDFBF7;
}

.dining-option.active {
  border-color: #C69C6D;
  background: rgba(198, 156, 109, 0.08);
  color: #5D4037;
  font-weight: 600;
  box-shadow: 0 0 0 1px #C69C6D inset;
}

.dining-option.active svg {
  stroke: #C69C6D;
  stroke-width: 2.2px;
}

.free-tag {
  background: linear-gradient(135deg, #1a1a1a, #2d2d2d);
  color: #C69C6D;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  margin-left: 4px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.3);
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}
</style>
