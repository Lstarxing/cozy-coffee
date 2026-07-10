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
        <!-- Cart item list -->
        <CartItemList
          :cart-items="cartItems"
          :get-image-url="getImageUrl"
          :handle-image-error="handleImageError"
          @update-quantity="updateQuantity"
          @remove-item="removeItem"
        />

        <div class="cart-options-area">
          <!-- Dining method -->
          <DiningMethodSelector
            v-model="diningMethod"
            :delivery-fee="deliveryFee"
            :is-black-gold-member="isBlackGoldMember"
          />

          <!-- Delivery address -->
          <DeliveryAddress
            :visible="diningMethod === 'DELIVERY'"
            :user-addresses="userAddresses"
            :selected-address-id="selectedAddressId"
            @update:selected-address-id="selectedAddressId = $event"
            @open-address-dialog="handleOpenAddressDialog"
          />

          <!-- Upsell -->
          <div class="cart-section">
            <label class="section-label">凑单好物</label>
            <div class="addons-grid">
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

          <!-- Coupon selector -->
          <CouponSelector
            :filtered-main-coupons="filteredMainCoupons"
            :addon-coupons="addonCoupons"
            :selected-coupon-code="selectedCouponCode"
            :selected-addon-coupons="selectedAddonCoupons"
            :remark="remark"
            :is-addon-coupon-disabled="couponIsAddonDisabled"
            :get-addon-coupon-desc="couponGetDesc"
            :get-addon-coupon-tip="couponGetTip"
            @update:selected-coupon-code="selectedCouponCode = $event"
            @update:selected-addon-coupons="selectedAddonCoupons = $event"
            @update:remark="remark = $event"
          />
        </div>
      </template>
    </div>

    <!-- Footer: price summary + checkout -->
    <PriceSummary
      v-if="cartItems.length > 0"
      :subtotal="subtotal"
      :dining-method="diningMethod"
      :is-black-gold-member="isBlackGoldMember"
      :delivery-fee="deliveryFee"
      :member-discount="memberDiscount"
      :discount="discount"
      :addon-discount="addonDiscount"
      :final-total="finalTotal"
      :estimated-points="estimatedPoints"
      :is-submitting="isSubmitting"
      @checkout="handleCheckout"
    />

    <!-- Address dialog -->
    <AddressDialog
      v-model="showAddressDialog"
      @saved="handleAddressSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { ShoppingBag, X, Coffee } from 'lucide-vue-next'
import AddressDialog from '@/components/address/AddressDialog.vue'
import { getImageUrl, handleImageError } from '@/utils/image'
import { MEMBER_LEVEL } from '@/constants/user'
import { useAddresses } from '@/composables/useAddresses'
import { useCoupons } from '@/composables/useCoupons'

import CartItemList from './CartItemList.vue'
import DiningMethodSelector from './DiningMethodSelector.vue'
import DeliveryAddress from './DeliveryAddress.vue'
import CouponSelector from './CouponSelector.vue'
import PriceSummary from './PriceSummary.vue'

import {
  isDrinkCategory,
  isBakeryCategory,
  filterCoupons,
  filterMainCoupons,
  calculateCouponDiscount,
  validateCouponForCart,
  getAddonCouponDesc as getAddonCouponDescUtil,
  getAddonCouponTip as getAddonCouponTipUtil,
  isAddonCouponDisabled as isAddonCouponDisabledUtil,
  findBestCoupon
} from '@/utils/couponRules'

// ───────────────────────────────
//  Props & emits
// ───────────────────────────────

const userStore = useUserStore()

const props = defineProps({
  isOpen: Boolean,
  cartItems: Array,
  pointsMultiplier: { type: Number, default: 1 },
  upsellProducts: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'update-cart', 'checkout'])

// ───────────────────────────────
//  Reactive state
// ───────────────────────────────

const selectedCouponCode = ref('')
const remark = ref('')
const isSubmitting = ref(false)

const diningMethod = ref('DINE_IN')
const deliveryFee = ref(3)

const showAddressDialog = ref(false)

const selectedAddonCoupons = ref([])

// ───────────────────────────────
//  Composables
// ───────────────────────────────

const {
  addresses: userAddresses,
  selectedAddressId,
  selectedAddress,
  loadAddresses: loadUserAddresses
} = useAddresses()

const {
  coupons: availableCoupons,
  loadAvailableCoupons
} = useCoupons()

// ───────────────────────────────
//  Basic computed
// ───────────────────────────────

const isBlackGoldMember = computed(() => userStore.userLevel === MEMBER_LEVEL.BLACK)

// ───────────────────────────────
//  Cart splitting helpers
// ───────────────────────────────

