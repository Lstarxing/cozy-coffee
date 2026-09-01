import { Bike, Coffee, Cake, Sparkles, Gift, Percent, Banknote, Bean } from 'lucide-vue-next'

export function getRules(coupon) {
  try {
    return JSON.parse(coupon.ruleJson || '{}')
  } catch {
    return {}
  }
}

export function getCouponTheme(coupon) {
  const code = coupon.couponCode || ''
  const type = coupon.couponType
  const name = (coupon.productName || coupon.displayTitle || '').toLowerCase()
  const codeUpper = code.toUpperCase()

  const isNewProduct = name.includes('新品') || codeUpper.includes('NEW_PRODUCT')
  const isNewUser = name.includes('新用户') || name.includes('新人') || codeUpper.includes('NEW_USER')

  if (type === 'DISCOUNT' && !isNewProduct && !isNewUser) return 'discount'

  if (type === 'EXCHANGE') {
    if (name.includes('尊享') || name.includes('黑金') || codeUpper.includes('BLACK')) return 'black-gold'
    if (name.includes('优选') || name.includes('钻石') || codeUpper.includes('DIAMOND')) return 'diamond'
    if (name.includes('标准') || name.includes('黄金') || codeUpper.includes('GOLD')) return 'gold'
  }

  if (codeUpper.includes('BLACK') || name.includes('黑金') || name.includes('尊享')) return 'black-gold'
  if (codeUpper.includes('DIAMOND') || name.includes('钻石') || name.includes('优选')) return 'diamond'
  if (codeUpper.includes('GOLD') || (name.includes('黄金') && !name.includes('黑金'))) return 'gold'
  if (codeUpper.includes('SILVER') || name.includes('白银')) return 'silver'

  if (type === 'BOGO') return 'social'

  if (isNewProduct && !isNewUser) return 'promo'
  if (isNewUser) return 'new-user'

  const isPointsRedeem = codeUpper.includes('POINTS') || codeUpper.includes('REDEEM') ||
                         name.includes('任选') || name.includes('积分') || name.includes('兑换')
  if (isPointsRedeem && type === 'EXCHANGE') return 'points-redeem'

  if (type === 'FULL_REDUCE' || name.includes('代金')) return 'voucher'

  if (codeUpper.includes('CAKE') || name.includes('蛋糕') || name.includes('烘焙') || name.includes('甜品')) return 'dessert'
  if (codeUpper.includes('DELIVERY') || name.includes('配送') || name.includes('运费')) return 'utility'
  if (type === 'SHOT' || codeUpper.includes('SHOT') || name.includes('加浓') || name.includes('加料')) return 'addon'

  if (codeUpper.includes('BIRTHDAY') || name.includes('生日')) {
    if (name.includes('黑金') || name.includes('尊享')) return 'black-gold'
    if (name.includes('钻石') || name.includes('优选')) return 'diamond'
    if (name.includes('黄金') || name.includes('标准')) return 'gold'
    return 'promo'
  }

  if (type === 'EXCHANGE') return 'points-redeem'
  return 'silver'
}

export function getValueSymbol(coupon) {
  const type = coupon.couponType
  const theme = getCouponTheme(coupon)

  if (type === 'EXCHANGE' || type === 'FREE_ITEM') {
    if (theme === 'black-gold') return 'ULTRA'
    return 'Free'
  }
  if (type === 'BOGO') return '1+1'
  if (type === 'DISCOUNT') {
    const rules = getRules(coupon)
    const rate = rules.discountRate || coupon.value / 100
    if (rate === 0.5) return '50%'
    if (rate === 0.88) return '8.8折'
    return `${Math.round(rate * 10)}折`
  }
  if (type === 'FULL_REDUCE') return `¥${coupon.value || 0}`
  return '🎁'
}

