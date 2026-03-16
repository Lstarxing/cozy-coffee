<template>
  <div class="cart-sidebar" :class="{ open: isOpen }">
    <div class="cart-header">
      <h3><ShoppingBag :size="20" :stroke-width="2" /> 购物车 ({{ cartItems.length }})</h3>
      <button class="close-btn" @click="$emit('close')">
        <X :size="20" :stroke-width="2" />
      </button>
    </div>

    <div class="cart-body">
      <div v-if="cartItems.length === 0" class="empty-cart">
        <Coffee :size="48" :stroke-width="1.5" class="empty-icon" />
        <p>购物车是空的</p>
        <span>快去选购咖啡吧~</span>
      </div>

      <template v-else>
        <!-- 商品列表 -->
        <div class="cart-items">
          <div v-for="(item, index) in cartItems" :key="index" class="cart-item">
            <img :src="getImageUrl(item.productImage)" @error="handleImageError" :alt="item.productName">
            <div class="item-details">
              <h4>{{ item.productName }}</h4>
              <div class="item-specs" v-if="item.category !== 'bakery'">
                <span>{{ getCupSizeLabel(item.cupSize) }}</span>
                <span>{{ getSugarLabel(item.sugarLevel) }}</span>
                <span>{{ getTempLabel(item.temperature) }}</span>
                <span>{{ getStrengthLabel(item.coffeeStrength) }}</span>
                <span v-if="item.milkType && item.milkType !== 'WHOLE'">{{ getMilkLabel(item.milkType) }}</span>
              </div>
              <div class="item-price">¥{{ item.unitPrice }}</div>
            </div>
            <div class="item-actions">
              <div class="quantity-ctrl">
                <button @click="updateQuantity(index, item.quantity - 1)">
                  <Minus :size="14" :stroke-width="2.5" />
                </button>
                <span>{{ item.quantity }}</span>
                <button @click="updateQuantity(index, item.quantity + 1)">
                  <Plus :size="14" :stroke-width="2.5" />
                </button>
              </div>
              <button class="remove-btn" @click="removeItem(index)">
                <Trash2 :size="14" :stroke-width="2" /> 删除
              </button>
            </div>
          </div>
        </div>

        <!-- 将原来的 footer 内容移动到这里，让它们也能滚动 -->
        <div class="cart-options-area">
          <!-- v5.0 用餐方式选择 -->
          <div class="dining-method-section">
            <label class="section-label">用餐方式</label>
            <div class="dining-options">
              <label class="dining-option" :class="{ active: diningMethod === 'DINE_IN' }">
                <input type="radio" v-model="diningMethod" value="DINE_IN" />
                <UtensilsCrossed :size="18" :stroke-width="1.8" />
                <span>堂食</span>
              </label>
              <label class="dining-option" :class="{ active: diningMethod === 'TAKEOUT' }">
                <input type="radio" v-model="diningMethod" value="TAKEOUT" />
                <CupSoda :size="18" :stroke-width="1.8" />
                <span>外带</span>
              </label>
              <label class="dining-option" :class="{ active: diningMethod === 'DELIVERY' }">
                <input type="radio" v-model="diningMethod" value="DELIVERY" />
                <Truck :size="18" :stroke-width="1.8" />
                <span v-if="isBlackGoldMember">外卖 <b class="free-tag">免运费</b></span>
                <span v-else>外卖 +¥{{ deliveryFee }}</span>
              </label>
            </div>
          </div>
          
          <!-- v5.0 外卖地址选择 -->
          <div class="delivery-address-section" v-if="diningMethod === 'DELIVERY'">
            <label class="section-label">配送地址</label>
            <div v-if="userAddresses.length === 0" class="no-address-hint">
              <span>暂无收货地址，请先添加</span>
              <button class="add-addr-btn" @click="handleOpenAddressDialog">去添加</button>
            </div>
            <select v-else v-model="selectedAddressId" class="address-select">
              <option value="">请选择配送地址</option>
              <option v-for="addr in userAddresses" :key="addr.id" :value="addr.id">
                {{ addr.contactName }} {{ addr.phone }} - {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
              </option>
            </select>
            <div v-if="selectedAddress" class="selected-address-preview">
              <span class="address-tag" v-if="selectedAddress.isDefault">默认</span>
              {{ selectedAddress.contactName }} {{ selectedAddress.phone }}
            </div>
          </div>
          
        <!-- 凑单推荐 (Upsell) - 简洁版 -->
        <div class="cart-section">
          <label class="section-label">凑单好物</label>
          <div class="addons-grid"> <!-- 复用之前的 addons-grid 样式 -->
            <div 
              v-for="uspell in upsellProducts" 
              :key="uspell.id" 
              class="addon-tag"
              @click="handleAddUpsell(uspell)"
            >
              <div class="upsell-row">
                <span class="addon-name">{{ uspell.name }}</span>
                <span class="addon-price">+¥{{ uspell.price }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 配送费 & 优惠券 -->
        <div class="cart-section" style="border-top: 1px solid rgba(0,0,0,0.03); padding-top: 20px;">
          <!-- 主券选择 - 改回下拉列表 -->
          <div class="coupon-section" style="margin-bottom: 20px;">
            <label class="section-label">优惠券</label>
            <select 
              v-model="selectedCouponCode" 
              class="coupon-select"
            >
              <option value="">不使用优惠券</option>
              <option 
                v-for="coupon in filteredMainCoupons" 
                :key="coupon.id" 
                :value="coupon.meetsThreshold ? coupon.couponCode : ''"
                :disabled="!coupon.meetsThreshold"
              >
                {{ coupon.displayName }} 
                {{ coupon.meetsThreshold ? `-¥${coupon.estimatedDiscount.toFixed(2)}` : `(${coupon.unavailableReason})` }}
              </option>
            </select>
          </div>

          <div class="addon-coupons-section" v-if="addonCoupons.length > 0">
            <label class="section-label">附加券（可与主券叠加）</label>
            <div class="addon-coupon-list">
              <label 
                class="addon-coupon-item" 
                :class="{
                  'delivery-fee': coupon.couponType === 'DELIVERY_FEE',
                  'shot-coupon': coupon.couponType === 'SHOT',
                  selected: selectedAddonCoupons.includes(coupon.couponCode),
                  disabled: isAddonCouponDisabled(coupon)
                }"
                v-for="coupon in addonCoupons" 
                :key="coupon.id"
              >
                <input 
                  type="checkbox" 
                  :value="coupon.couponCode" 
                  v-model="selectedAddonCoupons" 
                  :disabled="isAddonCouponDisabled(coupon)"
                />
                <span class="coupon-icon">
                  <Truck v-if="coupon.couponType === 'DELIVERY_FEE'" :size="18" />
                  <Coffee v-else-if="coupon.couponType === 'SHOT'" :size="18" />
                  <Gift v-else :size="18" />
                </span>
                <span class="coupon-name">{{ coupon.displayName }}</span>
                <span class="coupon-desc">{{ getAddonCouponDesc(coupon) }}</span>
                <span v-if="getAddonCouponTip(coupon)" class="coupon-tip">{{ getAddonCouponTip(coupon) }}</span>
              </label>
            </div>
          </div>

          <div class="remark-section">
            <input v-model="remark" type="text" placeholder="备注（选填）" class="remark-input">
          </div>
        </div>
      </div>
      </template>
    </div>

    <!-- 底部只保留价格汇总和按钮，确保存放得下 -->
    <div v-if="cartItems.length > 0" class="cart-footer">      
      <div class="price-summary">
        <div class="summary-details" v-if="showPriceDetails">
            <div class="summary-row">
              <span>商品金额</span>
              <span>¥{{ subtotal.toFixed(2) }}</span>
            </div>
            <div v-if="diningMethod === 'DELIVERY'" class="summary-row">
              <span>配送费</span>
              <span v-if="isBlackGoldMember" class="free-delivery">¥0 <small>(黑金免运费)</small></span>
              <span v-else>¥{{ deliveryFee.toFixed(2) }}</span>
            </div>
            <div v-if="memberDiscount > 0" class="summary-row discount">
              <span>黑金会员 SOE 8.5折</span>
              <span>-¥{{ memberDiscount.toFixed(2) }}</span>
            </div>
            <div v-if="discount > 0" class="summary-row discount">
              <span>优惠</span>
              <span>-¥{{ discount.toFixed(2) }}</span>
            </div>
            <div v-if="addonDiscount > 0" class="summary-row discount">
              <span>附加券优惠</span>
              <span>-¥{{ addonDiscount.toFixed(2) }}</span>
            </div>
        </div>
        
        <div class="summary-main">
            <div class="price-left">
                <span class="toggle-details" @click="showPriceDetails = !showPriceDetails">
                    {{ showPriceDetails ? '收起明细' : '明细' }} 
                    <i :class="showPriceDetails ? 'arrow-down' : 'arrow-up'"></i>
                </span>
                <div class="total-price-block">
                    <span class="label">实付</span>
                    <strong class="value">¥{{ finalTotal.toFixed(2) }}</strong>
                </div>
            </div>
            <button class="checkout-btn" @click="handleCheckout" :disabled="isSubmitting">
                {{ isSubmitting ? '提交中...' : '提交订单' }}
            </button>
        </div>
        
        <div class="cart-points-earn">
          预计获得 <strong>{{ estimatedPoints }}</strong> 积分
        </div>
      </div>
    </div>
  </div>
  <!-- 添加地址弹窗 -->
  <AddressDialog 
    v-model="showAddressDialog" 
    @saved="handleAddressSaved"
  />
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getAvailableCoupons } from '@/api/mall'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { 
  ShoppingBag, X, Coffee, Minus, Plus, Trash2,
  UtensilsCrossed, CupSoda, Truck, Gift
} from 'lucide-vue-next'
import AddressDialog from '@/components/address/AddressDialog.vue'

