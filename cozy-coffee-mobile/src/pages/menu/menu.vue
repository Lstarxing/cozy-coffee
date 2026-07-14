<!--
  菜单页 - 商业化重构版
  特点：极简侧边栏、无边框商品卡片、流畅交互
-->
<template>
  <view class="menu-page">
    <!-- 顶部状态栏占位 -->
    <view class="status-bar-placeholder"></view>

    <view class="menu-container">
      <!-- 左侧分类导航 -->
      <scroll-view 
        class="category-sidebar" 
        scroll-y 
        :scroll-into-view="'cat-' + currentCategoryIndex"
      >
        <view 
          v-for="(cat, index) in categories" 
          :key="cat.id"
          :id="'cat-' + index"
          class="category-item"
          :class="{ active: currentCategoryIndex === index }"
          @click="onCategoryClick(index)"
        >
          <!-- 激活指示条 -->
          <view class="active-indicator" v-if="currentCategoryIndex === index"></view>
          <text class="category-name">{{ cat.name }}</text>
          <!-- 选购数量角标 (可选) -->
          <text class="category-badge" v-if="getCategoryCount(cat.id) > 0">{{ getCategoryCount(cat.id) }}</text>
        </view>
        <!-- 底部占位 -->
        <view style="height: 100rpx"></view>
      </scroll-view>
      
      <!-- 右侧商品列表 -->
      <scroll-view 
        class="product-list" 
        scroll-y
        :scroll-into-view="scrollToProductId"
        scroll-with-animation
        @scroll="onProductScroll"
      >
        <view class="product-wrapper">
          <!-- 顶部广告位 -->
          <view class="menu-banner">
            <text class="banner-title">今日特调 ☕</text>
            <text class="banner-subtitle">探索每一杯的惊喜</text>
          </view>

          <!-- 按分类分组 -->
          <view 
            v-for="(cat, catIndex) in categories" 
            :key="cat.id"
            :id="'product-cat-' + catIndex"
            class="product-group"
          >
            <!-- 分类标题 -->
            <view class="group-header">
              <text class="group-title">{{ cat.name }}</text>
            </view>
            
            <!-- 商品卡片 -->
            <view 
              v-for="product in cat.products" 
              :key="product.id"
              class="product-card"
              @click="goToDetail(product.id)"
            >
              <image :src="product.image" class="product-image" mode="aspectFill" />
              
              <view class="product-content">
                <text class="product-name">{{ product.name }}</text>
                <text class="product-desc ellipsis-2">{{ product.description }}</text>
                
                <view class="product-action">
                  <text class="product-price">¥{{ product.price }}</text>
                  <!-- 选规格/加购按钮 -->
                  <view class="action-btn" @click.stop="addToCart(product)">
                    <text class="btn-icon">+</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 底部占位，防止被购物车栏遮挡 -->
        <view class="bottom-placeholder"></view>
      </scroll-view>
    </view>
    
    <!-- 底部购物车栏 (悬浮) -->
    <view class="cart-bar-wrapper" v-if="cartStore.totalCount > 0">
      <view class="cart-bar">
        <view class="cart-left" @click="showCartPopup = true">
          <view class="cart-icon-box">
            <text class="cart-icon">🛒</text>
            <view class="cart-badge">{{ cartStore.totalCount }}</view>
          </view>
          <view class="cart-price">
            <text class="total-price">¥{{ cartStore.totalPrice }}</text>
            <text class="delivery-tip">免配送费</text>
          </view>
        </view>
        <view class="cart-right" @click="goToCheckout">
          <text class="checkout-text">去结算</text>
        </view>
      </view>
    </view>
    
    <!-- 购物车弹窗 (简化版) -->
    <view class="cart-popup-mask" v-if="showCartPopup" @click="showCartPopup = false">
      <view class="cart-popup-content" @click.stop>
        <view class="popup-header">
          <text class="popup-title">已选商品</text>
          <text class="clear-btn" @click="cartStore.clearCart">清空</text>
        </view>
        <scroll-view scroll-y class="cart-scroll">
          <view class="cart-item" v-for="item in cartStore.items" :key="item.id">
            <text class="item-name">{{ item.name }}</text>
            <view class="item-actions">
              <text class="item-price">¥{{ item.price }}</text>
              <view class="qty-control">
                <view class="qty-btn" @click="cartStore.decreaseQty(item.id)">-</view>
                <text class="qty-num">{{ item.quantity }}</text>
                <view class="qty-btn" @click="cartStore.increaseQty(item.id)">+</view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useCartStore } from '@/stores/cart'
