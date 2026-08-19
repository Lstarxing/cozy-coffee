/**
 * cartPrice.js — Pure cart/checkout price computation helpers
 *
 * Extracted from ShoppingCart.vue computed blocks so the pricing logic
 * can be unit-tested without mounting Vue components. All functions are
 * pure: input data in, number out. Behavior is identical to the original
 * computed expressions.
 */

/**
 * Sum of (basePrice || unitPrice) * quantity across cart items.
 * @param {Array} items
 * @returns {number}
 */
export function computeBaseSubtotal(items) {
  if (!items || !Array.isArray(items)) return 0
  return items.reduce((sum, item) => {
    const basePrice = item.basePrice || item.unitPrice
    return sum + basePrice * item.quantity
  }, 0)
}

/**
 * Sum of extraPrices.cup * quantity across cart items.
 * @param {Array} items
 * @returns {number}
 */
export function computeCupExtraTotal(items) {
  if (!items || !Array.isArray(items)) return 0
  return items.reduce((sum, item) => {
    const cupExtra = item.extraPrices?.cup || 0
    return sum + cupExtra * item.quantity
  }, 0)
}

/**
 * Sum of extraPrices.strength * quantity across cart items.
 * @param {Array} items
 * @returns {number}
 */
export function computeStrengthExtraTotal(items) {
  if (!items || !Array.isArray(items)) return 0
  return items.reduce((sum, item) => {
    const strengthExtra = item.extraPrices?.strength || 0
    return sum + strengthExtra * item.quantity
  }, 0)
}

/**
 * Sum of extraPrices.milk * quantity across cart items.
 * @param {Array} items
 * @returns {number}
 */
export function computeMilkExtraTotal(items) {
  if (!items || !Array.isArray(items)) return 0
  return items.reduce((sum, item) => {
    const milkExtra = item.extraPrices?.milk || 0
    return sum + milkExtra * item.quantity
  }, 0)
}

/**
 * Full subtotal: base + cup + strength + milk, all * quantity.
 * @param {Array} items
 * @returns {number}
 */
export function computeSubtotal(items) {
  return computeBaseSubtotal(items) + computeStrengthExtraTotal(items) + computeCupExtraTotal(items) + computeMilkExtraTotal(items)
}

/**
 * Detect whether any cart item carries an extra shot (STRONG strength or
 * an optionsJson hint for extra shot / 加浓).
 * @param {Array} items
 * @returns {boolean}
 */
export function detectHasExtraShot(items) {
  if (!items || !Array.isArray(items)) return false
  return items.some(item => {
    if (item.coffeeStrength === 'STRONG') return true
    if (item.optionsJson) {
      const opts = item.optionsJson.toLowerCase()
      return opts.includes('extra_shot') || opts.includes('加浓')
    }
    return false
  })
}

/**
 * Total count of extra-shot cups (STRONG items, weighted by quantity).
 * @param {Array} items
 * @returns {number}
 */
export function computeExtraShotCount(items) {
  if (!items || !Array.isArray(items)) return 0
  let count = 0
  items.forEach(item => {
    if (item.coffeeStrength === 'STRONG') count += item.quantity
  })
  return count
}

/**
 * Black-gold member discount: SOE category items get a 15% discount
 * (LARGE cups add ¥3 to the base price first).
 *
 * @param {Array} items
 * @param {string} userLevel - member level (e.g. 'basic' | 'black')
 * @returns {number}
 */
export function computeMemberDiscount(items, userLevel) {
  const level = userLevel || 'basic'
  if (level !== 'black') return 0
  if (!items || !Array.isArray(items)) return 0

  let totalDiscount = 0
  items.forEach(item => {
    if (item.category === 'soe') {
      let itemBasePrice = item.basePrice || item.unitPrice
      if (item.cupSize === 'LARGE') itemBasePrice += 3
      const itemBaseAmount = itemBasePrice * item.quantity
      const itemDiscount = itemBaseAmount * 0.15
      totalDiscount += itemDiscount
    }
  })
  return totalDiscount
}

