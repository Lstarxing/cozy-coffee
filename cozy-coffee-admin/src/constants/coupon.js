export const COUPON_TYPE = {
  EXCHANGE: 'EXCHANGE',
  DISCOUNT: 'DISCOUNT',
  FULL_REDUCE: 'FULL_REDUCE',
  BOGO: 'BOGO',
  SHOT: 'SHOT',
  DELIVERY_FEE: 'DELIVERY_FEE'
}

export const COUPON_TYPE_MAP = {
  [COUPON_TYPE.EXCHANGE]: { label: '兑换券', category: '兑换', desc: '兑换指定商品' },
  [COUPON_TYPE.DISCOUNT]: { label: '折扣券', category: '折扣', desc: '订单打折优惠' },
  [COUPON_TYPE.FULL_REDUCE]: { label: '满减券', category: '满减', desc: '满额减免' },
  [COUPON_TYPE.BOGO]: { label: '买一赠一券', category: '买赠', desc: '买一赠一' },
  [COUPON_TYPE.SHOT]: { label: '加浓缩券', category: '加料', desc: '免费加浓缩' },
  [COUPON_TYPE.DELIVERY_FEE]: { label: '免配送费券', category: '配送', desc: '免配送费' }
}

export const COUPON_STATUS = {
  ISSUED: 'ISSUED',
  USED: 'USED',
  EXPIRED: 'EXPIRED'
}

export const COUPON_STATUS_MAP = {
  [COUPON_STATUS.ISSUED]: { label: '可使用', tagType: 'success' },
  [COUPON_STATUS.USED]: { label: '已使用', tagType: 'info' },
  [COUPON_STATUS.EXPIRED]: { label: '已过期', tagType: 'info' }
}
