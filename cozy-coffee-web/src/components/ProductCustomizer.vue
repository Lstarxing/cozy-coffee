<template>
  <div class="customizer-overlay" @click.self="$emit('close')">
    <div class="customizer-panel">
      <!-- 顶部大图区域 (无价格) -->
      <div class="product-banner">
        <img :src="getImageUrl(product.imageUrl)" :alt="product.name" @error="handleImageError">
        <button class="close-btn-float" @click="$emit('close')">×</button>
      </div>

      <!-- 内容区域 -->
      <div class="panel-body">
        <div class="product-header">
          <h3>{{ product.name }}</h3>
          <p v-if="product.description" class="product-desc-text">{{ product.description }}</p>
          <p v-if="product.servingDesc" class="serving-desc-text">出杯：{{ product.servingDesc }}</p>
        </div>

        <div class="options-section">
          <!-- 杯型 v6.2: 中/大杯价格来自接口 priceMedium/priceLarge，前端仅展示不推导 -->
          <div v-if="showCupSize" class="option-group">
            <label>杯型</label>
            <div class="option-buttons grid-layout">
              <button
v-for="size in sizeOptions" :key="size.value"
                :class="{ active: customization.cupSize === size.value }"
                @click="customization.cupSize = size.value">
                <span class="opt-label">{{ size.label }}</span>
                <span v-if="size.extra > 0" class="price-tag">+¥{{ size.extra }}</span>
              </button>
            </div>
            <p v-if="sizeOptions.length === 1" class="option-hint">💡 本品仅限{{ sizeOptions[0].label }}</p>
          </div>

          <!-- 糖度 v6.2: NO_SUGAR_ONLY 不显示糖度行；NO_ADDED_SUGAR = 不另外加糖 -->
          <div v-if="showSugar" class="option-group">
            <label>糖度</label>
            <div class="option-buttons grid-layout">
              <button
v-for="sugar in sugarOptions" :key="sugar.value"
                :class="{ active: customization.sugarLevel === sugar.value }"
                @click="customization.sugarLevel = sugar.value">
                <span class="opt-label">{{ sugar.label }}</span>
              </button>
            </div>
          </div>

          <!-- 温度 (免费) v6.1: 支持禁用状态 -->
          <div v-if="showTemp" class="option-group">
            <label>温度</label>
            <div class="option-buttons grid-layout">
              <button
v-for="temp in temperatures" :key="temp.value"
                :class="{
                  active: customization.temperature === temp.value,
                  disabled: temp.disabled
                }"
                :disabled="temp.disabled"
                @click="!temp.disabled && (customization.temperature = temp.value)">
                <span class="opt-label">{{ temp.label }}</span>
                <span v-if="temp.disabled" class="disabled-hint">不可选</span>
              </button>
            </div>
            <p v-if="tempConfig.hint" class="option-hint">💡 {{ tempConfig.hint }}</p>
          </div>

          <!-- V2 加料组 (P2-4): MILK/SHOT/SYRUP/OTHER，按 price_delta 展示，前端只提交 code -->
          <div v-for="group in addonGroups" :key="group.category" class="option-group">
            <label>{{ groupLabel(group.category) }}</label>
            <div class="option-buttons grid-layout">
              <button
v-for="item in group.items" :key="item.code"
                :class="{ active: isGroupSelected(group, item.code) }"
                @click="toggleGroup(group, item.code)">
                <span class="opt-label">{{ item.name }}</span>
                <span v-if="Number(item.priceDelta) > 0" class="price-tag">+¥{{ Number(item.priceDelta) }}</span>
                <span v-if="group.category === 'SHOT' && item.code === 'EXTRA_SHOT' && userHasShotCoupon" class="coupon-badge">🎁 免费券</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部固定栏 (唯一动态价格展示) -->
      <div class="panel-footer-fixed">
        <div class="footer-content">
          <div class="quantity-wrapper">
             <button :disabled="quantity <= 1" @click="quantity > 1 && quantity--">-</button>
             <span>{{ quantity }}</span>
             <button @click="quantity < 99 && quantity++">+</button>
          </div>
          <div class="price-summary">
            <span class="label">Total</span>
            <span class="amount">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
          <button class="add-cart-btn" @click="addToCart">加入购物车</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { getUserCoupons } from '@/api/mall'
import { getImageUrl, handleImageError } from '@/utils/image'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'add-to-cart'])

const quantity = ref(1)
const customization = reactive({
  cupSize: '',
  sugarLevel: '',
  temperature: 'HOT'
})

// v5.3: 检查是否持有加浓缩券
const userHasShotCoupon = ref(false)

// 分类判断 helper
const isBakery = computed(() => props.product.category === 'bakery')

