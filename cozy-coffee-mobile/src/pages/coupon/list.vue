<!--
  优惠券页 - 对齐 prototype/coupons.html：顶部固定筛选（原生导航标题）+ 券卡（面值/条件 + 名称/范围/到期 + 去使用/状态）
  数据源: /member/mall/coupons (UserCouponDTO，displayTitle/displaySubTitle 后端已算好)
-->
<template>
  <view class="coupon-page">
    <!-- 筛选 tabs（选择模式隐藏，仅展示可使用） -->
    <view v-if="!selectMode" class="filter-tabs">
      <view
        class="filter-tab"
        :class="{ active: currentTab === 'available' }"
        @click="switchTab('available')"
      >可使用</view>
      <view
        class="filter-tab"
        :class="{ active: currentTab === 'unavailable' }"
        @click="switchTab('unavailable')"
      >不可使用</view>
    </view>

    <!-- 选择模式：不使用优惠券（灰色提示） -->
    <view v-if="selectMode" class="none-hint" @click="pickCoupon(null)">
      <text>不使用优惠券，按商品原价结算</text>
    </view>

    <!-- 券列表 -->
    <view v-if="displayCoupons.length" class="coupon-list">
      <view
        v-for="item in displayCoupons"
        :key="item.id"
        class="coupon-tap"
        @click="onCardTap(item)"
      >
        <CouponCard :coupon="item" :selectable="selectMode" :disabled="item.disabled" :reason="item.reason" @use="handleUse(item)" />
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <view class="empty-mark"><CozyIcon name="coupon" :size="36" color="#753A22" /></view>
      <text class="empty-text">暂无{{ tabText }}的优惠券</text>
      <text class="empty-hint">去积分商城兑换更多优惠券</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCouponList } from '@/api/coupon'
import { validateCouponForCart } from '@/utils/couponRules'
import CozyIcon from '@/components/CozyIcon.vue'
import CouponCard from '@/components/coupon/CouponCard.vue'

const currentTab = ref('available')
const coupons = ref([])
const selectMode = ref(false)
const cartContext = ref(null)

function parseRule(coupon) {
  try { return JSON.parse(coupon.ruleJson || '{}') } catch (_) { return {} }
}

function computeHasExtraShot(items) {
  return (items || []).some(item => String(item.coffeeStrength || '').toUpperCase() === 'STRONG')
}

onLoad((options) => {
  if (options?.select === '1') {
    selectMode.value = true
    currentTab.value = 'available'
    try {
      const raw = uni.getStorageSync('cozy_coupon_cart')
      if (raw) {
        const cart = typeof raw === 'string' ? JSON.parse(raw) : raw
        cartContext.value = {
          items: cart.items || [],
          hasExtraShot: computeHasExtraShot(cart.items),
          diningMethod: cart.diningMethod || 'TAKEOUT'
        }
        uni.removeStorageSync('cozy_coupon_cart')
      }
    } catch (_) {
      cartContext.value = null
    }
  }
})

const TAB_TEXT = { available: '可使用', unavailable: '不可使用' }
const STATUS_TEXT = { available: '可使用', frozen: '冻结中', used: '已使用', expired: '已过期' }

const TYPE_NAME = {
  EXCHANGE: '咖啡兑换券',
  DISCOUNT: '折扣券',
  FULL_REDUCE: '满减券',
  BOGO: '买一赠一券',
  DELIVERY_FEE: '配送费抵扣券',
  SHOT: '加浓缩券'
}

const TYPE_SCOPE = {
  EXCHANGE: '全场饮品通兑',
  DISCOUNT: '全场通用',
  FULL_REDUCE: '全场通用',
  BOGO: '现制饮品',
  DELIVERY_FEE: '仅限外送',
  SHOT: '附加券 · 可叠加'
}

