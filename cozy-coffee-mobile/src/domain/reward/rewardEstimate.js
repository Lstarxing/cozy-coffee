/**
 * 奖励预估纯函数（本地试算用，与后端 OrderRewardService 口径一致）：
 * - 奖励基数 = 实付 − 配送费（配送费不计入）
 * - 成长值 = 基数 1:1，无等级倍率
 * - 积分 = 基数 × 会员积分倍率（等级/会员日/黑卡加速由后端精确计算，此处仅本地兜底）
 */
export function rewardBase(preview) {
  return Math.max(0, Number(preview?.payable || 0) - Number(preview?.deliveryFee || 0))
}

export function estimateEarned(preview, pointsRate = 1) {
  const base = rewardBase(preview)
  return {
    points: Math.floor(base * Number(pointsRate || 1)),
    exp: Math.floor(base)
  }
}
