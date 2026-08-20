<!--
  积分规则页 - 对齐 prototype/points-rules.html：价值/获得/等级/挑战/使用/有效期/通用说明 + 去商城 CTA（标题由原生导航栏提供）
  等级门槛与兑换折扣对齐 constants/member.js 与 domain/member/memberRules.js
-->
<template>
  <view class="rules-page">
    <!-- 积分价值 -->
    <view class="rules-section">
      <text class="rules-kicker">VALUE</text>
      <text class="rules-title">积分价值</text>
      <text class="rules-desc">每 <text class="desc-em">30 积分 ≈ 1 元</text>（约 3.3% 消费返还）。消费越多、等级越高，积分回馈越丰。</text>
    </view>

    <!-- 如何获得 -->
    <view class="rules-section">
      <text class="rules-kicker">EARN</text>
      <text class="rules-title">如何获得积分</text>
      <view v-for="rule in earnRules" :key="rule.name" class="rule-card">
        <view class="rule-head">
          <text class="rule-name">{{ rule.name }}</text>
          <text class="rule-pts">{{ rule.pts }}</text>
        </view>
        <text class="rule-desc">{{ rule.desc }}</text>
      </view>
    </view>

    <!-- 等级与消费倍率 -->
    <view class="rules-section">
      <text class="rules-kicker">LEVEL</text>
      <text class="rules-title">等级与消费倍率</text>
      <text class="rules-desc">成长值（EXP）按 1 元实付 = 1 EXP 累计，达到门槛自动晋升。</text>

      <view class="rate-table">
        <view class="rate-row head">
          <text class="rate-col col-lv">等级</text>
          <text class="rate-col">成长值门槛</text>
          <text class="rate-col col-val">积分倍率</text>
        </view>
        <view v-for="row in rateRows" :key="row.en" class="rate-row">
          <view class="rate-col col-lv">
            <text class="rate-lv">{{ row.zh }}</text>
            <text class="rate-en">{{ row.en }} · {{ row.exp }} EXP</text>
          </view>
          <text class="rate-col">{{ row.hint }}</text>
          <text class="rate-col col-val"><text class="rate-val">{{ row.mult }}</text><text class="rate-x">×</text></text>
        </view>
      </view>

      <view class="note-strip"><text class="note-strong">黑金加速包：</text>每月前 300 元消费享受 <text class="note-em">1.7×</text> 高额倍率，超出部分回归 1.5×。</view>
    </view>

    <!-- 月度挑战 -->
    <view class="rules-section">
      <text class="rules-kicker">CHALLENGE</text>
      <text class="rules-title">月度挑战任务</text>
      <view v-for="rule in challengeRules" :key="rule.name" class="rule-card">
        <view class="rule-head">
          <text class="rule-name">{{ rule.name }}</text>
          <text class="rule-pts">{{ rule.pts }}</text>
        </view>
        <text class="rule-desc">{{ rule.desc }}</text>
      </view>
    </view>

    <!-- 如何使用 -->
    <view class="rules-section">
      <text class="rules-kicker">REDEEM</text>
      <text class="rules-title">如何使用积分</text>
      <view v-for="rule in redeemRules" :key="rule.name" class="rule-card">
        <view class="rule-head">
          <text class="rule-name">{{ rule.name }}</text>
          <text class="rule-pts">{{ rule.pts }}</text>
        </view>
        <text class="rule-desc">{{ rule.desc }}</text>
      </view>
      <view class="note-strip">优惠券的具体适用范围（杯型、品类、加料、金额封顶）以券面说明为准，具体券种规则优先于通用规则。</view>
    </view>

    <!-- 有效期与扣减 -->
    <view class="rules-section">
      <text class="rules-kicker">VALIDITY</text>
      <text class="rules-title">有效期与扣减</text>
      <view v-for="rule in validityRules" :key="rule.name" class="rule-card">
        <view class="rule-head">
          <text class="rule-name">{{ rule.name }}</text>
          <text class="rule-pts">{{ rule.pts }}</text>
        </view>
        <text class="rule-desc">{{ rule.desc }}</text>
      </view>
    </view>

    <!-- 通用说明 -->
    <view class="rules-section">
      <text class="rules-kicker">NOTICE</text>
      <text class="rules-title">通用说明</text>
      <view v-for="notice in notices" :key="notice" class="rule-card">
        <text class="rule-name">{{ notice }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
const earnRules = [
  { name: '消费赚积分', pts: '1 元 = 1 积分', desc: '每笔实付 1 元获得 1 积分，并按会员等级倍率加成（基础 1.0× 至黑金 1.5×）。' },
  { name: '每日签到', pts: '+2 / 天', desc: '每日固定获得 2 积分；连续签到 7 天额外赠送「满 35 减 10 元券」，有效期 3 天。' },
  { name: '完善资料', pts: '+20', desc: '绑定手机号、生日等信息，一次性获得 20 积分。' },
  { name: '邀请好友', pts: '买一赠一券', desc: '成功邀请好友下单，赠送邀请人「买一赠一券」。' },
  { name: '会员日 Cozy Day', pts: '倍率 +0.5', desc: '每周五会员日积分倍率提升 0.5（会员日倍率 = 等级倍率 + 0.5）；积分商城限量推出「100 积分兑 10 元券」。' }
]

const rateRows = [
  { zh: '基础', en: 'Classic', exp: '0', hint: '注册即达', mult: '1.0' },
  { zh: '白银', en: 'Silver', exp: '500', hint: '约 15–20 杯', mult: '1.1' },
  { zh: '黄金', en: 'Gold', exp: '1,500', hint: '约 40–60 杯', mult: '1.2' },
  { zh: '钻石', en: 'Diamond', exp: '4,000', hint: '约 120 杯+', mult: '1.3' },
  { zh: '黑金', en: 'Black Gold', exp: '9,000', hint: '深度爱好者', mult: '1.5' }
]

const challengeRules = [
  { name: '打卡达人', pts: '+40', desc: '当月累计下单 4 次。' },
  { name: '晨间唤醒', pts: '+60', desc: '10:00 前下单累计 3 次。' },
  { name: '外卖尝鲜', pts: '+50', desc: '当月完成 2 笔外卖订单。' },
  { name: '新品猎人', pts: '+80', desc: '尝试 3 款限定新品。' }
]

const redeemRules = [
  { name: '积分商城兑换', pts: '优惠券 · 周边', desc: '用积分兑换优惠券（加浓缩券、配送券、代金券、买一赠一券、任选通兑券）与限量周边礼品。' },
  { name: '等级兑换折扣', pts: '最高 8.5 折', desc: '白银及以上会员在积分商城兑换享等级折扣：白银 9.8 折 / 黄金 9.5 折 / 钻石 9.0 折 / 黑金 8.5 折，结算积分按后端规则向上取整。' }
]

const validityRules = [
  { name: '有效期', pts: '365 天', desc: '积分自获得之日起 365 天内有效，到期自动作废，请在有效期内使用。' },
  { name: '扣减顺序', pts: '先到期先用', desc: '使用积分时优先消耗最早获得的积分（FIFO），帮你把每一分都花在刀刃上。' }
]

const notices = [
  '积分不可提现、不可转让，不能兑换现金。',
  '积分余额与变动以系统流水记录为准。',
  '已兑换订单如需取消，可在兑换订单中操作，积分将原路退还。',
  '本规则由 Cozy Coffee 制定并保留最终解释权，如有调整将提前公示。'
]
</script>

<style lang="scss" scoped>
.rules-page {
  min-height: 100vh;
  padding: 40rpx 40rpx 60rpx;
  background: $cozy-surface;
}

/* ── 规则卡片 ── */
.rules-section {
  margin-top: 36rpx;
  padding: 52rpx 48rpx;
  border-radius: 28rpx;
  background: $bg-white;
}
.rules-kicker {
  display: block;
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: .16em;
  color: $cozy-primary;
}
.rules-title {
  display: block;
  margin-top: 16rpx;
  font-family: $font-display;
  font-size: 38rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.rules-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: $cozy-muted;
}
.desc-em {
  font-style: normal;
  color: $cozy-primary;
  font-weight: 700;
}

.rule-card {
  padding: 32rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; padding-bottom: 0; }
}
.rule-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 24rpx;
}
.rule-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
  letter-spacing: .03em;
}
.rule-pts {
  flex: none;
  font-family: $font-display;
  font-size: 30rpx;
  font-weight: 700;
  color: $cozy-primary;
}
.rule-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: $cozy-muted;
}

