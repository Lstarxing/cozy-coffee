<template>
  <div class="customizer-overlay" @click.self="$emit('close')">
    <div class="customizer-panel">
      <button class="close-btn-float" @click="$emit('close')">×</button>

      <!-- 左栏：Sticky 商品展示区（图 + 豆/拼配 metadata，克制不拉高） -->
      <div class="customizer-left">
        <img :src="getImageUrl(product.imageUrl)" :alt="product.name" @error="handleImageError">
        <div v-if="beanProfile" class="product-visual-meta">
          <p v-if="beanProfile.nameEn" class="visual-meta-en">{{ beanProfile.nameEn }}</p>
          <p class="visual-meta-name">{{ beanProfile.name }}</p>
          <p v-if="metaProfileText" class="visual-meta-profile">{{ metaProfileText }}</p>
        </div>
      </div>

      <!-- 右栏：商品描述 + 选规格 + 底部加购 -->
      <div class="panel-body">
        <!-- 商品描述 -->
        <div class="product-header">
          <h3>{{ product.name }}</h3>
          <p v-if="product.description" class="product-desc-text">{{ product.description }}</p>
          <p v-if="product.servingDesc" class="serving-desc-text">出杯：{{ product.servingDesc }}</p>
        </div>

        <div class="options-section">
          <!-- V2 出品方式（精品 Bean 必选）：手冲 / 冷萃 -->
          <div v-if="showBrewMethod" class="option-group">
            <label>出品方式</label>
            <div class="option-buttons grid-layout">
              <button
v-for="option in brewMethodOptions" :key="option.value"
                :class="{ active: customization.brewMethod === option.value }"
                @click="customization.brewMethod = option.value">
                <span class="opt-label">{{ option.label }}</span>
                <span class="price-tag">{{ option.value === 'COLD_BREW' ? `¥${product.coldBrewPrice || ''}` : `¥${product.price || ''}` }}</span>
              </button>
            </div>
          </div>

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

      <!-- 底部结算栏（整宽：左侧价格/当前规格，右侧数量 + 加购） -->
      <div class="panel-footer-fixed">
        <div class="footer-content">
          <div class="price-summary">
            <span class="amount">¥{{ totalPrice.toFixed(2) }}</span>
            <span class="spec-summary">{{ specSummary }}</span>
          </div>
          <div class="footer-actions">
            <div class="quantity-wrapper">
               <button :disabled="quantity <= 1" @click="quantity > 1 && quantity--">-</button>
               <span>{{ quantity }}</span>
               <button @click="quantity < 99 && quantity++">+</button>
            </div>
            <button class="add-cart-btn" @click="addToCart">加入购物车</button>
          </div>
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
  temperature: 'HOT',
  brewMethod: 'POUR_OVER'
})

// V2 出品方式（精品 Bean 必选）：POUR_OVER 手冲 / COLD_BREW 冷萃
const showBrewMethod = computed(() => Boolean(props.product.brewMethod))
const brewMethodOptions = computed(() => [
  { value: 'POUR_OVER', label: '手冲' },
  { value: 'COLD_BREW', label: '冷萃' }
])

// v5.3: 检查是否持有加浓缩券
const userHasShotCoupon = ref(false)

// 单一事实源：后端 isFood 信号（V2 分类为大写 BAKERY 等，不能靠小写分类硬编码匹配）
const isFood = computed(() => {
  if (props.product.isFood !== undefined) return props.product.isFood
  // 旧数据兜底：食品无糖度/温度选项（allowed 数组为空）
  const s = props.product.allowedSugars, t = props.product.allowedTemps
  return (Array.isArray(s) && s.length === 0) && (Array.isArray(t) && t.length === 0)
})

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

// 豆/拼配档案（精品豆商品 bean/blend 二选一挂接；普通商品无此数据则不显示左栏 metadata）
const beanProfile = computed(() => props.product.beanProfile || props.product.blendProfile)
const metaProfileText = computed(() => {
  const b = beanProfile.value
  if (!b) return ''
  return [b.roast, b.flavorNotes].filter(Boolean).join(' · ')
})

