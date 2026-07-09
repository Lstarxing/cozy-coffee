/**
 * couponRules.js — Pure coupon validation and calculation utilities
 *
 * ALL functions are pure: they take data in, return data out.
 * No Vue refs, no reactivity, no side effects.
 *
 * 6 coupon types: EXCHANGE, DISCOUNT, FULL_REDUCE, BOGO, SHOT, DELIVERY_FEE
 */
import { COUPON_TYPE } from '@/constants/coupon'

// ──────────────────────────────────────────────
//  Category helpers
// ──────────────────────────────────────────────

/** Check if a category belongs to drinks (not bakery/snack/dessert). */
export function isDrinkCategory(category) {
  const drinkCategories = ['espresso', 'signature', 'cold_brew', 'tea', 'specialty', 'drink']
  return drinkCategories.includes(category) || (category && !['bakery', 'snack', 'dessert', 'food'].includes(category))
}

/** Check if a category belongs to bakery/dessert. */
export function isBakeryCategory(category) {
  if (!category) return false
  return ['bakery', 'snack', 'dessert', 'food', 'cake'].includes(category.toLowerCase())
}

// ──────────────────────────────────────────────
//  Coupon filtering
// ──────────────────────────────────────────────

/**
 * Split coupons into main (displayed in dropdown) and addon (checkbox) groups.
 * @param {Array} coupons - raw available coupons
 * @returns {{ mainCoupons: Array, addonCoupons: Array }}
 */
export function filterCoupons(coupons) {
  const mainCoupons = coupons.filter(c => !['DELIVERY_FEE', 'SHOT'].includes(c.couponType))
  const allAddon = coupons.filter(c => ['DELIVERY_FEE', 'SHOT'].includes(c.couponType))
  const deliveryFeeCoupons = allAddon.filter(c => c.couponType === 'DELIVERY_FEE').slice(0, 1)
  const shotCoupons = allAddon.filter(c => c.couponType === 'SHOT')
  return {
    mainCoupons,
    addonCoupons: [...deliveryFeeCoupons, ...shotCoupons]
  }
}

// ──────────────────────────────────────────────
//  Validation
// ──────────────────────────────────────────────

/**
 * Validate whether a coupon can be applied to the current cart.
 *
 * @param {Object} coupon - coupon object with parsedRule
 * @param {Array}  cartItems
 * @param {Object} context
 * @param {boolean} context.hasExtraShot
 * @param {string}  context.diningMethod  - DINE_IN | TAKEOUT | DELIVERY
 * @returns {{ valid: boolean, reason: string }}
 */
