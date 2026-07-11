import { ref } from 'vue'
import { getAvailableCoupons, getUserCoupons } from '@/api/mall'
import { getCouponTitle } from '@/composables/useCouponDisplay'

export function useCoupons() {
  const coupons = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function loadUserCoupons(status) {
    loading.value = true
    error.value = null
    try {
      const res = await getUserCoupons(status)
      coupons.value = res.data || []
    } catch (err) {
      console.error('加载用户优惠券失败:', err)
      error.value = err.message || '加载优惠券失败'
      coupons.value = []
    } finally {
      loading.value = false
    }
  }

  async function loadAvailableCoupons(data) {
    loading.value = true
    error.value = null
    try {
      const res = await getAvailableCoupons(data)
      coupons.value = (res.data || []).map(enrichCoupon)
    } catch (err) {
      console.error('加载优惠券失败', err)
      error.value = err.message || '加载优惠券失败'
      coupons.value = []
    } finally {
      loading.value = false
    }
  }

  function filterByStatus(status) {
    return coupons.value.filter(c => c.status === status)
  }

  return {
    coupons,
    loading,
    error,
    loadUserCoupons,
    loadAvailableCoupons,
    filterByStatus
  }
}

function enrichCoupon(c) {
  let rule = {}
  try {
    rule = JSON.parse(c.ruleJson || '{}')
  } catch (e) { /* ignore */ }

  let name = c.displayTitle || ''

  if (!name) {
    if (c.couponType === 'DISCOUNT') {
      let displayDiscount = c.value
      if (c.value >= 10) {
        displayDiscount = c.value / 10
        displayDiscount = displayDiscount % 1 === 0 ? displayDiscount : displayDiscount.toFixed(1)
      }
      name = displayDiscount + '折券'
    } else if (c.couponType === 'FULL_REDUCE') {
      const minAmt = c.minAmount || rule.minOrderAmount || 0
      const reduceVal = c.value || rule.value || 0
      name = minAmt > 0 ? `满${minAmt}减${reduceVal}` : `立减${reduceVal}元`
    } else if (c.couponType === 'EXCHANGE') {
      name = '免单券'
    } else if (c.couponType === 'BOGO') {
      name = '买一送一'
    } else if (c.couponType === 'DELIVERY_FEE') {
      name = '免运费券'
    } else if (c.couponType === 'SHOT') {
      name = '+1 Shot'
    } else {
      name = getCouponTitle(c)
    }
  }

  return {
    ...c,
    parsedRule: rule,
    displayName: name,
    minAmount: c.minAmount || rule.minOrderAmount || 0
  }
}
