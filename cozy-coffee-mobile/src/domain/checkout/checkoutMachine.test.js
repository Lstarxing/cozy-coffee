import { describe, expect, it } from 'vitest'
import { CHECKOUT_STATUS, transitionCheckout } from './checkoutMachine'
import { ValidationError } from '@/services/errors/AppError'

describe('checkout state machine', () => {
  it('supports the successful checkout path', () => {
    let state = CHECKOUT_STATUS.IDLE
    state = transitionCheckout(state, 'START_PREVIEW')
    state = transitionCheckout(state, 'PREVIEW_SUCCEEDED')
    state = transitionCheckout(state, 'SUBMIT')
    state = transitionCheckout(state, 'ORDER_CREATED')
    state = transitionCheckout(state, 'PAYMENT_SUCCEEDED')
    expect(state).toBe(CHECKOUT_STATUS.SUCCESS)
  })

  it('rejects impossible transitions', () => {
    expect(() => transitionCheckout('idle', 'ORDER_CREATED')).toThrow(ValidationError)
    expect(() => transitionCheckout('success', 'SUBMIT')).toThrow(ValidationError)
  })
})