export function validateCouponForCart(coupon, cartItems, context) {
  const rule = coupon.parsedRule || {}
  const items = cartItems || []
  const { hasExtraShot, diningMethod } = context

  // 1. NEW_PRODUCT constraints
  if (coupon.couponType === 'NEW_PRODUCT_HALF' || coupon.couponType === 'NEW_PRODUCT_FREE') {
    const hasNewProduct = items.some(item => item.isNewProduct === true)
    if (!hasNewProduct) {
      return { valid: false, reason: '此券仅限新品饮品使用' }
    }
  }

  // 2. EXCHANGE / DISCOUNT SKU & category constraints
  if (coupon.couponType === COUPON_TYPE.EXCHANGE || coupon.couponType === COUPON_TYPE.DISCOUNT) {
    const skuLimit = rule.skuLimit
    const categoryBlocklist = rule.categoryBlocklist || []
    const limitSingleItem = rule.limit === 'SINGLE_ITEM'

    // STANDARD_ONLY cup size
    if (skuLimit === 'STANDARD_ONLY') {
      const drinkItems = items.filter(item => isDrinkCategory(item.category))
      const standardItems = drinkItems.filter(item => {
        const size = (item.cupSize || 'STANDARD').toUpperCase()
        return size === 'STANDARD' || size === 'MEDIUM'
      })

      if (standardItems.length === 0) {
        return { valid: false, reason: '此券仅限标准杯饮品' }
      }

      if (limitSingleItem) {
        const hasStandardDrink = standardItems.length > 0
        if (!hasStandardDrink) {
          return { valid: false, reason: '购物车中无标准杯饮品' }
        }
      }
    }

    // Category blocklist (signature/soe)
    if (categoryBlocklist.length > 0) {
      const drinkItems = items.filter(item => isDrinkCategory(item.category))
      const allowedItems = drinkItems.filter(item => {
        const cat = (item.category || '').toLowerCase()
        const isBlocked = categoryBlocklist.some(blocked =>
          cat.includes(blocked.toLowerCase()) ||
          cat.includes('手冲')
        )
        return !isBlocked
      })

      if (allowedItems.length === 0) {
        return { valid: false, reason: '此券不适用于SOE/手冲类产品' }
      }
    }

    // Linked product check
    if (rule.linkedProductId) {
      const hasLinkedProduct = items.some(item =>
        item.productId == rule.linkedProductId || item.id == rule.linkedProductId
      )
      if (!hasLinkedProduct) {
        return { valid: false, reason: '此券仅限指定商品使用' }
      }
      const linkedItem = items.find(item =>
        item.productId == rule.linkedProductId || item.id == rule.linkedProductId
      )
      if (linkedItem) {
        const cupSize = (linkedItem.cupSize || 'STANDARD').toUpperCase()
        if (cupSize !== 'STANDARD' && cupSize !== 'MEDIUM') {
          return { valid: false, reason: '此兑换券仅限标准杯使用，请调整杯型后再试' }
        }
      }
    } else if (coupon.couponType === COUPON_TYPE.EXCHANGE) {
      // Generic exchange coupon: determine scope
      const couponName = (coupon.displayTitle || coupon.productName || '').toLowerCase()
      const isCakeCoupon = rule.scope === 'CAKE_ONLY' ||
        couponName.includes('烘培') ||
        couponName.includes('烘焙') ||
        couponName.includes('甜品') ||
        couponName.includes('蛋糕')

      if (isCakeCoupon) {
        const hasBakery = items.some(item => isBakeryCategory(item.category))
        if (!hasBakery) {
          return { valid: false, reason: '此券仅限烘培甜品使用' }
        }
      } else {
        const hasDrink = items.some(item => isDrinkCategory(item.category))
        if (!hasDrink) {
          return { valid: false, reason: '此券仅限饮品使用' }
        }
      }
    }
  }

  // 3. SHOT constraint (need extra-shot drink)
  if (coupon.couponType === COUPON_TYPE.SHOT) {
    if (!hasExtraShot) {
      return { valid: false, reason: '需先添加加浓缩饮品' }
    }
  }

  // 4. DELIVERY_FEE constraint (delivery mode only)
  if (coupon.couponType === COUPON_TYPE.DELIVERY_FEE) {
    if (diningMethod !== 'DELIVERY') {
      return { valid: false, reason: '此券仅限外卖订单使用' }
    }
  }

  // 5. DISCOUNT scope=DRINK_ONLY
  if (coupon.couponType === COUPON_TYPE.DISCOUNT && rule.scope === 'DRINK_ONLY') {
    const hasDrink = items.some(item => isDrinkCategory(item.category))
    if (!hasDrink) {
      return { valid: false, reason: '此券仅限饮品使用' }
    }
  }

  // 5.1 DISCOUNT scope=CAKE_ONLY
  if (coupon.couponType === COUPON_TYPE.DISCOUNT && rule.scope === 'CAKE_ONLY') {
    const hasBakery = items.some(item => isBakeryCategory(item.category))
    if (!hasBakery) {
      return { valid: false, reason: '此券仅限烘培甜品使用' }
    }
  }

  // 6. BOGO needs >=2 drinks
  if (coupon.couponType === COUPON_TYPE.BOGO) {
    const drinkItems = items.filter(item => isDrinkCategory(item.category))
    const drinkCount = drinkItems.reduce((sum, i) => sum + i.quantity, 0)
    if (drinkCount < 2) {
      return { valid: false, reason: '至少需要2杯饮品' }
    }
  }

  return { valid: true, reason: '' }
}

// ──────────────────────────────────────────────
//  Discount calculation
// ──────────────────────────────────────────────

/**
 * Calculate discount amount for a DISCOUNT coupon.
 *
 * @param {Object} coupon   - coupon with parsedRule
 * @param {Array}  cartItems
 * @param {Object} pricing
 * @param {number} pricing.baseSubtotal
 * @param {number} pricing.cupExtraTotal
 * @returns {number} discount amount
 */