const userStore = useUserStore()
const props = defineProps({
  isOpen: Boolean,
  cartItems: Array,
  pointsMultiplier: {
    type: Number,
    default: 1
  },
  upsellProducts: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'update-cart', 'checkout'])

const selectedCouponCode = ref('')
const availableCoupons = ref([])
const remark = ref('')
const isSubmitting = ref(false)
const showPriceDetails = ref(false)

// v5.2: 判断商品分类是否属于饮品（排除烘焙甜品）
const isDrinkCategory = (category) => {
  const drinkCategories = ['espresso', 'signature', 'cold_brew', 'tea', 'specialty', 'drink']
  return drinkCategories.includes(category) || (category && !['bakery', 'snack', 'dessert', 'food'].includes(category))
}

// v5.3.4: 判断商品分类是否属于烘培甜品
const isBakeryCategory = (category) => {
  if (!category) return false
  return ['bakery', 'snack', 'dessert', 'food', 'cake'].includes(category.toLowerCase())
}

// 图片 URL 处理增强
const getImageUrl = (url) => {
  if (!url) return 'https://placehold.co/400x400/F5F5F0/8D6E63?text=Coffee'
  if (url.startsWith('http')) return url
  return `http://localhost:8080${url.startsWith('/') ? '' : '/'}${url}`
}

const handleImageError = (e) => {
  e.target.src = 'https://placehold.co/400x400/F5F5F0/8D6E63?text=Coffee'
}

/* 用餐方式逻辑 */
const diningMethod = ref('DINE_IN') // DINE_IN, TAKEOUT, DELIVERY
const deliveryFee = ref(3) // 基础配送费（后端统一为3元）

// v5.3: 黑金会员判断（用于无限免运费）
const isBlackGoldMember = computed(() => userStore.userLevel === 'black')

/* v5.0 外卖地址相关 */
const userAddresses = ref([])
const selectedAddressId = ref('')

const selectedAddress = computed(() => {
  if (!selectedAddressId.value) return null
  return userAddresses.value.find(addr => addr.id === selectedAddressId.value)
})

// 加载用户地址列表
const loadUserAddresses = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return
    
    const response = await fetch('http://localhost:8080/api/member/addresses', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success) {
      userAddresses.value = data.data || []
      // 自动选择默认地址
      if (!selectedAddressId.value) {
          const defaultAddr = userAddresses.value.find(a => a.isDefault)
          if (defaultAddr) {
            selectedAddressId.value = defaultAddr.id
          }
      }
    }
  } catch (error) {
    console.error('加载地址失败:', error)
  }
}

// v5.3: 地址弹窗逻辑
// v5.3: 地址弹窗逻辑
const showAddressDialog = ref(false)

const handleOpenAddressDialog = () => {
    showAddressDialog.value = true
}

const handleAddressSaved = async () => {
    // 刷新列表
    await loadUserAddresses()
}

// 当选择外卖时自动加载地址
watch(diningMethod, (newVal) => {
  if (newVal === 'DELIVERY' && userAddresses.value.length === 0) {
    loadUserAddresses()
  }
})

const handleAddUpsell = (product) => {
  // 直接添加到购物车
  // 检查是否已存在
  const existingItem = props.cartItems.find(item => item.productId === product.id)
  if (existingItem) {
     const updatedCart = props.cartItems.map(item => 
       item.productId === product.id ? { ...item, quantity: item.quantity + 1 } : item
     )
     emit('update-cart', updatedCart)
     return
  }

  // 构造新 item
  const newItem = {
    productId: product.id, // 使用真实 ID
    productName: product.name,
    productImage: product.imageUrl,
    unitPrice: product.price,
    quantity: 1,
    category: product.category,
    isNewProduct: product.isNewProduct || false, // v5.3.4: 新品标记，用于新品券验证
    extraPrices: { cup:0, strength:0, milk:0 }
  }
  
  // v5.3: 只为饮品类商品添加规格配置（烘焙甜品不需要）
  const isBakery = ['bakery', 'snack', 'dessert', 'food'].includes(product.category)
  if (!isBakery) {
    // 默认饮品规格
    newItem.cupSize = 'STANDARD'
    newItem.sugarLevel = 'STANDARD'
    newItem.temperature = 'HOT'
    newItem.coffeeStrength = 'NORMAL'
    newItem.milkType = 'WHOLE'
  }
  
  const updatedCart = [...props.cartItems, newItem]
  emit('update-cart', updatedCart)
}

// v5.0 附加券 (仅保留配送费券)
const selectedAddonCoupons = ref([])

// 主券（排除 DELIVERY_FEE 和 SHOT）
const mainCoupons = computed(() => 
  availableCoupons.value.filter(c => !['DELIVERY_FEE', 'SHOT'].includes(c.couponType))
)

// 附加券（DELIVERY_FEE + SHOT）
// v5.3: 配送费券只显示一张，加浓缩券显示全部
const addonCoupons = computed(() => {
  const coupons = availableCoupons.value.filter(c => ['DELIVERY_FEE', 'SHOT'].includes(c.couponType))
  
  // 对配送费券去重，只保留第一张
  const deliveryFeeCoupons = coupons.filter(c => c.couponType === 'DELIVERY_FEE').slice(0, 1)
  const shotCoupons = coupons.filter(c => c.couponType === 'SHOT')
  
  return [...deliveryFeeCoupons, ...shotCoupons]
})

