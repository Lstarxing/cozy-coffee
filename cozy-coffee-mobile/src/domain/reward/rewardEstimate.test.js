import { describe, expect, it } from 'vitest'
import { estimateEarned, rewardBase } from './rewardEstimate'

describe('rewardEstimate（奖励预估，与后端口径一致）', () => {
  it('基数 = 实付 − 配送费', () => {
    expect(rewardBase({ payable: 24, deliveryFee: 2 })).toBe(22)
    expect(rewardBase({ payable: 22, deliveryFee: 0 })).toBe(22)
    expect(rewardBase({ payable: 10, deliveryFee: 20 })).toBe(0)
    expect(rewardBase(null)).toBe(0)
  })

  it('成长值 = 基数 1:1，不乘积分倍率', () => {
    // 白银倍率 1.1：22 元商品 + 2 配送费 → 成长值 22，不是 24.2
    const { exp } = estimateEarned({ payable: 24, deliveryFee: 2 }, 1.1)
    expect(exp).toBe(22)
  })

  it('积分 = 基数 × 积分倍率', () => {
    const { points } = estimateEarned({ payable: 24, deliveryFee: 2 }, 1.1)
    expect(points).toBe(24) // 22 * 1.1 = 24.2 → floor 24
  })

  it('无配送费时直接用实付', () => {
    const { points, exp } = estimateEarned({ payable: 22, deliveryFee: 0 })
    expect(points).toBe(22)
    expect(exp).toBe(22)
  })

  it('倍率缺省为 1', () => {
    const { points } = estimateEarned({ payable: 22, deliveryFee: 0 })
    expect(points).toBe(22)
  })
})