import { getMenuData } from '@/api/product'

const cartStore = useCartStore()
const categories = ref([])
const currentCategoryIndex = ref(0)
const scrollToProductId = ref('')
const showCartPopup = ref(false)

onMounted(async () => {
  try {
    const res = await getMenuData()
    if (res.code === 200 && res.data) {
      // 后端返回平铺的商品列表，需要按 category 分组
      const products = res.data
      const categoryMap = {}
      
      products.forEach(product => {
        const cat = product.category || 'other'
        const catName = getCategoryDisplayName(cat)
        if (!categoryMap[cat]) {
          categoryMap[cat] = {
            id: cat,
            name: catName,
            products: []
          }
        }
        // 适配字段名：后端用 imageUrl，前端用 image
        categoryMap[cat].products.push({
          ...product,
          image: product.imageUrl || product.image || '/static/images/default-product.png'
        })
      })
      
      categories.value = Object.values(categoryMap)
    }
  } catch (e) {
    console.error('加载菜单失败', e)
    // 降级到 Mock 数据
    categories.value = [
      {
        id: 'latte',
        name: '拿铁系列',
        products: [
          { id: 1, name: '经典拿铁', price: 32, image: 'https://picsum.photos/200/200?random=1', description: '浓缩咖啡与丝滑牛奶的经典搭配' }
        ]
      }
    ]
  }
})

const onCategoryClick = (index) => {
  currentCategoryIndex.value = index
  scrollToProductId.value = 'product-cat-' + index
}

const onProductScroll = (e) => {
  // 简化联动逻辑
}

const getCategoryCount = (catId) => {
  // 简单模拟分类下已选数量，实际需遍历购物车
  return 0
}

// v5.0 分类名称映射
const getCategoryDisplayName = (code) => {
  const categoryMap = {
    'espresso': '☕ 意式咖啡',
    'signature': '⭐ 季节限定',
    'bakery': '🍰 烘焙甜品',
    'addon': '➕ 加料/配料',
    'coffee': '☕ 咖啡',
    'dessert': '🍰 甜品',
    'other': '📦 其他'
  }
  return categoryMap[code] || code
}

const addToCart = (product) => {
  cartStore.addItem({
    id: product.id,
    name: product.name,
    price: product.price,
    image: product.image
  })
  uni.vibrateShort() // 震动反馈
}

const goToDetail = (id) => uni.navigateTo({ url: `/pages/menu/detail?id=${id}` })
const goToCheckout = () => uni.navigateTo({ url: '/pages/order/confirm' })
</script>

<style lang="scss" scoped>
.menu-page {
  height: 100vh;
  background: $bg-white;
  display: flex;
  flex-direction: column;
}

.status-bar-placeholder {
  height: var(--status-bar-height);
  background: $bg-white;
}

.menu-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// 左侧分类
.category-sidebar {
  width: 180rpx;
  background: #F9F9F9; // 极淡灰
  height: 100%;
  
  .category-item {
    height: 100rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    color: $text-sub;
    font-size: $font-size-sm;
    transition: all 0.2s;
    
    &.active {
      background: $bg-white;
      color: $text-main;
      font-weight: 700;
      font-size: $font-size-md;
    }
    
    .active-indicator {
      position: absolute;
      left: 0;
      top: 30rpx;
      bottom: 30rpx;
      width: 8rpx;
      background: $primary-color;
      border-radius: 0 4rpx 4rpx 0;
    }
    
    .category-badge {
      position: absolute;
      top: 10rpx;
      right: 10rpx;
      background: $error-color;
      color: white;
      font-size: 18rpx;
      padding: 0 8rpx;
      border-radius: 10rpx;
    }
  }
}

// 右侧列表
.product-list {
  flex: 1;
  height: 100%;
  background: $bg-white;
}

.product-wrapper {
  padding: 0 $spacing-md;
}

.menu-banner {
  margin: $spacing-md 0;
  padding: $spacing-lg;
  background: $cozy-surface;
  border-radius: $card-radius;
  
  .banner-title {
    font-size: 32rpx;
    font-weight: 700;
    color: $primary-dark;
    display: block;
    margin-bottom: 4rpx;
  }
  
  .banner-subtitle {
    font-size: 24rpx;
    color: $text-sub;
  }
}