const baseSubtotal = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  return props.cartItems.reduce((sum, item) => {
    const basePrice = item.basePrice || item.unitPrice
    return sum + basePrice * item.quantity
  }, 0)
})

const cupExtraTotal = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  return props.cartItems.reduce((sum, item) => {
    const cupExtra = item.extraPrices?.cup || 0
    return sum + cupExtra * item.quantity
  }, 0)
})

const strengthExtraTotal = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  return props.cartItems.reduce((sum, item) => {
    const strengthExtra = item.extraPrices?.strength || 0
    return sum + strengthExtra * item.quantity
  }, 0)
})

const milkExtraTotal = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  return props.cartItems.reduce((sum, item) => {
    const milkExtra = item.extraPrices?.milk || 0
    return sum + milkExtra * item.quantity
  }, 0)
})

const otherExtrasTotal = computed(() => cupExtraTotal.value + milkExtraTotal.value)

const subtotal = computed(() => baseSubtotal.value + strengthExtraTotal.value + otherExtrasTotal.value)

// ───────────────────────────────
//  Extra shot detection
// ───────────────────────────────

const hasExtraShot = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return false
  return props.cartItems.some(item => {
    if (item.coffeeStrength === 'STRONG') return true
    if (item.optionsJson) {
      const opts = item.optionsJson.toLowerCase()
      return opts.includes('extra_shot') || opts.includes('加浓')
    }
    return false
  })
})

const extraShotCount = computed(() => {
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0
  let count = 0
  props.cartItems.forEach(item => {
    if (item.coffeeStrength === 'STRONG') count += item.quantity
  })
  return count
})

// ───────────────────────────────
//  Coupon grouping (from couponRules)
// ───────────────────────────────

const filteredCouponsGroup = computed(() => filterCoupons(availableCoupons.value))

const mainCoupons = computed(() => filteredCouponsGroup.value.mainCoupons)

const addonCoupons = computed(() => filteredCouponsGroup.value.addonCoupons)

// ───────────────────────────────
//  Context object for couponRules
// ───────────────────────────────

const couponContext = computed(() => ({
  hasExtraShot: hasExtraShot.value,
  extraShotCount: extraShotCount.value,
  selectedShotCouponCount: selectedShotCouponCount.value,
  selectedAddonCoupons: selectedAddonCoupons.value,
  diningMethod: diningMethod.value
}))

const couponPricing = computed(() => ({
  baseSubtotal: baseSubtotal.value,
  cupExtraTotal: cupExtraTotal.value,
  subtotal: subtotal.value
}))

const selectedShotCouponCount = computed(() => {
  return selectedAddonCoupons.value.filter(code => {
    const coupon = addonCoupons.value.find(c => c.couponCode === code)
    return coupon?.couponType === 'SHOT'
  }).length
})

// ───────────────────────────────
//  Filtered main coupons (from couponRules)
// ───────────────────────────────

const filteredMainCoupons = computed(() => {
  return filterMainCoupons(mainCoupons.value, props.cartItems, couponPricing.value, couponContext.value)
})

// ───────────────────────────────
//  Wrapper functions for CouponSelector props
// ───────────────────────────────

const couponIsAddonDisabled = (coupon) => {
  return isAddonCouponDisabledUtil(coupon, props.cartItems, couponContext.value)
}

const couponGetDesc = (coupon) => {
  return getAddonCouponDescUtil(coupon, extraShotCount.value)
}

const couponGetTip = (coupon) => {
  return getAddonCouponTipUtil(coupon, props.cartItems, couponContext.value)
}

// ───────────────────────────────
//  Discount calculations
// ───────────────────────────────

const memberDiscount = computed(() => {
  const level = userStore.userLevel || 'basic'
  if (level !== MEMBER_LEVEL.BLACK) return 0
  if (!props.cartItems || !Array.isArray(props.cartItems)) return 0

  let totalDiscount = 0
  props.cartItems.forEach(item => {
    if (item.category === 'soe') {
      let itemBasePrice = item.basePrice || item.unitPrice
      if (item.cupSize === 'LARGE') itemBasePrice += 3
      const itemBaseAmount = itemBasePrice * item.quantity
      const itemDiscount = itemBaseAmount * 0.15
      totalDiscount += itemDiscount
    }
  })
  return totalDiscount
})

const discount = computed(() => {
  if (!selectedCouponCode.value) return 0
  const coupon = availableCoupons.value.find(c => c.couponCode === selectedCouponCode.value)
  if (!coupon) return 0
  return calculateCouponDiscount(coupon, props.cartItems, couponPricing.value)
})