/* ── 倍率表 ── */
.rate-table {
  margin-top: 36rpx;
}
.rate-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 22rpx 12rpx;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }

  &.head {
    padding: 18rpx 12rpx;
    border-bottom: 1rpx solid $cozy-border;
  }
}
.rate-col {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: $cozy-ink;
}
.rate-col.col-lv { flex: 1.2; }
.rate-col.col-val {
  flex: .9;
  text-align: right;
}
.rate-row.head .rate-col {
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: .1em;
  color: $cozy-placeholder;
}
.rate-lv {
  display: inline;
  font-weight: 650;
  color: $cozy-ink;
}
.rate-en {
  display: block;
  margin-top: 4rpx;
  font-size: 20rpx;
  font-weight: 500;
  color: $cozy-placeholder;
  letter-spacing: .06em;
}
.rate-val {
  font-family: $font-display;
  font-size: 30rpx;
  font-weight: 700;
  color: $cozy-primary;
}
.rate-x {
  font-size: 22rpx;
}

/* ── 提示条 ── */
.note-strip {
  margin-top: 36rpx;
  padding: 28rpx 36rpx;
  border-radius: $cozy-radius-lg;
  background: $cozy-surface;
  font-size: 25rpx;
  line-height: 1.75;
  color: $cozy-muted;
}
.note-strong { color: $cozy-ink; font-weight: 650; }
.note-em { color: $cozy-primary; font-weight: 700; }
</style>
