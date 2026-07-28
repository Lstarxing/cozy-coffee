<template>
  <view v-if="visible" class="sheet-layer">
    <view class="sheet-mask" @click="$emit('close')" />
    <view class="sheet-panel">
      <view class="sheet-handle" />
      <view class="sheet-header">
        <image class="sheet-image" :src="displayProduct.image" mode="aspectFill" />
        <view class="sheet-heading">
          <text class="sheet-title">{{ displayProduct.name }}</text>
          <text class="sheet-description">{{ displayProduct.description || '门店现制' }}</text>
          <text class="sheet-price">¥{{ totalPrice }}</text>
        </view>
        <view class="close-button" @click="$emit('close')">×</view>
      </view>

      <scroll-view scroll-y class="sheet-scroll">
        <view v-if="sizeOptions.length > 1" class="spec-section">
          <text class="spec-title">杯型</text>
          <view class="option-grid">
            <view v-for="option in sizeOptions" :key="option.value" class="option" :class="{ selected: form.cupSize === option.value }" @click="form.cupSize = option.value">
              <text>{{ option.label }}</text>
              <text v-if="option.extra" class="option-extra">+¥{{ option.extra }}</text>
            </view>
          </view>
        </view>

        <view v-if="tempOptions.length > 1" class="spec-section">
          <text class="spec-title">温度</text>
          <view class="option-grid">
            <view v-for="option in tempOptions" :key="option.value" class="option" :class="{ selected: form.temperature === option.value }" @click="form.temperature = option.value">{{ option.label }}</view>
          </view>
        </view>

        <view v-if="sugarOptions.length > 1" class="spec-section">
          <text class="spec-title">甜度</text>
          <view class="option-grid">
            <view v-for="option in sugarOptions" :key="option.value" class="option" :class="{ selected: form.sugarLevel === option.value }" @click="form.sugarLevel = option.value">{{ option.label }}</view>
          </view>
        </view>

        <view v-if="isCoffee" class="spec-section">
          <text class="spec-title">咖啡浓度</text>
          <view class="option-grid option-grid--two">
            <view class="option" :class="{ selected: form.coffeeStrength === 'NORMAL' }" @click="form.coffeeStrength = 'NORMAL'">标准</view>
            <view class="option" :class="{ selected: form.coffeeStrength === 'STRONG' }" @click="form.coffeeStrength = 'STRONG'">加浓 <text class="option-extra">+¥5</text></view>
          </view>
        </view>

        <view v-if="supportsMilk" class="spec-section">
          <text class="spec-title">奶类</text>
          <view class="option-grid option-grid--two">
            <view v-for="option in milkOptions" :key="option.value" class="option" :class="{ selected: form.milkType === option.value }" @click="form.milkType = option.value">
              <text>{{ option.label }}</text>
              <text v-if="option.extra" class="option-extra">+¥{{ option.extra }}</text>
            </view>
          </view>
        </view>
      </scroll-view>

      <view class="sheet-footer safe-area-bottom">
        <view class="quantity-control">
          <view class="quantity-button" :class="{ disabled: form.quantity <= 1 }" @click="changeQuantity(-1)">−</view>
          <text class="quantity-value">{{ form.quantity }}</text>
          <view class="quantity-button" :class="{ disabled: form.quantity >= 10 }" @click="changeQuantity(1)">＋</view>
        </view>
        <view class="confirm-button" @click="confirm">{{ editing ? '保存修改' : '加入购物车' }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  product: { type: Object, default: () => ({}) },
  line: { type: Object, default: null }
})

const emit = defineEmits(['close', 'confirm'])

const form = reactive({
  cupSize: 'STANDARD',
  temperature: 'HOT',
  sugarLevel: 'STANDARD',
  milkType: 'WHOLE',
  coffeeStrength: 'NORMAL',
  quantity: 1
})

const editing = computed(() => Boolean(props.line?.lineKey))
const displayProduct = computed(() => ({ ...props.product, ...props.line, image: props.product?.image || props.line?.image }))
const category = computed(() => String(displayProduct.value.category || '').toLowerCase())
const isFood = computed(() => ['bakery', 'dessert', 'food', 'addon'].includes(category.value))
const isCoffee = computed(() => !isFood.value && ['coffee', 'espresso', 'signature', 'soe', 'latte', 'other'].includes(category.value || 'coffee'))
const supportsMilk = computed(() => isCoffee.value && !['soe'].includes(category.value))

