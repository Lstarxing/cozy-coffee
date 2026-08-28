import { ref, watch } from 'vue'

const STORAGE_KEY = 'cozy_coffee_cart'

function loadFromStorage() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (!saved) return { items: [], selectedCouponCode: '', selectedAddonCoupons: [], diningMethod: 'TAKEOUT' }
    const data = JSON.parse(saved)
    if (Array.isArray(data)) return { items: data, selectedCouponCode: '', selectedAddonCoupons: [], diningMethod: 'TAKEOUT' }
    return {
      items: data.items || [],
      selectedCouponCode: data.selectedCouponCode || '',
      selectedAddonCoupons: data.selectedAddonCoupons || [],
      diningMethod: data.diningMethod || 'TAKEOUT'
    }
  } catch (e) {
    console.error('Failed to parse cart from localStorage', e)
    localStorage.removeItem(STORAGE_KEY)
    return { items: [], selectedCouponCode: '', selectedAddonCoupons: [], diningMethod: 'TAKEOUT' }
  }
}

const saved = loadFromStorage()
const cartItems = ref(saved.items)
const couponCode = ref(saved.selectedCouponCode)
const addonCouponCodes = ref(saved.selectedAddonCoupons)
const diningMethod = ref(saved.diningMethod)

let saveTimer = null

watch([cartItems, couponCode, addonCouponCodes, diningMethod], () => {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      items: cartItems.value,
      selectedCouponCode: couponCode.value,
      selectedAddonCoupons: addonCouponCodes.value,
      diningMethod: diningMethod.value
    }))
  }, 500)
}, { deep: true })

export function useCart() {

  function addToCart(item) {
    // V2: 加料选择参与去重（同杯型/糖度/温度但不同风味/奶型的订单不合并）
    const addonKey = (addons) => (Array.isArray(addons) ? addons.map(a => a.code).sort().join(',') : '')
    const existingIndex = cartItems.value.findIndex(
      cartItem =>
        cartItem.productId === item.productId &&
        cartItem.cupSize === item.cupSize &&
        cartItem.sugarLevel === item.sugarLevel &&
        cartItem.temperature === item.temperature &&
        cartItem.coffeeStrength === item.coffeeStrength &&
        (cartItem.milkType || 'WHOLE') === (item.milkType || 'WHOLE') &&
        addonKey(cartItem.addons) === addonKey(item.addons)
    )

    if (existingIndex > -1) {
      cartItems.value[existingIndex].quantity += (item.quantity || 1)
    } else {
      cartItems.value.push({ ...item })
    }
  }

  function updateQuantity(index, qty) {
    if (qty <= 0) {
      cartItems.value.splice(index, 1)
    } else {
      cartItems.value[index].quantity = qty
    }
  }

  function removeItem(index) {
    cartItems.value.splice(index, 1)
  }

  function clearCart() {
    cartItems.value = []
    couponCode.value = ''
    addonCouponCodes.value = []
    diningMethod.value = 'TAKEOUT'
  }

  return {
    cartItems,
    couponCode,
    addonCouponCodes,
    diningMethod,
    addToCart,
    updateQuantity,
    removeItem,
    clearCart
  }
}
