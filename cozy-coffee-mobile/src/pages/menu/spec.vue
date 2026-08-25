<template>
  <view class="spec-page">
    <view class="spec-content">
      <!-- 商品图（位于原生导航栏下方） -->
      <view class="spec-hero">
        <image :src="product?.image" class="spec-image" mode="aspectFill" />
        <view class="spec-hero-info">
          <text v-if="detailTag" class="spec-tag">{{ detailTag }}</text>
          <text class="spec-eyebrow">{{ detailEyebrow }}</text>
          <text class="spec-name">{{ product?.name }}</text>
          <text class="spec-notes">{{ detailNotes }}</text>
        </view>
      </view>

      <!-- 规格选择 -->
      <view class="spec-section">
        <view v-if="sizeOptions.length > 1" class="spec-group">
          <text class="spec-title">杯型</text>
          <view class="spec-options">
            <view v-for="option in sizeOptions" :key="option.value" class="spec-option" :class="{ active: form.cupSize === option.value }" @click="form.cupSize = option.value">
              <text>{{ option.label }}</text>
              <text v-if="option.extra" class="spec-extra">+¥{{ option.extra }}</text>
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
        <view v-if="isEspresso" class="spec-group">
          <text class="spec-title">咖啡浓度</text>
          <view class="spec-options">
            <view class="spec-option" :class="{ active: form.coffeeStrength === 'NORMAL' }" @click="form.coffeeStrength = 'NORMAL'">标准</view>
            <view class="spec-option" :class="{ active: form.coffeeStrength === 'STRONG' }" @click="form.coffeeStrength = 'STRONG'">加浓 <text class="spec-extra">+¥5</text></view>
          </view>
        </view>
        <view v-if="supportsMilk" class="spec-group">
          <text class="spec-title">基底奶</text>
          <view class="spec-options">
            <view v-for="option in milkOptions" :key="option.value" class="spec-option" :class="{ active: form.milkType === option.value }" @click="form.milkType = option.value">
              <text>{{ option.label }}</text>
              <text v-if="option.extra" class="spec-extra">+¥{{ option.extra }}</text>
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

const cartStore = useCartStore()

const product = ref({})
const line = ref(null)
const form = reactive({ cupSize: 'STANDARD', temperature: 'HOT', sugarLevel: 'STANDARD', milkType: 'WHOLE', coffeeStrength: 'NORMAL', quantity: 1 })

const editing = computed(() => Boolean(line.value?.lineKey))

onLoad(() => {
  const data = uni.getStorageSync('cozy_spec')
  uni.removeStorageSync('cozy_spec')
  product.value = data?.product || {}
  line.value = data?.line || null
  const source = line.value || {}
  form.cupSize = source.cupSize || sizeOptions.value[0]?.value || 'STANDARD'
  form.temperature = source.temperature || tempOptions.value[0]?.value || 'HOT'
  form.sugarLevel = source.sugarLevel || sugarOptions.value[0]?.value || 'STANDARD'
  form.milkType = source.milkType || 'WHOLE'
  form.coffeeStrength = source.coffeeStrength || 'NORMAL'
  form.quantity = Number(source.quantity || 1)
})

const isFood = computed(() => ['bakery', 'dessert', 'food', 'addon'].includes(String(product.value?.category || '').toLowerCase()))
const isEspresso = computed(() => String(product.value?.category || '').toLowerCase() === 'espresso')

