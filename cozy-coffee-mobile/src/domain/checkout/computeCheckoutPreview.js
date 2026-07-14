function money(value) {
  return Math.round((Number(value) || 0) * 100) / 100
}

function couponDiscount(coupon, subtotal) {
  if (!coupon) return 0
  const minimum = Number(coupon.minAmount ?? coupon.minimumAmount ?? 0)
  if (subtotal < minimum) return 0

  const rate = Number(coupon.discountRate ?? coupon.rate)
  if (Number.isFinite(rate) && rate > 0) {
    const normalizedRate = rate > 1 ? rate / 100 : rate
    return money(subtotal * Math.max(0, 1 - normalizedRate))
  }

  return money(coupon.discountAmount ?? coupon.value ?? coupon.amount ?? 0)
}

function fnv1a(value) {
  let hash = 0x811c9dc5
  for (let i = 0; i < value.length; i += 1) {
    hash ^= value.charCodeAt(i)
    hash = Math.imul(hash, 0x01000193)
  }
  return (hash >>> 0).toString(16).padStart(8, '0')
}

function canonicalInput(input, subtotal, discount) {
  const lines = [...(input.items || [])]
    .map(item => ({
      lineKey: item.lineKey || item.id || '',
      productId: String(item.productId ?? item.id ?? ''),
      quantity: Number(item.quantity || 0),
      price: money(item.price ?? item.unitPrice)
    }))
    .sort((a, b) => `${a.lineKey}:${a.productId}`.localeCompare(`${b.lineKey}:${b.productId}`))

  return JSON.stringify({
    version: 'v1',
    lines,
    couponId: input.coupon?.id ?? input.coupon?.couponCode ?? input.selectedCouponId ?? '',
    couponValue: discount,
    storeId: input.storeId ?? '',
    pickupTime: input.pickupTime ?? '',
    subtotal
  })
}

export function computeCheckoutPreview(input = {}) {
  const subtotal = money((input.items || []).reduce((sum, item) => (
    sum + Number(item.price ?? item.unitPrice ?? 0) * Number(item.quantity || 0)
  ), 0))
  const discount = Math.min(subtotal, couponDiscount(input.coupon, subtotal))
  const payable = money(Math.max(0, subtotal - discount))
  const previewVersion = `local-v1:${fnv1a(canonicalInput(input, subtotal, discount))}`

  return Object.freeze({
    subtotal,
    discount,
    payable,
    previewVersion,
    previewToken: previewVersion,
    source: 'local',
    expiresAt: null
  })
}
