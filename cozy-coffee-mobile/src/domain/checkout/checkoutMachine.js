import { ValidationError } from '@/services/errors/AppError'

export const CHECKOUT_STATUS = Object.freeze({
  IDLE: 'idle',
  PREVIEWING: 'previewing',
  AWAITING_AUTH: 'awaiting_auth',
  READY: 'ready',
  SUBMITTING: 'submitting',
  PAYING: 'paying',
  SUCCESS: 'success',
  FAILED: 'failed',
  OFFLINE: 'offline',
  CANCELLED: 'cancelled'
})

const transitions = {
  idle: { START_PREVIEW: 'previewing', OFFLINE: 'offline', RESET: 'idle' },
  previewing: { PREVIEW_SUCCEEDED: 'ready', AUTH_REQUIRED: 'awaiting_auth', PREVIEW_FAILED: 'failed', OFFLINE: 'offline', RESET: 'idle' },
  awaiting_auth: { AUTH_RESTORED: 'previewing', AUTH_FAILED: 'failed', OFFLINE: 'offline', RESET: 'idle' },
  ready: { START_PREVIEW: 'previewing', AUTH_REQUIRED: 'awaiting_auth', SUBMIT: 'submitting', OFFLINE: 'offline', RESET: 'idle' },
  submitting: { ORDER_CREATED: 'paying', AUTH_REQUIRED: 'awaiting_auth', SUBMIT_FAILED: 'failed', OFFLINE: 'offline', RESET: 'idle' },
  paying: { PAYMENT_SUCCEEDED: 'success', PAYMENT_CANCELLED: 'cancelled', PAYMENT_FAILED: 'failed', OFFLINE: 'offline', RESET: 'idle' },
  success: { RESET: 'idle' },
  failed: { RETRY: 'ready', START_PREVIEW: 'previewing', OFFLINE: 'offline', RESET: 'idle' },
  offline: { NETWORK_RESTORED: 'idle', RESET: 'idle' },
  cancelled: { RETRY: 'ready', START_PREVIEW: 'previewing', RESET: 'idle' }
}

export function transitionCheckout(current, event) {
  const next = transitions[current]?.[event]
  if (!next) {
    throw new ValidationError(`非法结算状态转换: ${current} -> ${event}`, {
      code: 'INVALID_CHECKOUT_TRANSITION',
      details: { current, event }
    })
  }
  return next
}