const sizeOptions = computed(() => {
  if (isFood.value) return [{ value: 'STANDARD', label: '单份', extra: 0 }]
  const type = displayProduct.value.sizeType || 'MEDIUM_LARGE'
  if (type === 'DEFAULT') return [{ value: 'STANDARD', label: '标准杯', extra: 0 }]
  if (type === 'ALL_SIZES') return [
    { value: 'SMALL', label: '小杯', extra: 0 },
    { value: 'MEDIUM', label: '中杯', extra: 0 },
    { value: 'LARGE', label: '大杯', extra: 3 }
  ]
  return [
    { value: 'MEDIUM', label: '中杯', extra: 0 },
    { value: 'LARGE', label: '大杯', extra: 3 }
  ]
})

const sugarOptions = computed(() => {
  if (isFood.value) return [{ value: '', label: '默认' }]
  const type = displayProduct.value.sugarType || 'FREE_CHOICE'
  if (type === 'NO_SUGAR_ONLY') return [{ value: 'NONE', label: '无糖' }]
  const values = [
    { value: 'STANDARD', label: '标准糖' },
    { value: 'LESS', label: '少糖' },
    { value: 'HALF', label: '半糖' }
  ]
  if (type !== 'MIN_LESS_SWEET') values.push({ value: 'NONE', label: '无糖' })
  return values
})

const tempOptions = computed(() => {
  if (isFood.value) return [{ value: '', label: '默认' }]
  const type = displayProduct.value.tempType || 'ALL_OK'
  if (type === 'COLD_ONLY') return [{ value: 'COLD', label: '冰' }]
  if (type === 'HOT_ONLY') return [{ value: 'HOT', label: '热' }]
  if (type === 'NO_HOT') return [{ value: 'COLD', label: '冰' }, { value: 'WARM', label: '温' }]
  return [{ value: 'HOT', label: '热' }, { value: 'COLD', label: '冰' }, { value: 'WARM', label: '温' }]
})

const milkOptions = [
  { value: 'WHOLE', label: '全脂奶', extra: 0 },
  { value: 'OAT', label: '燕麦奶', extra: 4 },
  { value: 'COCONUT', label: '椰奶', extra: 4 },
  { value: 'SOY', label: '豆奶', extra: 4 }
]

const unitPrice = computed(() => {
  const base = Number(displayProduct.value.basePrice ?? displayProduct.value.price ?? 0)
  const size = form.cupSize === 'LARGE' ? 3 : 0
  const strength = form.coffeeStrength === 'STRONG' ? 5 : 0
  const milk = form.milkType && form.milkType !== 'WHOLE' ? 4 : 0
  return Number((base + size + strength + milk).toFixed(2))
})

const totalPrice = computed(() => (unitPrice.value * form.quantity).toFixed(2))

function resetForm() {
  const source = props.line || {}
  form.cupSize = source.cupSize || sizeOptions.value[0]?.value || ''
  form.temperature = source.temperature || tempOptions.value[0]?.value || ''
  form.sugarLevel = source.sugarLevel || sugarOptions.value[0]?.value || ''
  form.milkType = source.milkType || 'WHOLE'
  form.coffeeStrength = source.coffeeStrength || 'NORMAL'
  form.quantity = Number(source.quantity || 1)
}

watch(() => [props.visible, props.product, props.line], () => {
  if (props.visible) resetForm()
}, { deep: true })

function changeQuantity(delta) {
  form.quantity = Math.max(1, Math.min(10, form.quantity + delta))
}