// v5.3: 验证券是否可用于当前购物车 (预验证逻辑)
const validateCouponForCart = (coupon) => {
  const rule = coupon.parsedRule || {}
  const items = props.cartItems || []
  
  // 1. 检查新品券约束
  if (coupon.couponType === 'NEW_PRODUCT_HALF' || coupon.couponType === 'NEW_PRODUCT_FREE') {
    const hasNewProduct = items.some(item => item.isNewProduct === true)
    if (!hasNewProduct) {
      return { valid: false, reason: '此券仅限新品饮品使用' }
    }
  }
  
  // 2. 检查兑换券/免单券和折扣券的 SKU 限制
  if (coupon.couponType === 'EXCHANGE' || coupon.couponType === 'DISCOUNT') {
    const skuLimit = rule.skuLimit
    const categoryBlocklist = rule.categoryBlocklist || []
    const limitSingleItem = rule.limit === 'SINGLE_ITEM'
    
    // v5.3.1: 检查杯型限制 (STANDARD_ONLY) - 需要至少有一个标准杯商品
    if (skuLimit === 'STANDARD_ONLY') {
      const drinkItems = items.filter(item => isDrinkCategory(item.category))
      const standardItems = drinkItems.filter(item => {
        const size = (item.cupSize || 'STANDARD').toUpperCase()
        return size === 'STANDARD' || size === 'MEDIUM'
      })
      
      if (standardItems.length === 0) {
        return { valid: false, reason: '此券仅限标准杯饮品' }
      }
      
      // 如果是"限单饮品"券，标准杯商品数量必须>=1
      if (limitSingleItem) {
        const hasStandardDrink = standardItems.length > 0
        if (!hasStandardDrink) {
          return { valid: false, reason: '购物车中无标准杯饮品' }
        }
      }
    }
    
    // 检查品类黑名单 (signature/soe/手冲) - 需要至少有一个非黑名单饮品
    if (categoryBlocklist.length > 0) {
      const drinkItems = items.filter(item => isDrinkCategory(item.category))
      const allowedItems = drinkItems.filter(item => {
        const cat = (item.category || '').toLowerCase()
        const isBlocked = categoryBlocklist.some(blocked => 
          cat.includes(blocked.toLowerCase()) || 
          cat.includes('手冲')
        )
        return !isBlocked
      })
      
      if (allowedItems.length === 0) {
        return { valid: false, reason: '此券不适用于SOE/手冲类产品' }
      }
    }
    
    // 检查指定商品兑换
    if (rule.linkedProductId) {
      const hasLinkedProduct = items.some(item => 
        item.productId == rule.linkedProductId || item.id == rule.linkedProductId
      )
      if (!hasLinkedProduct) {
        return { valid: false, reason: '此券仅限指定商品使用' }
      }
      // v6.1: 指定商品兑换券仅限标准杯
      const linkedItem = items.find(item => 
        item.productId == rule.linkedProductId || item.id == rule.linkedProductId
      )
      if (linkedItem) {
        const cupSize = (linkedItem.cupSize || 'STANDARD').toUpperCase()
        if (cupSize !== 'STANDARD' && cupSize !== 'MEDIUM') {
          return { valid: false, reason: '此兑换券仅限标准杯使用，请调整杯型后再试' }
        }
      }
    } else if (coupon.couponType === 'EXCHANGE') {
      // 通兑券：根据scope或券名称判断适用范围
      // v5.3.5: 兼容旧券（通过名称判断烘焙券）
      const couponName = (coupon.displayTitle || coupon.productName || '').toLowerCase()
      const isCakeCoupon = rule.scope === 'CAKE_ONLY' || 
                           couponName.includes('烘培') || 
                           couponName.includes('烘焙') || 
                           couponName.includes('甜品') ||
                           couponName.includes('蛋糕')
      
      if (isCakeCoupon) {
        // 烘培甜品免单券
        const hasBakery = items.some(item => isBakeryCategory(item.category))
        if (!hasBakery) {
          return { valid: false, reason: '此券仅限烘培甜品使用' }
        }
      } else {
        // 默认饮品通兑券
        const hasDrink = items.some(item => isDrinkCategory(item.category))
        if (!hasDrink) {
          return { valid: false, reason: '此券仅限饮品使用' }
        }
      }
    }
  }
  
  // 3. 检查 SHOT 券约束 (需要有加浓饮品)
  if (coupon.couponType === 'SHOT') {
    if (!hasExtraShot.value) {
      return { valid: false, reason: '需先添加加浓缩饮品' }
    }
  }
  
  // 4. 检查 DELIVERY_FEE 券约束 (需要是外卖订单)
  if (coupon.couponType === 'DELIVERY_FEE') {
    if (diningMethod.value !== 'DELIVERY') {
      return { valid: false, reason: '此券仅限外卖订单使用' }
    }
  }
  
  // 5. 检查 DISCOUNT 券的 scope=DRINK_ONLY
  if (coupon.couponType === 'DISCOUNT' && rule.scope === 'DRINK_ONLY') {
    const hasDrink = items.some(item => isDrinkCategory(item.category))
    if (!hasDrink) {
      return { valid: false, reason: '此券仅限饮品使用' }
    }
  }
  
  // 5.1 检查 DISCOUNT 券的 scope=CAKE_ONLY (烘培甜品券)
  if (coupon.couponType === 'DISCOUNT' && rule.scope === 'CAKE_ONLY') {
    const hasBakery = items.some(item => isBakeryCategory(item.category))
    if (!hasBakery) {
      return { valid: false, reason: '此券仅限烘培甜品使用' }
    }
  }
  
  // 6. 检查 BOGO 买一送一券 (至少2杯饮品)
  if (coupon.couponType === 'BOGO') {
    const drinkItems = items.filter(item => isDrinkCategory(item.category))
    const drinkCount = drinkItems.reduce((sum, i) => sum + i.quantity, 0)
    if (drinkCount < 2) {
      return { valid: false, reason: '至少需要2杯饮品' }
    }
  }
  
  return { valid: true, reason: '' }
}