export function calculateDiscountAmount(coupon, cartItems, pricing) {
  const rule = coupon.parsedRule || {}
  const { baseSubtotal, cupExtraTotal } = pricing

  // 1. Get discount percentage
  let discountPercent = coupon.value
  if (!discountPercent || discountPercent <= 0) {
    const rate = rule.discountRate || 0
    if (rate > 0 && rate < 1) {
      discountPercent = rate * 100
    } else if (rate >= 1 && rate <= 10) {
      discountPercent = rate * 10
    } else {
      discountPercent = rate
    }
  }

  if (discountPercent <= 0 || discountPercent > 100) return 0

  // 2. Parse config
  const isDrinkOnly = rule.scope === 'DRINK_ONLY'
  const isCakeOnly = rule.scope === 'CAKE_ONLY'
  const isSingleItem = rule.limit === 'SINGLE_ITEM'

  // 3. Compute base amount (base price + cup upgrade, excludes milk & strength)
  let baseAmount = baseSubtotal + cupExtraTotal

  if (isCakeOnly) {
    const bakeryItems = cartItems.filter(item => isBakeryCategory(item.category))
    if (bakeryItems.length === 0) return 0

    const maxBakery = bakeryItems.reduce((prev, curr) => {
      const prevPrice = prev.basePrice || prev.unitPrice
      const currPrice = curr.basePrice || curr.unitPrice
      return currPrice > prevPrice ? curr : prev
    })
    baseAmount = maxBakery.basePrice || maxBakery.unitPrice

  } else if (isDrinkOnly || isSingleItem) {
    const drinkItems = cartItems.filter(item => isDrinkCategory(item.category))
    if (drinkItems.length === 0) return 0

    if (isSingleItem) {
      const maxItem = drinkItems.reduce((prev, curr) => {
        let prevPrice = prev.basePrice || prev.unitPrice
        if (prev.cupSize === 'LARGE') prevPrice += 3

        let currPrice = curr.basePrice || curr.unitPrice
        if (curr.cupSize === 'LARGE') currPrice += 3

        return currPrice > prevPrice ? curr : prev
      })
      let maxItemPrice = maxItem.basePrice || maxItem.unitPrice
      if (maxItem.cupSize === 'LARGE') maxItemPrice += 3
      baseAmount = maxItemPrice
    } else {
      baseAmount = drinkItems.reduce((sum, i) => {
        let itemBase = i.basePrice || i.unitPrice
        if (i.cupSize === 'LARGE') itemBase += 3
        return sum + itemBase * i.quantity
      }, 0)
    }
  }

  // 4. Compute discount
  let discountAmount = baseAmount * (1 - discountPercent / 100)

  // 5. Cap
  const maxDiscount = rule.maxDiscountAmount || 0
  if (maxDiscount > 0 && discountAmount > maxDiscount) {
    discountAmount = maxDiscount
  }

  return discountAmount
}

/**
 * Calculate discount for ANY coupon type (unified entry point).
 *
 * @param {Object} coupon   - coupon object
 * @param {Array}  cartItems
 * @param {Object} pricing
 * @param {number} pricing.baseSubtotal
 * @param {number} pricing.cupExtraTotal
 * @param {number} pricing.subtotal
 * @returns {number} discount amount
 */