function confirm() {
  const product = displayProduct.value
  const addons = []
  if (form.coffeeStrength === 'STRONG') addons.push({ code: 'EXTRA_SHOT', name: '加浓', price: 5 })
  if (form.milkType && form.milkType !== 'WHOLE') addons.push({ code: 'SPECIAL_MILK', name: form.milkType, price: 4 })

  emit('confirm', {
    ...product,
    productId: String(product.productId || product.id),
    id: product.id,
    name: product.name,
    image: product.image,
    basePrice: Number(product.basePrice ?? product.price ?? 0),
    price: unitPrice.value,
    cupSize: form.cupSize,
    temperature: form.temperature,
    sugarLevel: form.sugarLevel,
    milkType: supportsMilk.value ? form.milkType : '',
    coffeeStrength: isCoffee.value ? form.coffeeStrength : '',
    addons,
    quantity: form.quantity
  })
}
</script>

<style lang="scss" scoped>
.sheet-layer { position: fixed; inset: 0; z-index: 1000; }
.sheet-mask { position: absolute; inset: 0; background: rgba(25, 18, 14, .54); }
.sheet-panel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  max-height: 88vh;
  overflow: hidden;
  border-radius: 24rpx 24rpx 0 0;
  background: #fff;
  box-shadow: 0 -6rpx 12rpx rgba(43,30,22,.08);
  animation: sheet-in 220ms $cozy-ease-out;
}
.sheet-handle { width: 72rpx; height: 8rpx; margin: 16rpx auto 6rpx; border-radius: 999rpx; background: $cozy-border; }
.sheet-header { position: relative; display: flex; gap: 20rpx; padding: 18rpx 32rpx 24rpx; border-bottom: 1rpx solid $cozy-border; }
.sheet-image { width: 136rpx; height: 136rpx; flex: none; border-radius: $cozy-radius-md; background: $cozy-surface; }
.sheet-heading { min-width: 0; flex: 1; }
.sheet-title { display: block; padding-right: 44rpx; color: $cozy-ink; font-size: 34rpx; font-weight: 700; }
.sheet-description { display: block; overflow: hidden; margin-top: 8rpx; color: $cozy-muted; font-size: 22rpx; white-space: nowrap; text-overflow: ellipsis; }
.sheet-price { display: block; margin-top: 14rpx; color: $cozy-primary; font-size: 34rpx; font-weight: 750; }
.close-button { position: absolute; top: 12rpx; right: 20rpx; width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; color: $cozy-muted; font-size: 44rpx; }
.sheet-scroll { max-height: 54vh; padding: 0 32rpx; box-sizing: border-box; }
.spec-section { padding: 26rpx 0 6rpx; }
.spec-title { display: block; margin-bottom: 16rpx; color: $cozy-ink; font-size: 27rpx; font-weight: 650; }
.option-grid { display: flex; flex-wrap: wrap; gap: 14rpx; }
.option {
  min-width: 148rpx;
  min-height: 80rpx;
  padding: 14rpx 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  border: 2rpx solid transparent;
  border-radius: $cozy-radius-md;
  background: $cozy-surface;
  color: $cozy-ink;
  font-size: 25rpx;
  box-sizing: border-box;
}
.option.selected { border-color: $cozy-primary; background: #f6ece7; color: $cozy-primary; font-weight: 650; }
.option-extra { color: $cozy-muted; font-size: 20rpx; }
.option.selected .option-extra { color: $cozy-primary; }
.option-grid--two .option { width: calc(50% - 7rpx); }
.sheet-footer { display: flex; align-items: center; gap: 24rpx; padding: 20rpx 32rpx max(20rpx, env(safe-area-inset-bottom)); border-top: 1rpx solid $cozy-border; background: #fff; }
.quantity-control { height: 88rpx; display: flex; align-items: center; border: 1rpx solid $cozy-border; border-radius: $cozy-radius-md; }
.quantity-button { width: 76rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; color: $cozy-ink; font-size: 38rpx; }
.quantity-button.disabled { color: $cozy-placeholder; }
.quantity-value { min-width: 48rpx; text-align: center; color: $cozy-ink; font-size: 27rpx; font-weight: 650; }
.confirm-button { flex: 1; height: 88rpx; display: flex; align-items: center; justify-content: center; border-radius: $cozy-radius-md; background: $cozy-primary; color: #fff; font-size: 28rpx; font-weight: 700; }
@keyframes sheet-in { from { transform: translateY(100%); } to { transform: translateY(0); } }
@media (prefers-reduced-motion: reduce) { .sheet-panel { animation: none; } }
</style>