// ==================== V2 加料组（P2-4）：消费 addonGroups ====================

const addonGroups = computed(() => props.product.addonGroups || [])

const GROUP_LABELS = { MILK: '基底', SHOT: '浓度', SYRUP: '风味', OTHER: '其他' }
const groupLabel = (category) => GROUP_LABELS[category] || category

// { category: code(SINGLE) | [codes](MULTI) }
const selection = reactive({})

function isGroupSelected(group, code) {
  const sel = selection[group.category]
  return group.selectionMode === 'MULTI'
    ? (Array.isArray(sel) && sel.includes(code))
    : sel === code
}

function toggleGroup(group, code) {
  if (group.selectionMode === 'MULTI') {
    const cur = selection[group.category] || []
    selection[group.category] = cur.includes(code) ? cur.filter(c => c !== code) : [...cur, code]
  } else {
    selection[group.category] = selection[group.category] === code
      ? (group.minSelect > 0 ? code : '')
      : code
  }
}

function resetAddons() {
  Object.keys(selection).forEach(k => delete selection[k])
  addonGroups.value.forEach(group => {
    if (group.selectionMode === 'SINGLE' && group.minSelect > 0) {
      const def = group.items.find(i => i.isDefault) || group.items[0]
      selection[group.category] = def?.code || ''
    }
  })
}

const selectedAddons = computed(() => {
  const items = []
  addonGroups.value.forEach(group => {
    const sel = selection[group.category]
    const codes = group.selectionMode === 'MULTI'
      ? (Array.isArray(sel) ? sel : [])
      : (sel ? [sel] : [])
    codes.forEach(code => {
      const item = group.items.find(i => i.code === code)
      if (item) items.push(item)
    })
  })
  return items
})

const addonFee = computed(() => selectedAddons.value.reduce((sum, i) => sum + Number(i.priceDelta || 0), 0))
const addons = computed(() => selectedAddons.value.map(i => ({ code: i.code })))

// 除 MILK/SHOT 外的加料（SYRUP / OTHER 等）：购物车展示摘要 + 金额
const extraAddons = computed(() => selectedAddons.value.filter(i => {
  const group = addonGroups.value.find(g => g.items.some(it => it.code === i.code))
  return group && group.category !== 'MILK' && group.category !== 'SHOT'
}))
const otherExtra = computed(() => extraAddons.value.reduce((sum, i) => sum + Number(i.priceDelta || 0), 0))
const addonText = computed(() => extraAddons.value.map(i => i.name).join(' · '))

// 向后兼容字段（购物车去重/展示/券逻辑）：从加料选择派生
const coffeeStrength = computed(() => selection['SHOT'] === 'EXTRA_SHOT' ? 'STRONG' : 'NORMAL')
const milkType = computed(() => {
  const milk = selection['MILK']
  return ({ WHOLE_MILK: 'WHOLE', OAT_MILK: 'OAT', COCONUT_MILK: 'COCONUT', SOY_MILK: 'SOY' })[milk] || 'WHOLE'
})

onMounted(async () => {
  try {
    const res = await getUserCoupons('ISSUED')
    if (res.success && res.data) {
      userHasShotCoupon.value = res.data.some(c => c.couponType === 'SHOT')
    }
  } catch (e) {
    console.warn('Check coupons failed in customizer', e)
  }
})

// ==================== 规格配置（杯型/糖度/温度） ====================

// 杯型：中/大杯价来自接口 priceMedium/priceLarge，前端仅展示（V2 禁止硬编码 +3）
const sizeOptions = computed(() => {
  if (isBakery.value) return [{ value: 'STANDARD', label: '标准杯', base: Number(props.product.price || 0), extra: 0 }]
  const type = props.product.sizeType || 'MEDIUM_LARGE'
  if (type === 'DEFAULT') return [{ value: 'STANDARD', label: '标准杯', base: Number(props.product.price || 0), extra: 0 }]
  const medium = Number(props.product.priceMedium || 0)
  const large = Number(props.product.priceLarge || 0)
  return [
    { value: 'MEDIUM', label: '中杯', base: medium, extra: 0 },
    { value: 'LARGE', label: '大杯', base: large, extra: large - medium }
  ]
})

const showCupSize = computed(() => !isBakery.value)

// 糖度：NO_SUGAR_ONLY 无糖度行；NO_ADDED_SUGAR = 不另外加糖
const sugarOptions = computed(() => {
  if (isBakery.value) return []
  const type = props.product.sugarType || 'FREE_CHOICE'
  if (type === 'NO_SUGAR_ONLY') return []
  const values = [
    { value: 'STANDARD', label: '标准糖' },
    { value: 'LESS', label: '少糖' },
    { value: 'HALF', label: '半糖' }
  ]
  if (type !== 'MIN_LESS_SWEET') values.push({ value: 'NO_ADDED_SUGAR', label: '不另外加糖' })
  return values
})