export function calculateCouponDiscount(coupon, cartItems, pricing) {
  if (!coupon) return 0

  const rule = coupon.parsedRule || {}
  const { subtotal, baseSubtotal, cupExtraTotal } = pricing

  if (coupon.couponType === COUPON_TYPE.DISCOUNT) {
    return calculateDiscountAmount(coupon, cartItems, { baseSubtotal, cupExtraTotal })
  }

  if (coupon.couponType === COUPON_TYPE.BOGO) {
    const drinkItems = cartItems.filter(item => isDrinkCategory(item.category))

    let prices = []
    drinkItems.forEach(item => {
      let itemBasePrice = item.basePrice || item.unitPrice
      if (item.cupSize === 'LARGE') itemBasePrice += 3

      for (let i = 0; i < item.quantity; i++) {
        prices.push(itemBasePrice)
      }
    })

    if (prices.length < 2) return 0
    prices.sort((a, b) => a - b)
    const maxPerCup = rule.maxDiscount || 9999
    return Math.min(prices[0], maxPerCup)
  }

  if (coupon.couponType === COUPON_TYPE.FULL_REDUCE) {
    return subtotal >= (coupon.minAmount || 0) ? coupon.value : 0
  }

  if (coupon.couponType === COUPON_TYPE.EXCHANGE) {
    if (!cartItems || !Array.isArray(cartItems)) return 0

    const couponName = (coupon.displayTitle || coupon.productName || '').toLowerCase()
    const isCakeCoupon = rule.scope === 'CAKE_ONLY' ||
      couponName.includes('烘培') ||
      couponName.includes('烘焙') ||
      couponName.includes('甜品') ||
      couponName.includes('蛋糕')

    if (isCakeCoupon) {
      const bakeryItems = cartItems.filter(item => isBakeryCategory(item.category))
      if (bakeryItems.length === 0) return 0
      const maxBakeryPrice = Math.max(...bakeryItems.map(i => i.unitPrice || 0))
      const maxDeduct = rule.maxDiscount || 9999
      return Math.min(maxBakeryPrice, maxDeduct)
    }

    if (!rule.linkedProductId && !rule.productId) {
      let drinkItems = cartItems.filter(item => isDrinkCategory(item.category))
      if (drinkItems.length === 0) return 0

      const skuLimit = rule.skuLimit
      const categoryBlocklist = rule.categoryBlocklist || []

      drinkItems = drinkItems.filter(item => {
        if (skuLimit === 'STANDARD_ONLY') {
          const size = (item.cupSize || 'STANDARD').toUpperCase()
          if (size !== 'STANDARD' && size !== 'MEDIUM') return false
        }
        if (categoryBlocklist.length > 0 && item.category) {
          const cat = item.category.toLowerCase()
          const isBlocked = categoryBlocklist.some(blocked =>
            cat.includes(blocked.toLowerCase()) || cat.includes('手冲')
          )
          if (isBlocked) return false
        }
        return true
      })

      if (drinkItems.length === 0) return 0

      const maxDrinkPrice = Math.max(...drinkItems.map(i => {
        let itemBasePrice = i.basePrice || i.unitPrice
        if (i.cupSize === 'LARGE') itemBasePrice += 3
        return itemBasePrice
      }))
      const maxDeduct = rule.maxDeductAmount || rule.maxDiscount || 9999
      let baseDiscount = Math.min(maxDrinkPrice, maxDeduct)

      const freeAddonCount = rule.freeAddon || 0
      if (freeAddonCount > 0) {
        const addonList = []
        drinkItems.forEach(item => {
          if (item.extraPrices?.strength) addonList.push({ price: item.extraPrices.strength, type: 'strength' })
          if (item.extraPrices?.milk) addonList.push({ price: item.extraPrices.milk, type: 'milk' })
        })
        addonList.sort((a, b) => b.price - a.price)
        const freeAddons = addonList.slice(0, freeAddonCount)
        baseDiscount += freeAddons.reduce((sum, addon) => sum + addon.price, 0)
      }

      return baseDiscount
    }

    const targetItem = cartItems.find(item => {
      if (rule.linkedProductId && (item.id == rule.linkedProductId || item.productId == rule.linkedProductId)) return true
      if (rule.productId && (item.id == rule.productId || item.productId == rule.productId)) return true
      if (rule.productIds && (rule.productIds.includes(item.id) || rule.productIds.includes(item.productId))) return true
      return false
    })
    if (targetItem) {
      const standardPrice = targetItem.basePrice || targetItem.unitPrice
      const maxDeduct = rule.maxDeductAmount || rule.maxDiscount || 9999
      return Math.min(standardPrice, maxDeduct)
    }
  }

  return 0
}

/**
 * Map raw coupons to enriched display objects and filter/sort main coupons.
 *
 * @param {Array}  mainCoupons - main coupons (already split)
 * @param {Array}  cartItems
 * @param {Object} pricing     - { baseSubtotal, cupExtraTotal, subtotal }
 * @param {Object} context     - { hasExtraShot, diningMethod }
 * @returns {Array} enriched & sorted main coupon list
 */