// v6.1: 通用券折扣计算函数（复用discount逻辑）
const calculateCouponDiscount = (coupon) => {
  if (!coupon) return 0

  if (coupon.couponType === 'DISCOUNT') {
    return calculateDiscountAmount(coupon)
  } else if (coupon.couponType === 'BOGO') {
    const rule = coupon.parsedRule || {}
    const drinkItems = props.cartItems.filter(item => isDrinkCategory(item.category))
    
    let prices = []
    drinkItems.forEach(item => {
      let itemBasePrice = item.basePrice || item.unitPrice
      if (item.cupSize === 'LARGE') itemBasePrice += 3
      
      for (let i = 0; i < item.quantity; i++) {
        prices.push(itemBasePrice)
      }
    })
    
    if (prices.length < 2) return 0
    prices.sort((a, b) => a - b)
    const maxPerCup = rule.maxDiscount || 9999
    return Math.min(prices[0], maxPerCup)
    
  } else if (coupon.couponType === 'FULL_REDUCE') {
    return subtotal.value >= (coupon.minAmount || 0) ? coupon.value : 0
    
  } else if (coupon.couponType === 'EXCHANGE') {
    const rule = coupon.parsedRule || {}
    if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
    
    const couponName = (coupon.displayTitle || coupon.productName || '').toLowerCase()
    const isCakeCoupon = rule.scope === 'CAKE_ONLY' || 
                         couponName.includes('烘培') || 
                         couponName.includes('烘焙') || 
                         couponName.includes('甜品') ||
                         couponName.includes('蛋糕')
    
    if (isCakeCoupon) {
      const bakeryItems = props.cartItems.filter(item => isBakeryCategory(item.category))
      if (bakeryItems.length === 0) return 0
      const maxBakeryPrice = Math.max(...bakeryItems.map(i => i.unitPrice || 0))
      const maxDeduct = rule.maxDiscount || 9999
      return Math.min(maxBakeryPrice, maxDeduct)
    }
    
    if (!rule.linkedProductId && !rule.productId) {
      let drinkItems = props.cartItems.filter(item => isDrinkCategory(item.category))
      if (drinkItems.length === 0) return 0
      
      const skuLimit = rule.skuLimit
      const categoryBlocklist = rule.categoryBlocklist || []
      
      drinkItems = drinkItems.filter(item => {
        if (skuLimit === 'STANDARD_ONLY') {
          const size = (item.cupSize || 'STANDARD').toUpperCase()
          if (size !== 'STANDARD' && size !== 'MEDIUM') return false
        }
        if (categoryBlocklist.length > 0 && item.category) {
          const cat = item.category.toLowerCase()
          const isBlocked = categoryBlocklist.some(blocked => 
            cat.includes(blocked.toLowerCase()) || cat.includes('手冲')
          )
          if (isBlocked) return false
        }
        return true
      })
      
      if (drinkItems.length === 0) return 0
      
      const maxDrinkPrice = Math.max(...drinkItems.map(i => {
        let itemBasePrice = i.basePrice || i.unitPrice
        if (i.cupSize === 'LARGE') itemBasePrice += 3
        return itemBasePrice
      }))
      const maxDeduct = rule.maxDeductAmount || rule.maxDiscount || 9999
      let baseDiscount = Math.min(maxDrinkPrice, maxDeduct)
      
      const freeAddonCount = rule.freeAddon || 0
      if (freeAddonCount > 0) {
        const addonList = []
        drinkItems.forEach(item => {
          if (item.extraPrices?.strength) addonList.push({ price: item.extraPrices.strength, type: 'strength' })
          if (item.extraPrices?.milk) addonList.push({ price: item.extraPrices.milk, type: 'milk' })
        })
        addonList.sort((a, b) => b.price - a.price)
        const freeAddons = addonList.slice(0, freeAddonCount)
        baseDiscount += freeAddons.reduce((sum, addon) => sum + addon.price, 0)
      }
      
      return baseDiscount
    }
    
    const targetItem = props.cartItems.find(item => {
       if (rule.linkedProductId && (item.id == rule.linkedProductId || item.productId == rule.linkedProductId)) return true
       if (rule.productId && (item.id == rule.productId || item.productId == rule.productId)) return true
       if (rule.productIds && (rule.productIds.includes(item.id) || rule.productIds.includes(item.productId))) return true
       return false
    })
    if (targetItem) {
      const standardPrice = targetItem.basePrice || targetItem.unitPrice
      const maxDeduct = rule.maxDeductAmount || rule.maxDiscount || 9999
      return Math.min(standardPrice, maxDeduct)
    }
  }
  
  return 0
}

// 筛选可用主券（添加门槛检查和排序）
const filteredMainCoupons = computed(() => {
  return mainCoupons.value.map(coupon => {
    let meetsThreshold = true
    let amountNeeded = 0
    let unavailableReason = ''
    
    // v6.1: 直接使用通用计算函数获取折扣金额
    let estimatedDiscount = calculateCouponDiscount(coupon)
    
    // 预验证券约束条件
    const validation = validateCouponForCart(coupon)
    if (!validation.valid) {
      meetsThreshold = false
      unavailableReason = validation.reason
      estimatedDiscount = 0
    }
    
    // 满减券门槛检查
    if (coupon.couponType === 'FULL_REDUCE') {
       const minAmount = coupon.parsedRule?.minOrderAmount || coupon.minAmount || 0
       if (minAmount > 0 && subtotal.value < minAmount) {
         meetsThreshold = false
         amountNeeded = (minAmount - subtotal.value).toFixed(0)
         if (!unavailableReason) {
           unavailableReason = `需满¥${minAmount}`
         }
         estimatedDiscount = 0
       }
    }
    
    // 如果折扣为0且没有明确的不可用原因，说明购物车中无匹配商品
    if (estimatedDiscount === 0 && !unavailableReason && meetsThreshold) {
      meetsThreshold = false
      unavailableReason = '购物车中无匹配商品'
    }
    
    return { ...coupon, meetsThreshold, amountNeeded, estimatedDiscount, unavailableReason }
  }).sort((a, b) => {
    // 按预估折扣金额从高到低排序
    return (b.estimatedDiscount || 0) - (a.estimatedDiscount || 0)
  })
})

// 配送费抵扣（v5.3: 黑金会员自动全免）
const deliveryFeeDiscount = computed(() => {
  // 黑金会员自动免运费
  if (isBlackGoldMember.value && diningMethod.value === 'DELIVERY') {
    return deliveryFee.value
  }
  
  // 普通用户使用配送费抵扣券
  const hasDeliveryCoupon = selectedAddonCoupons.value.some(code => {
    const coupon = addonCoupons.value.find(c => c.couponCode === code)
    return coupon?.couponType === 'DELIVERY_FEE'
  })
  if (hasDeliveryCoupon && diningMethod.value === 'DELIVERY') {
    return Math.min(10, deliveryFee.value)
  }
  return 0
})

// v5.3: 加浓缩券抵扣（支持多张券）
const shotDiscount = computed(() => {
  // 统计选中的 SHOT 券数量
  const shotCouponCount = selectedAddonCoupons.value.filter(code => {
    const coupon = addonCoupons.value.find(c => c.couponCode === code)
    return coupon?.couponType === 'SHOT'
  }).length
  
  if (shotCouponCount > 0 && hasExtraShot.value) {
    // 每张券抵扣 ¥5，总抵扣 = 券数 × ¥5
    // 但不超过实际的加浓费用总额
    const maxDiscount = shotCouponCount * 5
    return Math.min(strengthExtraTotal.value, maxDiscount)
  }
  return 0
})

// 附加券总优惠 (配送费 + 加浓缩)
const addonDiscount = computed(() => deliveryFeeDiscount.value + shotDiscount.value)



// v5.3: 检查购物车是否有加浓缩修饰符 (修复: 检查 coffeeStrength 字段)
const hasExtraShot = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return false
  return props.cartItems.some(item => {
    // 检查 coffeeStrength 属性 (值为 'STRONG' 代表加浓)
    if (item.coffeeStrength === 'STRONG') return true
    
    // 兼容旧逻辑 (防止 optionsJson 存在的情况)
    if (item.optionsJson) {
      const opts = item.optionsJson.toLowerCase()
      return opts.includes('extra_shot') || opts.includes('加浓')
    }
    return false
  })
})

// v5.3: 计算购物车中有多少杯加浓饮品
const extraShotCount = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  let count = 0
  props.cartItems.forEach(item => {
    if (item.coffeeStrength === 'STRONG') {
      count += item.quantity
    }
  })
  return count
})

// v5.3: 计算已选中的 SHOT 券数量
const selectedShotCouponCount = computed(() => {
  return selectedAddonCoupons.value.filter(code => {
    const coupon = addonCoupons.value.find(c => c.couponCode === code)
    return coupon?.couponType === 'SHOT'
  }).length
})

// v5.3: 监听加浓饮品数量变化，自动调整券选择
watch(extraShotCount, (newCount, oldCount) => {
  // 只在数量减少时处理
  if (newCount < oldCount) {
    const currentShotCouponCount = selectedAddonCoupons.value.filter(code => {
      const coupon = addonCoupons.value.find(c => c.couponCode === code)
      return coupon?.couponType === 'SHOT'
    }).length
    
    // 如果选中的券数超过了当前加浓饮品数量
    if (currentShotCouponCount > newCount) {
      // 找出所有 SHOT 券
      const shotCoupons = []
      selectedAddonCoupons.value.forEach(code => {
        const coupon = addonCoupons.value.find(c => c.couponCode === code)
        if (coupon?.couponType === 'SHOT') {
          shotCoupons.push(code)
        }
      })
      
      // 计算需要移除的数量
      const excessCount = shotCoupons.length - newCount
      
      // 移除最后选中的几张券（LIFO策略）
      const toRemove = shotCoupons.slice(-excessCount)
      selectedAddonCoupons.value = selectedAddonCoupons.value.filter(
        code => !toRemove.includes(code)
      )
      
      ElMessage.info(`已自动取消${excessCount}张加浓券（当前仅${newCount}杯加浓饮品）`)
    }
  }
})