const sizeOptions = computed(() => {
  if (isFood.value) return [{ value: 'STANDARD', label: '单份', extra: 0 }]
  const type = String(product.value?.sizeType || 'MEDIUM_LARGE').toUpperCase()
  if (type === 'DEFAULT') return [{ value: 'STANDARD', label: '标准杯', extra: 0 }]
  if (type === 'ALL_SIZES') return [
    { value: 'MEDIUM', label: '中杯', extra: 0 },
    { value: 'LARGE', label: '大杯', extra: 3 },
    { value: 'EXTRA_LARGE', label: '超大杯', extra: 5 }
  ]
  return [
    { value: 'STANDARD', label: '标准杯', extra: 0 },
    { value: 'LARGE', label: '大杯', extra: 3 }
  ]
})
const sugarOptions = computed(() => {
  if (isFood.value) return [{ value: '', label: '默认' }]
  const type = String(product.value?.sugarType || 'FREE_CHOICE').toUpperCase()
  if (type === 'NO_SUGAR_ONLY') return [{ value: 'NONE', label: '无糖' }]
  const values = [{ value: 'STANDARD', label: '标准糖' }, { value: 'LESS', label: '少糖' }, { value: 'HALF', label: '半糖' }]
  if (type !== 'MIN_LESS_SWEET') values.push({ value: 'NONE', label: '无糖' })
  return values
})
const tempOptions = computed(() => {
  if (isFood.value) return [{ value: '', label: '默认' }]
  const type = String(product.value?.tempType || 'ALL_OK').toUpperCase()
  if (type === 'COLD_ONLY') return [{ value: 'COLD', label: '冰' }]
  if (type === 'HOT_ONLY') return [{ value: 'HOT', label: '热' }]
  if (type === 'NO_HOT') return [{ value: 'COLD', label: '冰' }, { value: 'WARM', label: '温' }]
  return [{ value: 'HOT', label: '热' }, { value: 'COLD', label: '冰' }]
})

// 与 web 端 ProductCustomizer 一致：仅意式咖啡且非固定奶底/无奶商品可选基底奶
const NO_MILK_CHANGE = ['美式', 'Americano', '卡布奇诺', '摩卡', '焦糖玛奇朵', '脏咖', 'Dirty', '澳白', 'Flat White', '生椰拿铁', '燕麦拿铁', 'SOE', '手冲']
const supportsMilk = computed(() => {
  if (!isEspresso.value) return false
  return !NO_MILK_CHANGE.some(n => String(product.value?.name || '').includes(n))
})
const milkOptions = [
  { value: 'WHOLE', label: '标准牛乳', extra: 0 },
  { value: 'OAT', label: '换燕麦奶', extra: 4 },
  { value: 'COCONUT', label: '换椰奶', extra: 4 }
]

const unitPrice = computed(() => {
  const base = Number(product.value?.basePrice ?? product.value?.price ?? 0)
  const size = ({ STANDARD: 0, MEDIUM: 0, LARGE: 3, EXTRA_LARGE: 5 })[form.cupSize] || 0
  const strength = form.coffeeStrength === 'STRONG' ? 5 : 0
  const milk = supportsMilk.value && form.milkType && form.milkType !== 'WHOLE' ? 4 : 0
  return Number((base + size + strength + milk).toFixed(2))
})
const totalPrice = computed(() => (unitPrice.value * form.quantity).toFixed(2))
const selectedSpecsText = computed(() => {
  const parts = []
  if (sizeOptions.value.length > 1) parts.push({ STANDARD: '标准杯', MEDIUM: '中杯', LARGE: '大杯', EXTRA_LARGE: '超大杯' }[form.cupSize] || form.cupSize)
  if (tempOptions.value.length > 1) parts.push({ HOT: '热', COLD: '冰', WARM: '温' }[form.temperature] || form.temperature)
  if (sugarOptions.value.length > 1) parts.push(form.sugarLevel === 'NONE' ? '无糖' : (form.sugarLevel === 'HALF' ? '半糖' : (form.sugarLevel === 'LESS' ? '少糖' : '标准糖')))
  if (supportsMilk.value) parts.push({ WHOLE: '标准牛乳', OAT: '换燕麦奶', COCONUT: '换椰奶' }[form.milkType] || form.milkType)
  if (isEspresso.value) parts.push(form.coffeeStrength === 'STRONG' ? '加浓' : '标准')
  return parts.length ? parts.join(' · ') : '默认规格'
})
const detailTag = computed(() => product.value?.tag || (product.value?.isNewProduct ? '新品' : ''))
const detailEyebrow = computed(() => categoryEn(String(product.value?.category || '').toLowerCase()))
const detailNotes = computed(() => product.value?.notes || product.value?.description || '')