// 当前规格摘要（底部结算栏左侧展示）
const specSummary = computed(() => {
  const parts = []
  if (!isFood.value) {
    if (showCupSize.value) {
      const o = sizeOptions.value.find(s => s.value === customization.cupSize)
      if (o) parts.push(o.label)
    }
    if (sugarOptions.value.length > 1) {
      const o = sugarOptions.value.find(s => s.value === customization.sugarLevel)
      if (o) parts.push(o.label)
    }
    if (enabledTempCount.value > 1) {
      const t = temperatures.value.find(o => !o.disabled && o.value === customization.temperature)
      if (t) parts.push(t.label)
    }
    parts.push({ WHOLE: '全脂奶', OAT: '燕麦奶', COCONUT: '椰奶', SOY: '豆奶' }[milkType.value] || milkType.value)
  }
  extraAddons.value.forEach(a => parts.push(a.name))
  return parts.length ? parts.join(' · ') : '含所选规格'
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

// 单一事实源：allowedSizes/allowedSugars/allowedTemps 由后端按 sizeType/sugarType/tempType 规范值下发
const sizeOptions = computed(() => {
  const allowed = props.product.allowedSizes || []
  const medium = Number(props.product.priceMedium || 0)
  const large = Number(props.product.priceLarge || 0)
  const labels = { STANDARD: '标准杯', MEDIUM: '中杯', LARGE: '大杯', SMALL: '小杯', EXTRA_LARGE: '超大杯' }
  return allowed.map(v => {
    const base = { MEDIUM: medium, LARGE: large }[v] ?? Number(props.product.price || 0)
    return { value: v, label: labels[v] || v, base, extra: v === 'LARGE' ? large - medium : 0 }
  })
})

const showCupSize = computed(() => sizeOptions.value.length > 1)

const sugarOptions = computed(() => {
  const allowed = props.product.allowedSugars || []
  const labels = { STANDARD: '标准糖', LESS: '少糖', HALF: '半糖', NONE: '无糖', NO_ADDED_SUGAR: '不另外加糖' }
  return allowed.map(v => ({ value: v, label: labels[v] || v }))
})

const showSugar = computed(() => sugarOptions.value.length > 1)

// 温度（精品 Bean 冷萃固定冰饮覆盖；其余由后端 allowedTemps 驱动）
const tempConfig = computed(() => {
  if (showBrewMethod.value && customization.brewMethod === 'COLD_BREW') {
    return {
      show: true,
      options: [{ label: '冰', value: 'COLD', disabled: false }, { label: '热', value: 'HOT', disabled: true }],
      defaultValue: 'COLD',
      hint: '冷萃仅限冰饮'
    }
  }
  const allowed = props.product.allowedTemps || []
  if (allowed.length === 0) return { show: false, options: [], defaultValue: 'HOT', hint: '' }
  const hotAllowed = allowed.includes('HOT')
  const coldAllowed = allowed.includes('COLD')
  return {
    show: true,
    options: [
      { label: '热', value: 'HOT', disabled: !hotAllowed },
      { label: '冰', value: 'COLD', disabled: !coldAllowed }
    ],
    defaultValue: coldAllowed && !hotAllowed ? 'COLD' : 'HOT',
    hint: !hotAllowed ? '本品仅供冰饮' : (!coldAllowed ? '本品仅供热饮' : '')
  }
})
// 对齐移动端：温度行仅在可用选项 > 1 时展示（固定热/冰/冷萃隐藏整行）
const enabledTempCount = computed(() => tempConfig.value.options.filter(o => !o.disabled).length)
const showTemp = computed(() => enabledTempCount.value > 1)
const temperatures = computed(() => tempConfig.value.options)

// ==================== 初始化（每次打开重挂载） ====================

function resetForm() {
  resetAddons()
  customization.brewMethod = 'POUR_OVER'
  customization.cupSize = sizeOptions.value[0]?.value || ''
  customization.temperature = tempConfig.value.defaultValue || 'HOT'
  const defSugar = props.product.defaultSugarLevel
  customization.sugarLevel = sugarOptions.value.some(o => o.value === defSugar)
    ? defSugar
    : (sugarOptions.value[0]?.value || '')
}

// 冷萃固定冰饮
watch(() => customization.brewMethod, (v) => {
  if (v === 'COLD_BREW') customization.temperature = 'COLD'
})

watch(() => props.product, (product) => {
  if (!product) return
  resetForm()
}, { immediate: true })

// ==================== 价格展示（前端仅估算，后端权威） ====================

const basePrice = computed(() => {
  // 精品 Bean：POUR_OVER 用 price / COLD_BREW 用 cold_brew_price
  if (showBrewMethod.value) {
    return customization.brewMethod === 'COLD_BREW'
      ? Number(props.product.coldBrewPrice || 0)
      : Number(props.product.price || 0)
  }
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
  // 显示阈值对齐移动端（选项 >1 才显示行）；payload 只要有选项就传（与改动前行为一致，单杯型/固定温度仍回传）
  if (!isFood.value) {
    if (sizeOptions.value.length > 0) payload.cupSize = customization.cupSize
    if (sugarOptions.value.length > 0) payload.sugarLevel = customization.sugarLevel
    if (enabledTempCount.value > 0) payload.temperature = customization.temperature
    if (showBrewMethod.value) payload.brewMethod = customization.brewMethod
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
  border-radius: 20px;
  width: min(900px, calc(100vw - 64px));
  max-height: min(680px, calc(100vh - 64px));
  overflow: hidden;
  display: grid;
  grid-template-columns: 320px 1fr;
  grid-template-rows: minmax(0, 1fr) auto;
  column-gap: 44px;
  padding: 32px 32px 0;
  box-shadow: 0 24px 60px rgba(93, 64, 55, 0.25);
  position: relative;
}

/* 左栏：完整视觉区（图放大到 320 + 少量 bean/blend 信息，整组在主体内垂直居中） */
.customizer-left {
  grid-column: 1;
  grid-row: 1;
  align-self: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.customizer-left img {
  width: 320px;
  height: 320px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #EEE;
  background: #fff;
  object-fit: cover;
  display: block;
}

/* 左栏豆/拼配档案（克制 2–3 行，随图一起垂直居中） */
.product-visual-meta {
  width: 320px;
  text-align: center;
}

.visual-meta-en {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #443A35;
}

.visual-meta-name {
  margin: 4px 0 0;
  font-size: 12px;
  color: #9E948E;
}

.visual-meta-profile {
  margin: 8px 0 0;
  font-size: 12px;
  color: #756A63;
  line-height: 1.6;
}

/* 关闭按钮（面板右上角浮动） */
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
  z-index: 5;
}

.close-btn-float:hover {
  background: rgba(0, 0, 0, 0.5);
  transform: rotate(90deg);
}

/* 内容主体（右栏，独立滚动；间距由 panel padding 承担） */
.panel-body {
  grid-column: 2;
  grid-row: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 8px;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.panel-body::-webkit-scrollbar {
  display: none;
}

/* 商品描述（右栏顶部，标题层级重建；右上角给关闭按钮留位） */
.product-header {
  margin-bottom: 28px;
  padding-right: 36px;
}

.product-header h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #2B1E16;
  line-height: 1.2;
}

.product-desc-text {
  margin: 12px 0 0;
  font-size: 13px;
  color: #777;
  line-height: 1.7;
}

.serving-desc-text {
  margin: 10px 0 0;
  font-size: 12px;
  color: #A9712F;
  line-height: 1.6;
}

.options-section {
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.option-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #443A35;
  margin-bottom: 12px;
}

/* Choice Cards — 弹性换行、等高手势区；未选中无边框，靠浅底区分 */
.option-buttons.grid-layout {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.option-buttons button {
  min-height: 44px;
  padding: 8px 18px;
  border: 1px solid transparent;
  background: #F7F5F2;
  border-radius: 10px;
  font-size: 13px;
  color: #443A35;
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
  background: #F0EBE5;
}

/* v6.1: 禁用状态样式 */
.option-buttons button.disabled,
.option-buttons button:disabled {
  background: #F7F5F2;
  color: #9E948E;
  cursor: not-allowed;
  opacity: 0.45;
}

.option-buttons button.disabled:hover,
.option-buttons button:disabled:hover {
  background: #F7F5F2;
}

/* Active State — 选中态：暖白底 + 主色描边 + 主色字 */
.option-buttons button.active {
  background: #FFF9F5;
  border-color: #8A4528;
  color: #74371F;
  font-weight: 600;
}

/* 禁用状态不覆盖选中状态（如果商品默认值被禁用，这种情况不应该发生但做容错处理） */
.option-buttons button.active.disabled {
  border-color: transparent;
  background: #F7F5F2;
}

/* 溢价标签样式 — 次要色文字，选中时转主色 */
.price-tag {
  font-size: 11px;
  color: #756A63;
  font-weight: 500;
}

.option-buttons button.active .price-tag {
  color: #74371F;
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

/* 底部结算栏（整宽 + 分隔线；左侧价格/当前规格，右侧数量 + 加购） */
.panel-footer-fixed {
  grid-column: 1 / -1;
  grid-row: 2;
  margin: 28px -32px 0; /* 上间距 + 两侧出血：分隔线横跨整宽 */
  padding: 18px 32px 28px;
  border-top: 1px solid #EEEAE6;
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 数量控制器 — 对齐移动端（描边 + 墨字） */
.quantity-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #E3DED8;
  border-radius: 10px;
  padding: 4px;
  background: #fff;
}

.quantity-wrapper button {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: #F7F5F3;
  color: #2B1E16;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.quantity-wrapper button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.quantity-wrapper span {
  color: #2B1E16;
  font-weight: 600;
  min-width: 20px;
  text-align: center;
}

.price-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.price-summary .amount {
  font-size: 28px;
  font-weight: 700;
  color: #2B1E16;
  line-height: 1;
}

.price-summary .spec-summary {
  font-size: 12px;
  color: #756A63;
}

.add-cart-btn {
  width: 144px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #753A22;
  color: #FFF;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(117, 58, 34, 0.25);
}

.add-cart-btn:hover {
  background: #5E2E1B;
  transform: scale(1.02);
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
