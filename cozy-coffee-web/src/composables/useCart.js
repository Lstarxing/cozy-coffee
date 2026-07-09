import { ref, watch } from 'vue'

const STORAGE_KEY = 'cozy_coffee_cart'

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

let saveTimer = null

export function useCart() {
  const cartItems = ref(loadFromStorage())

  // Auto-save to localStorage with 500ms debounce
  watch(cartItems, (newVal) => {
    if (saveTimer) clearTimeout(saveTimer)
    saveTimer = setTimeout(() => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(newVal))
    }, 500)
  }, { deep: true })

  function addToCart(item) {
    const existingIndex = cartItems.value.findIndex(
      cartItem =>
        cartItem.productId === item.productId &&
        cartItem.cupSize === item.cupSize &&
        cartItem.sugarLevel === item.sugarLevel &&
        cartItem.temperature === item.temperature &&
        cartItem.coffeeStrength === item.coffeeStrength
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
  }

  return {
    cartItems,
    addToCart,
    updateQuantity,
    removeItem,
    clearCart
  }
}