function categoryEn(code) {
  return ({
    espresso: 'ESPRESSO', coffee: 'CLASSIC COFFEE', latte: 'LATTE', signature: 'SIGNATURE',
    soe: 'HAND BREW', bakery: 'BAKERY', dessert: 'DESSERT', addon: 'ADD-ON', other: 'COZY'
  })[code] || 'COZY'
}

function changeQty(delta) {
  form.quantity = Math.max(1, Math.min(10, form.quantity + delta))
}

function buildSelection() {
  const addons = []
  if (form.coffeeStrength === 'STRONG') addons.push({ code: 'EXTRA_SHOT', name: '加浓', price: 5 })
  if (supportsMilk.value && form.milkType && form.milkType !== 'WHOLE') addons.push({ code: 'SPECIAL_MILK', name: form.milkType, price: 4 })
  return {
    price: unitPrice.value,
    cupSize: form.cupSize,
    temperature: form.temperature,
    sugarLevel: isFood.value ? '' : form.sugarLevel,
    milkType: supportsMilk.value ? form.milkType : '',
    coffeeStrength: isEspresso.value ? form.coffeeStrength : '',
    addons,
    quantity: form.quantity
  }
}

function addToCart() {
  if (!product.value || !product.value.name) return
  const p = product.value
  const selection = buildSelection()

  if (editing.value && line.value?.lineKey) {
    // 从购物车进入编辑：更新当前行（新规格与已有其他行一致时自动合并），而非新增
    cartStore.updateOptions(line.value.lineKey, selection)
  } else {
    cartStore.addItem({
      ...p,
      productId: String(p.productId || p.id),
      id: p.id,
      name: p.name,
      image: p.image,
      basePrice: Number(p.basePrice ?? p.price ?? 0),
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
.spec-content { padding-bottom: 220rpx; }

.spec-hero { position: relative; }
.spec-image { width: 100%; height: 520rpx; display: block; background: linear-gradient(135deg,#E8DDD2,#D8C8B4); }
.spec-hero-info { padding: 40rpx 40rpx 12rpx; }
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

.spec-card { border-top: 1rpx solid $cozy-border; margin-top: 40rpx; padding: 36rpx 40rpx 4rpx; }
.spec-card-title { display: block; margin-bottom: 20rpx; font-family: $font-display; font-size: 32rpx; font-weight: 600; color: $cozy-ink; }
.spec-row { display: flex; align-items: flex-start; gap: 32rpx; padding: 16rpx 0; }
.spec-row-label { flex: none; width: 104rpx; font-size: 22rpx; font-weight: 700; letter-spacing: .1em; color: $cozy-muted; }
.spec-row-value { flex: 1; font-size: 26rpx; line-height: 1.6; color: $cozy-ink; }
.spec-disclaimer { border-top: 1rpx solid $cozy-border; margin-top: 40rpx; padding: 28rpx 40rpx 48rpx; font-size: 22rpx; line-height: 1.7; color: $cozy-placeholder; }

.spec-bottom { position: fixed; left: 0; right: 0; bottom: 0; padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1rpx solid $cozy-border; }
.spec-bottom-top { display: flex; align-items: center; gap: 24rpx; margin-bottom: 20rpx; }
.spec-price-col { flex: 1; min-width: 0; }
.spec-bottom-price { font-size: 44rpx; font-weight: 700; color: $cozy-ink; line-height: 1.1; }
.spec-selected { display: block; margin-top: 8rpx; font-size: 22rpx; color: $cozy-muted; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.spec-qty { display: flex; align-items: center; gap: 16rpx; border: 1rpx solid $cozy-border; border-radius: 12rpx; padding: 8rpx 18rpx; }
.qty-btn { width: 48rpx; height: 48rpx; display: flex; align-items: center; justify-content: center; font-size: 32rpx; color: $cozy-ink; }
.qty-value { min-width: 36rpx; text-align: center; font-size: 28rpx; font-weight: 600; color: $cozy-ink; }
.spec-add { width: 100%; height: 92rpx; display: flex; align-items: center; justify-content: center; border-radius: 12rpx; background: $cozy-ink; color: #fff; font-size: 30rpx; font-weight: 650; }
.spec-add:active { background: darken($cozy-ink, 8%); }
</style>