export function filterMainCoupons(mainCoupons, cartItems, pricing, context) {
  return mainCoupons.map(coupon => {
    let meetsThreshold = true
    let amountNeeded = 0
    let unavailableReason = ''

    let estimatedDiscount = calculateCouponDiscount(coupon, cartItems, pricing)

    const validation = validateCouponForCart(coupon, cartItems, context)
    if (!validation.valid) {
      meetsThreshold = false
      unavailableReason = validation.reason
      estimatedDiscount = 0
    }

    if (coupon.couponType === COUPON_TYPE.FULL_REDUCE) {
      const minAmount = coupon.parsedRule?.minOrderAmount || coupon.minAmount || 0
      if (minAmount > 0 && pricing.subtotal < minAmount) {
        meetsThreshold = false
        amountNeeded = (minAmount - pricing.subtotal).toFixed(0)
        if (!unavailableReason) {
          unavailableReason = `需满¥${minAmount}`
        }
        estimatedDiscount = 0
      }
    }

    if (estimatedDiscount === 0 && !unavailableReason && meetsThreshold) {
      meetsThreshold = false
      unavailableReason = '购物车中无匹配商品'
    }

    return { ...coupon, meetsThreshold, amountNeeded, estimatedDiscount, unavailableReason }
  }).sort((a, b) => {
    return (b.estimatedDiscount || 0) - (a.estimatedDiscount || 0)
  })
}

/**
 * Pick the best coupon from a filtered list.
 * @param {Array} filteredMainCoupons
 * @returns {string|null} best coupon code, or null
 */
export function findBestCoupon(filteredMainCoupons) {
  const usableCoupons = filteredMainCoupons.filter(c => c.meetsThreshold)
  if (usableCoupons.length === 0) return null
  return usableCoupons[0].couponCode
}

// ──────────────────────────────────────────────
//  Addon coupon helpers
// ──────────────────────────────────────────────

/**
 * Get description text for an addon coupon.
 * @param {Object} coupon
 * @param {number} extraShotCount
 * @returns {string}
 */
export function getAddonCouponDesc(coupon, extraShotCount) {
  if (coupon.couponType === COUPON_TYPE.DELIVERY_FEE) return '抵扣配送费（限用1张）'
  if (coupon.couponType === COUPON_TYPE.SHOT) {
    if (extraShotCount > 0) {
      return `每张抵¥5（最多${extraShotCount}张）`
    }
    return '免费加浓缩'
  }
  return ''
}

/**
 * Get tip/hint text for an addon coupon.
 *
 * @param {Object} coupon
 * @param {Array}  cartItems
 * @param {Object} context
 * @param {boolean} context.hasExtraShot
 * @param {number}  context.extraShotCount
 * @param {number}  context.selectedShotCouponCount
 * @param {string[]} context.selectedAddonCoupons
 * @param {string}  context.diningMethod
 * @returns {string|null}
 */
export function getAddonCouponTip(coupon, cartItems, context) {
  const validation = validateCouponForCart(coupon, cartItems, context)
  if (!validation.valid) {
    return validation.reason
  }

  const { diningMethod, hasExtraShot, extraShotCount, selectedShotCouponCount, selectedAddonCoupons } = context

  if (coupon.couponType === COUPON_TYPE.DELIVERY_FEE && diningMethod !== 'DELIVERY') {
    return '仅外卖可用'
  }
  if (coupon.couponType === COUPON_TYPE.SHOT) {
    if (!hasExtraShot) {
      return '请先选择加浓缩'
    }
    if (selectedShotCouponCount >= extraShotCount &&
      !selectedAddonCoupons.includes(coupon.couponCode)) {
      return `已选${selectedShotCouponCount}/${extraShotCount}张`
    }
  }
  return null
}

/**
 * Check if an addon coupon should be disabled.
 *
 * @param {Object} coupon
 * @param {Array}  cartItems
 * @param {Object} context
 * @returns {boolean}
 */
export function isAddonCouponDisabled(coupon, cartItems, context) {
  const validation = validateCouponForCart(coupon, cartItems, context)
  if (!validation.valid) return true

  const { diningMethod, hasExtraShot, extraShotCount, selectedShotCouponCount, selectedAddonCoupons } = context

  if (coupon.couponType === COUPON_TYPE.DELIVERY_FEE) {
    return diningMethod !== 'DELIVERY'
  }
  if (coupon.couponType === COUPON_TYPE.SHOT) {
    if (!hasExtraShot) return true
    if (selectedShotCouponCount >= extraShotCount &&
      !selectedAddonCoupons.includes(coupon.couponCode)) {
      return true
    }
  }
  return false
}