// v5.3: 判断附加券是否应该禁用
const isAddonCouponDisabled = (coupon) => {
  // v5.3: 使用统一的预验证逻辑
  const validation = validateCouponForCart(coupon)
  if (!validation.valid) return true
  
  if (coupon.couponType === 'DELIVERY_FEE') {
    return diningMethod.value !== 'DELIVERY'
  }
  if (coupon.couponType === 'SHOT') {
    // 没有加浓饮品时禁用
    if (!hasExtraShot.value) return true
    // 已选数量达到加浓饮品数量时，禁用其他未选中的券
    if (selectedShotCouponCount.value >= extraShotCount.value && 
        !selectedAddonCoupons.value.includes(coupon.couponCode)) {
      return true
    }
  }
  return false
}

// v5.3: 获取附加券描述
const getAddonCouponDesc = (coupon) => {
  if (coupon.couponType === 'DELIVERY_FEE') return '抵扣配送费（限用1张）'
  if (coupon.couponType === 'SHOT') {
    // 显示可用数量提示
    if (extraShotCount.value > 0) {
      return `每张抵¥5（最多${extraShotCount.value}张）`
    }
    return '免费加浓缩'
  }
  return ''
}

// v5.3: 获取附加券提示文案
const getAddonCouponTip = (coupon) => {
  // v5.3: 优先显示预验证失败原因
  const validation = validateCouponForCart(coupon)
  if (!validation.valid) {
    return validation.reason
  }
  
  if (coupon.couponType === 'DELIVERY_FEE' && diningMethod.value !== 'DELIVERY') {
    return '仅外卖可用'
  }
  if (coupon.couponType === 'SHOT') {
    if (!hasExtraShot.value) {
      return '请先选择加浓缩'
    }
    // 显示已选/可用数量
    if (selectedShotCouponCount.value >= extraShotCount.value && 
        !selectedAddonCoupons.value.includes(coupon.couponCode)) {
      return `已选${selectedShotCouponCount.value}/${extraShotCount.value}张`
    }
  }
  return null
}

// v5.3: 区分基价和修饰符加价
const baseSubtotal = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  return props.cartItems.reduce((sum, item) => {
    const basePrice = item.basePrice || item.unitPrice // 兼容旧数据
    return sum + basePrice * item.quantity
  }, 0)
})

const strengthExtraTotal = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  return props.cartItems.reduce((sum, item) => {
    const strengthExtra = item.extraPrices?.strength || 0
    return sum + strengthExtra * item.quantity
  }, 0)
})

// v6.1: 杯型加价单独统计（参与折扣）
const cupExtraTotal = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  return props.cartItems.reduce((sum, item) => {
    const cupExtra = item.extraPrices?.cup || 0
    return sum + cupExtra * item.quantity
  }, 0)
})

// v6.1: 奶类加价单独统计（不参与折扣，视为加料）
const milkExtraTotal = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  return props.cartItems.reduce((sum, item) => {
    const milkExtra = item.extraPrices?.milk || 0
    return sum + milkExtra * item.quantity
  }, 0)
})

const otherExtrasTotal = computed(() => {
  // v6.1: 保持兼容性，返回杯型+奶类总和（用于UI显示）
  return cupExtraTotal.value + milkExtraTotal.value
})

// 商品总计（用于UI展示）
const subtotal = computed(() => {
  return baseSubtotal.value + strengthExtraTotal.value + otherExtrasTotal.value
})

// v5.8: 黑金会员 SOE 8.5折优惠计算
const memberDiscount = computed(() => {
  const level = userStore.userLevel || 'basic'
  
  // 仅黑金会员享受 soe 产品 8.5 折（15% off）
  if (level !== 'black') return 0
  
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  
  let totalDiscount = 0
  
  props.cartItems.forEach(item => {
    // 判断是否为 soe 类产品（手冲精品）
    if (item.category === 'soe') {
      // 计算基础价格（含杯型加价）
      let itemBasePrice = item.basePrice || item.unitPrice
      if (item.cupSize === 'LARGE') {
        itemBasePrice += 3
      }
      
      // 15% 折扣应用于基础价格（不包含加料）
      const itemBaseAmount = itemBasePrice * item.quantity
      const itemDiscount = itemBaseAmount * 0.15
      
      totalDiscount += itemDiscount
    }
  })
  
  return totalDiscount
})

// v5.7.2: 提取 DISCOUNT 折扣计算逻辑，确保选择框和实际使用的计算一致
const calculateDiscountAmount = (coupon) => {
  const rule = coupon.parsedRule || {}
  
  // 1. 获取折扣百分比
  let discountPercent = coupon.value
  if (!discountPercent || discountPercent <= 0) {
    const rate = rule.discountRate || 0
    if (rate > 0 && rate < 1) {
      discountPercent = rate * 100
    } else if (rate >= 1 && rate <= 10) {
      discountPercent = rate * 10
    } else {
      discountPercent = rate
    }
  }
  
  if (discountPercent <= 0 || discountPercent > 100) return 0
  
  // 2. 解析配置
  const isDrinkOnly = rule.scope === 'DRINK_ONLY'
  const isCakeOnly = rule.scope === 'CAKE_ONLY'
  const isSingleItem = rule.limit === 'SINGLE_ITEM'
  
  // 3. v6.1: 计算折扣基数（基价+杯型加价，不含奶类和加浓）
  let baseAmount = baseSubtotal.value + cupExtraTotal.value
  
  if (isCakeOnly) {
    const bakeryItems = props.cartItems.filter(item => isBakeryCategory(item.category))
    if (bakeryItems.length === 0) return 0
    
    const maxBakery = bakeryItems.reduce((prev, curr) => {
      const prevPrice = prev.basePrice || prev.unitPrice
      const currPrice = curr.basePrice || curr.unitPrice
      return currPrice > prevPrice ? curr : prev
    })
    baseAmount = maxBakery.basePrice || maxBakery.unitPrice
    
  } else if (isDrinkOnly || isSingleItem) {
    const drinkItems = props.cartItems.filter(item => isDrinkCategory(item.category))
    if (drinkItems.length === 0) return 0
    
    if (isSingleItem) {
      // v6.1: 折扣券计算基价（含杯型加价），不含加料费（strength等）
      const maxItem = drinkItems.reduce((prev, curr) => {
        let prevPrice = prev.basePrice || prev.unitPrice
        if (prev.cupSize === 'LARGE') prevPrice += 3
        
        let currPrice = curr.basePrice || curr.unitPrice
        if (curr.cupSize === 'LARGE') currPrice += 3
        
        return currPrice > prevPrice ? curr : prev
      })
      let maxItemPrice = maxItem.basePrice || maxItem.unitPrice
      if (maxItem.cupSize === 'LARGE') maxItemPrice += 3
      baseAmount = maxItemPrice
    } else {
      // v6.1: 折扣券计算总基价（含杯型加价），不含加料费
      baseAmount = drinkItems.reduce((sum, i) => {
        let itemBase = i.basePrice || i.unitPrice
        if (i.cupSize === 'LARGE') itemBase += 3
        return sum + itemBase * i.quantity
      }, 0)
    }
  }
  
  // 4. 计算折扣金额
  let discountAmount = baseAmount * (1 - discountPercent / 100)
  
  // 5. 封顶控制
  const maxDiscount = rule.maxDiscountAmount || 0
  if (maxDiscount > 0 && discountAmount > maxDiscount) {
    discountAmount = maxDiscount
  }
  
  return discountAmount
}

