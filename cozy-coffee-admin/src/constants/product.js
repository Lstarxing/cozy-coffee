export const PRODUCT_STATUS = {
  ACTIVE: 'active',
  INACTIVE: 'inactive',
  SOLD_OUT: 'sold_out'
}

export const PRODUCT_STATUS_MAP = {
  [PRODUCT_STATUS.ACTIVE]: { label: '上架', tagType: 'success' },
  [PRODUCT_STATUS.INACTIVE]: { label: '下架', tagType: 'info' },
  [PRODUCT_STATUS.SOLD_OUT]: { label: '售罄', tagType: 'warning' }
}

export const PRODUCT_CATEGORY = {
  ESPRESSO: 'espresso',
  SIGNATURE: 'signature',
  SOE: 'soe',
  BAKERY: 'bakery',
  MERCHANDISE: 'merchandise',
  ADDON: 'addon'
}

export const PRODUCT_CATEGORY_MAP = {
  [PRODUCT_CATEGORY.ESPRESSO]: { label: '意式咖啡', color: '#6F4E37' },
  [PRODUCT_CATEGORY.SIGNATURE]: { label: '季节限定', color: '#8B5E3C' },
  [PRODUCT_CATEGORY.SOE]: { label: '精品手冲', color: '#5D4037' },
  [PRODUCT_CATEGORY.BAKERY]: { label: '烘焙甜品', color: '#D84315' },
  [PRODUCT_CATEGORY.MERCHANDISE]: { label: '积分商品', color: '#8D6E63' },
  [PRODUCT_CATEGORY.ADDON]: { label: '配料/加料', color: '#9CA3AF' },
  // V2 分类（大写，023 迁移后商品实际值）
  'ESPRESSO': { label: '经典咖啡', color: '#6F4E37' },
  'MILK': { label: '奶咖', color: '#8B5E3C' },
  'SIGNATURE': { label: '招牌特调', color: '#5D4037' },
  'SPECIALTY': { label: '精品咖啡', color: '#D84315' },
  'NON_COFFEE': { label: '非咖啡', color: '#8D6E63' },
  'BAKERY': { label: '烘焙轻食', color: '#9CA3AF' }
}

export const SKU_SIZE_TYPE = {
  DEFAULT: 'DEFAULT',
  MEDIUM_LARGE: 'MEDIUM_LARGE',
  ALL_SIZES: 'ALL_SIZES'
}

export const SKU_SUGAR_TYPE = {
  NO_SUGAR_ONLY: 'NO_SUGAR_ONLY',
  MIN_LESS_SWEET: 'MIN_LESS_SWEET',
  FREE_CHOICE: 'FREE_CHOICE'
}

export const SKU_TEMP_TYPE = {
  NO_TEMP: 'NO_TEMP',
  HOT_COLD: 'HOT_COLD'
}

// V2 商品标签词汇（3.1）：展示用；TOP1 数据驱动不静态录入
export const PRODUCT_TAG_MAP = {
  'NEW': { label: '新品', color: '#E65100' },
  'TOP1': { label: '招牌', color: '#F5A623' },
  'LIMITED': { label: '限量', color: '#8E24AA' },
  'SEASONAL': { label: '季节', color: '#00838F' },
  'COLD': { label: '冰饮', color: '#1E88E5' },
  'FRUITY': { label: '果香', color: '#43A047' },
  'CITRUS': { label: '柑橘', color: '#F9A825' },
  'FLORAL': { label: '花香', color: '#D81B60' },
  'PLANT-BASED': { label: '植物基', color: '#6D4C41' },
  'COCONUT': { label: '椰香', color: '#C69C6D' },
  'SIGNATURE': { label: '特调', color: '#8B5E3C' },
  'EXPERIENCE': { label: '体验', color: '#5D4037' },
  'CLASSIC': { label: '经典', color: '#757575' },
  'STRONG': { label: '加浓', color: '#C62828' }
}
export const PRODUCT_TAG_OPTIONS = Object.entries(PRODUCT_TAG_MAP).map(([value, meta]) => ({ value, label: meta.label }))
