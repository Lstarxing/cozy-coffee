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
  const selectedCouponId = ref(null)
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
    ensureIdempotencyKey()
    if ([CHECKOUT_STATUS.SUCCESS, CHECKOUT_STATUS.CANCELLED].includes(status.value)) status.value = CHECKOUT_STATUS.IDLE
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
      selectedCouponId.value = null
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
    selectedCouponId,
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
    reset
  }
})
