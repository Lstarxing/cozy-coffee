// 咖啡规格枚举 → 中文文案；默认项（标准甜/全脂奶/标准浓度）省略
const SPEC_CN = {
  cupSize: { STANDARD: '标准杯', MEDIUM: '中杯', LARGE: '大杯', SMALL: '小杯' },
  temperature: { HOT: '热', COLD: '冰', ICED: '冰', WARM: '温' },
  sugarLevel: { NONE: '无糖', LESS: '少糖', HALF: '半糖', LIGHT: '微甜', MEDIUM: '少甜', STANDARD: '标准甜', FULL: '标准甜', NO_ADDED_SUGAR: '不另外加糖' },
  milkType: { OAT: '燕麦奶', COCONUT: '椰奶', SOY: '豆奶' },
  coffeeStrength: { NORMAL: '标准浓度', STRONG: '加浓' }
}

function parseOptions(value) {
  try { return typeof value === 'string' ? JSON.parse(value) : (value || {}) } catch (_) { return {} }
}

function specToCn(field, value) {
  if (!value) return ''
  return SPEC_CN[field][String(value).toUpperCase()] || String(value)
}

export function formatCoffeeSpec(item) {
  const options = parseOptions(item?.optionsJson)
  const milk = options.milkType || item?.milkType
  const parts = []
  if (item?.cupSize) parts.push(specToCn('cupSize', item.cupSize))
  if (item?.temperature) parts.push(specToCn('temperature', item.temperature))
  if (item?.sugarLevel && !/^(standard|full)$/i.test(item.sugarLevel)) parts.push(specToCn('sugarLevel', item.sugarLevel))
  if (milk && !/^whole$/i.test(milk)) parts.push(specToCn('milkType', milk))
  if (item?.coffeeStrength && /^strong$/i.test(item.coffeeStrength)) parts.push(specToCn('coffeeStrength', item.coffeeStrength))
  return parts.join(' · ')
}
