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

// 规格允许选项：单一事实源取后端 allowedSizes/allowedTemps/allowedSugars（旧数据缺字段时兜底默认值）
function allowedSizes(product) {
  return Array.isArray(product?.allowedSizes) ? product.allowedSizes : ['MEDIUM', 'LARGE']
}
function allowedTemperatures(product) {
  return Array.isArray(product?.allowedTemps) ? product.allowedTemps : ['HOT', 'COLD']
}
function allowedSugarLevels(product) {
  return Array.isArray(product?.allowedSugars) ? product.allowedSugars : ['STANDARD', 'LESS', 'HALF', 'NO_ADDED_SUGAR']
}

function restoreAllowedOption(value, allowed, aliases = {}) {
  const original = normalizeOption(value)
  const normalized = aliases[original] || original
  if (allowed.includes(normalized)) return { value: normalized, adjusted: false }
  return { value: allowed[0] || '', adjusted: Boolean(original) }
}

// V2：奶型以 addons_json 成交快照为准（后端规范化，WHOLE_MILK=全脂/OAT_MILK/COCONUT_MILK）
function milkCodeFromAddons(item) {
  if (!item?.addonsJson) return ''
  try {
    const addons = JSON.parse(item.addonsJson)
    const map = { OAT_MILK: 'OAT', COCONUT_MILK: 'COCONUT', WHOLE_MILK: 'WHOLE' }
    const m = (Array.isArray(addons) ? addons : []).find(a => map[a.code])
    return m ? map[m.code] : ''
  } catch (_) { return '' }
}

function parseAddonsList(value) {
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch (_) { return [] }
}

function productHasMilkGroup(product) {
  const groups = Array.isArray(product?.addonGroups) ? product.addonGroups : []
  return groups.some(g => g.category === 'MILK')
}

function currentUnitPrice(product, line, addons) {
  let base = 0
  const sizeType = String(product?.sizeType || 'MEDIUM_LARGE').toUpperCase()
  if (isFood(product) || sizeType === 'DEFAULT') {
    base = Number(product?.price || 0)
  } else {
    const medium = Number(product?.priceMedium)
    const large = Number(product?.priceLarge)
    if (medium || large) {
      base = line.cupSize === 'LARGE' ? large : medium
    } else {
      // 旧商品无中/大杯价：基础价 + 大杯 +3 兜底
      base = Number(product?.price || 0) + (line.cupSize === 'LARGE' ? 3 : 0)
    }
  }
  const addonFee = addons.reduce((s, a) => s + Number(a.price || 0), 0)
  return Number((base + addonFee).toFixed(2))
}

export function createReorderCartLine(item, product) {
  const options = parseJsonObject(item?.optionsJson)
  const size = restoreAllowedOption(item?.cupSize, allowedSizes(product), { DEFAULT: 'STANDARD' })
  const temperature = restoreAllowedOption(item?.temperature, allowedTemperatures(product), { ICE: 'COLD', ICED: 'COLD' })
  const sugar = restoreAllowedOption(item?.sugarLevel, allowedSugarLevels(product), { FULL: 'STANDARD', NORMAL: 'STANDARD', NONE: 'NO_ADDED_SUGAR' })
  const food = isFood(product)
  const category = String(product?.category || '').toLowerCase()
  const originalStrength = normalizeOption(item?.coffeeStrength)
  const coffeeStrength = food ? '' : (['NORMAL', 'STRONG'].includes(originalStrength) ? originalStrength : 'NORMAL')

  // 奶型：addons_json 权威；旧订单回退 options_json.milkType；商品无 MILK 组（黑咖）→ 无奶
  const milkFromAddons = milkCodeFromAddons(item)
  const legacyMilk = normalizeOption(options.milkType ?? item?.milkType)
  const supportsMilk = !food && (
    productHasMilkGroup(product) ||
    (!Array.isArray(product?.addonGroups) && category !== 'soe') // 旧商品无加料组：按旧分类判断
  )
  const milkType = milkFromAddons || (supportsMilk ? (legacyMilk || 'WHOLE') : '')
  const modifierAdjusted =
    (!food && Boolean(originalStrength) && !['NORMAL', 'STRONG'].includes(originalStrength)) ||
    (Boolean(legacyMilk) && !milkFromAddons && !supportsMilk)

  // 加料：addons_json 还原（剔除默认全脂奶 WHOLE_MILK——后端自动注入必选默认项）
  const addons = []
  const parsedAddons = parseAddonsList(item?.addonsJson)
  if (parsedAddons.length) {
    parsedAddons.forEach(a => {
      if (!a || !a.code || a.code === 'WHOLE_MILK') return
      addons.push({ code: a.code, name: a.name, price: Number(a.price || 0) })
    })
  } else {
    // 旧订单兜底（无 addons_json）：coffeeStrength / options.milkType → 真实加料码
    if (coffeeStrength === 'STRONG') addons.push({ code: 'EXTRA_SHOT', name: '加浓', price: 5 })
    const legacyMilkCode = milkType === 'OAT' ? 'OAT_MILK' : milkType === 'COCONUT' ? 'COCONUT_MILK' : ''
    if (legacyMilkCode) addons.push({ code: legacyMilkCode, name: milkType === 'OAT' ? '燕麦奶' : '椰奶', price: 3 })
  }

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

  line.price = currentUnitPrice(product, line, addons)
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
