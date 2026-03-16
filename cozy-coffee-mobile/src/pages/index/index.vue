<!--
  首页 - 商业化重构版
  特点：沉浸式导航、艺术轮播、悬浮会员卡、金刚区、横向滚动推荐
-->
<template>
  <view class="home-page">
    
    <!-- 顶部背景 (随滚动变色) -->
    <view class="nav-bg" :style="{ opacity: navOpacity }"></view>

    <!-- 沉浸式导航栏 -->
    <view class="custom-nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <!-- 搜索框 -->
        <view class="search-bar" @click="goToPage('/pages/search/index')">
          <text class="search-icon">🔍</text>
          <text class="placeholder">想喝点什么？</text>
        </view>
        <!-- 消息图标 -->
        <view class="nav-icon">🔔</view>
      </view>
    </view>
    
    <!-- 内容滚动区域 -->
    <scroll-view 
      scroll-y 
      class="content-scroll" 
      @scroll="onScroll"
    >
      <!-- 艺术轮播 -->
      <view class="banner-section">
        <swiper 
          class="banner-swiper" 
          autoplay 
          circular 
          interval="5000"
          @change="onBannerChange"
        >
          <swiper-item v-for="(banner, index) in (banners.length ? banners : defaultBanners)" :key="index">
            <image :src="banner.image" mode="aspectFill" class="banner-image" />
          </swiper-item>
        </swiper>
        <!-- 自定义指示器 -->
        <view class="custom-dots">
          <view 
            class="dot" 
            v-for="(banner, index) in banners" 
            :key="index"
            :class="{ active: currentBanner === index }"
          ></view>
        </view>
      </view>

      <!-- 悬浮会员卡 (Overlapping Effect) -->
      <view class="member-card-wrapper" @click="handleMemberClick">
        <view class="member-card" :class="userLevel">
          <view class="card-left">
            <view class="level-badge">
              <text class="level-icon">👑</text>
              <text class="level-name">{{ getLevelName(userLevel) }}</text>
            </view>
            <text class="points-text">当前积分 <text class="points-num">{{ currentPoints }}</text></text>
            <!-- 进度条 -->
            <view class="level-progress">
              <view class="progress-bar">
                <view class="progress-fill" :style="{ width: expPercent + '%' }"></view>
              </view>
              <text class="progress-hint">再消费 {{ nextLevelExp }}EXP 升级</text>
            </view>
          </view>
          <view class="card-right">
            <view class="signin-btn" @click.stop="goToPage('/pages/signin/index')">
              <text>📅 签到</text>
            </view>
            <text class="benefits-link">会员权益 ></text>
          </view>
          <!-- 背景纹理 -->
          <view class="card-texture"></view>
        </view>
      </view>

      <!-- 金刚区 (Grid Menu) -->
      <view class="grid-menu">
        <view class="grid-item" @click="switchToTab('/pages/menu/menu')">
          <view class="icon-box primary">☕</view>
          <text class="grid-text">现在点单</text>
        </view>
        <view class="grid-item" @click="goToPage('/pages/mall/index')">
          <view class="icon-box">🎁</view>
          <text class="grid-text">积分商城</text>
        </view>
        <view class="grid-item" @click="goToPage('/pages/coupon/list')">
          <view class="icon-box">🎫</view>
          <text class="grid-text">我的券包</text>
        </view>
        <view class="grid-item" @click="goToPage('/pages/store/list')">
          <view class="icon-box">🏪</view>
          <text class="grid-text">附近门店</text>
        </view>
      </view>

      <!-- 新品推荐 (Horizontal Scroll) -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">本季新品 🔥</text>
          <text class="section-more" @click="switchToTab('/pages/menu/menu')">全部 ></text>
        </view>
        <scroll-view scroll-x class="horizontal-scroll" show-scrollbar="false">
          <view class="scroll-inner">
            <view 
              class="polaroid-card" 
              v-for="(item, index) in recommendProducts" 
              :key="index"
              @click="goToDetail(item.id)"
            >
              <image :src="item.image" mode="aspectFill" class="card-image" />
              <view class="card-info">
                <text class="card-name">{{ item.name }}</text>
                <view class="card-bottom">
                  <text class="card-price">¥{{ item.price }}</text>
                  <view class="add-btn">+</view>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 营销大图 -->
      <view class="promo-banner" @click="switchToTab('/pages/menu/menu')">
        <image src="/static/images/promo.png" mode="aspectFill" class="promo-image" />
        <view class="promo-overlay">
          <text class="promo-title">下午茶套餐</text>
          <text class="promo-desc">拿铁 + 提拉米苏 立减 ¥8</text>
        </view>
      </view>
      
      <!-- 底部留白 -->
      <view class="bottom-spacer"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getRecommendProducts, getBanners } from '@/api/product'

const userStore = useUserStore()

// 状态栏高度适配
const statusBarHeight = ref(20)
// 导航背根据滚动渐变
const navOpacity = ref(0)
// 当前轮播索引
const currentBanner = ref(0)

