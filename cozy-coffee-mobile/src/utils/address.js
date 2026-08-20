// 地址格式化工具（菜单/地址列表/地址抽屉/结算多页面共用）

// 后端地址 → 前端展示结构（name/phone/region/detail/isDefault）
export function normalizeAddress(a) {
  return {
    ...a,
    name: a.receiverName,
    gender: a.gender || 'MALE',
    phone: a.receiverPhone,
    region: [a.province, a.city, a.district].filter(Boolean).join(' '),
    detail: a.detailAddress,
    isDefault: a.isDefault
  }
}

export const maskPhone = (phone) => String(phone || '').replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')

export const genderSuffix = (gender) => String(gender || '').toUpperCase() === 'FEMALE' ? '女士' : '先生'

export const labelText = (label) => ({ HOME: '家', COMPANY: '公司', SCHOOL: '学校' }[label] || label || '')

export function contactNameText(addr) {
  return `${addr.name || ''}（${genderSuffix(addr.gender)}）`
}

export function addressText(addr) {
  return [addr.region, addr.detail].filter(Boolean).join(' ').replace(/\s+/g, '')
}
