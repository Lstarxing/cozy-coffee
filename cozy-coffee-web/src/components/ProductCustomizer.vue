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
        </div>

        <div class="options-section">
          <!-- 杯型 v6.1: 支持禁用状态 -->
          <div v-if="showCupSize" class="option-group">
            <label>杯型</label>
            <div class="option-buttons grid-layout">
              <button
v-for="size in cupSizes" :key="size.value"
                :class="{ 
                  active: customization.cupSize === size.value,
                  disabled: size.disabled 
                }"
                :disabled="size.disabled"
                @click="!size.disabled && (customization.cupSize = size.value)">
                <span class="opt-label">{{ size.label }}</span>
                <span v-if="size.extraPrice > 0 && !size.disabled" class="price-tag">+¥{{ size.extraPrice }}</span>
                <span v-if="size.disabled" class="disabled-hint">不可选</span>
              </button>
            </div>
            <p v-if="sizeConfig.hint" class="option-hint">💡 {{ sizeConfig.hint }}</p>
          </div>

          <!-- 糖度 (免费) v6.1: 支持禁用状态 -->
          <div v-if="showSugar" class="option-group">
            <label>糖度</label>
            <div class="option-buttons grid-layout">
              <button
v-for="sugar in sugarLevels" :key="sugar.value"
                :class="{ 
                  active: customization.sugarLevel === sugar.value,
                  disabled: sugar.disabled 
                }"
                :disabled="sugar.disabled"
                @click="!sugar.disabled && (customization.sugarLevel = sugar.value)">
                <span class="opt-label">{{ sugar.label }}</span>
                <span v-if="sugar.disabled" class="disabled-hint">不可选</span>
              </button>
            </div>
            <p v-if="sugarConfig.hint" class="option-hint">💡 {{ sugarConfig.hint }}</p>
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

          <!-- 浓度 (含付费加浓) -->
          <div v-if="showStrength" class="option-group">
            <label>浓度</label>
            <div class="option-buttons grid-layout-large"> <!-- 使用大按钮布局 -->
              <button
v-for="strength in coffeeStrengths" :key="strength.value"
                :class="{ active: customization.coffeeStrength === strength.value }"
                @click="customization.coffeeStrength = strength.value">
                <span class="opt-label">{{ strength.label }}</span>
                <span v-if="strength.extraPrice > 0" class="price-tag">+¥{{ strength.extraPrice }}</span>
                <!-- v5.3: 免费券提示 -->
                <span v-if="strength.value === 'STRONG' && userHasShotCoupon" class="coupon-badge">🎁 免费券</span>
              </button>
            </div>
          </div>

          <!-- 基底/奶类 (含付费选项) -->
          <div v-if="showMilk" class="option-group">
            <label>基底</label>
            <div class="option-buttons grid-layout">
              <button
v-for="milk in milkOptions" :key="milk.value"
                :class="{ active: customization.milkType === milk.value }"
                @click="customization.milkType = milk.value">
                <span class="opt-label">{{ milk.label }}</span>
                <span v-if="milk.extraPrice > 0" class="price-tag">+¥{{ milk.extraPrice }}</span>
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
  cupSize: 'STANDARD',
  sugarLevel: 'STANDARD',
  temperature: 'HOT',
  coffeeStrength: 'NORMAL',
  milkType: 'WHOLE'
})

// v5.3: 检查是否持有加浓缩券
const userHasShotCoupon = ref(false)

// v5.3: 根据 SKU 配置初始化默认值
watch(() => props.product, (product) => {
  if (!product) return
  
  // 杯型默认值
  const sizeType = product.sizeType || 'MEDIUM_LARGE'
  if (sizeType === 'DEFAULT') {
    customization.cupSize = 'STANDARD'
  }
  
  // 甜度默认值
  const sugarType = product.sugarType || 'FREE_CHOICE'
  if (sugarType === 'NO_SUGAR_ONLY') {
    customization.sugarLevel = 'NONE'
  } else if (sugarType === 'MIN_LESS_SWEET') {
    customization.sugarLevel = 'STANDARD'
  }
  
  // 温度默认值
  const tempType = product.tempType || 'HOT_COLD'
  if (tempType === 'COLD_ONLY') {
    customization.temperature = 'COLD'
  } else if (tempType === 'HOT_ONLY') {
    customization.temperature = 'HOT'
  }
}, { immediate: true })

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

// 分类判断 helper
const isBakery = computed(() => props.product.category === 'bakery')
const isEspresso = computed(() => props.product.category === 'espresso')
const isSignature = computed(() => props.product.category === 'signature')

// ==================== v5.3: 基于后端 SKU 配置动态渲染 ====================

