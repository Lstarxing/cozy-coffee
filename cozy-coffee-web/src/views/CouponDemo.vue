<!-- 优惠券UI演示页面 - 用于开发调试 -->
<template>
  <div class="coupon-demo-page">
    <h1>优惠券UI设计系统演示</h1>
    <p class="subtitle">基于6模板优惠券系统 v5.5.1</p>

    <div class="demo-section">
      <h2>身份等级系列 (Status Series)</h2>
      
      <h3>👑 黑金 - 尊享通兑券</h3>
      <CouponCardPremium :coupon="mockCoupons.blackPremium" />

      <h3>💎 钻石 - 月度免单券</h3>
      <CouponCardPremium :coupon="mockCoupons.diamondMonthly" />

      <h3>🥇 黄金 - 生日免单券</h3>
      <CouponCardPremium :coupon="mockCoupons.goldBirthday" />

      <h3>🥈 白银 - 生日5折券</h3>
      <CouponCardPremium :coupon="mockCoupons.silverBirthday" />
    </div>

    <div class="demo-section">
      <h2>营销社交系列 (Promo & Social)</h2>
      
      <h3>🟠 促销 - 新品5折券</h3>
      <CouponCardPremium :coupon="mockCoupons.newProduct" />

      <h3>🟢 社交 - 买一赠一券 (BOGO)</h3>
      <CouponCardPremium :coupon="mockCoupons.bogo" />
    </div>

    <div class="demo-section">
      <h2>辅助系列 (Utility)</h2>
      
      <h3>🌸 甜品 - 蛋糕5折券</h3>
      <CouponCardPremium :coupon="mockCoupons.dessert" />

      <h3>🔘 工具 - 配送费抵扣券</h3>
      <CouponCardPremium :coupon="mockCoupons.delivery" />
    </div>

    <div class="demo-section">
      <h2>特殊状态展示</h2>
      
      <h3>⚠️ 即将过期（剩2天）</h3>
      <CouponCardPremium :coupon="mockCoupons.expiringSoon" />

      <h3>❌ 已过期</h3>
      <CouponCardPremium :coupon="mockCoupons.expired" />
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import CouponCardPremium from '@/components/coupon/CouponCardPremium.vue'

