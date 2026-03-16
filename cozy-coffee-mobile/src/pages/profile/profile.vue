<!--
  个人中心 - 商业化重构版
  特点：品牌化头部、极简资产展示、分块式功能组
-->
<template>
  <view class="profile-page">
    
    <!-- 头部区域 (Brand Header) -->
    <view class="profile-header" :class="userLevel">
      <view class="user-info" @click="handleLogin">
        <image 
          :src="isLoggedIn ? userInfo.avatar : '/static/images/default-avatar.png'" 
          class="user-avatar" 
          mode="aspectFill"
        />
        <view class="user-text">
          <text class="user-name">{{ isLoggedIn ? (userInfo.nickname || 'Cozy用户') : '点击登录/注册' }}</text>
          <view class="level-tag" v-if="isLoggedIn">
            <text class="level-icon">👑</text>
            <text>{{ getLevelName(userLevel) }}</text>
          </view>
          <text class="user-desc" v-else>登录享受更多会员权益</text>
        </view>
      </view>
      
      <!-- 头部装饰 -->
      <view class="header-decoration"></view>
    </view>
    
    <!-- 资产数据 (Assets Grid) -->
    <view class="assets-card">
      <view class="asset-item" @click="navigateTo('/pages/points/history')">
        <text class="asset-value">{{ memberInfo.currentPoints || 0 }}</text>
        <text class="asset-label">当前积分</text>
      </view>
      <view class="asset-item" @click="navigateTo('/pages/coupon/list')">
        <text class="asset-value">{{ couponStats.exchange }}</text>
        <text class="asset-label">兑换券</text>
      </view>
      <view class="asset-item" @click="navigateTo('/pages/coupon/list')">
        <text class="asset-value">{{ couponStats.total - couponStats.exchange }}</text>
        <text class="asset-label">优惠券</text>
      </view>
    </view>
    
    <!-- 订单状态 (Order Status) -->
    <view class="section-card">
      <view class="card-header" @click="navigateTo('/pages/order/order?tab=all')">
        <text class="card-title">我的订单</text>
        <text class="card-more">全部 ></text>
      </view>
      <view class="order-grid">
        <view class="grid-item" @click="navigateTo('/pages/order/order?tab=pending')">
          <text class="grid-icon">⏳</text>
          <text class="grid-text">待支付</text>
        </view>
        <view class="grid-item" @click="navigateTo('/pages/order/order?tab=processing')">
          <text class="grid-icon">☕</text>
          <text class="grid-text">制作中</text>
        </view>
        <view class="grid-item" @click="navigateTo('/pages/order/order?tab=delivering')">
          <text class="grid-icon">🛵</text>
          <text class="grid-text">配送中</text>
        </view>
        <view class="grid-item" @click="navigateTo('/pages/order/order?tab=completed')">
          <text class="grid-icon">💬</text>
          <text class="grid-text">待评价</text>
        </view>
      </view>
    </view>
    
    <!-- 核心功能入口 -->
    <view class="section-card menu-group">
      <view class="menu-row" @click="navigateTo('/pages/mall/index')">
        <view class="menu-left">
          <text class="menu-icon">🎁</text>
          <text class="menu-text">积分商城</text>
        </view>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-row" @click="navigateTo('/pages/benefits/index')">
        <view class="menu-left">
          <text class="menu-icon">💎</text>
          <text class="menu-text">会员权益</text>
        </view>
        <text class="menu-desc">查看等级特权</text>
      </view>
      <view class="menu-row" @click="navigateTo('/pages/address/list')">
        <view class="menu-left">
          <text class="menu-icon">📍</text>
          <text class="menu-text">收货地址</text>
        </view>
        <text class="menu-arrow">></text>
      </view>
    </view>
    
    <!-- 系统服务 -->
    <view class="section-card menu-group">
      <view class="menu-row">
        <view class="menu-left">
          <text class="menu-icon">🎧</text>
          <text class="menu-text">联系客服</text>
        </view>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-row" @click="navigateTo('/pages/settings/index')">
        <view class="menu-left">
          <text class="menu-icon">⚙️</text>
          <text class="menu-text">设置</text>
        </view>
        <text class="menu-arrow">></text>
      </view>
    </view>
    
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getMemberInfo } from '@/api/member'
import { getCouponList } from '@/api/coupon'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)
const memberInfo = computed(() => userStore.memberInfo)
const userLevel = computed(() => userStore.userLevel)

// 本地优惠券统计（直接从接口获取，不依赖后端 MemberDTO）
const couponStats = ref({ total: 0, exchange: 0 })

