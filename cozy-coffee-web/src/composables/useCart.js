import { ref, watch } from 'vue'

const STORAGE_KEY = 'cozy_coffee_cart'
const SESSION_KEY = 'cozy_coffee_cart_session'

function loadFromStorage() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : []
  } catch (e) {
    console.error('Failed to parse cart from localStorage', e)
    localStorage.removeItem(STORAGE_KEY)
    return []
  }
}

function loadSession() {
  try {
    const saved = localStorage.getItem(SESSION_KEY)
    return saved ? JSON.parse(saved) : {}
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return {}
  }
}

function saveSession(state) {
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(state))
  } catch { /* quota exceeded, ignore */ }
}

// Singleton state — shared across all useCart() calls
const cartItems = ref(loadFromStorage())
const initialSession = loadSession()
const couponCode = ref(initialSession.couponCode || '')
const addonCouponCodes = ref(initialSession.addonCouponCodes || [])
const diningMethod = ref(initialSession.diningMethod || 'DINE_IN')

// Auto-save session on change
watch([couponCode, addonCouponCodes, diningMethod], ([code, addons, method]) => {
  saveSession({ couponCode: code, addonCouponCodes: addons, diningMethod: method })
}, { deep: true })

let saveTimer = null

// Auto-save cart items to localStorage with 500ms debounce
watch(cartItems, (newVal) => {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newVal))
  }, 500)
}, { deep: true })

export function useCart() {

  function addToCart(item) {
    const existingIndex = cartItems.value.findIndex(
      cartItem =>
        cartItem.productId === item.productId &&
        cartItem.cupSize === item.cupSize &&
        cartItem.sugarLevel === item.sugarLevel &&
        cartItem.temperature === item.temperature &&
        cartItem.coffeeStrength === item.coffeeStrength &&
        (cartItem.milkType || 'WHOLE') === (item.milkType || 'WHOLE')
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
    diningMethod.value = 'DINE_IN'
    saveSession({ couponCode: '', addonCouponCodes: [], diningMethod: 'DINE_IN' })
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
