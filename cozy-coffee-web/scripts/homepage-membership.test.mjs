import assert from 'node:assert/strict'
import test from 'node:test'

import {
  calculateEarnedPoints,
  calculateRedemptionCost,
  calculateProgress
} from '../src/utils/homepageMembership.js'

test('Silver member earns 35 points from a ¥32 normal-day purchase', () => {
  assert.equal(calculateEarnedPoints(32, 'silver'), 35)
})

test('Silver member pays 147 points for a 150-point voucher', () => {
  assert.equal(calculateRedemptionCost(150, 'silver'), 147)
})

test('131 points is 89% progress toward the Silver voucher target', () => {
  assert.equal(calculateProgress(131, 147), 89)
})
