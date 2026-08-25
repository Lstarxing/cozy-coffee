import { getMenuData } from '@/api/product'
import { resolveImageUrl } from '@/config/image'
import { BusinessError } from '@/services/errors/AppError'

function parseJsonObject(value) {
  if (!value) return {}
  if (typeof value === 'object' && !Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch (_) {
    return {}
  }
}

function normalizeOption(value) {
  return value == null ? '' : String(value).trim().replace(/[\s-]+/g, '_').toUpperCase()
}

function isFood(product) {
  return ['bakery', 'dessert', 'food', 'addon'].includes(String(product?.category || '').toLowerCase())
}

function allowedSizes(product) {
  if (isFood(product)) return ['STANDARD']
  const type = String(product?.sizeType || 'MEDIUM_LARGE').toUpperCase()
  // 与选规格页 spec.vue 的 sizeOptions 对齐
  if (type === 'DEFAULT') return ['STANDARD']
  if (type === 'ALL_SIZES') return ['MEDIUM', 'LARGE', 'EXTRA_LARGE']
  return ['STANDARD', 'LARGE']
}

function allowedTemperatures(product) {
  if (isFood(product)) return ['']
  const type = String(product?.tempType || 'ALL_OK').toUpperCase()
  if (type === 'COLD_ONLY') return ['COLD']
  if (type === 'HOT_ONLY') return ['HOT']
  if (type === 'NO_HOT') return ['COLD', 'WARM']
  return ['HOT', 'COLD', 'WARM']
}

function allowedSugarLevels(product) {
  if (isFood(product)) return ['']
  const type = String(product?.sugarType || 'FREE_CHOICE').toUpperCase()
  if (type === 'NO_SUGAR_ONLY') return ['NONE']
  if (type === 'MIN_LESS_SWEET') return ['STANDARD', 'LESS', 'HALF']
  return ['STANDARD', 'LESS', 'HALF', 'NONE']
}

function restoreAllowedOption(value, allowed, aliases = {}) {
  const original = normalizeOption(value)
  const normalized = aliases[original] || original
  if (allowed.includes(normalized)) return { value: normalized, adjusted: false }
  return { value: allowed[0] || '', adjusted: Boolean(original) }
}

function currentUnitPrice(product, line) {
  const base = Number(product?.price ?? 0)
  const sizeExtra = line.cupSize === 'LARGE' ? 3 : 0
  const strengthExtra = line.coffeeStrength === 'STRONG' ? 5 : 0
  const milkExtra = line.milkType && line.milkType !== 'WHOLE' ? 4 : 0
  return Number((base + sizeExtra + strengthExtra + milkExtra).toFixed(2))
}

export function createReorderCartLine(item, product) {
  const options = parseJsonObject(item?.optionsJson)
  const size = restoreAllowedOption(item?.cupSize, allowedSizes(product), { DEFAULT: 'STANDARD' })
  const temperature = restoreAllowedOption(item?.temperature, allowedTemperatures(product), { ICE: 'COLD', ICED: 'COLD' })
  const sugar = restoreAllowedOption(item?.sugarLevel, allowedSugarLevels(product), { FULL: 'STANDARD', NORMAL: 'STANDARD' })
  const food = isFood(product)
  const category = String(product?.category || '').toLowerCase()
  const originalStrength = normalizeOption(item?.coffeeStrength)
  const coffeeStrength = food ? '' : (['NORMAL', 'STRONG'].includes(originalStrength) ? originalStrength : 'NORMAL')
  const originalMilkType = normalizeOption(options.milkType ?? item?.milkType)
  const supportsMilk = !food && category !== 'soe'
  const milkType = supportsMilk ? (originalMilkType || 'WHOLE') : ''
  const modifierAdjusted = (!food && Boolean(originalStrength) && !['NORMAL', 'STRONG'].includes(originalStrength)) ||
    (!supportsMilk && Boolean(originalMilkType))
  const addons = []

  if (coffeeStrength === 'STRONG') addons.push({ code: 'EXTRA_SHOT', name: '加浓', price: 5 })
  if (milkType && milkType !== 'WHOLE') addons.push({ code: 'SPECIAL_MILK', name: milkType, price: 4 })

  const line = {
    ...product,
    productId: String(product.id ?? product.productId),
    id: product.id ?? product.productId,
    skuId: options.skuId ?? item?.skuId ?? '',
    name: product.name || item?.productName || item?.name || '商品',
    image: resolveImageUrl(product.imageUrl) || resolveImageUrl(product.image)
      || resolveImageUrl(item?.productImage) || resolveImageUrl(item?.image) || '/static/images/default-product.png',
    basePrice: Number(product.price || 0),
    cupSize: size.value,
    temperature: temperature.value,
    sugarLevel: sugar.value,
    milkType,
    coffeeStrength,
    addons,
    quantity: Math.max(1, Math.min(10, Number.parseInt(item?.quantity, 10) || 1))
  }

  line.price = currentUnitPrice(product, line)
  return {
    line,
    adjusted: size.adjusted || temperature.adjusted || sugar.adjusted || modifierAdjusted
  }
}

export async function restoreOrderToCart({ order, cartStore, menuApi = getMenuData } = {}) {
  if (!cartStore?.addItem) {
    throw new BusinessError('购物车暂不可用，请稍后重试', { code: 'CART_UNAVAILABLE' })
  }

  const orderItems = Array.isArray(order?.items) ? order.items : []
  if (!orderItems.length) {
    return { restoredItems: [], invalidItems: [], adjustedItems: [], restoredQuantity: 0 }
  }

  const response = await menuApi()
  const source = response?.data ?? response
  if (!Array.isArray(source)) {
    throw new BusinessError('当前菜单数据异常，请稍后重试', { code: 'MENU_INVALID_RESPONSE', retryable: true })
  }

  const activeProducts = new Map(source
    .filter(product => !product.status || String(product.status).toLowerCase() === 'active')
    .map(product => [String(product.id ?? product.productId), product]))

  const restoredItems = []
  const invalidItems = []
  const adjustedItems = []

  orderItems.forEach(item => {
    const productId = String(item?.productId ?? '')
    const product = activeProducts.get(productId)
    if (!product) {
      invalidItems.push({ item, reason: 'ITEM_OFFLINE' })
      return
    }

    const restored = createReorderCartLine(item, product)
    cartStore.addItem(restored.line, restored.line.quantity)
    restoredItems.push(restored.line)
    if (restored.adjusted) adjustedItems.push(restored.line)
  })

  return {
    restoredItems,
    invalidItems,
    adjustedItems,
    restoredQuantity: restoredItems.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
  }
}
