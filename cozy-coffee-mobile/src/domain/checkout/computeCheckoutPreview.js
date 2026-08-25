function money(value) {
  return Math.round((Number(value) || 0) * 100) / 100
}

function couponDiscount(coupon, subtotal) {
  if (!coupon) return 0
  const minimum = Number(coupon.minAmount ?? coupon.minimumAmount ?? 0)
  if (subtotal < minimum) return 0

  const type = String(coupon.couponType || '').toUpperCase()

  // DISCOUNT 券：value 为折扣百分比（50 = 5 折），按小计折算，而非抵扣固定金额
  if (type === 'DISCOUNT') {
    const percent = Number(coupon.value ?? coupon.discountRate ?? 0)
    if (percent > 0 && percent < 100) {
      return money(subtotal * (1 - percent / 100))
    }
  }

  const rate = Number(coupon.discountRate ?? coupon.rate)
  if (Number.isFinite(rate) && rate > 0) {
    const normalizedRate = rate > 1 ? rate / 100 : rate
    return money(subtotal * Math.max(0, 1 - normalizedRate))
  }

  // 满减 / 抵扣券：value 即抵扣金额
  return money(coupon.discountAmount ?? coupon.value ?? coupon.amount ?? 0)
}

/** 辅券抵扣（SHOT 加浓缩券 / DELIVERY_FEE 配送费券）：取面值；加入折扣后与后端「实付=小计−券+配送费−配送费券」等价 */
function addonCouponsDiscount(addonCoupons) {
  return (addonCoupons || []).reduce((sum, c) => {
    const type = String(c.couponType || '').toUpperCase()
    if (type === 'SHOT' || type === 'DELIVERY_FEE') return sum + Number(c.value || 0)
    return sum
  }, 0)
}

function fnv1a(value) {
  let hash = 0x811c9dc5
  for (let i = 0; i < value.length; i += 1) {
    hash ^= value.charCodeAt(i)
    hash = Math.imul(hash, 0x01000193)
  }
  return (hash >>> 0).toString(16).padStart(8, '0')
}

function canonicalInput(input, subtotal, discount, deliveryFee) {
  const lines = [...(input.items || [])]
    .map(item => ({
      lineKey: item.lineKey || item.id || '',
      productId: String(item.productId ?? item.id ?? ''),
      quantity: Number(item.quantity || 0),
      price: money(item.price ?? item.unitPrice)
    }))
    .sort((a, b) => `${a.lineKey}:${a.productId}`.localeCompare(`${b.lineKey}:${b.productId}`))

  const addonCodes = (input.addonCoupons || [])
    .map(c => String(c.couponCode ?? c.code ?? c.id ?? ''))
    .sort()
    .join(',')

  return JSON.stringify({
    version: 'v1',
    lines,
    couponId: input.coupon?.id ?? input.coupon?.couponCode ?? input.selectedCouponId ?? '',
    couponValue: discount,
    addonCodes,
    storeId: input.storeId ?? '',
    pickupTime: input.pickupTime ?? '',
    subtotal,
    deliveryFee
  })
}

export function computeCheckoutPreview(input = {}) {
  const subtotal = money((input.items || []).reduce((sum, item) => (
    sum + Number(item.price ?? item.unitPrice ?? 0) * Number(item.quantity || 0)
  ), 0))
  const discount = Math.min(subtotal, money(couponDiscount(input.coupon, subtotal) + addonCouponsDiscount(input.addonCoupons)))
  const deliveryFee = money(input.deliveryFee ?? 0)
  const payable = money(Math.max(0, subtotal - discount + deliveryFee))
  const previewVersion = `local-v1:${fnv1a(canonicalInput(input, subtotal, discount, deliveryFee))}`

  return Object.freeze({
    subtotal,
    discount,
    deliveryFee,
    payable,
    previewVersion,
    previewToken: previewVersion,
    source: 'local',
    expiresAt: null
  })
}
