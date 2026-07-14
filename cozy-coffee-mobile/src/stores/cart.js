import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { CART_KEY_VERSION, createCartLineKey } from '@/domain/cart/cartLineKey'
import { migrateCartStorage, normalizeCartLine } from '@/domain/cart/cartMigrations'

const STORAGE_KEY = 'cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const discardedItems = ref([])

  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const subtotal = computed(() => Number(items.value.reduce(
    (sum, item) => sum + Number(item.price || 0) * item.quantity,
    0
  ).toFixed(2)))
  const totalPrice = computed(() => subtotal.value.toFixed(2))

  function saveToStorage() {
    uni.setStorageSync(STORAGE_KEY, JSON.stringify({ version: CART_KEY_VERSION, items: items.value }))
  }

  function restore() {
    const migration = migrateCartStorage(uni.getStorageSync(STORAGE_KEY))
    items.value = migration.items
    discardedItems.value = migration.discardedItems
    saveToStorage()
    return migration
  }

  function resolveLineKey(value) {
    if (typeof value === 'string' && value.startsWith(`${CART_KEY_VERSION}:`)) return value
    return items.value.find(item => item.id === value || item.productId === String(value))?.lineKey || value
  }

  function addItem(product, quantity = product?.quantity || 1) {
    const normalized = normalizeCartLine({ ...product, quantity })
    const existing = items.value.find(item => item.lineKey === normalized.lineKey)
    if (existing) {
      existing.quantity = Math.min(10, existing.quantity + normalized.quantity)
    } else {
      items.value.push(normalized)
    }
    saveToStorage()
    return normalized.lineKey
  }

  function setQuantity(lineKey, quantity) {
    const resolved = resolveLineKey(lineKey)
    const item = items.value.find(line => line.lineKey === resolved)
    if (!item) return
    const next = Math.max(0, Math.min(10, Number.parseInt(quantity, 10) || 0))
    if (next === 0) items.value = items.value.filter(line => line.lineKey !== resolved)
    else item.quantity = next
    saveToStorage()
  }

  function increaseQty(lineKey) {
    const resolved = resolveLineKey(lineKey)
    const item = items.value.find(line => line.lineKey === resolved)
    if (item) setQuantity(resolved, item.quantity + 1)
  }

  function decreaseQty(lineKey) {
    const resolved = resolveLineKey(lineKey)
    const item = items.value.find(line => line.lineKey === resolved)
    if (item) setQuantity(resolved, item.quantity - 1)
  }

  function removeItem(lineKey) {
    const resolved = resolveLineKey(lineKey)
    items.value = items.value.filter(item => item.lineKey !== resolved)
    saveToStorage()
  }

  function updateOptions(lineKey, options) {
    const resolved = resolveLineKey(lineKey)
    const index = items.value.findIndex(item => item.lineKey === resolved)
    if (index < 0) return null

    const updated = normalizeCartLine({ ...items.value[index], ...options })
    const targetIndex = items.value.findIndex((item, itemIndex) => itemIndex !== index && item.lineKey === updated.lineKey)
    if (targetIndex >= 0) {
      items.value[targetIndex].quantity = Math.min(10, items.value[targetIndex].quantity + updated.quantity)
      items.value.splice(index, 1)
    } else {
      items.value.splice(index, 1, updated)
    }
    saveToStorage()
    return updated.lineKey
  }

  function clearCart() {
    items.value = []
    saveToStorage()
  }

  restore()

  return {
    items,
    discardedItems,
    totalCount,
    subtotal,
    totalPrice,
    addItem,
    setQuantity,
    increaseQty,
    decreaseQty,
    removeItem,
    updateOptions,
    clearCart,
    restore,
    createCartLineKey
  }
})
