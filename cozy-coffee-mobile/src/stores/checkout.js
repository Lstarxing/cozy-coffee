import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { CHECKOUT_STATUS, transitionCheckout } from '@/domain/checkout/checkoutMachine'

function newIdempotencyKey() {
  const random = Math.random().toString(36).slice(2, 12)
  return `checkout-${Date.now().toString(36)}-${random}`
}

export const useCheckoutStore = defineStore('checkout', () => {
  const storeId = ref(1)
  const pickupTime = ref('ASAP')
  const diningMethod = ref('TAKEOUT') // TAKEOUT 自提 / DELIVERY 外送
  const deliveryAddressId = ref(null)
  const deliveryAddress = ref(null) // 选中的收货地址对象（外送）
  const selectedCoupon = ref(null) // 选中的优惠券对象（与 selectedCouponId 同生同灭，供展示/预览）
  const selectedCouponId = ref(null)
  const selectedAddonCoupons = ref([]) // 选中的辅券对象数组（DELIVERY_FEE 单选 / SHOT 多选）
  const remark = ref('')
  const phone = ref('')
  const status = ref(CHECKOUT_STATUS.IDLE)
  const idempotencyKey = ref('')
  const latestPreview = ref(null)

  const isBusy = computed(() => ['previewing', 'submitting', 'paying'].includes(status.value))
  const isPreviewStale = computed(() => {
    if (!latestPreview.value) return true
    if (!latestPreview.value.expiresAt) return false
    return new Date(latestPreview.value.expiresAt).getTime() <= Date.now()
  })

  function ensureIdempotencyKey() {
    if (!idempotencyKey.value) idempotencyKey.value = newIdempotencyKey()
    return idempotencyKey.value
  }

  function start() {
    // 新一轮结算：成功/取消后清空幂等 key，避免复用旧 key 幂等返回旧订单（可能已取消）
    if ([CHECKOUT_STATUS.SUCCESS, CHECKOUT_STATUS.CANCELLED].includes(status.value)) {
      status.value = CHECKOUT_STATUS.IDLE
      idempotencyKey.value = ''
    }
    ensureIdempotencyKey()
    return idempotencyKey.value
  }

  function transition(event) {
    status.value = transitionCheckout(status.value, event)
    return status.value
  }

  function applyPreview(preview) {
    latestPreview.value = Object.freeze({ ...preview })
    if (status.value === CHECKOUT_STATUS.PREVIEWING) transition('PREVIEW_SUCCEEDED')
  }

  function invalidatePreview() {
    latestPreview.value = null
    if (![CHECKOUT_STATUS.SUBMITTING, CHECKOUT_STATUS.PAYING].includes(status.value)) {
      status.value = CHECKOUT_STATUS.IDLE
    }
  }

  function clearCoupon() {
    selectedCoupon.value = null
    selectedCouponId.value = null
    selectedAddonCoupons.value = []
  }

  function reset({ preserveIntent = false } = {}) {
    status.value = CHECKOUT_STATUS.IDLE
    latestPreview.value = null
    idempotencyKey.value = ''
    if (!preserveIntent) {
      storeId.value = 1
      pickupTime.value = 'ASAP'
      diningMethod.value = 'TAKEOUT'
      deliveryAddressId.value = null
      deliveryAddress.value = null
      clearCoupon()
      remark.value = ''
      phone.value = ''
    }
  }

  return {
    storeId,
    pickupTime,
    diningMethod,
    deliveryAddressId,
    deliveryAddress,
    selectedCoupon,
    selectedCouponId,
    selectedAddonCoupons,
    remark,
    phone,
    status,
    idempotencyKey,
    latestPreview,
    isBusy,
    isPreviewStale,
    start,
    ensureIdempotencyKey,
    transition,
    applyPreview,
    invalidatePreview,
    clearCoupon,
    reset
  }
})