const discount = computed(() => {
  if (!selectedCouponCode.value) return 0
  
  const coupon = availableCoupons.value.find(c => c.couponCode === selectedCouponCode.value)
  if (!coupon) return 0

  // v6.1: 统一使用通用计算函数，确保与 filteredMainCoupons 中 estimatedDiscount 完全一致
  return calculateCouponDiscount(coupon)
})

const filteredCoupons = computed(() => {
  return availableCoupons.value.filter(coupon => {
    // 兑换券：必须购物车里有对应商品
    if (coupon.couponType === 'EXCHANGE') {
       if (!props.cartItems || !Array.isArray(props.cartItems)) return false
       const rule = coupon.parsedRule || {}
       console.log('[DEBUG] 检查兑换券:', coupon.displayName, 'Rule:', rule)
       console.log('[DEBUG] 购物车商品:', props.cartItems.map(i => ({ id: i.id, productId: i.productId, name: i.productName })))
       
       const matched = props.cartItems.some(item => {
         // 优先使用 linkedProductId 匹配
         if (rule.linkedProductId && (item.id == rule.linkedProductId || item.productId == rule.linkedProductId)) return true
         // 兼容旧字段
         if (rule.productId && (item.id == rule.productId || item.productId == rule.productId)) return true
         if (rule.productIds && (rule.productIds.includes(item.id) || rule.productIds.includes(item.productId))) return true
         return false
       })
       console.log('[DEBUG] 匹配结果:', matched)
       return matched
    }
    // 其他券：后端已根据金额筛选，前端默认放行
    return true
  })
})

// v5.7.2: 计算主券免除的加料金额，分别追踪 strength 和 milk 部分
// 因为 strength 在 strengthExtraTotal（独立计算），milk 在 otherExtrasTotal（含在baseAmount）
const mainCouponAddonDiscountDetails = computed(() => {
  const result = { strength: 0, milk: 0, total: 0 }
  
  if (!selectedCouponCode.value) return result
  
  const coupon = availableCoupons.value.find(c => c.couponCode === selectedCouponCode.value)
  if (!coupon) return result
  
  const rule = coupon.parsedRule || {}
  const freeAddonCount = rule.freeAddon || 0
  
  if (freeAddonCount > 0 && coupon.couponType === 'EXCHANGE') {
    // 收集所有加料，标记来源类型
    const drinkItems = props.cartItems.filter(item => isDrinkCategory(item.category))
    const addonList = [] // { price, type: 'strength' | 'milk' }
    
    drinkItems.forEach(item => {
      if (item.extraPrices?.strength) {
        addonList.push({ price: item.extraPrices.strength, type: 'strength' })
      }
      if (item.extraPrices?.milk) {
        addonList.push({ price: item.extraPrices.milk, type: 'milk' })
      }
    })
    
    // 按金额从高到低排序，取前 freeAddonCount 个
    addonList.sort((a, b) => b.price - a.price)
    const freeAddons = addonList.slice(0, freeAddonCount)
    
    freeAddons.forEach(addon => {
      if (addon.type === 'strength') {
        result.strength += addon.price
      } else {
        result.milk += addon.price
      }
      result.total += addon.price
    })
  }
  
  return result
})

// 兼容旧引用
const mainCouponAddonDiscount = computed(() => mainCouponAddonDiscountDetails.value.total)

const finalTotal = computed(() => {
  // v6.1: 完全对齐后端计算逻辑（OrderServiceImpl Line 464-469）
  // 后端计算顺序：
  // payAmount = totalAmount - discountAmount - addonDiscount + actualDeliveryFee
  // 
  // 其中：
  // - totalAmount = baseTotalAmount + addonsTotalAmount（商品基价+杯型+加料）
  // - discountAmount = 主券折扣（仅针对基价+杯型，除尊享通兑券外不含加料）
  // - addonDiscount = 附加券折扣（加浓券、配送费券）+ 尊享通兑券的免费加料
  
  // 1. 商品基础金额（基价 + 杯型加价）- 这是主券作用的基数
  const baseAmountForDiscount = baseSubtotal.value + cupExtraTotal.value
  
  // 2. 加料总费用（奶类 + 加浓）- 除尊享通兑券外，需全额支付
  const addonsAmount = milkExtraTotal.value + strengthExtraTotal.value
  
  // 3. 商品总额（基价 + 杯型 + 加料）
  const totalAmount = baseAmountForDiscount + addonsAmount
  
  // 4. v5.8: 先应用会员折扣（黑金SOE 8.5折）
  const afterMemberDiscount = Math.max(0, totalAmount - memberDiscount.value)
  
  // 5. 主券折扣（仅针对基价+杯型，不含加料，除非是尊享通兑券）
  const mainDiscount = discount.value
  
  // 6. 附加券折扣（加浓券）
  const addonCouponDiscount = shotDiscount.value
  
  // 7. 计算实付商品金额
  const afterAllDiscount = Math.max(0, afterMemberDiscount - mainDiscount - addonCouponDiscount)
  
  // 8. 配送费（外卖订单）
  let deliveryAmount = 0
  if (diningMethod.value === 'DELIVERY') {
    deliveryAmount = Math.max(0, deliveryFee.value - deliveryFeeDiscount.value)
  }
  
  // 9. 最终实付 = 商品实付 + 配送费
  const total = afterAllDiscount + deliveryAmount
  return Math.max(0, total)
})

const estimatedPoints = computed(() => {
  const amount = finalTotal.value
  if (amount <= 0) return 0
  
  const level = userStore.userLevel || 'basic'
  const accelerateRemaining = userStore.userInfo?.monthlyAccelerateRemaining ?? 0
  
  // 等级基础倍率 (v5.0 白皮书)
  const baseMultiplierMap = {
    basic: 1,
    silver: 1.1,
    gold: 1.2,
    diamond: 1.3,
    black: 1.5
  }
  const baseMultiplier = baseMultiplierMap[level] || 1
  
  // 黑卡加速包逻辑
  if (level === 'black' && accelerateRemaining > 0) {
    const accelerateMultiplier = 1.7
    if (amount <= accelerateRemaining) {
      return Math.floor(amount * accelerateMultiplier)
    } else {
      const accelPart = accelerateRemaining * accelerateMultiplier
      const normalPart = (amount - accelerateRemaining) * baseMultiplier
      return Math.floor(accelPart + normalPart)
    }
  }
  
  return Math.floor(amount * baseMultiplier)
})

const updateQuantity = (index, newQuantity) => {
  if (newQuantity < 1) {
    removeItem(index)
    return
  }
  const updatedCart = [...props.cartItems]
  updatedCart[index].quantity = newQuantity
  emit('update-cart', updatedCart)
}

const removeItem = (index) => {
  const updatedCart = props.cartItems.filter((_, i) => i !== index)
  emit('update-cart', updatedCart)
}

const handleCheckout = () => {
  isSubmitting.value = true
  
  // 外卖订单校验地址
  if (diningMethod.value === 'DELIVERY' && !selectedAddressId.value) {
    isSubmitting.value = false
    alert('请选择配送地址')
    return
  }
  
  emit('checkout', {
    items: props.cartItems,
    couponCode: selectedCouponCode.value,
    addonCouponCodes: selectedAddonCoupons.value, 
    diningMethod: diningMethod.value,
    deliveryAddressId: diningMethod.value === 'DELIVERY' ? selectedAddressId.value : null,
    remark: remark.value
  })
}