// 页面加载时刷新会员信息和优惠券数量
onMounted(async () => {
  if (isLoggedIn.value) {
    try {
      // 获取会员基本信息（积分、等级等）
      const res = await getMemberInfo()
      if (res.code === 200) {
        userStore.setMemberInfo(res.data)
      }
    } catch (e) {
      console.error('获取会员信息失败', e)
    }
    
    // 单独获取优惠券数量（直接调用券包接口）
    try {
      // 不传 status，获取所有券
      const couponRes = await getCouponList()
      console.log('个人中心-优惠券接口返回:', couponRes)
      
      if (couponRes.code === 200 && couponRes.data) {
        // 只统计可用的券 (ISSUED 状态且 available = true)
        const availableCoupons = couponRes.data.filter(c => 
          c.status === 'ISSUED' && c.available === true
        )
        couponStats.value.total = availableCoupons.length
        couponStats.value.exchange = availableCoupons.filter(c => c.couponType === 'EXCHANGE').length
        console.log('可用优惠券统计:', couponStats.value)
      }
    } catch (e) {
      console.error('获取优惠券数量失败', e)
    }
  }
})

const getLevelName = (level) => {
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', diamond: '钻石会员', black: '黑金会员' }
  return map[level] || '注册会员'
}

const handleLogin = () => {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
  }
}

const navigateTo = (url) => {
  if (isLoggedIn.value) {
    uni.navigateTo({ url })
  } else {
    uni.navigateTo({ url: '/pages/login/index' })
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: $spacing-xl;
}

// 头部区域
.profile-header {
  height: 400rpx;
  padding: 100rpx $spacing-lg 0;
  color: #fff;
  position: relative;
  overflow: hidden;
  
  // 主题色背景
  background: linear-gradient(135deg, $primary-color, $primary-dark);
  
  // 等级皮肤
  &.black { background: linear-gradient(135deg, #333, #000); }
  &.silver { background: linear-gradient(135deg, #7F8C8D, #BDC3C7); }
  
  .user-info {
    display: flex;
    align-items: center;
    position: relative;
    z-index: 10;
    
    .user-avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      border: 4rpx solid rgba(255,255,255,0.3);
      background: #eee;
    }
    
    .user-text {
      margin-left: $spacing-md;
      
      .user-name {
        font-size: 40rpx;
        font-weight: 700;
        display: block;
        margin-bottom: 8rpx;
      }
      
      .user-desc {
        font-size: 24rpx;
        opacity: 0.8;
      }
      
      .level-tag {
        display: inline-flex;
        align-items: center;
        background: rgba(0,0,0,0.2);
        padding: 4rpx 16rpx;
        border-radius: 20rpx;
        font-size: 22rpx;
        
        .level-icon { margin-right: 6rpx; }
      }
    }
  }
  
  .header-decoration {
    position: absolute;
    bottom: -50rpx;
    right: -50rpx;
    width: 300rpx;
    height: 300rpx;
    background: rgba(255,255,255,0.1);
    border-radius: 50%;
    pointer-events: none;
  }
}

// 资产卡片
.assets-card {
  margin: -60rpx $spacing-md $spacing-md;
  background: $bg-white;
  border-radius: $card-radius-lg;
  padding: $spacing-lg 0;
  display: flex;
  justify-content: space-around;
  position: relative;
  z-index: 10;
  box-shadow: $box-shadow;
  
  .asset-item {
    text-align: center;
    
    .asset-value {
      font-size: 40rpx;
      font-weight: 700;
      color: $text-main;
      display: block;
      margin-bottom: 4rpx;
      font-family: 'DIN', sans-serif; // 建议引入数字字体
    }
    
    .asset-label {
      font-size: 24rpx;
      color: $text-sub;
    }
  }
}

// 通用卡片容器
.section-card {
  background: $bg-white;
  border-radius: $card-radius-lg;
  margin: 0 $spacing-md $spacing-md;
  padding: $spacing-md;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-md;
    padding-bottom: $spacing-xs;
    
    .card-title {
      font-size: 30rpx;
      font-weight: 700;
      color: $text-main;
    }
    
    .card-more {
      font-size: 24rpx;
      color: $text-placeholder;
    }
  }
}

// 订单 Grid
.order-grid {
  display: flex;
  justify-content: space-between;
  padding: 0 $spacing-sm;
  
  .grid-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .grid-icon {
      font-size: 48rpx;
      margin-bottom: $spacing-xs;
      color: $primary-color;
    }
    
    .grid-text {
      font-size: 24rpx;
      color: $text-main;
    }
  }
}

// 菜单组列表
.menu-group {
  padding: $spacing-sm $spacing-md;
  
  .menu-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-md 0;
    border-bottom: 1rpx solid rgba(0,0,0,0.03); // 极淡分割线
    
    &:last-child {
      border-bottom: none;
    }
    
    .menu-left {
      display: flex;
      align-items: center;
      
      .menu-icon {
        font-size: 36rpx;
        margin-right: $spacing-md;
        width: 40rpx; // 对齐
        text-align: center;
      }
      
      .menu-text {
        font-size: 28rpx;
        color: $text-main;
      }
    }
    
    .menu-arrow, .menu-desc {
      font-size: 24rpx;
      color: $text-placeholder;
    }
  }
}
</style>