// 模拟券数据
const mockCoupons = reactive({
  // 黑金尊享通兑券
  blackPremium: {
    id: 1,
    couponCode: 'MONTHLY_BLACK_FREE_001',
    couponType: 'EXCHANGE',
    status: 'ISSUED',
    productName: '尊享通兑券',
    description: '可兑换任意饮品一杯（不限杯型，含特调/SOE）',
    ruleJson: JSON.stringify({
      type: 'EXCHANGE',
      maxDiscount: 45,
      skuLimit: 'ALL',
      freeAddon: 1,
      productName: '黑金月度免单券',
      description: '任意饮品免单（全品类，不限杯型，最高抵扣¥45）'
    }),
    expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString()
  },

  // 钻石月度免单券
  diamondMonthly: {
    id: 2,
    couponCode: 'MONTHLY_DIAMOND_FREE_001',
    couponType: 'EXCHANGE',
    status: 'ISSUED',
    productName: '钻石月度免单券',
    description: '限标准杯饮品（经典意式），最高抵扣¥35',
    ruleJson: JSON.stringify({
      type: 'EXCHANGE',
      maxDiscount: 35,
      skuLimit: 'STANDARD_ONLY',
      categoryBlocklist: ['signature', 'soe', 'pour-over']
    }),
    expiresAt: new Date(Date.now() + 25 * 24 * 60 * 60 * 1000).toISOString()
  },

  // 黄金生日免单券
  goldBirthday: {
    id: 3,
    couponCode: 'BIRTHDAY_GOLD_FREE_001',
    couponType: 'EXCHANGE',
    status: 'ISSUED',
    productName: '生日免单券',
    description: '🎂生日特权：限标准杯饮品（经典意式），最高抵扣¥40）',
    ruleJson: JSON.stringify({
      type: 'EXCHANGE',
      maxDiscount: 40,
      skuLimit: 'STANDARD_ONLY',
      categoryBlocklist: ['signature', 'soe', 'pour-over']
    }),
    expiresAt: new Date(Date.now() + 20 * 24 * 60 * 60 * 1000).toISOString()
  },

  // 白银生日5折券
  silverBirthday: {
    id: 4,
    couponCode: 'BIRTHDAY_SILVER_HALF_001',
    couponType: 'DISCOUNT',
    status: 'ISSUED',
    productName: '生日5折券',
    description: '🎂生日特权：单饮品5折（限标准杯，最高抵¥20）',
    ruleJson: JSON.stringify({
      type: 'DISCOUNT',
      discountRate: 0.5,
      limit: 'SINGLE_ITEM',
      skuLimit: 'STANDARD_ONLY',
      scope: 'DRINK_ONLY',
      maxDiscountAmount: 20
    }),
    expiresAt: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000).toISOString()
  },

  // 新品5折券
  newProduct: {
    id: 5,
    couponCode: 'NEW_PRODUCT_HALF_001',
    couponType: 'DISCOUNT',
    status: 'ISSUED',
    productName: '新品5折券',
    description: '新品上市特惠，限时5折',
    ruleJson: JSON.stringify({
      type: 'DISCOUNT',
      discountRate: 0.5,
      scope: 'NEW_PRODUCT_ONLY',
      maxDiscountAmount: 25
    }),
    expiresAt: new Date(Date.now() + 10 * 24 * 60 * 60 * 1000).toISOString()
  },

  // 买一赠一券
  bogo: {
    id: 6,
    couponCode: 'BOGO_001',
    couponType: 'BOGO',
    status: 'ISSUED',
    productName: '好友分享券 (BOGO)',
    description: '买一送一，第二杯免费',
    ruleJson: JSON.stringify({
      type: 'BOGO',
      maxDiscount: 40,
      scope: 'DRINK_ONLY'
    }),
    expiresAt: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString()
  },

  // 蛋糕5折券
  dessert: {
    id: 7,
    couponCode: 'BIRTHDAY_CAKE_HALF_001',
    couponType: 'DISCOUNT',
    status: 'ISSUED',
    productName: '烘培甜品五折券',
    description: '生日礼：烘焙甜品享5折优惠',
    ruleJson: JSON.stringify({
      type: 'DISCOUNT',
      discountRate: 0.5,
      scope: 'CAKE_ONLY'
    }),
    expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString()
  },

  // 配送费抵扣券
  delivery: {
    id: 8,
    couponCode: 'DELIVERY_FEE_001',
    couponType: 'ADDON',
    status: 'ISSUED',
    productName: '配送费抵扣券',
    description: '外卖订单可用，最高抵扣¥6配送费',
    ruleJson: JSON.stringify({
      type: 'ADDON',
      value: 6,
      scope: 'DELIVERY_ONLY'
    }),
    expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString()
  },

  // 即将过期（剩2天）
  expiringSoon: {
    id: 9,
    couponCode: 'EXPIRING_SOON_001',
    couponType: 'DISCOUNT',
    status: 'ISSUED',
    productName: '即将过期测试券',
    description: '演示即将过期的UI效果',
    ruleJson: JSON.stringify({
      type: 'DISCOUNT',
      discountRate: 0.88,
      maxDiscountAmount: 10
    }),
    expiresAt: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString()
  },

  // 已过期
  expired: {
    id: 10,
    couponCode: 'EXPIRED_001',
    couponType: 'EXCHANGE',
    status: 'EXPIRED',
    productName: '已过期测试券',
    description: '演示已过期的UI效果',
    ruleJson: JSON.stringify({
      type: 'EXCHANGE',
      maxDiscount: 30
    }),
    expiresAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString()
  }
})
</script>

<style scoped lang="scss">
.coupon-demo-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 40px 20px;

  h1 {
    text-align: center;
    font-size: 32px;
    color: #333;
    margin-bottom: 8px;
  }

  .subtitle {
    text-align: center;
    color: #999;
    margin-bottom: 40px;
  }

  .demo-section {
    max-width: 800px;
    margin: 0 auto 60px;

    h2 {
      font-size: 24px;
      color: #333;
      margin-bottom: 24px;
      padding-bottom: 12px;
      border-bottom: 2px solid #FF7043;
    }

    h3 {
      font-size: 16px;
      color: #666;
      margin: 24px 0 12px;
      padding-left: 12px;
      border-left: 3px solid #FF7043;
    }
  }
}
</style>
