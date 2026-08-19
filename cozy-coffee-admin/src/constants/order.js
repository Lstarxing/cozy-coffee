export const ORDER_STATUS = {
  PENDING: 'pending',
  PREPARING: 'preparing',
  DELIVERING: 'delivering',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled'
}

export const ORDER_STATUS_MAP = {
  [ORDER_STATUS.PENDING]: { label: '待支付', tagType: 'danger' },
  [ORDER_STATUS.PREPARING]: { label: '制作中', tagType: 'warning' },
  [ORDER_STATUS.DELIVERING]: { label: '配送中', tagType: 'primary' },
  [ORDER_STATUS.COMPLETED]: { label: '已完成', tagType: 'success' },
  [ORDER_STATUS.CANCELLED]: { label: '已取消', tagType: 'info' }
}

export const DINING_METHOD = {
  TAKEOUT: 'TAKEOUT',
  DELIVERY: 'DELIVERY'
}

export const DINING_METHOD_MAP = {
  [DINING_METHOD.TAKEOUT]: '自提',
  [DINING_METHOD.DELIVERY]: '外卖'
}

export const ORDER_SPEC = {
  TEMP: { HOT: 'HOT', COLD: 'COLD', WARM: 'WARM' },
  TEMP_MAP: { HOT: '热', COLD: '冰', WARM: '温', iced: '冰', hot: '热', warm: '温' },
  SUGAR: { NONE: 'NONE', LIGHT: 'LIGHT', STANDARD: 'STANDARD', MEDIUM: 'MEDIUM', LESS: 'LESS', HALF: 'HALF', FULL: 'FULL' },
  SUGAR_MAP: { NONE: '无糖', LIGHT: '微甜', STANDARD: '标准甜', MEDIUM: '少甜', LESS: '少糖', HALF: '半糖', none: '无糖', light: '微甜', standard: '标准甜', less: '少甜', half: '半糖', full: '标准甜' },
  SIZE: { STANDARD: 'STANDARD', LARGE: 'LARGE', MEDIUM: 'MEDIUM' },
  SIZE_MAP: { STANDARD: '标准杯', LARGE: '大杯', MEDIUM: '中杯', standard: '标准杯', large: '大杯', medium: '中杯' },
  STRENGTH: { STRONG: 'STRONG', NORMAL: 'NORMAL' },
  STRENGTH_MAP: { STRONG: '加浓', NORMAL: '标准浓' }
}

export const EXPIRE_SECONDS = 900

export const getLabel = (map, value) => (value && map[value]) || value || '-'