const deliveryFeeDiscount = computed(() => {
  if (isBlackGoldMember.value && diningMethod.value === 'DELIVERY') {
    return deliveryFee.value
  }
  const hasDeliveryCoupon = selectedAddonCoupons.value.some(code => {
    const coupon = addonCoupons.value.find(c => c.couponCode === code)
    return coupon?.couponType === 'DELIVERY_FEE'
  })
  if (hasDeliveryCoupon && diningMethod.value === 'DELIVERY') {
    return Math.min(10, deliveryFee.value)
  }
  return 0
})

const shotDiscount = computed(() => {
  const shotCouponCount = selectedAddonCoupons.value.filter(code => {
    const coupon = addonCoupons.value.find(c => c.couponCode === code)
    return coupon?.couponType === 'SHOT'
  }).length

  if (shotCouponCount > 0 && hasExtraShot.value) {
    const maxDiscount = shotCouponCount * 5
    return Math.min(strengthExtraTotal.value, maxDiscount)
  }
  return 0
})

const addonDiscount = computed(() => deliveryFeeDiscount.value + shotDiscount.value)

const mainCouponAddonDiscountDetails = computed(() => {
  const result = { strength: 0, milk: 0, total: 0 }
  if (!selectedCouponCode.value) return result

  const coupon = availableCoupons.value.find(c => c.couponCode === selectedCouponCode.value)
  if (!coupon) return result

  const rule = coupon.parsedRule || {}
  const freeAddonCount = rule.freeAddon || 0

  if (freeAddonCount > 0 && coupon.couponType === 'EXCHANGE') {
    const drinkItems = props.cartItems.filter(item => isDrinkCategory(item.category))
    const addonList = []
    drinkItems.forEach(item => {
      if (item.extraPrices?.strength) addonList.push({ price: item.extraPrices.strength, type: 'strength' })
      if (item.extraPrices?.milk) addonList.push({ price: item.extraPrices.milk, type: 'milk' })
    })
    addonList.sort((a, b) => b.price - a.price)
    const freeAddons = addonList.slice(0, freeAddonCount)
    freeAddons.forEach(addon => {
      if (addon.type === 'strength') result.strength += addon.price
      else result.milk += addon.price
      result.total += addon.price
    })
  }
  return result
})

const mainCouponAddonDiscount = computed(() => mainCouponAddonDiscountDetails.value.total)

const finalTotal = computed(() => {
  const baseAmountForDiscount = baseSubtotal.value + cupExtraTotal.value
  const addonsAmount = milkExtraTotal.value + strengthExtraTotal.value
  const totalAmount = baseAmountForDiscount + addonsAmount

  const afterMemberDiscount = Math.max(0, totalAmount - memberDiscount.value)
  const mainDiscount = discount.value
  const addonCouponDiscount = shotDiscount.value
  const afterAllDiscount = Math.max(0, afterMemberDiscount - mainDiscount - addonCouponDiscount)

  let deliveryAmount = 0
  if (diningMethod.value === 'DELIVERY') {
    deliveryAmount = Math.max(0, deliveryFee.value - deliveryFeeDiscount.value)
  }

  return Math.max(0, afterAllDiscount + deliveryAmount)
})