const loadAvailableCoupons = async () => {
  if (subtotal.value > 0) {
    try {
      const data = {
        orderAmount: subtotal.value,
        items: props.cartItems.map(item => ({
          productId: item.productId,
          price: item.unitPrice,
          category: item.category,
          quantity: item.quantity,
          cupSize: item.cupSize || 'STANDARD' // v6.1: 添加cupSize以支持指定商品兑换券杯型限制
        }))
      }
      const res = await getAvailableCoupons(data)
      if (res.data.success) {
        // v5.7: 优先使用后端 displayTitle/displaySubTitle，大幅简化前端逻辑
        availableCoupons.value = (res.data.data || []).map(c => {
           let rule = {}
           try { rule = JSON.parse(c.ruleJson || '{}') } catch(e) {}
           
           // v5.7: 优先使用后端 displayTitle，仅对极端老数据回退
           let name = c.displayTitle || ''
           
           if (!name) {
             // 兜底逻辑（仅针对历史遗留数据）
             if (c.couponType === 'DISCOUNT') {
                let displayDiscount = c.value
                if (c.value >= 10) {
                  displayDiscount = c.value / 10
                  displayDiscount = displayDiscount % 1 === 0 ? displayDiscount : displayDiscount.toFixed(1)
                }
                name = displayDiscount + '折券'
             } else if (c.couponType === 'FULL_REDUCE') {
               const minAmt = c.minAmount || rule.minOrderAmount || 0
               const reduceVal = c.value || rule.value || 0
               name = minAmt > 0 ? `满${minAmt}减${reduceVal}` : `立减${reduceVal}元`
             } else if (c.couponType === 'EXCHANGE') {
               name = '免单券'
             } else if (c.couponType === 'BOGO') {
               name = '买一送一'
             } else if (c.couponType === 'DELIVERY_FEE') {
               name = '免运费券'
             } else if (c.couponType === 'SHOT') {
               name = '+1 Shot'
             } else {
               name = '优惠券'
             }
           }
           
           // v5.7: 去除描述拼接，保持名称简洁
           // if (c.displaySubTitle && !name.includes(c.displaySubTitle)) {
           //   fullName = name + ' (' + c.displaySubTitle + ')'
           // }
           // name = fullName
           
           return {
             ...c,
             parsedRule: rule,
             displayName: name,
             minAmount: c.minAmount || rule.minOrderAmount || 0
           }
        })
        
        // 自动选择最优券
        autoSelectBestCoupon()
      }
    } catch (error) {
      console.error('加载优惠券失败', error)
    }
  }
}

// 自动选择当前购物车最优的券
const autoSelectBestCoupon = () => {
  // 获取可用主券（满足门槛的）
  const usableCoupons = filteredMainCoupons.value.filter(c => c.meetsThreshold)
  if (usableCoupons.length === 0) {
    selectedCouponCode.value = ''
    return
  }
  
  // 已按 estimatedDiscount 降序排列，选第一个
  selectedCouponCode.value = usableCoupons[0].couponCode
  console.log('[AutoSelect] 自动选择最优券:', usableCoupons[0].displayName, '预估优惠:', usableCoupons[0].estimatedDiscount)
}

watch(() => subtotal.value, () => {
  if (subtotal.value > 0) {
    loadAvailableCoupons()
  } else {
    availableCoupons.value = []
    selectedCouponCode.value = ''
  }
})

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    loadAvailableCoupons()
    isSubmitting.value = false
  }
})

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

const getCouponDisplay = (coupon) => {
  // ... (保持不变)
  if (coupon.couponType === 'DISCOUNT') {
    return `${(coupon.value / 10).toFixed(1)}折`
  } else if (coupon.couponType === 'FULL_REDUCE') {
    return `满${coupon.minAmount}减${coupon.value}`
  } else if (coupon.couponType === 'EXCHANGE') {
    return '兑换券'
  }
  return ''
}
</script>

<style scoped>
/* ========== 购物车侧边栏 ========== */
.cart-sidebar {
  position: fixed;
  top: 0;
  right: 0;
  width: 560px; /* v5.3.2: 增宽以更好展示优惠券信息 */
  height: 100vh;
  background: #FCFAF8; /* 极浅的暖米色背景 */
  box-shadow: -10px 0 40px rgba(0, 0, 0, 0.08); /* 更柔和的阴影 */
  z-index: 1000;
  transform: translateX(100%);
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1); /* iOS 风格的顺滑缓动 */
  display: flex;
  flex-direction: column;
}

.cart-sidebar.open {
  transform: translateX(0);
}

/* 头部设计 - 极简无背景 */
.cart-header {
  padding: 32px 32px 20px;
  background: transparent; /* 去除色块 */
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0,0,0,0.03); /* 极淡的分割线 */
}

.cart-header h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #5D4037; /* 深暖棕色 */
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.5px;
}

.close-btn {
  background: transparent;
  border: 1px solid rgba(93, 64, 55, 0.1);
  color: #8D6E63;
  font-size: 0; /* 隐藏可能的文字，只显示图标 */
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: rgba(93, 64, 55, 0.05);
  color: #5D4037;
  border-color: rgba(93, 64, 55, 0.3);
  transform: rotate(90deg);
}

/* 购物车主体 */
.cart-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
}

/* 空状态 */
.empty-cart {
  text-align: center;
  padding: 120px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  color: #D7CCC8; /* 浅咖啡色 */
  margin-bottom: 24px;
}

.empty-cart p {
  font-size: 18px;
  font-weight: 600;
  color: #8D6E63;
  margin-bottom: 8px;
}

.empty-cart span {
  font-size: 14px;
  color: #BCAAA4;
}

/* 商品列表 */
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
  box-shadow: 0 4px 20px rgba(141, 110, 99, 0.06); /* 暖色调微投影 */
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

/* 商品详情 */
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
  background: #FDF8F3; /* 极淡的暖色背景 */
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid rgba(198, 156, 109, 0.15);
}

.item-price {
  font-size: 16px;
  font-weight: 600;
  color: #5D4037;
}

/* 操作区 */
.item-actions {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
}

/* 现代数量控制器 */
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

/* 底部区域 - 去除大色块 */
/* 选项区域（跟随滚动） */
.cart-options-area {
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px dashed #E0E0E0;
}

/* 底部固定区域 */
.cart-footer {
  position: relative;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid rgba(0,0,0,0.05);
  box-shadow: 0 -4px 20px rgba(0,0,0,0.05);
  z-index: 100;
}

.price-summary {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin: 0;
  border: none;
  padding: 0;
}