.group-header {
  padding: $spacing-lg 0 $spacing-sm;
  background: $bg-white;
  position: sticky;
  top: 0;
  z-index: 10;
  
  .group-title {
    font-size: $font-size-md;
    font-weight: 700;
    color: $text-main;
  }
}

.product-card {
  display: flex;
  margin-bottom: $spacing-lg;
  
  .product-image {
    width: 180rpx;
    height: 180rpx;
    border-radius: $card-radius;
    background: $bg-gray;
  }
  
  .product-content {
    flex: 1;
    margin-left: $spacing-md;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding: 4rpx 0;
    
    .product-name {
      font-size: 30rpx;
      color: $text-main;
      font-weight: 600;
    }
    
    .product-desc {
      font-size: 22rpx;
      color: $text-placeholder;
      line-height: 1.4;
    }
    
    .product-action {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .product-price {
        font-size: 32rpx;
        color: $secondary-color;
        font-weight: 700;
      }
      
      .action-btn {
        width: 50rpx;
        height: 50rpx;
        background: $primary-color;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        &:active { transform: scale(0.9); }
        
        .btn-icon {
          color: white;
          font-size: 36rpx;
          margin-top: -4rpx;
        }
      }
    }
  }
}

.bottom-placeholder {
  height: 160rpx;
}

// 购物车栏
.cart-bar-wrapper {
  position: fixed;
  bottom: 0; // 适配 tabbar 时需要调整，这里假设是 tabbar 页面
  /* #ifdef H5 */
  bottom: 50px; // H5 Tabbar 高度
  /* #endif */
  left: 0;
  right: 0;
  padding: 0 $spacing-md $spacing-md;
  z-index: 99;
}

.cart-bar {
  background: $secondary-color;
  border-radius: 50rpx;
  height: 100rpx;
  display: flex;
  align-items: center;
  padding: 0 8rpx 0 32rpx;

  .cart-left {
    flex: 1;
    display: flex;
    align-items: center;
    
    .cart-icon-box {
      position: relative;
      margin-right: $spacing-md;
      
      .cart-icon { font-size: 48rpx; }
      
      .cart-badge {
        position: absolute;
        top: -10rpx;
        right: -10rpx;
        background: $error-color;
        color: white;
        font-size: 20rpx;
        min-width: 32rpx;
        text-align: center;
        border-radius: 16rpx;
        border: 2rpx solid $secondary-color;
      }
    }
    
    .cart-price {
      .total-price { display: block; color: white; font-size: 36rpx; font-weight: 700; }
      .delivery-tip { display: block; color: rgba(255,255,255,0.6); font-size: 20rpx; }
    }
  }
  
  .cart-right {
    background: $primary-color;
    height: 84rpx;
    padding: 0 48rpx;
    border-radius: 42rpx;
    display: flex;
    align-items: center;
    
    .checkout-text {
      color: white;
      font-size: 30rpx;
      font-weight: 600;
    }
  }
}

// 购物车弹窗
.cart-popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 90;
}

.cart-popup-content {
  position: absolute;
  bottom: 140rpx; // 避开购物车栏
  /* #ifdef H5 */
  bottom: 240rpx;
  /* #endif */
  left: $spacing-md;
  right: $spacing-md;
  background: white;
  border-radius: $card-radius-lg;
  padding: $spacing-md;
  animation: slide-up 0.3s ease-out;
  
  .popup-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: $spacing-md;
    
    .popup-title { font-weight: 700; }
    .clear-btn { color: $text-sub; font-size: 24rpx; }
  }
  
  .cart-scroll {
    max-height: 400rpx;
  }
  
  .cart-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-sm 0;
    
    .item-name { flex: 1; font-size: 28rpx; }
    
    .item-actions {
      display: flex;
      align-items: center;
      gap: $spacing-md;
      
      .item-price { font-weight: 600; }
      
      .qty-control {
        display: flex;
        align-items: center;
        gap: 16rpx;
        
        .qty-btn {
          width: 40rpx;
          height: 40rpx;
          border-radius: 50%;
          border: 1rpx solid #ddd;
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }
    }
  }
}

@keyframes slide-up {
  from { transform: translateY(100%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>