/**
 * Delivery fee discount from black-gold membership or a DELIVERY_FEE coupon.
 *
 * @param {Object} ctx
 * @param {boolean} ctx.isBlackGoldMember
 * @param {string}  ctx.diningMethod - TAKEOUT | DELIVERY
 * @param {number}  ctx.deliveryFee
 * @param {boolean} ctx.hasDeliveryCoupon
 * @returns {number}
 */
export function computeDeliveryFeeDiscount({ isBlackGoldMember, diningMethod, deliveryFee, hasDeliveryCoupon }) {
  if (isBlackGoldMember && diningMethod === 'DELIVERY') {
    return deliveryFee
  }
  if (hasDeliveryCoupon && diningMethod === 'DELIVERY') {
    return Math.min(10, deliveryFee)
  }
  return 0
}

/**
 * Shot coupon discount: ¥5 per selected shot coupon, capped by the total
 * strength-extra amount. Only applies when the cart has an extra shot.
 *
 * @param {Object} ctx
 * @param {number} ctx.shotCouponCount
 * @param {boolean} ctx.hasExtraShot
 * @param {number} ctx.strengthExtraTotal
 * @returns {number}
 */
export function computeShotDiscount({ shotCouponCount, hasExtraShot, strengthExtraTotal }) {
  if (shotCouponCount > 0 && hasExtraShot) {
    const maxDiscount = shotCouponCount * 5
    return Math.min(strengthExtraTotal, maxDiscount)
  }
  return 0
}

/**
 * Final payable amount after member discount, main coupon discount and
 * shot-coupon discount, plus delivery fee (net of delivery discount).
 *
 * @param {Object} ctx
 * @param {number} ctx.baseSubtotal
 * @param {number} ctx.cupExtraTotal
 * @param {number} ctx.milkExtraTotal
 * @param {number} ctx.strengthExtraTotal
 * @param {number} ctx.memberDiscount
 * @param {number} ctx.mainDiscount
 * @param {number} ctx.shotDiscount
 * @param {string} ctx.diningMethod - TAKEOUT | DELIVERY
 * @param {number} ctx.deliveryFee
 * @param {number} ctx.deliveryFeeDiscount
 * @returns {number}
 */
export function computeFinalTotal({
  baseSubtotal,
  cupExtraTotal,
  milkExtraTotal,
  strengthExtraTotal,
  otherAddonTotal = 0,
  memberDiscount,
  mainDiscount,
  shotDiscount,
  diningMethod,
  deliveryFee,
  deliveryFeeDiscount
}) {
  const baseAmountForDiscount = baseSubtotal + cupExtraTotal
  const addonsAmount = milkExtraTotal + strengthExtraTotal + otherAddonTotal
  const totalAmount = baseAmountForDiscount + addonsAmount

  const afterMemberDiscount = Math.max(0, totalAmount - memberDiscount)
  const afterAllDiscount = Math.max(0, afterMemberDiscount - mainDiscount - shotDiscount)

  let deliveryAmount = 0
  if (diningMethod === 'DELIVERY') {
    deliveryAmount = Math.max(0, deliveryFee - deliveryFeeDiscount)
  }

  return Math.max(0, afterAllDiscount + deliveryAmount)
}

const BASE_MULTIPLIER_MAP = {
  basic: 1,
  silver: 1.1,
  gold: 1.2,
  diamond: 1.3,
  black: 1.5
}

/**
 * Estimated points earned for a final amount, per member level, with the
 * black-gold monthly acceleration window applied when available.
 *
 * @param {Object} ctx
 * @param {number} ctx.amount
 * @param {string} ctx.level - member level
 * @param {number} ctx.accelerateRemaining
 * @returns {number}
 */
export function computeEstimatedPoints({ amount, level, accelerateRemaining }) {
  if (amount <= 0) return 0

  const currentLevel = level || 'basic'
  const baseMultiplier = BASE_MULTIPLIER_MAP[currentLevel] || 1

  if (currentLevel === 'black' && accelerateRemaining > 0) {
    const accelerateMultiplier = 1.7
    if (amount <= accelerateRemaining) {
      return Math.floor(amount * accelerateMultiplier)
    }
    const accelPart = accelerateRemaining * accelerateMultiplier
    const normalPart = (amount - accelerateRemaining) * baseMultiplier
    return Math.floor(accelPart + normalPart)
  }

  return Math.floor(amount * baseMultiplier)
}