/* 价格明细面板 - 位于 Footer 上方 */
.summary-details {
  position: absolute;
  bottom: 100%; /* 位于 Footer 顶部 */
  left: 0;
  right: 0;
  background: #fff;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  box-shadow: 0 -4px 12px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: -1; /* 在 Footer 下层，但比遮罩层高 */
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUp {
  from { transform: translateY(10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.summary-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.price-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toggle-details {
  font-size: 12px;
  color: #8D6E63;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
.toggle-details:hover {
  text-decoration: underline;
}

/* CSS 箭头 */
.arrow-up, .arrow-down {
  border: solid #8D6E63;
  border-width: 0 1.5px 1.5px 0;
  display: inline-block;
  padding: 2px;
  margin-bottom: 2px;
}
.arrow-up { transform: rotate(-135deg); }
.arrow-down { transform: rotate(45deg); }

.total-price-block {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.total-price-block .label {
  font-size: 13px;
  color: #3E2723;
}

.total-price-block .value {
  font-size: 24px; /* 加大实付金额 */
  font-weight: 700;
  color: #D84315;
  line-height: 1;
}

/* 调整结算按钮 */
.checkout-btn {
  width: auto; /* 不再占满全宽 */
  flex: 1;
  padding: 12px 24px;
  border-radius: 24px;
  font-size: 16px;
}

.cart-points-earn {
  margin-top: 8px;
  justify-content: flex-start;
  padding: 0;
  background: none;
  border: none;
  font-size: 11px;
}

/* 用餐方式 - 优雅 Toggle Button */
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

/* 按钮样式 */
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
  background: rgba(198, 156, 109, 0.08); /* 暖色浅背景 */
  color: #5D4037;
  font-weight: 600;
  box-shadow: 0 0 0 1px #C69C6D inset; /* 内描边加重选中感 */
}

/* 特殊逻辑：让图标在选中时也有变化 */
.dining-option.active svg {
  stroke: #C69C6D;
  stroke-width: 2.2px;
}

/* 优惠券选择器 - 极简白风格 */
.coupon-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.coupon-select {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #E0E0E0;
  border-radius: 10px;
  background: #FFF;
  color: #5D4037;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  cursor: pointer;
  appearance: none; /* 移除默认箭头 */
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%238D6E63' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 10px center;
  background-size: 16px;
}

.coupon-select:focus {
  border-color: #C69C6D;
}

/* 备注信息 */
.remark-section {
  width: 100%;
}
.remark-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #E0E0E0;
  border-radius: 10px;
  background: #FFF;
  color: #5D4037;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}
.remark-input:focus {
  border-color: #C69C6D;
}

/* v5.0 加料列表简化版 */
.addons-section {
  margin-top: 12px;
  margin-bottom: 24px;
  padding-top: 12px;
  border-top: 1px dashed #E0E0E0;
}
.addons-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.addon-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid #E0E0E0;
  border-radius: 14px;
  font-size: 12px;
  color: #8D6E63;
  cursor: pointer;
  transition: all 0.2s;
  background: #FFF;
}
.addon-item.selected {
  background: #FFF3E0;
  border-color: #FFB74D;
  color: #E65100;
}
.addon-item input {
  display: none;
}
.addon-name {
  font-weight: 500;
}
.addon-price {
  font-size: 11px;
  opacity: 0.8;
}

.addons-summary {
  margin-top: 8px;
  font-size: 12px;
  color: #8D6E63;
}
.addon-discount {
  color: #D84315;
}

/* v5.0 附加券选择 */
.addon-coupons-section {
  margin-bottom: 16px;
}

.addon-coupon-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.addon-coupon-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #E0E0E0;
  border-radius: 10px;
  background: #FAFAFA;
  cursor: pointer;
  transition: all 0.2s;
}

.addon-coupon-item input { display: none; }

.addon-coupon-item.selected {
  border-color: #C69C6D;
  background: #FFF8E1;
}

.addon-coupon-item .coupon-name {
  font-weight: 600;
  color: #5D4037;
  font-size: 13px;
}

.addon-coupon-item .coupon-desc {
  font-size: 12px;
  color: #9E9E9E;
  margin-left: auto;
}

/* 不可用券选项置灰 - v5.3.1: 优化禁用样式 */
.coupon-select option:disabled,
.coupon-select option.coupon-unavailable {
  color: #999;
  background: #F9F9F9;
  font-style: italic;
}

.coupon-select option:disabled::before {
  content: '🚫 ';
}

/* 配送费抵扣券专属样式 */
.addon-coupon-item.delivery-fee {
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
  border: 1px dashed #2196F3;
  position: relative;
}

.addon-coupon-item.delivery-fee .coupon-icon {
  font-size: 18px;
  margin-right: 4px;
}

.addon-coupon-item.delivery-fee.selected {
  background: linear-gradient(135deg, #BBDEFB 0%, #90CAF9 100%);
  border-color: #1976D2;
  border-style: solid;
}

.addon-coupon-item.delivery-fee.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(0.5);
}

.addon-coupon-item.delivery-fee .coupon-name {
  color: #1565C0;
}

.addon-coupon-item.delivery-fee .coupon-tip {
  font-size: 10px;
  color: #F57C00;
  margin-left: 8px;
  padding: 2px 6px;
  background: #FFF3E0;
  border-radius: 4px;
}

/* v5.3: 加浓缩券专属样式 (Coffee Theme) */
.addon-coupon-item.shot-coupon {
  background: linear-gradient(135deg, #EFEBE9 0%, #D7CCC8 100%);
  border: 1px dashed #5D4037;
}

.addon-coupon-item.shot-coupon .coupon-icon {
  font-size: 18px;
  color: #3E2723;
}

.addon-coupon-item.shot-coupon.selected {
  background: linear-gradient(135deg, #D7CCC8 0%, #BCAAA4 100%);
  border-color: #3E2723;
  border-style: solid;
}

.addon-coupon-item.shot-coupon.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(0.8);
}

.addon-coupon-item.shot-coupon .coupon-name {
  color: #3E2723;
  font-weight: 700;
}

.addon-coupon-item.shot-coupon .coupon-tip {
  font-size: 10px;
  color: #FFF;
  margin-left: 8px;
  padding: 2px 6px;
  background: #8D6E63;
  border-radius: 4px;
  font-weight: 600;
}

/* v5.0 外卖地址选择样式 */
.delivery-address-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #F5F5F5;
  border-radius: 12px;
  border: 1px dashed #E0E0E0;
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

/* 优惠券下拉列表 - 复用地址选择样式 */
.coupon-select {
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

.coupon-select:focus {
  border-color: #C69C6D;
}

.coupon-select option:disabled {
  color: #BDBDBD;
}

.no-address-hint {
  color: #9E9E9E;
  font-size: 13px;
  padding: 8px 0;
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

<style scoped>
/* 凑单好物 (Upsell) - 简洁 Tag 样式 */
.addons-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.addon-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  background: #FFF;
  border: 1px solid #E0E0E0;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
}

.addon-tag:hover {
  background: #FDFBF7;
  border-color: #C69C6D;
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(198, 156, 109, 0.15);
}

.addon-tag:active {
  transform: translateY(0);
}

.upsell-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.addon-tag .addon-name {
  font-size: 13px;
  color: #5D4037;
  font-weight: 600;
}

.addon-tag .addon-price {
  font-size: 12px;
  color: #D84315;
  font-weight: 500;
  background: rgba(216, 67, 21, 0.08); /* 极淡的橙色背景 */
  padding: 2px 6px;
  border-radius: 4px;
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
  
  &:hover {
    background: #6F3709;
  }
}

/* v5.3: 黑金会员免运费标签 */
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

.free-delivery {
  color: #C69C6D;
  font-weight: 600;
}

.free-delivery small {
  color: #999;
  font-weight: normal;
  font-size: 11px;
}

/* v5.3.12: 优惠券选择器可点击行 */
.coupon-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: #FAFAFA;
  border: 1px solid #E8E8E8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: #F5F5F5;
    border-color: #D0D0D0;
  }
  
  &:active {
    transform: scale(0.99);
  }
  
  .selector-left {
    flex: 1;
    min-width: 0;
    
    .selected-coupon-name {
      font-size: 14px;
      font-weight: 500;
      color: #333;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
  
  .selector-right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    
    .discount-preview {
      font-size: 15px;
      font-weight: 600;
      color: #E53935;
      font-family: 'DIN Alternate', 'Roboto Condensed', sans-serif;
    }
    
    .chevron-icon {
      color: #BDBDBD;
      transition: transform 0.2s;
    }
  }
  
  &:hover .chevron-icon {
    color: #999;
    transform: translateX(2px);
  }
}

/* v5.7: 优化优惠券选择区域布局 - 上下结构 */
.coupon-section {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  
  .section-label {
    font-weight: 600;
    color: #5D4037;
    margin-left: 4px;
  }
  
  .coupon-selector {
    width: 100%;
  }
}
</style>
