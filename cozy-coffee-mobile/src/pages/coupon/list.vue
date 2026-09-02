<!--
  优惠券页 - 对齐 prototype/coupons.html：顶部固定筛选（原生导航标题）+ 券卡（面值/条件 + 名称/范围/到期 + 去使用/状态）
  数据源: /member/mall/coupons (UserCouponDTO，displayTitle/displaySubTitle 后端已算好)

  选择模式（select=1，从确认页进入）：
  - 主券区：单张可选（兑换/折扣/满减/BOGO 等）
  - 辅券区：DELIVERY_FEE 单选、SHOT 多选（≤ 订单加浓缩杯数），可叠加
  - 底部"完成"栏统一确认并回跳；已选券再点可取消，主券区空时"不使用优惠券"清空全部
-->
<template>
  <view class="coupon-page">
    <!-- 筛选 tabs（选择模式隐藏，仅展示可使用） -->
    <FilterTabs v-if="!selectMode" :options="couponTabs" v-model="currentTab" sticky />

    <template v-if="selectMode">
      <!-- 顶部主券/辅券分类栏（对齐订单页 FilterTabs） -->
      <FilterTabs :options="selectTabs" v-model="selectTab" sticky />

      <!-- 主券 tab：单张选择 -->
      <template v-if="selectTab === 'main'">
        <view v-if="mainCoupons.length" class="coupon-list">
          <view v-for="item in mainCoupons" :key="item.id" class="coupon-tap" @click="toggleMain(item)">
            <CouponCard :coupon="item" :selectable="true" :disabled="item.disabled" :reason="item.reason" :selected="item.selected" @use="toggleMain(item)" />
          </view>
        </view>
        <view v-else class="empty-state">
          <view class="empty-mark"><CozyIcon name="coupon" :size="36" color="#753A22" /></view>
          <text class="empty-text">暂无可用主券</text>
          <text class="empty-hint">去积分商城兑换更多优惠券</text>
          <view class="empty-none-hint" @click="pickNone">
            <text>不使用优惠券，按商品原价结算</text>
          </view>
        </view>
      </template>

      <!-- 辅券 tab：可叠加 -->
      <template v-else>
        <view v-if="addonCoupons.length" class="coupon-list">
          <view v-for="item in addonCoupons" :key="item.id" class="coupon-tap" @click="toggleAddon(item)">
            <CouponCard :coupon="item" :selectable="true" :disabled="item.disabled" :reason="item.reason" :selected="item.selected" @use="toggleAddon(item)" />
          </view>
        </view>
        <view v-else class="empty-state">
          <view class="empty-mark"><CozyIcon name="coupon" :size="36" color="#753A22" /></view>
          <text class="empty-text">暂无可用辅券</text>
          <text class="empty-hint">加浓缩券/配送费券可叠加主券使用</text>
        </view>
      </template>

      <!-- 底部完成栏 -->
      <view class="confirm-bar safe-area-bottom">
        <view class="confirm-summary" :class="{ active: totalSelectedCount > 0 }">{{ confirmSummary }}</view>
        <view class="confirm-btn" @click="confirmSelection">完成{{ totalSelectedCount ? `(${totalSelectedCount})` : '' }}</view>
      </view>
    </template>

    <!-- 非选择模式：券列表 -->
    <template v-else>
      <view v-if="displayCoupons.length" class="coupon-list">
        <view
          v-for="item in displayCoupons"
          :key="item.id"
          class="coupon-tap"
        >
          <CouponCard :coupon="item" :selectable="false" :disabled="item.disabled" :reason="item.reason" @use="handleUse(item)" />
        </view>
      </view>

      <!-- 空状态 -->
      <view v-else class="empty-state">
        <view class="empty-mark"><CozyIcon name="coupon" :size="36" color="#753A22" /></view>
        <text class="empty-text">暂无{{ tabText }}的优惠券</text>
        <text class="empty-hint">去积分商城兑换更多优惠券</text>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCouponList } from '@/api/coupon'
import { validateCouponForCart } from '@/utils/couponRules'
import CozyIcon from '@/components/CozyIcon.vue'
import CouponCard from '@/components/coupon/CouponCard.vue'
import FilterTabs from '@/components/common/FilterTabs.vue'

const currentTab = ref('available')
const couponTabs = [
  { value: 'available', label: '可使用' },
  { value: 'unavailable', label: '不可使用' }
]
const selectTab = ref('main') // 选择模式顶部分类栏：主券 / 辅券
const selectTabs = [
  { value: 'main', label: '主券' },
  { value: 'addon', label: '辅券' }
]
const coupons = ref([])
const selectMode = ref(false)
const cartContext = ref(null)
const selectedMainCode = ref('')
const selectedAddonCodes = ref(new Set())
const extraShotCount = ref(0)

const ADDON_TYPES = ['DELIVERY_FEE', 'SHOT']

