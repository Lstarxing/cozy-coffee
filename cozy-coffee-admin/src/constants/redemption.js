export const REDEMPTION_STATUS = {
  PENDING: 'pending',
  PROCESSING: 'processing',
  SHIPPED: 'shipped',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled'
}

export const REDEMPTION_STATUS_MAP = {
  [REDEMPTION_STATUS.PENDING]: { label: '待处理', tagType: 'warning' },
  [REDEMPTION_STATUS.PROCESSING]: { label: '待取货/发货', tagType: 'primary' },
  [REDEMPTION_STATUS.SHIPPED]: { label: '配送中', tagType: 'primary' },
  [REDEMPTION_STATUS.COMPLETED]: { label: '已完成', tagType: 'success' },
  [REDEMPTION_STATUS.CANCELLED]: { label: '已取消', tagType: 'info' }
}