// 杯型配置解析（v6.1: 改为禁用模式）
const sizeConfig = computed(() => {
  const sizeType = props.product.sizeType || 'MEDIUM_LARGE'
  switch (sizeType) {
    case 'DEFAULT':
      // 固定标准杯：显示选项，但禁用其他杯型
      return { 
        show: true, 
        options: [
          { label: '标准杯', value: 'STANDARD', extraPrice: 0, disabled: false },
          { label: '大杯', value: 'LARGE', extraPrice: 3, disabled: true }
        ],
        defaultValue: 'STANDARD',
        hint: '本品仅限标准杯'
      }
    case 'MEDIUM_LARGE':
      return { 
        show: true, 
        options: [
          { label: '标准杯', value: 'STANDARD', extraPrice: 0, disabled: false },
          { label: '大杯', value: 'LARGE', extraPrice: 3, disabled: false }
        ],
        defaultValue: 'STANDARD'
      }
    case 'ALL_SIZES':
      return { 
        show: true, 
        options: [
          { label: '中杯', value: 'MEDIUM', extraPrice: 0, disabled: false },
          { label: '大杯', value: 'LARGE', extraPrice: 3, disabled: false },
          { label: '超大杯', value: 'EXTRA_LARGE', extraPrice: 5, disabled: false }
        ],
        defaultValue: 'MEDIUM'
      }
    default:
      return { 
        show: true, 
        options: [
          { label: '标准杯', value: 'STANDARD', extraPrice: 0, disabled: false }
        ], 
        defaultValue: 'STANDARD' 
      }
  }
})

// 甜度配置解析（v6.1: 改为禁用模式）
const sugarConfig = computed(() => {
  const sugarType = props.product.sugarType || 'FREE_CHOICE'
  switch (sugarType) {
    case 'NO_SUGAR_ONLY':
      // 固定无糖：显示选项，但禁用其他甜度
      return { 
        show: true, 
        options: [
          { label: '全糖', value: 'STANDARD', disabled: true },
          { label: '少糖', value: 'LESS', disabled: true },
          { label: '半糖', value: 'HALF', disabled: true },
          { label: '无糖', value: 'NONE', disabled: false }
        ],
        defaultValue: 'NONE', 
        hint: '本品不加糖' 
      }
    case 'MIN_LESS_SWEET':
      // 最低少甜：显示选项，但禁用无糖
      // v5.3.1: 规范化为 STANDARD/LESS/HALF
      return { 
        show: true, 
        options: [
          { label: '全糖', value: 'STANDARD', disabled: false },
          { label: '少糖', value: 'LESS', disabled: false },
          { label: '半糖', value: 'HALF', disabled: false },
          { label: '无糖', value: 'NONE', disabled: true }
        ],
        defaultValue: 'STANDARD',
        hint: '含糖浆/酱料，不可完全去糖'
      }
    case 'FREE_CHOICE':
    default:
      // v5.3.1: 规范化为 STANDARD/LESS/HALF/NONE (4个标准选项)
      return { 
        show: true, 
        options: [
          { label: '全糖', value: 'STANDARD', disabled: false },
          { label: '少糖', value: 'LESS', disabled: false },
          { label: '半糖', value: 'HALF', disabled: false },
          { label: '无糖', value: 'NONE', disabled: false }
        ],
        defaultValue: 'STANDARD'
      }
  }
})

// 温度配置解析（v6.1: 改为禁用模式，保留选项但标记为不可选）
const tempConfig = computed(() => {
  const tempType = props.product.tempType || 'HOT_COLD'
  switch (tempType) {
    case 'COLD_ONLY':
      // 仅限冰：显示选项，但禁用热选项
      return { 
        show: true, 
        options: [
          { label: '冰', value: 'COLD', disabled: false },
          { label: '热', value: 'HOT', disabled: true }
        ],
        defaultValue: 'COLD', 
        hint: '本品仅供冰饮' 
      }
    case 'HOT_ONLY':
      // 仅限热：显示选项，但禁用冰选项
      return {
        show: true,
        options: [
          { label: '冰', value: 'COLD', disabled: true },
          { label: '热', value: 'HOT', disabled: false }
        ],
        defaultValue: 'HOT',
        hint: '本品仅供热饮'
      }
    case 'HOT_COLD':
    default:
      return { 
        show: true, 
        options: [
          { label: '冰', value: 'COLD', disabled: false },
          { label: '热', value: 'HOT', disabled: false }
        ],
        defaultValue: 'HOT'
      }
  }
})

