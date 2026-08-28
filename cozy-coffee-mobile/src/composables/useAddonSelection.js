import { computed, reactive } from 'vue'

/**
 * V2 加料组选择（P2-3）：消费 CoffeeProductDTO.addonGroups。
 * 前端只提交「选择意图」（code），权威价格由后端 price_delta 计算；
 * 本 composable 仅负责前端展示 + 构建 addons_json + 展示用加料费。
 */
export function useAddonSelection(addonGroups) {
  // { category: code(SINGLE) | [codes](MULTI) }
  const selection = reactive({})

  const groups = computed(() => (Array.isArray(addonGroups.value) ? addonGroups.value : []))

  const groupLabel = (category) => ({
    MILK: '奶类', SHOT: '咖啡浓度', SYRUP: '糖浆', OTHER: '其他'
  })[category] || category

  function isSelected(group, code) {
    const sel = selection[group.category]
    return group.selectionMode === 'MULTI'
      ? (Array.isArray(sel) && sel.includes(code))
      : sel === code
  }

  function toggle(group, code) {
    if (group.selectionMode === 'MULTI') {
      const cur = selection[group.category] || []
      selection[group.category] = cur.includes(code) ? cur.filter(c => c !== code) : [...cur, code]
    } else {
      // SINGLE：选中即切换；min>0 必选项不可取消
      selection[group.category] = selection[group.category] === code
        ? (group.minSelect > 0 ? code : '')
        : code
    }
  }

  /** 初始化：SINGLE 且 min>0 的组预选默认项（MILK 默认全脂等） */
  function reset() {
    Object.keys(selection).forEach(k => delete selection[k])
    groups.value.forEach(group => {
      if (group.selectionMode === 'SINGLE' && group.minSelect > 0) {
        const def = group.items.find(i => i.isDefault) || group.items[0]
        selection[group.category] = def?.code || ''
      }
    })
  }

  const selectedItems = computed(() => {
    const items = []
    groups.value.forEach(group => {
      const sel = selection[group.category]
      const codes = group.selectionMode === 'MULTI'
        ? (Array.isArray(sel) ? sel : [])
        : (sel ? [sel] : [])
      codes.forEach(code => {
        const item = group.items.find(i => i.code === code)
        if (item) items.push(item)
      })
    })
    return items
  })

  const addonFee = computed(() =>
    selectedItems.value.reduce((sum, i) => sum + Number(i.priceDelta || 0), 0))

  /** 提交意图：仅 code（后端按 price_delta 权威定价 + 规范化快照） */
  const addons = computed(() => selectedItems.value.map(i => ({ code: i.code })))

  /** 展示用已选摘要 */
  const selectedText = computed(() => selectedItems.value.map(i => i.name).join(' · '))

  return { groups, groupLabel, selection, isSelected, toggle, reset, selectedItems, addonFee, addons, selectedText }
}
