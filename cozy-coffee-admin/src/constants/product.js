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
  [PRODUCT_CATEGORY.ADDON]: { label: '配料/加料', color: '#9CA3AF' }
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