export function getValueType(coupon) {
  const type = coupon.couponType
  const theme = getCouponTheme(coupon)
  const name = (coupon.productName || coupon.displayTitle || '').toLowerCase()
  const code = (coupon.couponCode || '').toUpperCase()

  const isNewProduct = name.includes('新品') || code.includes('NEW_PRODUCT')
  const isNewUser = name.includes('新用户') || name.includes('新人') || code.includes('NEW_USER')

  if (isNewProduct && !isNewUser) {
    if (name.includes('免') || type === 'EXCHANGE') return '新品免单'
    return '新品折扣'
  }

  if (type === 'EXCHANGE') {
    const subTitle = coupon.displaySubTitle || ''
    if (theme === 'black-gold') return '全通兑'
    if (subTitle.includes('限标准杯') || subTitle.includes('指定商品')) return '指定兑换'
    return '免单'
  }
  if (type === 'BOGO') return '买一送一'
  if (type === 'DISCOUNT') return '折扣'
  if (type === 'FULL_REDUCE') return '代金'
  if (type === 'SHOT') return '+Shot'
  if (type === 'DELIVERY_FEE') return '配送'
  return '兑换'
}

export function getCouponIcon(coupon) {
  const theme = getCouponTheme(coupon)
  const type = coupon.couponType
  const subTitle = coupon.displaySubTitle || ''
  const name = (coupon.productName || coupon.displayTitle || '').toLowerCase()

  if (type === 'SHOT' || name.includes('加浓') || name.includes('加料')) return Bean
  if (type === 'DELIVERY_FEE') return Bike
  if (theme === 'dessert') return Cake
  if (theme === 'promo') return Sparkles
  if (type === 'FULL_REDUCE') return Banknote
  if (type === 'DISCOUNT') return Percent
  if (type === 'EXCHANGE') {
    if (subTitle.includes('限标准杯') || subTitle.includes('指定商品')) return Gift
    return Coffee
  }
  if (type === 'BOGO') return Coffee
  return Gift
}

export function isPremium(coupon) {
  return getCouponTheme(coupon) === 'black-gold'
}

export function getPremiumIcon(coupon) {
  return isPremium(coupon) ? '👑 ' : ''
}

export function getSourceTag(coupon) {
  const code = coupon.couponCode || ''
  if (code.includes('BIRTHDAY')) return '生日礼'
  if (code.includes('MONTHLY')) return '月权益'
  if (code.includes('UPGRADE')) return '晋升礼'
  if (code.includes('NEW_USER')) return '新人礼'
  return ''
}