const showSugar = computed(() => !isBakery.value && sugarOptions.value.length > 0)

// 温度（P1D 已同步 HOT_COLD/COLD_ONLY/HOT_ONLY）
const tempConfig = computed(() => {
  const tempType = props.product.tempType || 'HOT_COLD'
  switch (tempType) {
    case 'COLD_ONLY':
      return {
        show: true,
        options: [{ label: '冰', value: 'COLD', disabled: false }, { label: '热', value: 'HOT', disabled: true }],
        defaultValue: 'COLD',
        hint: '本品仅供冰饮'
      }
    case 'HOT_ONLY':
      return {
        show: true,
        options: [{ label: '冰', value: 'COLD', disabled: true }, { label: '热', value: 'HOT', disabled: false }],
        defaultValue: 'HOT',
        hint: '本品仅供热饮'
      }
    case 'HOT_COLD':
    default:
      return {
        show: true,
        options: [{ label: '冰', value: 'COLD', disabled: false }, { label: '热', value: 'HOT', disabled: false }],
        defaultValue: 'HOT'
      }
  }
})
const showTemp = computed(() => !isBakery.value && props.product.category !== 'addon' && tempConfig.value.show)
const temperatures = computed(() => tempConfig.value.options)

// ==================== 初始化（每次打开重挂载） ====================

function resetForm() {
  resetAddons()
  customization.cupSize = sizeOptions.value[0]?.value || ''
  customization.temperature = tempConfig.value.defaultValue || 'HOT'
  const defSugar = props.product.defaultSugarLevel
  customization.sugarLevel = sugarOptions.value.some(o => o.value === defSugar)
    ? defSugar
    : (sugarOptions.value[0]?.value || '')
}

watch(() => props.product, (product) => {
  if (!product) return
  resetForm()
}, { immediate: true })

// ==================== 价格展示（前端仅估算，后端权威） ====================

const basePrice = computed(() => {
  const opt = sizeOptions.value.find(o => o.value === customization.cupSize)
  return opt ? opt.base : Number(props.product.price || 0)
})

const strengthExtra = computed(() => {
  const shot = selectedAddons.value.find(i => i.code === 'EXTRA_SHOT')
  return shot ? Number(shot.priceDelta || 0) : 0
})

const milkExtra = computed(() => {
  const milk = selection['MILK']
  if (!milk) return 0
  const group = addonGroups.value.find(g => g.category === 'MILK')
  const item = group?.items.find(i => i.code === milk)
  return item ? Number(item.priceDelta || 0) : 0
})

const unitPrice = computed(() => Number((basePrice.value + addonFee.value).toFixed(2)))
const totalPrice = computed(() => Number((unitPrice.value * quantity.value).toFixed(2)))

const addToCart = () => {
  // 构建 payload
  const payload = {
    productId: props.product.id,
    productName: props.product.name,
    productImage: props.product.imageUrl,
    unitPrice: unitPrice.value,
    basePrice: basePrice.value,
    quantity: quantity.value,
    category: props.product.category, // 传递分类方便后续判断
    isNewProduct: props.product.isNewProduct || false, // v5.3.4: 新品标记，用于新品券验证
    extraPrices: {
      cup: 0,
      strength: strengthExtra.value,
      milk: milkExtra.value,
      other: otherExtra.value
    }
  }

  // 甜品不传递饮品参数，后端 SKU 验证已支持 null 值
  if (!isBakery.value) {
    if (showCupSize.value) payload.cupSize = customization.cupSize
    if (showSugar.value) payload.sugarLevel = customization.sugarLevel
    if (showTemp.value) payload.temperature = customization.temperature
    payload.coffeeStrength = coffeeStrength.value
    payload.milkType = milkType.value
    // V2：真实加料码（不含默认项，后端 price_delta 权威定价 + 自动注入必选默认项）
    payload.addons = addons.value
    if (addonText.value) payload.addonText = addonText.value
  }

  emit('add-to-cart', payload)
  emit('close')
}
</script>

<style scoped>
.customizer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.customizer-panel {
  background: #FCFAF8;
  border-radius: 24px;
  width: 90%;
  max-width: 440px;
  max-height: 85vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 60px rgba(93, 64, 55, 0.25);
  position: relative;
}

/* 顶部 Banner */
.product-banner {
  position: relative;
  width: 100%;
  height: 240px;
  flex-shrink: 0;
}

