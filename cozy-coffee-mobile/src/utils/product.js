// 积分商品虚实判断：以 category 为权威（coupon=优惠券/虚拟，gift=实物礼品），兼容 productType
// 与后端 redeem 逻辑一致：productType=VIRTUAL 或 category=coupon 视为虚拟

export function isVirtualProduct(product = {}) {
  return product.category === 'coupon' || product.productType === 'VIRTUAL'
}

export function isPhysicalProduct(product = {}) {
  return product.category === 'gift' || (product.productType === 'PHYSICAL' && product.category !== 'coupon')
}
