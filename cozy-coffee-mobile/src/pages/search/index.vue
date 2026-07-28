<!--
  搜索页 - 商品搜索
-->
<template>
  <view class="search-page">
    <!-- 搜索框 -->
    <view class="search-header">
      <view class="search-input-wrapper">
        <view class="search-icon" />
        <input 
          v-model="keyword" 
          class="search-input" 
          placeholder="搜索咖啡、茶饮"
          confirm-type="search"
          focus
          @confirm="doSearch"
        />
        <text class="clear-btn" v-if="keyword" @click="keyword = ''">×</text>
      </view>
      <text class="cancel-btn" @click="goBack">取消</text>
    </view>
    
    <!-- 搜索历史 -->
    <view class="history-section" v-if="!keyword && searchHistory.length > 0">
      <view class="section-header">
        <text class="section-title">搜索历史</text>
        <text class="section-action" @click="clearHistory">清空</text>
      </view>
      <view class="history-tags">
        <view 
          class="history-tag" 
          v-for="(item, index) in searchHistory" 
          :key="index"
          @click="keyword = item; doSearch()"
        >
          {{ item }}
        </view>
      </view>
    </view>
    
    <!-- 热门搜索 -->
    <view class="hot-section" v-if="!keyword">
      <view class="section-header">
        <text class="section-title cozy-display">热门搜索</text>
      </view>
      <view class="hot-list">
        <view 
          class="hot-item" 
          v-for="(item, index) in hotKeywords" 
          :key="index"
          @click="keyword = item; doSearch()"
        >
          <text class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</text>
          <text class="hot-text">{{ item }}</text>
        </view>
      </view>
    </view>
    
    <!-- 搜索结果 -->
    <view class="result-section" v-if="keyword && hasSearched">
      <view class="result-header">
        <text class="result-count">找到 {{ searchResults.length }} 个结果</text>
      </view>
      
      <view class="result-list" v-if="searchResults.length > 0">
        <view 
          class="product-card" 
          v-for="item in searchResults" 
          :key="item.id"
          @click="goToDetail(item.id)"
        >
          <image :src="item.image" class="product-image" mode="aspectFill" />
          <view class="product-info">
            <text class="product-name">{{ item.name }}</text>
            <text class="product-desc">{{ item.description }}</text>
            <text class="product-price">¥{{ item.price }}</text>
          </view>
        </view>
      </view>
      
      <!-- 无结果 -->
      <view class="empty-state" v-else>
        <text class="empty-icon">搜</text>
        <text class="empty-text">没有找到相关商品</text>
        <text class="empty-hint">换个关键词试试</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { searchCoffeeProducts } from '@/api/product'

const keyword = ref('')
const hasSearched = ref(false)
const searchResults = ref([])
const searchHistory = ref(['拿铁', '美式', '生椰'])
const hotKeywords = ref(['拿铁', '美式咖啡', '生椰拿铁', '卡布奇诺', '摩卡', '茉莉奶绿'])

// 执行搜索
const doSearch = async () => {
  if (!keyword.value.trim()) return
  hasSearched.value = true
  try {
    const res = await searchCoffeeProducts(keyword.value.trim())
    searchResults.value = (res.data || []).map(p => ({
      ...p,
      image: p.imageUrl || p.image || '/static/images/default-product.png'
    }))
  } catch (error) {
    searchResults.value = []
    uni.showToast({ title: error?.message || '搜索失败，请重试', icon: 'none' })
  }
  
  // 添加到历史记录
  if (!searchHistory.value.includes(keyword.value)) {
    searchHistory.value.unshift(keyword.value)
    if (searchHistory.value.length > 10) {
      searchHistory.value.pop()
    }
  }
}

// 清空历史
const clearHistory = () => {
  searchHistory.value = []
}

// 返回上一页
const goBack = () => {
  uni.navigateBack()
}

// 跳转详情
const goToDetail = (productId) => {
  uni.navigateTo({ url: `/pages/menu/detail?id=${productId}` })
}
</script>

