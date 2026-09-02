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
        <!-- V2 豆档案（P2 收尾）：烘焙 · 风味 · 醇厚 · 酸度 -->
        <BeanBlendProfile v-if="displayProduct.beanProfile || displayProduct.blendProfile" :product="displayProduct" />

        <!-- 固定组合出杯说明（一豆两喝/三喝） -->
        <view v-if="displayProduct.servingDesc" class="serving-desc">{{ displayProduct.servingDesc }}</view>

        <view v-if="showBrewMethod" class="spec-section">
          <text class="spec-title">出品方式</text>
          <view class="option-grid">
            <view v-for="option in brewMethodOptions" :key="option.value" class="option" :class="{ selected: form.brewMethod === option.value }" @click="form.brewMethod = option.value">
              <text>{{ option.label }}</text>
              <text class="option-extra">{{ option.value === 'COLD_BREW' ? `¥${displayProduct.coldBrewPrice || ''}` : `¥${displayProduct.price || ''}` }}</text>
            </view>
          </view>
        </view>

        <view v-if="sizeOptions.length > 1" class="spec-section">
          <text class="spec-title">杯型</text>
          <view class="option-grid">
            <view v-for="option in sizeOptions" :key="option.value" class="option" :class="{ selected: form.cupSize === option.value }" @click="form.cupSize = option.value">
              <text>{{ option.label }}</text>
              <text v-if="option.extra > 0" class="option-extra">+¥{{ option.extra }}</text>
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

        <!-- V2 加料组（P2-3）：MILK / SHOT / SYRUP / OTHER，按 price_delta 展示，前端只提交 code -->
        <view v-for="group in addonGroups" :key="group.category" class="spec-section">
          <text class="spec-title">{{ groupLabel(group.category) }}</text>
          <view class="option-grid">
            <view v-for="item in group.items" :key="item.code" class="option"
              :class="{ selected: isSelected(group, item.code) }"
              @click="toggleAddon(group, item.code)">
              <text>{{ item.name }}</text>
              <text v-if="Number(item.priceDelta) > 0" class="option-extra">+¥{{ Number(item.priceDelta) }}</text>
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
import { useAddonSelection } from '@/composables/useAddonSelection'
import BeanBlendProfile from '@/components/product/BeanBlendProfile.vue'

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
  brewMethod: 'POUR_OVER',
  quantity: 1
})

const editing = computed(() => Boolean(props.line?.lineKey))
const displayProduct = computed(() => ({ ...props.product, ...props.line, image: props.product?.image || props.line?.image }))
const category = computed(() => String(displayProduct.value.category || '').toLowerCase())
const isFood = computed(() => {
  if (displayProduct.value.isFood !== undefined) return displayProduct.value.isFood
  // 旧数据兜底：食品无糖度/温度选项（allowed 数组为空），不硬编码分类列表
  const s = displayProduct.value.allowedSugars, t = displayProduct.value.allowedTemps
  return (Array.isArray(s) && s.length === 0) && (Array.isArray(t) && t.length === 0)
})

// V2 出品方式（精品 Bean 必选）：POUR_OVER 手冲 / COLD_BREW 冷萃
const showBrewMethod = computed(() => Boolean(displayProduct.value.brewMethod))
const brewMethodOptions = computed(() => [
  { value: 'POUR_OVER', label: '手冲' },
  { value: 'COLD_BREW', label: '冷萃' }
])

// V2 加料组选择（P2-3）
const addonGroups = computed(() => displayProduct.value.addonGroups || [])
const { groupLabel, selection, isSelected, toggle, reset: resetAddons, addonFee, addons } = useAddonSelection(addonGroups)

const sizeOptions = computed(() => {
  // 单一事实源：allowedSizes 由后端按 sizeType 规范值下发；价格按杯型读 price/priceMedium/priceLarge
  const allowed = displayProduct.value.allowedSizes || []
  const labels = { STANDARD: '标准杯', MEDIUM: '中杯', LARGE: '大杯', SMALL: '小杯', EXTRA_LARGE: '超大杯' }
  return allowed.map(v => {
    const base = { MEDIUM: Number(displayProduct.value.priceMedium || 0), LARGE: Number(displayProduct.value.priceLarge || 0) }[v] ?? Number(displayProduct.value.price || 0)
    const extra = v === 'LARGE' ? Number(displayProduct.value.priceLarge || 0) - Number(displayProduct.value.priceMedium || 0) : 0
    return { value: v, label: isFood.value ? '单份' : (labels[v] || v), base, extra }
  })
})

const sugarOptions = computed(() => {
  // 单一事实源：allowedSugars 由后端按 sugarType 规范值下发（NO_SUGAR_ONLY/食品为空数组 → 隐藏糖度行）
  const allowed = displayProduct.value.allowedSugars || []
  const labels = { STANDARD: '标准糖', LESS: '少糖', HALF: '半糖', NONE: '无糖', NO_ADDED_SUGAR: '不另外加糖' }
  return allowed.map(v => ({ value: v, label: labels[v] || v }))
})