.product-banner img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.close-btn-float {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
  color: #fff;
  border: none;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn-float:hover {
  background: rgba(0, 0, 0, 0.5);
  transform: rotate(90deg);
}

/* 内容主体 */
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 24px 100px; /* Leave space for footer */
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.panel-body::-webkit-scrollbar {
  display: none;
}

.product-header {
  margin-bottom: 24px;
}

.product-header h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #3E2723;
  line-height: 1.2;
}

.product-desc-text {
  margin: 8px 0 0;
  font-size: 13px;
  color: #8D6E63;
  line-height: 1.5;
}

.serving-desc-text {
  margin: 8px 0 0;
  font-size: 12px;
  color: #A9712F;
  line-height: 1.5;
  background: rgba(169, 113, 47, 0.08);
  padding: 6px 10px;
  border-radius: 8px;
}

.options-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.option-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #5D4037;
  margin-bottom: 12px;
}

/* Button Layouts */
.option-buttons.grid-layout {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  gap: 10px;
}

.option-buttons.grid-layout-large {
  display: grid;
  grid-template-columns: 1fr 1fr; /* Two large buttons */
  gap: 12px;
}

/* Button Styles */
.option-buttons button {
  padding: 10px 8px;
  border: none;
  background: #EFEBE9; /* Unselected: Light beige */
  border-radius: 12px;
  font-size: 13px;
  color: #5D4037;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  white-space: nowrap;
}

.option-buttons button:hover {
  background: #D7CCC8;
}

/* v6.1: 禁用状态样式 */
.option-buttons button.disabled,
.option-buttons button:disabled {
  background: #F5F5F5;
  color: #BDBDBD;
  cursor: not-allowed;
  opacity: 0.6;
}

.option-buttons button.disabled:hover,
.option-buttons button:disabled:hover {
  background: #F5F5F5;
}

/* Active State */
.option-buttons button.active {
  background: #5D4037; /* Dark Coffee Brown */
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(93, 64, 55, 0.2);
}

/* 禁用状态不覆盖选中状态（如果商品默认值被禁用，这种情况不应该发生但做容错处理） */
.option-buttons button.active.disabled {
  background: #9E9E9E;
  box-shadow: none;
}

/* 溢价标签样式 */
.price-tag {
  font-size: 11px;
  padding: 1px 4px;
  border-radius: 4px;
  background: rgba(216, 67, 21, 0.1); /* 浅橙色背景 */
  color: #D84315; /* 醒目橙色字 */
  font-weight: 700;
}

.option-buttons button.active .price-tag {
  background: rgba(255, 255, 255, 0.2); /* 选中时改为白色半透明 */
  color: #FFCCBC; /* 浅色字 */
}

/* v6.1: 禁用提示标签 */
.disabled-hint {
  font-size: 10px;
  padding: 2px 4px;
  border-radius: 3px;
  background: rgba(189, 189, 189, 0.2);
  color: #9E9E9E;
  font-weight: 500;
}

/* v6.1: 选项提示文字 */
.option-hint {
  font-size: 12px;
  color: #795548;
  margin-top: 8px;
  margin-bottom: 0;
  padding: 6px 10px;
  background: rgba(255, 160, 0, 0.08);
  border-radius: 6px;
  border-left: 3px solid #FFA000;
}

/* 底部固定栏 */
.panel-footer-fixed {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(180deg, rgba(93, 64, 55, 0.95) 0%, #3E2723 100%);
  padding: 16px 24px;
  z-index: 10;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.1);
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

/* 数量控制器 */
.quantity-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.1);
  padding: 4px;
  border-radius: 20px;
}

.quantity-wrapper button {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: #FFF;
  color: #5D4037;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.quantity-wrapper button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quantity-wrapper span {
  color: #FFF;
  font-weight: 600;
  min-width: 20px;
  text-align: center;
}

.price-summary {
  display: flex;
  flex-direction: column;
  margin-left: auto;
  align-items: flex-end;
  margin-right: 12px;
}

.price-summary .label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.price-summary .amount {
  font-size: 20px;
  font-weight: 700;
  color: #FFF;
  line-height: 1;
}

.add-cart-btn {
  background: linear-gradient(135deg, #FFB74D 0%, #FF9800 100%); /* 暖橙色渐变 */
  color: #FFF; /* 白色文字 */
  border: none;
  border-radius: 12px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.1s;
  box-shadow: 0 4px 12px rgba(255, 152, 0, 0.3);
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}


.add-cart-btn:hover {
  transform: scale(1.03);
  filter: brightness(1.05);
}

/* 免费券提示标签 */
.coupon-badge {
  font-size: 10px;
  background: linear-gradient(135deg, #AB47BC 0%, #7B1FA2 100%);
  color: #fff;
  padding: 1px 5px;
  border-radius: 4px;
  margin-left: 4px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.8; }
  100% { opacity: 1; }
}
</style>