<style lang="scss" scoped>
.search-page {
  min-height: 100vh;
  background: #fff;
}

// 搜索头部
.search-header {
  display: flex;
  align-items: center;
  padding: $spacing-md;
  background: $bg-white;
  position: sticky;
  top: 0;
  z-index: 10;
  
  .search-input-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    background: $bg-gray;
    border-radius: $cozy-radius-md;
    padding: $spacing-sm $spacing-md;
    
    .search-icon {
      position: relative;
      width: 22rpx;
      height: 22rpx;
      border: 3rpx solid $cozy-muted;
      border-radius: 50%;
      margin-right: $spacing-sm;

      &::after {
        position: absolute;
        right: -9rpx;
        bottom: -6rpx;
        width: 11rpx;
        height: 3rpx;
        border-radius: 2rpx;
        background: $cozy-muted;
        content: '';
        transform: rotate(45deg);
      }
    }
    
    .search-input {
      flex: 1;
      font-size: $font-size-md;
    }
    
    .clear-btn {
      font-size: 36rpx;
      color: $text-placeholder;
      padding: 0 $spacing-sm;
    }
  }
  
  .cancel-btn {
    margin-left: $spacing-md;
    color: $primary-color;
    font-size: $font-size-md;
  }
}

// 区块样式
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
  
  .section-title {
    font-size: 32rpx;
    font-weight: 600;
    color: $text-primary;
  }
  
  .section-action {
    font-size: $font-size-sm;
    color: $text-placeholder;
  }
}

// 搜索历史
.history-section {
  padding: 34rpx 28rpx;
  background: $bg-white;
  border-bottom: 1rpx solid $cozy-border;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  
  .history-tag {
    background: $bg-gray;
    padding: $spacing-xs $spacing-md;
    border-radius: 999rpx;
    font-size: $font-size-sm;
    color: $text-secondary;
  }
}

// 热门搜索
.hot-section {
  padding: 34rpx 28rpx;
  background: $bg-white;
}

.hot-list {
  .hot-item {
    display: flex;
    align-items: center;
    padding: $spacing-sm 0;
    
    .hot-rank {
      width: 40rpx;
      height: 40rpx;
      line-height: 40rpx;
      text-align: center;
      border-radius: 8rpx;
      background: $bg-gray;
      font-size: $font-size-sm;
      color: $text-secondary;
      margin-right: $spacing-md;
      
      &.top {
        background: $primary-color;
        color: white;
      }
    }
    
    .hot-text {
      font-size: $font-size-md;
      color: $text-primary;
    }
  }
}

// 搜索结果
.result-section {
  padding: $spacing-md;
}

.result-header {
  margin-bottom: $spacing-md;
  
  .result-count {
    font-size: $font-size-sm;
    color: $text-secondary;
  }
}

.product-card {
  display: flex;
  background: $bg-white;
  padding: 22rpx 0;
  border-bottom: 1rpx solid $cozy-border;
  
  .product-image {
    width: 160rpx;
    height: 160rpx;
    border-radius: $cozy-radius-sm;
  }
  
  .product-info {
    flex: 1;
    margin-left: $spacing-md;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    
    .product-name {
      font-size: $font-size-md;
      font-weight: 500;
      color: $text-primary;
    }
    
    .product-desc {
      font-size: $font-size-sm;
      color: $text-placeholder;
    }
    
    .product-price {
      font-size: $font-size-lg;
      font-weight: 600;
      color: $primary-color;
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
  
  .empty-icon {
    width: 88rpx;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: $cozy-surface;
    color: $cozy-primary;
    font-size: 28rpx;
    font-weight: 750;
    margin-bottom: $spacing-md;
  }
  
  .empty-text {
    font-size: $font-size-md;
    color: $text-secondary;
    margin-bottom: $spacing-xs;
  }
  
  .empty-hint {
    font-size: $font-size-sm;
    color: $text-placeholder;
  }
}
</style>
