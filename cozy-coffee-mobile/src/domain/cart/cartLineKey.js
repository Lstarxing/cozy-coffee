export const CART_KEY_VERSION = 'v1'

const OPTION_FIELDS = [
  'productId',
  'skuId',
  'cupSize',
  'temperature',
  'sugarLevel',
  'milkType',
  'coffeeStrength'
]

function normalizeId(value) {
  if (value == null) return ''
  return String(value).trim()
}

function normalizeOption(value) {
  if (value == null) return ''
  return String(value).trim().replace(/[\s-]+/g, '_').toUpperCase()
}

export function normalizeCartOptions(item = {}) {
  return {
    productId: normalizeId(item.productId ?? item.id),
    skuId: normalizeId(item.skuId),
    cupSize: normalizeOption(item.cupSize ?? item.size ?? item.selectedSize),
    temperature: normalizeOption(item.temperature ?? item.temp ?? item.selectedTemp),
    sugarLevel: normalizeOption(item.sugarLevel ?? item.sugar ?? item.selectedSugar),
    milkType: normalizeOption(item.milkType ?? item.milk ?? item.selectedMilk),
    coffeeStrength: normalizeOption(item.coffeeStrength ?? item.strength ?? item.selectedStrength)
  }
}

export function createCartLineKey(item) {
  const normalized = normalizeCartOptions(item)
  const parts = OPTION_FIELDS.map(field => encodeURIComponent(normalized[field]))
  return `${CART_KEY_VERSION}:${parts.join('|')}`
}

export function isCurrentCartLineKey(value) {
  return typeof value === 'string' && value.startsWith(`${CART_KEY_VERSION}:`)
}