onMounted(async () => {
  try {
    const res = await getCouponList()
    if (res.code === 200 && res.data) {
      coupons.value = res.data.map(item => {
        const mappedStatus = mapStatus(item)
        return {
          ...item,
          status: mappedStatus,
          displayTitle: item.displayTitle || (item.value ? `¥${item.value}` : TYPE_NAME[item.couponType] || '优惠券'),
          displaySubTitle: item.displaySubTitle || (item.minAmount ? `满${item.minAmount}可用` : '无门槛'),
          name: item.productName || item.title || TYPE_NAME[item.couponType] || '优惠券',
          scope: TYPE_SCOPE[item.couponType] || '全场通用',
          expireText: formatExpire(item, mappedStatus)
        }
      })
    }
  } catch (e) {
    console.error('获取优惠券失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
})

// 后端返回 ISSUED/FROZEN/USED/EXPIRED + available，映射为前端状态
function mapStatus(item) {
  if (item.status === 'USED') return 'used'
  if (item.status === 'FROZEN') return 'frozen'
  if (item.status === 'EXPIRED' || item.available === false) return 'expired'
  return 'available'
}

function formatExpire(item, status) {
  if (status !== 'available') return STATUS_TEXT[status]
  if (!item.expiresAt) return ''
  try {
    const d = new Date(item.expiresAt)
    if (Number.isNaN(d.getTime())) return String(item.expiresAt).slice(0, 10)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `至 ${mm}-${dd} 到期`
  } catch {
    return ''
  }
}

const filteredCoupons = computed(() => {
  if (currentTab.value === 'unavailable') {
    return coupons.value.filter(c => ['frozen', 'used', 'expired'].includes(c.status))
  }
  return coupons.value.filter(c => c.status === 'available')
})
const displayCoupons = computed(() => filteredCoupons.value.map(c => {
  if (!cartContext.value) return { ...c, disabled: false, reason: '' }
  const result = validateCouponForCart({ ...c, parsedRule: parseRule(c) }, cartContext.value.items, {
    hasExtraShot: cartContext.value.hasExtraShot,
    diningMethod: cartContext.value.diningMethod
  })
  return { ...c, disabled: !result.valid, reason: result.reason || '' }
}))
const tabText = computed(() => TAB_TEXT[currentTab.value])

function switchTab(value) {
  if (currentTab.value === value) return
  currentTab.value = value
}

function onCardTap(item) {
  if (selectMode.value && item.status === 'available') pickCoupon(item)
}

function handleUse(item) {
  if (selectMode.value) pickCoupon(item)
  else useCoupon(item)
}

function pickCoupon(coupon) {
  if (coupon && coupon.disabled) return
  uni.$emit('couponSelected', coupon)
  uni.navigateBack()
}

function useCoupon(coupon) {
  uni.switchTab({ url: '/pages/menu/menu' })
}
</script>

<style lang="scss" scoped>
.coupon-page {
  min-height: 100vh;
  padding-bottom: 220rpx;
  background: $cozy-surface;
}

/* ── 筛选 tabs（顶部固定条 · 下划线式，对齐订单页） ── */
.filter-tabs {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  height: 96rpx;
  background: $bg-white;
  border-bottom: 1rpx solid $cozy-border;
}
.filter-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: $cozy-muted;
  position: relative;
  transition: color $cozy-duration $cozy-ease-out;

  &.active { color: $cozy-ink; font-weight: 600; }
  &.active::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: 0;
    transform: translateX(-50%);
    width: 44rpx;
    height: 4rpx;
    border-radius: 2rpx;
    background: $cozy-ink;
  }
}

/* ── 券列表 ── */
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin: 32rpx 40rpx 0;
}
.coupon-tap { border-radius: 28rpx; }

/* ── 选择模式：不使用优惠券（灰色提示） ── */
.none-hint {
  margin: 36rpx 40rpx 0;
  padding: 22rpx 24rpx;
  border-radius: 16rpx;
  background: $cozy-surface;
  color: $cozy-muted;
  font-size: 24rpx;
  text-align: center;

  &:active { opacity: .8; }
}

/* ── 空状态 ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 320rpx);
  padding: 80rpx 40rpx;
}
.empty-mark {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  background: $cozy-surface;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-primary;
}
.empty-text {
  margin-top: 40rpx;
  font-family: $font-display;
  font-size: 40rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.empty-hint {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: $cozy-muted;
}
</style>
