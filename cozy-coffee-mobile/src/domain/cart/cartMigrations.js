import { CART_KEY_VERSION, createCartLineKey, isCurrentCartLineKey, normalizeCartOptions } from './cartLineKey'

function parseStorage(raw) {
  if (!raw) return null
  if (typeof raw === 'string') return JSON.parse(raw)
  return raw
}

function isSafeLegacyProductId(value) {
  return typeof value === 'number' || (typeof value === 'string' && /^\d+$/.test(value.trim()))
}

export function normalizeCartLine(item) {
  const options = normalizeCartOptions(item)
  const lineKey = createCartLineKey(options)
  const quantity = Math.max(1, Math.min(10, Number.parseInt(item.quantity, 10) || 1))
  const price = Number(item.price ?? item.unitPrice ?? item.basePrice ?? 0)

  return {
    ...item,
    ...options,
    id: lineKey,
    lineKey,
    quantity,
    price: Number.isFinite(price) ? price : 0
  }
}

export function migrateCartStorage(raw) {
  let parsed
  try {
    parsed = parseStorage(raw)
  } catch (_) {
    return { version: CART_KEY_VERSION, items: [], discardedItems: [{ reason: 'INVALID_STORAGE' }] }
  }

  const sourceItems = Array.isArray(parsed) ? parsed : (Array.isArray(parsed?.items) ? parsed.items : [])
  const items = []
  const discardedItems = []

  sourceItems.forEach(item => {
    const productId = item?.productId ?? item?.id
    const alreadyVersioned = isCurrentCartLineKey(item?.lineKey)
    if (!alreadyVersioned && !isSafeLegacyProductId(productId)) {
      discardedItems.push({ item, reason: 'AMBIGUOUS_PRODUCT_ID' })
      return
    }

    const normalized = normalizeCartLine({ ...item, productId: item.productId ?? item.id })
    const existing = items.find(line => line.lineKey === normalized.lineKey)
    if (existing) {
      existing.quantity = Math.min(10, existing.quantity + normalized.quantity)
    } else {
      items.push(normalized)
    }
  })

  return { version: CART_KEY_VERSION, items, discardedItems }
}
