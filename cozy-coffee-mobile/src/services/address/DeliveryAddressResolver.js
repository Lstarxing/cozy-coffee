import { get } from '@/api/request'
import { normalizeAddress } from '@/utils/address'

// 解析默认/首个收货地址写入结算，返回解析到的地址（无地址返回 null）
export async function resolveDeliveryAddress(checkoutStore) {
  try {
    const res = await get('/member/addresses')
    const list = Array.isArray(res?.data) ? res.data : []
    const addr = list.find(a => a.isDefault) || list[0] || null
    const normalized = addr ? normalizeAddress(addr) : null
    checkoutStore.deliveryAddress = normalized
    checkoutStore.deliveryAddressId = normalized ? normalized.id : null
    return normalized
  } catch (e) {
    return null
  }
}