export function getRestrictionTags(coupon) {
  const tags = []
  const r = getRules(coupon)
  const name = (coupon.productName || coupon.displayTitle || '').toLowerCase()
  const theme = getCouponTheme(coupon)

  if (coupon.couponType === 'EXCHANGE') {
    if (theme === 'black-gold' || name.includes('尊享')) {
      tags.push({ icon: '✨', text: '全场通兑', type: 'check' })
      if (r.freeAddon) tags.push({ icon: '☕', text: '含1份免费加料', type: 'check' })
      if (r.maxDiscount) tags.push({ icon: '💰', text: `封顶¥${r.maxDiscount}`, type: 'warning' })
      return tags
    }
    if (theme === 'diamond' || name.includes('优选')) {
      tags.push({ icon: '💎', text: '优选饮品任选', type: 'check' })
      if (r.skuLimit === 'ALL') tags.push({ icon: '✅', text: '不限杯型', type: 'check' })
      if (r.freeAddon) tags.push({ icon: '☕', text: '含1份免费加料', type: 'check' })
      else tags.push({ icon: '➕', text: '加料需补差价', type: 'warning' })
      if (r.maxDiscount) tags.push({ icon: '💰', text: `封顶¥${r.maxDiscount}`, type: 'warning' })
      return tags
    }
    if (theme === 'gold' || name.includes('标准')) {
      tags.push({ icon: '☕', text: '标准饮品', type: 'rule' })
      if (r.skuLimit === 'STANDARD_ONLY') tags.push({ icon: '🔒', text: '限标准杯', type: 'lock' })
      tags.push({ icon: '➕', text: '加料需补差价', type: 'warning' })
      if (r.maxDiscount) tags.push({ icon: '💰', text: `封顶¥${r.maxDiscount}`, type: 'warning' })
      return tags
    }
    if (r.linkedProductId) tags.push({ icon: '🎯', text: '指定商品', type: 'rule' })
  }

  const couponName = (coupon.productName || coupon.displayTitle || '').toLowerCase()
  if (coupon.couponType === 'SHOT' || couponName.includes('加浓') || couponName.includes('加料')) {
    tags.push({ icon: '🫘', text: '辅助券', type: 'rule' })
    tags.push({ icon: '🔗', text: '可叠加使用', type: 'check' })
    return tags
  }

  if (r.scope === 'CAKE_ONLY') {
    tags.push({ icon: '🍰', text: '限烘焙甜品', type: 'rule' })
    if (r.maxDiscount && r.maxDiscount < 100) tags.push({ icon: '💰', text: `封顶¥${r.maxDiscount}`, type: 'warning' })
    return tags
  }

  if (r.skuLimit === 'STANDARD_ONLY') tags.push({ icon: '🔒', text: '仅限标准杯', type: 'lock' })
  else if (r.skuLimit === 'ALL') tags.push({ icon: '✅', text: '不限杯型', type: 'check' })

  const blocklist = r.categoryBlocklist || []
  const blockSignature = blocklist.some(b => b.toLowerCase() === 'signature')
  const blockSpecialty = blocklist.some(b => ['specialty', 'soe', 'pour-over'].includes(b.toLowerCase()))
  if (blocklist.length === 0 && r.skuLimit === 'ALL') tags.push({ icon: '✅', text: '含精品咖啡', type: 'check' })
  else if (blockSignature) tags.push({ icon: '🚫', text: '排除特调', type: 'ban' })
  else if (!blockSignature && r.skuLimit) tags.push({ icon: '✅', text: '含特调', type: 'check' })
  if (blockSpecialty) tags.push({ icon: '🚫', text: '不含精品', type: 'ban' })

  if (r.freeAddon) tags.push({ icon: '✅', text: '免1份加料', type: 'check' })
  if (r.limit === 'SINGLE_ITEM') tags.push({ icon: '🔒', text: '限单饮品', type: 'lock' })

  if (r.maxDiscount && r.maxDiscount < 100) tags.push({ icon: '💰', text: `封顶¥${r.maxDiscount}`, type: 'warning' })
  else if (r.maxDiscountAmount) tags.push({ icon: '💰', text: `最高抵¥${r.maxDiscountAmount}`, type: 'warning' })

  if (coupon.couponType === 'BOGO') {
    tags.push({ icon: '👥', text: '免低价杯', type: 'rule' })
    if (r.maxDiscount) tags.push({ icon: '💰', text: `赠品封顶¥${r.maxDiscount}`, type: 'warning' })
  }

  if (coupon.couponCode?.includes('DELIVERY')) tags.push({ icon: '🛵', text: '仅限外卖', type: 'rule' })
  if (coupon.couponCode?.includes('CAKE')) tags.push({ icon: '🍰', text: '限切片蛋糕', type: 'rule' })

  return tags.slice(0, 3)
}

export function getExpiryText(coupon) {
  if (!coupon.expiresAt) return '永久有效'
  const date = new Date(coupon.expiresAt)
  return `有效期至 ${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

export function getDaysLeft(coupon) {
  if (!coupon.expiresAt) return 999
  const now = new Date()
  const expiry = new Date(coupon.expiresAt)
  const diff = expiry - now
  return Math.ceil(diff / (1000 * 60 * 60 * 24))
}

export function isExpired(coupon) {
  return coupon.status === 'EXPIRED' || getDaysLeft(coupon) <= 0
}

export function getCouponTitle(coupon) {
  if (coupon.displayTitle) return coupon.displayTitle
  if (coupon.title) return coupon.title
  if (coupon.productName) return coupon.productName
  const typeNames = {
    'EXCHANGE': '饮品免单券',
    'BOGO': '买一送一券',
    'DISCOUNT': '折扣券',
    'FULL_REDUCE': '代金券',
    'DELIVERY_FEE': '配送费抵扣券',
    'SHOT': '加浓缩券'
  }
  return typeNames[coupon.couponType] || coupon.description || '优惠券'
}
