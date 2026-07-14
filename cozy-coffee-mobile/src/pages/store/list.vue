<!--
  门店列表页 - 附近门店
-->
<template>
  <view class="store-page">
    <!-- 搜索定位 -->
    <view class="location-bar">
      <view class="location-info" @click="getLocation">
        <text class="location-icon">📍</text>
        <text class="location-text">{{ currentLocation || '正在定位...' }}</text>
      </view>
      <text class="refresh-btn" @click="getLocation">刷新</text>
    </view>
    
    <!-- 门店列表 -->
    <view class="store-list">
      <view 
        class="store-card" 
        v-for="store in stores" 
        :key="store.id"
        @click="selectStore(store)"
      >
        <image :src="store.image" class="store-image" mode="aspectFill" />
        <view class="store-info">
          <view class="store-header">
            <text class="store-name">{{ store.name }}</text>
            <text class="store-distance">{{ store.distance }}</text>
          </view>
          <text class="store-address">{{ store.address }}</text>
          <view class="store-footer">
            <text class="store-time">🕐 {{ store.businessHours }}</text>
            <view class="store-status" :class="{ open: store.isOpen }">
              {{ store.isOpen ? '营业中' : '已打烊' }}
            </view>
          </view>
          <view class="store-tags">
            <text class="tag" v-for="tag in store.tags" :key="tag">{{ tag }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 空状态 -->
    <view class="empty-state" v-if="stores.length === 0">
      <text class="empty-icon">🏪</text>
      <text class="empty-text">附近暂无门店</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const currentLocation = ref('')
const stores = ref([])

onMounted(() => {
  getLocation()
  loadStores()
})

// 获取定位
const getLocation = () => {
  // 模拟定位
  currentLocation.value = '北京市海淀区中关村'
  
  // 真实环境使用 uni.getLocation
  // #ifdef MP-WEIXIN
  // uni.getLocation({
  //   type: 'gcj02',
  //   success: (res) => {
  //     currentLocation.value = `${res.latitude}, ${res.longitude}`
  //   }
  // })
  // #endif
}

// 加载门店列表
const loadStores = () => {
  stores.value = [
    {
      id: 1,
      name: 'CozyCoffee 中关村店',
      address: '北京市海淀区中关村大街1号鼎好大厦B1层',
      distance: '500m',
      businessHours: '08:00-22:00',
      isOpen: true,
      image: 'https://picsum.photos/seed/store1/200/200',
      tags: ['可自提', '可外卖', '有座位']
    },
    {
      id: 2,
      name: 'CozyCoffee 五道口店',
      address: '北京市海淀区成府路28号优盛大厦1层',
      distance: '1.2km',
      businessHours: '07:30-21:30',
      isOpen: true,
      image: 'https://picsum.photos/seed/store2/200/200',
      tags: ['可自提', '可外卖']
    },
    {
      id: 3,
      name: 'CozyCoffee 知春路店',
      address: '北京市海淀区知春路甲48号',
      distance: '2.5km',
      businessHours: '08:00-20:00',
      isOpen: false,
      image: 'https://picsum.photos/seed/store3/200/200',
      tags: ['可自提']
    }
  ]
}

// 选择门店
const selectStore = (store) => {
  if (!store.isOpen) {
    uni.showToast({ title: '该门店已打烊', icon: 'none' })
    return
  }
  
  // 可以保存选中的门店，或跳转到点单页
  uni.showModal({
    title: store.name,
    content: `地址：${store.address}\n营业时间：${store.businessHours}`,
    confirmText: '去点单',
    success: (res) => {
      if (res.confirm) {
        uni.switchTab({ url: '/pages/menu/menu' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.store-page {
  min-height: 100vh;
  background: $bg-color;
}

// 定位栏
.location-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md;
  background: $bg-white;
  position: sticky;
  top: 0;
  z-index: 10;
  
  .location-info {
    display: flex;
    align-items: center;
    
    .location-icon {
      font-size: 36rpx;
      margin-right: $spacing-sm;
    }
    
    .location-text {
      font-size: $font-size-md;
      color: $text-primary;
    }
  }
  
  .refresh-btn {
    font-size: $font-size-sm;
    color: $primary-color;
  }
}

// 门店列表
.store-list {
  padding: $spacing-md;
}

.store-card {
  display: flex;
  background: $bg-white;
  border-radius: $border-radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
  box-shadow: none;
  
  .store-image {
    width: 160rpx;
    height: 160rpx;
    border-radius: $border-radius-sm;
    flex-shrink: 0;
  }
  
  .store-info {
    flex: 1;
    margin-left: $spacing-md;
    
    .store-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: $spacing-xs;
      
      .store-name {
        font-size: $font-size-md;
        font-weight: 600;
        color: $text-primary;
      }
      
      .store-distance {
        font-size: $font-size-sm;
        color: $primary-color;
      }
    }
    
    .store-address {
      font-size: $font-size-sm;
      color: $text-secondary;
      display: block;
      margin-bottom: $spacing-xs;
    }
    
    .store-footer {
      display: flex;
      align-items: center;
      margin-bottom: $spacing-xs;
      
      .store-time {
        font-size: $font-size-xs;
        color: $text-placeholder;
      }
      
      .store-status {
        margin-left: $spacing-md;
        font-size: $font-size-xs;
        padding: 2rpx 12rpx;
        border-radius: 4rpx;
        background: #eee;
        color: $text-secondary;
        
        &.open {
          background: rgba($success-color, 0.1);
          color: $success-color;
        }
      }
    }
    
    .store-tags {
      display: flex;
      gap: $spacing-xs;
      
      .tag {
        font-size: 20rpx;
        color: $primary-color;
        background: rgba($primary-color, 0.1);
        padding: 4rpx 12rpx;
        border-radius: 4rpx;
      }
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 0;
  
  .empty-icon {
    font-size: 120rpx;
    margin-bottom: $spacing-md;
  }
  
  .empty-text {
    color: $text-placeholder;
  }
}
</style>