// 用户数据
const userLevel = ref(userStore.userLevel || 'silver')
const currentPoints = ref(userStore.memberInfo?.currentPoints || 0)
const expPercent = ref(65) // 模拟
const nextLevelExp = ref(350) // 模拟

const banners = ref([])
const defaultBanners = ref([
  { image: '/static/images/banner1.png' },
  { image: '/static/images/banner2.png' },
  { image: '/static/images/banner3.png' }
])
const recommendProducts = ref([])

onMounted(async () => {
  // 获取状态栏高度
  const info = uni.getSystemInfoSync()
  statusBarHeight.value = info.statusBarHeight || 20

  // 加载推荐商品
  try {
    const res = await getRecommendProducts()
    if (res.code === 200 && res.data) {
      // 适配字段名：后端用 imageUrl，前端用 image
      // 只取前6个作为推荐
      recommendProducts.value = res.data.slice(0, 6).map(item => ({
        ...item,
        image: item.imageUrl || item.image || '/static/images/default-product.png'
      }))
    }
  } catch (e) {
    console.error('Failed to load recommend products', e)
  }
  
  // 加载 Banner（目前后端没有专门接口，使用默认数据）
  try {
    const bannerRes = await getBanners()
    if (bannerRes.code === 200 && bannerRes.data && bannerRes.data.length > 0) {
      banners.value = bannerRes.data
    }
  } catch (e) {
    console.error('Failed to load banners', e)
    // 使用默认 Banner，模板中已有 fallback 逻辑
  }
})

// 滚动监听
const onScroll = (e) => {
  const scrollTop = e.detail.scrollTop
  // 滚动 100px 内渐变
  navOpacity.value = Math.min(scrollTop / 100, 1)
}

const onBannerChange = (e) => {
  currentBanner.value = e.detail.current
}

// 辅助函数
const getLevelName = (level) => {
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', diamond: '钻石会员', black: '黑金会员' }
  return map[level] || '注册会员'
}

// 跳转逻辑
const goToPage = (url) => uni.navigateTo({ url })
const switchToTab = (url) => uni.switchTab({ url })
const goToDetail = (id) => uni.navigateTo({ url: `/pages/menu/detail?id=${id}` })
const handleMemberClick = () => uni.navigateTo({ url: '/pages/benefits/index' })
</script>

<style lang="scss" scoped>
.home-page {
  height: 100vh;
  background: $bg-color;
  position: relative;
  overflow: hidden;
}

// 导航背景遮罩
.nav-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: calc(var(--status-bar-height) + 88rpx);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20rpx);
  z-index: 99;
  pointer-events: none;
}

// 沉浸式导航
.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  
  .nav-content {
    height: 44px;
    display: flex;
    align-items: center;
    padding: 0 $spacing-md;
    
    .search-bar {
      flex: 1;
      height: 64rpx;
      background: rgba(255, 255, 255, 0.6); // 半透明背景
      border-radius: 32rpx;
      display: flex;
      align-items: center;
      padding: 0 $spacing-md;
      border: 1rpx solid rgba(0,0,0,0.05);
      
      .search-icon {
        font-size: 28rpx;
        margin-right: $spacing-xs;
      }
      
      .placeholder {
        font-size: $font-size-sm;
        color: $text-secondary; // 深一些以便在浅色背景可见
      }
    }
    
    .nav-icon {
      margin-left: $spacing-md;
      font-size: 40rpx;
    }
  }
}

.content-scroll {
  height: 100%;
}

// 艺术轮播
.banner-section {
  position: relative;
  width: 100%;
  height: 500rpx;
  
  .banner-swiper {
    width: 100%;
    height: 100%;
    
    .banner-image {
      width: 100%;
      height: 100%;
      background: #E0E0E0;
    }
  }
  
  // 自定义指示器
  .custom-dots {
    position: absolute;
    bottom: 60rpx; // 向上移，为了给悬浮卡片留空间
    left: $spacing-lg;
    display: flex;
    gap: 8rpx;
    
    .dot {
      width: 12rpx;
      height: 12rpx;
      background: rgba(255, 255, 255, 0.5);
      border-radius: 6rpx;
      transition: all 0.3s;
      
      &.active {
        width: 32rpx;
        background: #FFFFFF;
      }
    }
  }
}

// 悬浮会员卡
.member-card-wrapper {
  margin: -40rpx $spacing-md 0;
  position: relative;
  z-index: 10;
}

