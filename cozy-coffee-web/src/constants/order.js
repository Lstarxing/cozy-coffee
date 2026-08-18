export const ORDER_STATUS = {
  PENDING: 'pending',
  PREPARING: 'preparing',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled'
}

export const ORDER_STATUS_MAP = {
  [ORDER_STATUS.PENDING]: '待支付',
  [ORDER_STATUS.PREPARING]: '制作中',
  [ORDER_STATUS.COMPLETED]: '已完成',
  [ORDER_STATUS.CANCELLED]: '已取消'
}
