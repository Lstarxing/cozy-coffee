<!--
  积分商城 - 复现 prototype/mall.html 极简 Editorial
  衬线积分头 + 积分下方 | 分隔快捷链接（积分明细/兑换记录/积分规则）+ 1:1 商品网格
-->
<template>
  <view class="mall-page">
    <!-- 积分头 + 快捷链接 -->
    <view class="points-header">
      <text class="points-label">当前可用积分</text>
      <text class="points-value">{{ userStore.memberInfo.currentPoints || 0 }}</text>
      <view class="quick-links">
        <text class="quick-link" @click="goToHistory">积分明细</text>
        <text class="link-sep">|</text>
        <text class="quick-link" @click="goToRedemptions">兑换订单</text>
        <text class="link-sep">|</text>
        <text class="quick-link" @click="goToRules">积分规则</text>
      </view>
    </view>

    <!-- 商品网格 -->
    <view v-if="productLoading && products.length === 0" class="page-state">正在加载积分商品…</view>
    <view v-else-if="productError && products.length === 0" class="page-state error">
      <text>{{ productError }}</text>
      <view class="retry-button" @click="loadProducts">重新加载</view>
    </view>
    <view v-else-if="products.length === 0" class="page-state">暂无可兑换商品</view>
    <view v-else class="products-grid">
      <view
        v-for="item in products"
        :key="item.id"
        class="product-card"
        @click="openDetail(item)"
      >
        <view class="product-image">
          <image v-if="item.image" :src="item.image" class="product-photo" mode="aspectFill" />
        </view>
        <view class="product-info">
          <text class="product-name">{{ item.name }}</text>
          <view class="product-points">
            <text class="points-num">{{ getDiscountedPointsCost(item.pointsPrice, 1, redeemDiscount) }}</text>
            <text class="points-unit">积分</text>
            <text v-if="redeemDiscount < 1" class="points-original">{{ item.pointsPrice }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getPointsProducts } from '@/api/product'
import { getMemberInfo } from '@/api/member'
import {
  getDiscountedPointsCost
} from '@/domain/member/memberRules'

const userStore = useUserStore()
const products = ref([])
const productLoading = ref(false)
const productError = ref('')

const redeemDiscount = computed(() => Number(userStore.memberInfo?.redeemDiscount) || 1)

async function loadMember() {
  const response = await getMemberInfo()
  if (response.code === 200 && response.data) userStore.setMemberInfo(response.data)
}

async function loadProducts() {
  productLoading.value = true
  productError.value = ''
  try {
    const response = await getPointsProducts()
    products.value = (response.data || []).map(item => ({
      ...item,
      image: item.imageUrl || item.image || '',
      description: item.description || '',
      category: item.category || (item.productType === 'PHYSICAL' ? 'physical' : 'coupon')
    }))
  } catch (error) {
    products.value = []
    productError.value = error?.message || '积分商品加载失败'
  } finally {
    productLoading.value = false
  }
}

async function loadMallData() {
  try {
    await loadMember()
  } catch (error) {
    productError.value = error?.message || '会员积分加载失败'
  }
  await loadProducts()
}

function openDetail(item) {
  uni.setStorageSync('cozy_mall_product', item)
  uni.navigateTo({ url: '/pages/mall/detail' })
}

function goToHistory() { uni.navigateTo({ url: '/pages/points/history' }) }
function goToRedemptions() { uni.navigateTo({ url: '/pages/points/redemptions' }) }
function goToRules() { uni.navigateTo({ url: '/pages/points/rules' }) }

onShow(loadMallData)
</script>

<style lang="scss" scoped>
.mall-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding-bottom: 60rpx;
}

/* ── 积分头 ── */
.points-header {
  padding: 40rpx 48rpx 28rpx;
  background: $cozy-surface;
}
.points-label {
  display: block;
  font-size: 26rpx;
  color: $cozy-muted;
  letter-spacing: .04em;
}
.points-value {
  display: block;
  margin-top: 16rpx;
  font-family: $font-display;
  font-size: 92rpx;
  font-weight: 600;
  color: $cozy-ink;
  line-height: 1;
}

/* ── 快捷链接（积分下方 · | 分隔） ── */
.quick-links {
  display: flex;
  align-items: center;
  gap: 28rpx;
  margin-top: 36rpx;
}
.quick-link {
  font-size: 28rpx;
  color: $cozy-muted;
  letter-spacing: .04em;

  &:active { opacity: .6; }
}
.link-sep {
  font-size: 24rpx;
  color: $cozy-border;
}

/* ── 商品网格 ── */
.products-grid {
  margin: 32rpx 40rpx 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32rpx 28rpx;
  padding-bottom: 48rpx;
}
.product-card {
  &:active .product-image { transform: scale(.98); }
}
.product-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 16rpx;
  background: $cozy-bg;
  overflow: hidden;
  transition: transform $cozy-duration $cozy-ease-out;
}
.product-photo {
  width: 100%;
  height: 100%;
}
.product-info { padding: 24rpx 4rpx 0; }
.product-name {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-family: $font-display;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
  line-height: 1.4;
}
.product-points {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
  margin-top: 14rpx;
}
.points-num { font-size: 34rpx; font-weight: 700; color: $cozy-primary; }
.points-unit { font-size: 22rpx; color: $cozy-muted; }
.points-original {
  margin-left: 10rpx;
  font-size: 20rpx;
  color: $cozy-placeholder;
  text-decoration: line-through;
}

.page-state { padding: 120rpx 40rpx; color: $cozy-muted; text-align: center; }
.page-state text { display: block; }
.page-state.error { color: $error-color; }
.retry-button {
  width: 220rpx;
  margin: 32rpx auto 0;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $cozy-radius-md;
  background: $cozy-primary;
  color: #fff;
  font-size: 24rpx;
}
</style>