// 选项可见性控制（基于 SKU 配置）
const showCupSize = computed(() => {
  if (isBakery.value) return false
  return sizeConfig.value.show
})
const showSugar = computed(() => {
  if (isBakery.value) return false
  return sugarConfig.value.show
})
const showTemp = computed(() => {
  if (isBakery.value) return false
  if (props.product.category === 'addon') return false
  return tempConfig.value.show
})
const showStrength = computed(() => isEspresso.value) // 只有意式咖啡显示浓度
const showMilk = computed(() => {
  if (!isEspresso.value) return false
  // v5.3 修复：美式、特调、澳白、Dirty 等不支持换奶，或者已经有固定奶底的产品
  const noMilkChange = ['美式', 'Americano', '卡布奇诺', '摩卡', '焦糖玛奇朵', '脏咖', 'Dirty', '澳白', 'Flat White', '生椰拿铁', '燕麦拿铁', 'SOE', '手冲']
  return !noMilkChange.some(n => props.product.name.includes(n))
})

// 动态选项数据（基于 SKU 配置）
const cupSizes = computed(() => sizeConfig.value.options)
const sugarLevels = computed(() => sugarConfig.value.options)
const temperatures = computed(() => tempConfig.value.options)

const coffeeStrengths = [
  { label: '标准', value: 'NORMAL', extraPrice: 0 },
  { label: '加浓', value: 'STRONG', extraPrice: 5 }
]

const milkOptions = [
  { label: '标准牛乳', value: 'WHOLE', extraPrice: 0 },
  { label: '换燕麦奶', value: 'OAT', extraPrice: 4 },
  { label: '换椰奶', value: 'COCONUT', extraPrice: 4 }
]

// 动态计算总价
const totalPrice = computed(() => {
  if (!props.product || props.product.price == null) {
    return 0
  }
  
  if (isBakery.value) {
    return props.product.price * quantity.value
  }

  let unit = Number(props.product.price) || 0
  
  if (showCupSize.value && cupSizes.value) {
    const selectedSize = cupSizes.value.find(s => s.value === customization.cupSize)
    if (selectedSize) unit += selectedSize.extraPrice || 0
  }

  if (showStrength.value) {
    const selectedStrength = coffeeStrengths.find(s => s.value === customization.coffeeStrength)
    if (selectedStrength) unit += selectedStrength.extraPrice || 0
  }

  if (showMilk.value) {
    const selectedMilk = milkOptions.find(m => m.value === customization.milkType)
    if (selectedMilk) unit += selectedMilk.extraPrice || 0
  }

  return unit * quantity.value
})

const addToCart = () => {
  // 构建 payload
  const payload = {
    productId: props.product.id,
    productName: props.product.name,
    productImage: props.product.imageUrl,
    unitPrice: totalPrice.value / quantity.value,
    basePrice: props.product.price,
    quantity: quantity.value,
    category: props.product.category, // 传递分类方便后续判断
    isNewProduct: props.product.isNewProduct || false, // v5.3.4: 新品标记，用于新品券验证
    extraPrices: {
      cup: 0, strength: 0, milk: 0
    }
  }

  // v5.3 修复：只传递实际可见的选项字段，避免甜品显示饮品参数、美式显示奶类
  if (!isBakery.value) {
    // 只传递可见选项的值
    if (showCupSize.value) payload.cupSize = customization.cupSize
    if (showSugar.value) payload.sugarLevel = customization.sugarLevel
    if (showTemp.value) payload.temperature = customization.temperature
    if (showStrength.value) payload.coffeeStrength = customization.coffeeStrength
    if (showMilk.value) payload.milkType = customization.milkType
    
    // 计算 extraPrices 用于展示
    if (showCupSize.value && cupSizes.value) payload.extraPrices.cup = cupSizes.value.find(s => s.value === customization.cupSize)?.extraPrice || 0
    if (showStrength.value) payload.extraPrices.strength = coffeeStrengths.find(s => s.value === customization.coffeeStrength)?.extraPrice || 0
    if (showMilk.value) payload.extraPrices.milk = milkOptions.find(m => m.value === customization.milkType)?.extraPrice || 0
    
    // v5.3.2: 构建 addonsJson 用于后端计算加料费用
    const addons = []
    
    // 加浓缩费用 (coffeeStrength: STRONG)
    if (showStrength.value && customization.coffeeStrength === 'STRONG') {
      const strengthOption = coffeeStrengths.find(s => s.value === 'STRONG')
      if (strengthOption && strengthOption.extraPrice > 0) {
        addons.push({
          name: '加浓缩',
          price: strengthOption.extraPrice
        })
      }
    }
    
    // 如果有加料，序列化为 JSON 字符串
    if (addons.length > 0) {
      payload.addonsJson = JSON.stringify(addons)
    }
  }
  // v5.3: 甜品不传递饮品参数，后端 SKU 验证已支持 null 值

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
