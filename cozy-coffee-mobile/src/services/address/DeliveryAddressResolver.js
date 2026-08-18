import { get } from '@/api/request'

function normalize(addr) {
  return {
    ...addr,
    name: addr.receiverName,
    gender: addr.gender || 'MALE',
    phone: addr.receiverPhone,
    region: [addr.province, addr.city, addr.district].filter(Boolean).join(' '),
    detail: addr.detailAddress,
    isDefault: addr.isDefault
  }
}

// 解析默认/首个收货地址写入结算，返回解析到的地址（无地址返回 null）
export async function resolveDeliveryAddress(checkoutStore) {
  try {
    const res = await get('/member/addresses')
    const list = Array.isArray(res?.data) ? res.data : []
    const addr = list.find(a => a.isDefault) || list[0] || null
    const normalized = addr ? normalize(addr) : null
    checkoutStore.deliveryAddress = normalized
    checkoutStore.deliveryAddressId = normalized ? normalized.id : null
    return normalized
  } catch (e) {
    return null
  }
}