function parseRule(coupon) {
  try { return JSON.parse(coupon.ruleJson || '{}') } catch (_) { return {} }
}

function computeHasExtraShot(items) {
  return (items || []).some(item => String(item.coffeeStrength || '').toUpperCase() === 'STRONG')
}

function countExtraShot(items) {
  return (items || []).reduce((sum, item) => {
    const strong = String(item.coffeeStrength || '').toUpperCase() === 'STRONG'
    const addons = String(item.addons || '[]')
    const hasAddon = addons.toLowerCase().includes('extra_shot') || addons.includes('加浓')
    return sum + ((strong || hasAddon) ? Number(item.quantity || 1) : 0)
  }, 0)
}

function couponKey(c) { return String(c.couponCode || c.code || c.id || '') }

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
        extraShotCount.value = countExtraShot(cart.items)
        selectedMainCode.value = String(cart.selectedCouponCode || '')
        selectedAddonCodes.value = new Set((cart.selectedAddonCodes || []).map(String))
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

// ==================== 选择模式：主券/辅券 ====================

const mainCoupons = computed(() => displayCoupons.value
  .filter(c => !ADDON_TYPES.includes(c.couponType))
  .map(c => ({ ...c, selected: couponKey(c) === selectedMainCode.value })))
const addonCoupons = computed(() => displayCoupons.value
  .filter(c => ADDON_TYPES.includes(c.couponType))
  .map(c => ({ ...c, selected: selectedAddonCodes.value.has(couponKey(c)) })))

const totalSelectedCount = computed(() => (selectedMainCode.value ? 1 : 0) + selectedAddonCodes.value.size)
const confirmSummary = computed(() => {
  const parts = []
  if (selectedMainCode.value) {
    const c = coupons.value.find(x => couponKey(x) === selectedMainCode.value)
    parts.push(c ? c.name : '主券')
  }
  if (selectedAddonCodes.value.size) {
    parts.push(`辅券 ×${selectedAddonCodes.value.size}`)
  }
  return parts.length ? parts.join(' + ') : '未使用优惠券'
})

function toggleMain(item) {
  if (item.disabled) return
  selectedMainCode.value = selectedMainCode.value === couponKey(item) ? '' : couponKey(item)
}

function toggleAddon(item) {
  if (item.disabled) return
  const key = couponKey(item)
  const next = new Set(selectedAddonCodes.value)
  if (next.has(key)) {
    next.delete(key)
  } else if (item.couponType === 'DELIVERY_FEE') {
    // 配送费券单选：先移除已有配送费券
    for (const c of addonCoupons.value) {
      if (c.couponType === 'DELIVERY_FEE') next.delete(couponKey(c))
    }
    next.add(key)
  } else if (item.couponType === 'SHOT') {
    const shotSelected = [...next].filter(k => {
      const c = coupons.value.find(x => couponKey(x) === k)
      return c && c.couponType === 'SHOT'
    }).length
    if (shotSelected >= extraShotCount.value) {
      uni.showToast({ title: `最多使用 ${extraShotCount.value} 张加浓缩券`, icon: 'none' })
      return
    }
    next.add(key)
  } else {
    next.add(key)
  }
  selectedAddonCodes.value = next
}

function pickNone() {
  selectedMainCode.value = ''
  selectedAddonCodes.value = new Set()
  uni.$emit('couponSelected', null)
  uni.$emit('addonCouponsSelected', [])
  uni.navigateBack()
}

function confirmSelection() {
  const main = selectedMainCode.value
    ? coupons.value.find(x => couponKey(x) === selectedMainCode.value) || null
    : null
  const addons = coupons.value.filter(c => selectedAddonCodes.value.has(couponKey(c)))
  uni.$emit('couponSelected', main)
  uni.$emit('addonCouponsSelected', addons)
  uni.navigateBack()
}

// ==================== 非选择模式 ====================

function handleUse(item) {
  if (item.status === 'available') useCoupon(item)
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

/* ── 券列表 ── */
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin: 32rpx 40rpx 0;
}
.coupon-tap { border-radius: 28rpx; }

/* ── 底部完成栏 ── */
.confirm-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 32rpx max(20rpx, env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, .96);
  box-shadow: 0 -4rpx 24rpx rgba(25, 18, 14, .06);
}
.confirm-summary {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: $cozy-muted;
  font-size: 24rpx;
  white-space: nowrap;
  text-overflow: ellipsis;

  &.active { color: $cozy-ink; }
}
.confirm-btn {
  flex: none;
  min-width: 200rpx;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: $cozy-ink;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;

  &:active { opacity: .85; }
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
.empty-none-hint {
  margin-top: 48rpx;
  width: 100%;
  padding: 22rpx 24rpx;
  box-sizing: border-box;
  border-radius: 16rpx;
  background: $cozy-surface;
  color: $cozy-muted;
  font-size: 24rpx;
  text-align: center;

  &:active { opacity: .8; }
}
</style>