.member-card {
  height: 180rpx;
  background: white;
  border-radius: $card-radius-lg;
  box-shadow: $box-shadow;
  display: flex;
  justify-content: space-between;
  padding: $spacing-lg;
  position: relative;
  overflow: hidden;
  color: #333; // 默认深色字
  
  // 等级皮肤
  &.basic { 
    background: linear-gradient(135deg, #FFFFFF, #F5F5F7); 
    color: #555; 
  }
  &.silver { 
    background: linear-gradient(135deg, #E6E9F0, #c1c8d3); 
    color: #2C3E50; 
    .progress-bar { background: rgba(44, 62, 80, 0.2); }
  }
  &.gold { 
    background: linear-gradient(135deg, #FFE5B4, #D4AF37); 
    color: #5A3A00; 
    .progress-bar { background: rgba(90, 58, 0, 0.2); }
  }
  &.diamond { 
    background: linear-gradient(135deg, #E0F7FA, #4DD0E1); 
    color: #006064; 
    .progress-bar { background: rgba(0, 96, 100, 0.2); }
  }
  &.black { 
    background: linear-gradient(135deg, #333333, #000000); 
    color: #D4AF37; 
    .progress-bar { background: rgba(212, 175, 55, 0.3); }
    .signin-btn { color: #333; }
  }

  .card-left {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    z-index: 2;
    
    .level-badge {
      display: flex;
      align-items: center;
      
      .level-icon { margin-right: $spacing-xs; font-size: 32rpx; }
      .level-name { font-weight: 700; font-size: $font-size-lg; }
    }
    
    .points-text {
      font-size: $font-size-sm;
      opacity: 0.8;
      
      .points-num {
        font-size: $font-size-xl;
        font-weight: 700;
        margin-left: 4rpx;
      }
    }
  }
  
  .card-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    justify-content: space-between;
    z-index: 2;
    
    .signin-btn {
      background: rgba(255,255,255,0.9);
      padding: 8rpx 20rpx;
      border-radius: 30rpx;
      font-size: $font-size-sm;
      color: $text-main;
      box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.1);
    }
    
    .benefits-link {
      font-size: $font-size-xs;
      opacity: 0.7;
    }
  }
  
  .level-progress {
    margin-top: $spacing-xs;
    
    .progress-bar {
      width: 240rpx;
      height: 6rpx;
      background: rgba(0,0,0,0.1);
      border-radius: 3rpx;
      overflow: hidden;
      margin-bottom: 4rpx;
      
      .progress-fill {
        height: 100%;
        background: currentColor; // 使用当前文字颜色
        opacity: 0.8;
      }
    }
    
    .progress-hint {
      font-size: 18rpx;
      opacity: 0.6;
    }
  }
  
  // 纹理装饰
  .card-texture {
    position: absolute;
    right: -20rpx;
    bottom: -20rpx;
    width: 200rpx;
    height: 200rpx;
    border-radius: 50%;
    background: rgba(255,255,255,0.1);
    pointer-events: none;
  }
}

// 金刚区
.grid-menu {
  display: flex;
  justify-content: space-between;
  padding: $spacing-lg $spacing-md;
  
  .grid-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .icon-box {
      width: 96rpx;
      height: 96rpx;
      background: $bg-white;
      border-radius: 32rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 48rpx;
      box-shadow: $box-shadow;
      margin-bottom: $spacing-sm;
      transition: transform 0.1s;
      
      &:active { transform: scale(0.95); }
      
      &.primary {
        background: $primary-color;
        color: white;
        box-shadow: 0 8rpx 20rpx rgba(198, 156, 109, 0.4);
      }
    }
    
    .grid-text {
      font-size: $font-size-sm;
      color: $text-main;
      font-weight: 500;
    }
  }
}

// 推荐区块
.section {
  padding: $spacing-md 0 $spacing-lg;
  
  .section-header {
    padding: 0 $spacing-md $spacing-md;
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    
    .section-title {
      font-size: 34rpx;
      font-weight: 700;
      color: $secondary-color;
    }
    
    .section-more {
      font-size: $font-size-sm;
      color: $text-sub;
    }
  }
}

.horizontal-scroll {
  width: 100%;
  white-space: nowrap;
}

.scroll-inner {
  padding: 0 $spacing-md;
  display: flex;
  gap: $spacing-md;
}

.polaroid-card {
  flex-shrink: 0;
  width: 260rpx;
  background: $bg-white;
  border-radius: $card-radius;
  overflow: hidden;
  box-shadow: $box-shadow;
  
  .card-image {
    width: 260rpx;
    height: 260rpx;
  }
  
  .card-info {
    padding: $spacing-sm;
    
    .card-name {
      font-size: $font-size-md;
      color: $text-main;
      font-weight: 600;
      display: block;
      margin-bottom: $spacing-xs;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    
    .card-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .card-price {
        font-size: $font-size-md;
        font-weight: 700;
        color: $secondary-color;
      }
      
      .add-btn {
        width: 44rpx;
        height: 44rpx;
        background: $primary-color;
        color: white;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32rpx;
      }
    }
  }
}

// 营销大图
.promo-banner {
  margin: $spacing-md;
  height: 240rpx;
  border-radius: $card-radius-lg;
  overflow: hidden;
  position: relative;
  box-shadow: $box-shadow;
  
  .promo-image {
    width: 100%;
    height: 100%;
  }
  
  .promo-overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    padding: $spacing-md;
    background: linear-gradient(to top, rgba(0,0,0,0.6), transparent);
    
    .promo-title {
      color: white;
      font-size: $font-size-lg;
      font-weight: 700;
      display: block;
    }
    
    .promo-desc {
      color: rgba(255,255,255,0.9);
      font-size: $font-size-sm;
    }
  }
}

.bottom-spacer {
  height: 120rpx;
}
</style>