const estimatedPoints = computed(() => {
  const amount = finalTotal.value
  if (amount <= 0) return 0

  const level = userStore.userLevel || 'basic'
  const accelerateRemaining = userStore.userInfo?.monthlyAccelerateRemaining ?? 0

  const baseMultiplierMap = {
    basic: 1,
    silver: 1.1,
    gold: 1.2,
    diamond: 1.3,
    black: 1.5
  }
  const baseMultiplier = baseMultiplierMap[level] || 1

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

// ───────────────────────────────
//  Cart actions
// ───────────────────────────────

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
  if (diningMethod.value === 'DELIVERY' && !selectedAddressId.value) {
    ElMessage.warning('请选择配送地址')
    return
  }
  isSubmitting.value = true
  emit('checkout', {
    items: props.cartItems,
    couponCode: selectedCouponCode.value,
    addonCouponCodes: selectedAddonCoupons.value,
    diningMethod: diningMethod.value,
    deliveryAddressId: diningMethod.value === 'DELIVERY' ? selectedAddressId.value : null,
    remark: remark.value
  })
}

const resetSubmitting = () => {
  isSubmitting.value = false
}

defineExpose({ resetSubmitting })

// ───────────────────────────────
//  Upsell
// ───────────────────────────────

const handleAddUpsell = (product) => {
  const existingItem = props.cartItems.find(item => item.productId === product.id)
  if (existingItem) {
    const updatedCart = props.cartItems.map(item =>
      item.productId === product.id ? { ...item, quantity: item.quantity + 1 } : item
    )
    emit('update-cart', updatedCart)
    return
  }

  const newItem = {
    productId: product.id,
    productName: product.name,
    productImage: product.imageUrl,
    unitPrice: product.price,
    quantity: 1,
    category: product.category,
    isNewProduct: product.isNewProduct || false,
    extraPrices: { cup: 0, strength: 0, milk: 0 }
  }

  const isBakery = ['bakery', 'snack', 'dessert', 'food'].includes(product.category)
  if (!isBakery) {
    newItem.cupSize = 'STANDARD'
    newItem.sugarLevel = 'STANDARD'
    newItem.temperature = 'HOT'
    newItem.coffeeStrength = 'NORMAL'
    newItem.milkType = 'WHOLE'
  }

  const updatedCart = [...props.cartItems, newItem]
  emit('update-cart', updatedCart)
}

// ───────────────────────────────
//  Address loading (via composable)
// ───────────────────────────────

const handleOpenAddressDialog = () => {
  showAddressDialog.value = true
}

const handleAddressSaved = async () => {
  await loadUserAddresses()
}

watch(diningMethod, (newVal) => {
  if (newVal === 'DELIVERY' && userAddresses.value.length === 0) {
    loadUserAddresses()
  }
})

// ───────────────────────────────
//  Coupon loading (via composable)
// ───────────────────────────────

const loadAndSelectCoupons = async () => {
  if (subtotal.value > 0) {
    const data = {
      orderAmount: subtotal.value,
      items: props.cartItems.map(item => ({
        productId: item.productId,
        price: item.unitPrice,
        category: item.category,
        quantity: item.quantity,
        cupSize: item.cupSize || 'STANDARD'
      }))
    }
    await loadAvailableCoupons(data)
    autoSelectBestCoupon()
  }
}

const autoSelectBestCoupon = () => {
  const bestCode = findBestCoupon(filteredMainCoupons.value)
  if (bestCode) {
    selectedCouponCode.value = bestCode
  } else {
    selectedCouponCode.value = ''
  }
}

// ───────────────────────────────
//  Extra shot auto-deselect
// ───────────────────────────────

watch(extraShotCount, (newCount, oldCount) => {
  if (newCount < oldCount) {
    const currentShotCouponCount = selectedAddonCoupons.value.filter(code => {
      const coupon = addonCoupons.value.find(c => c.couponCode === code)
      return coupon?.couponType === 'SHOT'
    }).length

    if (currentShotCouponCount > newCount) {
      const shotCoupons = []
      selectedAddonCoupons.value.forEach(code => {
        const coupon = addonCoupons.value.find(c => c.couponCode === code)
        if (coupon?.couponType === 'SHOT') shotCoupons.push(code)
      })
      const excessCount = shotCoupons.length - newCount
      const toRemove = shotCoupons.slice(-excessCount)
      selectedAddonCoupons.value = selectedAddonCoupons.value.filter(
        code => !toRemove.includes(code)
      )
      ElMessage.info(`已自动取消${excessCount}张加浓券（当前仅${newCount}杯加浓饮品）`)
    }
  }
})

// ───────────────────────────────
//  Watchers: reload coupons
// ───────────────────────────────

watch(() => subtotal.value, () => {
  if (subtotal.value > 0) {
    loadAndSelectCoupons()
  } else {
    availableCoupons.value = []
    selectedCouponCode.value = ''
  }
})

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    loadAndSelectCoupons()
    isSubmitting.value = false
  }
})
</script>

<style scoped>
/* ========== 购物车侧边栏 ========== */
.cart-sidebar {
  position: fixed;
  top: 0;
  right: 0;
  width: 560px;
  height: 100vh;
  background: #FCFAF8;
  box-shadow: -10px 0 40px rgba(0, 0, 0, 0.08);
  z-index: 1000;
  transform: translateX(100%);
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex;
  flex-direction: column;
}

.cart-sidebar.open {
  transform: translateX(0);
}

/* 头部 */
.cart-header {
  padding: 32px 32px 20px;
  background: transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0,0,0,0.03);
}

.cart-header h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #5D4037;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.5px;
}

.close-btn {
  background: transparent;
  border: 1px solid rgba(93, 64, 55, 0.1);
  color: #8D6E63;
  font-size: 0;
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

/* 主体 */
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
  color: #D7CCC8;
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

/* 选项区域 */
.cart-options-area {
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px dashed #E0E0E0;
}

/* 凑单好物 */
.cart-section {
  margin-bottom: 24px;
}

.section-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #5D4037;
  margin-bottom: 12px;
}

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
  background: rgba(216, 67, 21, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
