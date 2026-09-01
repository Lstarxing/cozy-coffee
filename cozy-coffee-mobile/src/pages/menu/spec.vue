<template>
  <view class="spec-page">
    <view class="spec-content">
      <!-- 商品图（位于原生导航栏下方） -->
      <view class="spec-hero">
        <view class="spec-image-wrap" :class="{ 'spec-image-wide': isWideImage }">
          <image :src="product?.image" class="spec-image" mode="aspectFit" />
        </view>
        <view class="spec-hero-info">
          <text v-if="detailTag" class="spec-tag">{{ detailTag }}</text>
          <text class="spec-eyebrow">{{ detailEyebrow }}</text>
          <text class="spec-name">{{ product?.name }}</text>
          <text class="spec-notes">{{ detailNotes }}</text>
        </view>
      </view>

      <!-- V2 豆档案（P2 收尾）：烘焙 · 风味 · 醇厚 · 酸度 -->
      <view class="bean-profile-wrap">
        <BeanBlendProfile v-if="product?.beanProfile || product?.blendProfile" :product="product" />
      </view>

      <!-- 固定组合出杯说明（一豆两喝/三喝） -->
      <view v-if="product?.servingDesc" class="serving-desc">{{ product.servingDesc }}</view>

      <!-- 规格选择 -->
      <view class="spec-section">
        <!-- V2 出品方式（精品 Bean 必选）：手冲 / 冷萃 -->
        <view v-if="showBrewMethod" class="spec-group">
          <text class="spec-title">出品方式</text>
          <view class="spec-options">
            <view v-for="option in brewMethodOptions" :key="option.value" class="spec-option" :class="{ active: form.brewMethod === option.value }" @click="form.brewMethod = option.value">
              <text>{{ option.label }}</text>
              <text class="spec-extra">{{ option.value === 'COLD_BREW' ? `¥${product?.coldBrewPrice || ''}` : `¥${product?.price || ''}` }}</text>
            </view>
          </view>
        </view>

        <view v-if="sizeOptions.length > 1" class="spec-group">
          <text class="spec-title">杯型</text>
          <view class="spec-options">
            <view v-for="option in sizeOptions" :key="option.value" class="spec-option" :class="{ active: form.cupSize === option.value }" @click="form.cupSize = option.value">
              <text>{{ option.label }}</text>
              <text v-if="option.extra > 0" class="spec-extra">+¥{{ option.extra }}</text>
            </view>
          </view>
        </view>
        <view v-if="tempOptions.length > 1" class="spec-group">
          <text class="spec-title">温度</text>
          <view class="spec-options">
            <view v-for="option in tempOptions" :key="option.value" class="spec-option" :class="{ active: form.temperature === option.value }" @click="form.temperature = option.value">{{ option.label }}</view>
          </view>
        </view>
        <view v-if="sugarOptions.length > 1" class="spec-group">
          <text class="spec-title">甜度</text>
          <view class="spec-options">
            <view v-for="option in sugarOptions" :key="option.value" class="spec-option" :class="{ active: form.sugarLevel === option.value }" @click="form.sugarLevel = option.value">{{ option.label }}</view>
          </view>
        </view>
        <!-- V2 加料组（P2-3）：MILK / SHOT / SYRUP / OTHER，按 price_delta 展示，前端只提交 code -->
        <view v-for="group in addonGroups" :key="group.category" class="spec-group">
          <text class="spec-title">{{ groupLabel(group.category) }}</text>
          <view class="spec-options">
            <view v-for="item in group.items" :key="item.code" class="spec-option"
              :class="{ active: isSelected(group, item.code) }"
              @click="toggleAddon(group, item.code)">
              <text>{{ item.name }}</text>
              <text v-if="Number(item.priceDelta) > 0" class="spec-extra">+¥{{ Number(item.priceDelta) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="spec-disclaimer">本商品按所选规格现制，出品以门店实物为准。</view>
    </view>

    <!-- 底部固定加购栏 -->
    <view class="spec-bottom">
      <view class="spec-bottom-top">
        <view class="spec-price-col">
          <text class="spec-bottom-price">¥{{ totalPrice }}</text>
          <text class="spec-selected">{{ selectedSpecsText }}</text>
        </view>
        <view class="spec-qty">
          <view class="qty-btn" @click="changeQty(-1)">−</view>
          <text class="qty-value">{{ form.quantity }}</text>
          <view class="qty-btn" @click="changeQty(1)">＋</view>
        </view>
      </view>
      <view class="spec-add" @click="addToCart">{{ editing ? '保存修改' : '加入购物车' }}</view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useCartStore } from '@/stores/cart'
import { useAddonSelection } from '@/composables/useAddonSelection'
import BeanBlendProfile from '@/components/product/BeanBlendProfile.vue'

const cartStore = useCartStore()

const product = ref({})
const line = ref(null)
const form = reactive({ cupSize: 'STANDARD', temperature: 'HOT', sugarLevel: 'STANDARD', brewMethod: 'POUR_OVER', quantity: 1 })

const editing = computed(() => Boolean(line.value?.lineKey))

// 一豆两喝/三喝：体验商品图单独放大（短码 04-one-bean-two/three）
const isWideImage = computed(() => ['04-one-bean-two', '04-one-bean-three'].includes(product.value?.productCode))

// V2 出品方式（精品 Bean 必选）：POUR_OVER 手冲 / COLD_BREW 冷萃
const showBrewMethod = computed(() => Boolean(product.value?.brewMethod))
const brewMethodOptions = computed(() => [
  { value: 'POUR_OVER', label: '手冲' },
  { value: 'COLD_BREW', label: '冷萃' }
])

onLoad(() => {
  const data = uni.getStorageSync('cozy_spec')
  uni.removeStorageSync('cozy_spec')
  product.value = data?.product || {}
  line.value = data?.line || null
  const source = line.value || {}
  resetAddons()
  form.cupSize = sizeOptions.value.some(o => o.value === source.cupSize) ? source.cupSize : sizeOptions.value[0]?.value || 'STANDARD'
  form.temperature = tempOptions.value.some(o => o.value === source.temperature) ? source.temperature : tempOptions.value[0]?.value || 'HOT'
  form.sugarLevel = sugarOptions.value.some(o => o.value === source.sugarLevel)
    ? source.sugarLevel
    : (product.value.defaultSugarLevel && sugarOptions.value.some(o => o.value === product.value.defaultSugarLevel)
      ? product.value.defaultSugarLevel
      : sugarOptions.value[0]?.value || 'NO_ADDED_SUGAR')
  form.brewMethod = source.brewMethod || 'POUR_OVER'
  form.quantity = Number(source.quantity || 1)
})

const isFood = computed(() => ['bakery', 'dessert', 'food', 'addon'].includes(String(product.value?.category || '').toLowerCase()))

// V2 加料组选择（P2-3）
const addonGroups = computed(() => product.value.addonGroups || [])
const { groupLabel, selection, isSelected, toggle, reset: resetAddons, addonFee, addons, selectedText } = useAddonSelection(addonGroups)

const sizeOptions = computed(() => {
  // 单一事实源：allowedSizes 由后端按 sizeType 规范值下发；价格按杯型读 price/priceMedium/priceLarge
  const allowed = product.value?.allowedSizes || []
  const labels = { STANDARD: '标准杯', MEDIUM: '中杯', LARGE: '大杯', SMALL: '小杯', EXTRA_LARGE: '超大杯' }
  return allowed.map(v => {
    const base = { MEDIUM: Number(product.value?.priceMedium || 0), LARGE: Number(product.value?.priceLarge || 0) }[v] ?? Number(product.value?.price || 0)
    const extra = v === 'LARGE' ? Number(product.value?.priceLarge || 0) - Number(product.value?.priceMedium || 0) : 0
    return { value: v, label: isFood.value ? '单份' : (labels[v] || v), base, extra }
  })
})
const sugarOptions = computed(() => {
  // 单一事实源：allowedSugars 由后端按 sugarType 规范值下发（NO_SUGAR_ONLY/食品为空数组 → 隐藏糖度行）
  const allowed = product.value?.allowedSugars || []
  const labels = { STANDARD: '标准糖', LESS: '少糖', HALF: '半糖', NONE: '无糖', NO_ADDED_SUGAR: '不另外加糖' }
  return allowed.map(v => ({ value: v, label: labels[v] || v }))
})
const tempOptions = computed(() => {
  // 精品 Bean 冷萃固定冰饮（出品方式交互决定温度，覆盖后端 allowedTemps）
  if (showBrewMethod.value && form.brewMethod === 'COLD_BREW') return [{ value: 'COLD', label: '冰' }]
  const allowed = product.value?.allowedTemps || []
  return allowed.map(v => ({ value: v, label: v === 'HOT' ? '热' : '冰' }))
})

const basePrice = computed(() => {
  // 精品 Bean：POUR_OVER 用 price / COLD_BREW 用 cold_brew_price
  if (showBrewMethod.value) {
    return form.brewMethod === 'COLD_BREW'
      ? Number(product.value?.coldBrewPrice || 0)
      : Number(product.value?.price || 0)
  }
  const opt = sizeOptions.value.find(o => o.value === form.cupSize)
  return opt ? opt.base : Number(product.value?.price || 0)
})
const unitPrice = computed(() => Number((basePrice.value + addonFee.value).toFixed(2)))
const totalPrice = computed(() => (unitPrice.value * form.quantity).toFixed(2))

// 向后兼容字段（购物车/重购旧逻辑）：从加料选择派生
const coffeeStrength = computed(() => selection['SHOT'] === 'EXTRA_SHOT' ? 'STRONG' : 'NORMAL')
const milkType = computed(() => {
  const milk = selection['MILK']
  return ({ WHOLE_MILK: 'WHOLE', OAT_MILK: 'OAT', COCONUT_MILK: 'COCONUT', SOY_MILK: 'SOY' })[milk] || 'WHOLE'
})

const selectedSpecsText = computed(() => {
  const parts = []
  if (showBrewMethod.value) parts.push({ POUR_OVER: '手冲', COLD_BREW: '冷萃' }[form.brewMethod] || form.brewMethod)
  if (sizeOptions.value.length > 1) parts.push({ STANDARD: '标准杯', MEDIUM: '中杯', LARGE: '大杯', EXTRA_LARGE: '超大杯' }[form.cupSize] || form.cupSize)
  if (tempOptions.value.length > 1) parts.push({ HOT: '热', COLD: '冰' }[form.temperature] || form.temperature)
  if (sugarOptions.value.length > 1) parts.push({ STANDARD: '标准糖', LESS: '少糖', HALF: '半糖', NONE: '无糖', NO_ADDED_SUGAR: '不另外加糖' }[form.sugarLevel] || form.sugarLevel)
  if (selectedText.value) parts.push(selectedText.value)
  return parts.length ? parts.join(' · ') : '默认规格'
})
const detailTag = computed(() => product.value?.tag || (product.value?.isNewProduct ? '新品' : ''))
const detailEyebrow = computed(() => categoryEn(String(product.value?.category || '').toLowerCase()))
const detailNotes = computed(() => product.value?.notes || product.value?.description || '')

function categoryEn(code) {
  return ({
    espresso: 'ESPRESSO', signature: 'SIGNATURE',
    bakery: 'BAKERY', addon: 'ADD-ON', other: 'COZY'
  })[code] || 'COZY'
}

function changeQty(delta) {
  form.quantity = Math.max(1, Math.min(10, form.quantity + delta))
}

function toggleAddon(group, code) {
  toggle(group, code)
}

function buildSelection() {
  return {
    price: unitPrice.value,
    cupSize: form.cupSize,
    temperature: form.temperature,
    sugarLevel: isFood.value ? '' : form.sugarLevel,
    brewMethod: showBrewMethod.value ? form.brewMethod : '',
    milkType: milkType.value,
    coffeeStrength: coffeeStrength.value,
    addons: addons.value,
    quantity: form.quantity
  }
}

function addToCart() {
  if (!product.value || !product.value.name) return
  const p = product.value
  const selection = buildSelection()

  if (editing.value && line.value?.lineKey) {
    cartStore.updateOptions(line.value.lineKey, selection)
  } else {
    cartStore.addItem({
      ...p,
      productId: String(p.productId || p.id),
      id: p.id,
      name: p.name,
      image: p.image,
      basePrice: basePrice.value,
      ...selection
    }, form.quantity)
  }
  try { uni.vibrateShort({ type: 'light' }) } catch (_) {}
  uni.showToast({ title: editing.value ? '已保存修改' : `已加入购物车 · ${p.name} ×${form.quantity}`, icon: 'none', duration: 900 })
  setTimeout(() => uni.navigateBack(), 500)
}
</script>

<style lang="scss" scoped>
.spec-page { min-height: 100vh; background: $cozy-bg; }
.spec-content { padding-bottom: 300rpx; }

.spec-hero { padding: 48rpx 0 0; background: #fff; }
.spec-image-wrap {
  width: 560rpx;
  height: 460rpx;
  margin: 0 auto;
  box-sizing: border-box;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: #fff;
}
.spec-image { width: 460rpx; height: 460rpx; display: block; }
.spec-image-wrap.spec-image-wide { height: 560rpx; }
.spec-image-wrap.spec-image-wide .spec-image { width: 560rpx; height: 560rpx; }
.spec-hero-info { padding: 40rpx 40rpx 0; }
.spec-tag { display: inline-block; margin-bottom: 18rpx; padding: 6rpx 18rpx; border-radius: 8rpx; background: $cozy-ink; color: #fff; font-size: 22rpx; font-weight: 700; letter-spacing: .06em; }
.spec-eyebrow { display: block; font-size: 20rpx; font-weight: 700; letter-spacing: .24em; color: $cozy-muted; }
.spec-name { display: block; margin-top: 14rpx; font-family: $font-display; font-size: 44rpx; font-weight: 600; color: $cozy-ink; line-height: 1.2; }
.spec-notes { display: block; margin-top: 16rpx; font-family: $font-display; font-size: 24rpx; color: $cozy-muted; letter-spacing: .02em; }

.spec-section { border-top: 1rpx solid $cozy-border; padding: 36rpx 40rpx 8rpx; }
.spec-group { margin-top: 28rpx; }
.spec-group:first-child { margin-top: 0; }
.spec-title { display: block; margin-bottom: 20rpx; font-size: 26rpx; font-weight: 600; color: $cozy-ink; }
.spec-options { display: flex; flex-wrap: wrap; gap: 14rpx; }
.spec-option { height: 56rpx; padding: 0 26rpx; display: flex; align-items: center; justify-content: center; gap: 6rpx; font-size: 24rpx; color: $cozy-ink; border-radius: 8rpx; border: 1rpx solid $cozy-border; background: #fff; }
.spec-option.active { background: $cozy-surface; border-color: $cozy-ink; color: $cozy-ink; font-weight: 600; }
.spec-extra { color: $cozy-muted; font-size: 18rpx; }
.spec-option.active .spec-extra { color: $cozy-ink; }

.spec-disclaimer { margin-top: 24rpx; padding: 0 40rpx 48rpx; font-size: 22rpx; line-height: 1.7; color: $cozy-placeholder; }
.serving-desc { padding: 20rpx 40rpx 4rpx; color: $cozy-muted; font-size: $font-size-sm; line-height: 1.6; }
.bean-profile-wrap { padding: 0 40rpx; }
.spec-bottom { position: fixed; left: 0; right: 0; bottom: 0; padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1rpx solid $cozy-border; }
.spec-bottom-top { display: flex; align-items: center; gap: 24rpx; margin-bottom: 20rpx; }
.spec-price-col { flex: 1; min-width: 0; }
.spec-bottom-price { font-size: 44rpx; font-weight: 700; color: $cozy-ink; line-height: 1.1; }
.spec-selected { display: block; margin-top: 8rpx; font-size: 22rpx; color: $cozy-muted; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.spec-qty { display: flex; align-items: center; gap: 16rpx; border: 1rpx solid $cozy-border; border-radius: 12rpx; padding: 8rpx 18rpx; }
.qty-btn { width: 48rpx; height: 48rpx; display: flex; align-items: center; justify-content: center; font-size: 32rpx; color: $cozy-ink; }
.qty-value { min-width: 36rpx; text-align: center; font-size: 28rpx; font-weight: 600; color: $cozy-ink; }
.spec-add { width: 100%; height: 92rpx; display: flex; align-items: center; justify-content: center; border-radius: 12rpx; background: $cozy-ink; color: #fff; font-size: 30rpx; font-weight: 650; }
.spec-add:active { background: mix(#000, $cozy-ink, 92%); }
</style>