const tempOptions = computed(() => {
  // 精品 Bean 冷萃固定冰饮（出品方式交互决定温度，覆盖后端 allowedTemps）
  if (showBrewMethod.value && form.brewMethod === 'COLD_BREW') return [{ value: 'COLD', label: '冰' }]
  const allowed = displayProduct.value.allowedTemps || []
  return allowed.map(v => ({ value: v, label: v === 'HOT' ? '热' : '冰' }))
})

const basePrice = computed(() => {
  // 精品 Bean：POUR_OVER 用 price / COLD_BREW 用 cold_brew_price
  if (showBrewMethod.value) {
    return form.brewMethod === 'COLD_BREW'
      ? Number(displayProduct.value.coldBrewPrice || 0)
      : Number(displayProduct.value.price || 0)
  }
  const opt = sizeOptions.value.find(o => o.value === form.cupSize)
  return opt ? opt.base : Number(displayProduct.value.price || 0)
})
const unitPrice = computed(() => Number((basePrice.value + addonFee.value).toFixed(2)))
const totalPrice = computed(() => (unitPrice.value * form.quantity).toFixed(2))

// 向后兼容字段（购物车/重购旧逻辑）：从加料选择派生
const coffeeStrength = computed(() => selection['SHOT'] === 'EXTRA_SHOT' ? 'STRONG' : 'NORMAL')
const milkType = computed(() => {
  const milk = selection['MILK']
  return ({ WHOLE_MILK: 'WHOLE', OAT_MILK: 'OAT', COCONUT_MILK: 'COCONUT', SOY_MILK: 'SOY' })[milk] || 'WHOLE'
})

function resetForm() {
  const source = props.line || {}
  resetAddons()
  form.cupSize = sizeOptions.value.some(o => o.value === source.cupSize) ? source.cupSize : sizeOptions.value[0]?.value || ''
  form.temperature = tempOptions.value.some(o => o.value === source.temperature) ? source.temperature : tempOptions.value[0]?.value || ''
  form.sugarLevel = sugarOptions.value.some(o => o.value === source.sugarLevel)
    ? source.sugarLevel
    : (displayProduct.value.defaultSugarLevel && sugarOptions.value.some(o => o.value === displayProduct.value.defaultSugarLevel)
      ? displayProduct.value.defaultSugarLevel
      : sugarOptions.value[0]?.value || 'NO_ADDED_SUGAR')
  form.brewMethod = source.brewMethod || 'POUR_OVER'
  form.quantity = Number(source.quantity || 1)
}

watch(() => [props.visible, props.product, props.line], () => {
  if (props.visible) resetForm()
}, { deep: true })

function changeQuantity(delta) {
  form.quantity = Math.max(1, Math.min(10, form.quantity + delta))
}

function toggleAddon(group, code) {
  toggle(group, code)
}

function confirm() {
  const product = displayProduct.value
  emit('confirm', {
    ...product,
    productId: String(product.productId || product.id),
    id: product.id,
    name: product.name,
    image: product.image,
    basePrice: basePrice.value,
    price: unitPrice.value,
    cupSize: form.cupSize,
    temperature: form.temperature,
    sugarLevel: form.sugarLevel,
    brewMethod: showBrewMethod.value ? form.brewMethod : '',
    milkType: milkType.value,
    coffeeStrength: coffeeStrength.value,
    addons: addons.value,
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
.serving-desc { padding: 8rpx 0 4rpx; color: $cozy-muted; font-size: 22rpx; line-height: 1.6; }
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
.sheet-footer { display: flex; align-items: center; gap: 24rpx; padding: 20rpx 32rpx max(20rpx, env(safe-area-inset-bottom)); border-top: 1rpx solid $cozy-border; background: #fff; }
.quantity-control { height: 88rpx; display: flex; align-items: center; border: 1rpx solid $cozy-border; border-radius: $cozy-radius-md; }
.quantity-button { width: 76rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; color: $cozy-ink; font-size: 38rpx; }
.quantity-button.disabled { color: $cozy-placeholder; }
.quantity-value { min-width: 48rpx; text-align: center; color: $cozy-ink; font-size: 27rpx; font-weight: 650; }
.confirm-button { flex: 1; height: 88rpx; display: flex; align-items: center; justify-content: center; border-radius: $cozy-radius-md; background: $cozy-primary; color: #fff; font-size: 28rpx; font-weight: 700; }
@keyframes sheet-in { from { transform: translateY(100%); } to { transform: translateY(0); } }
@media (prefers-reduced-motion: reduce) { .sheet-panel { animation: none; } }
</style>
