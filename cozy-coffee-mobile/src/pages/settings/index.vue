<!--
  设置页 - 个人设置
-->
<template>
  <view class="settings-page">
    <!-- 账号安全 -->
    <view class="settings-section">
      <view class="section-title">账号安全</view>
      <view class="settings-item" @click="goToPage('/pages/login/index')">
        <text class="item-label">绑定手机</text>
        <view class="item-right">
          <text class="item-value">{{ userPhone || '未绑定' }}</text>
          <text class="item-arrow">></text>
        </view>
      </view>
      <view class="settings-item">
        <text class="item-label">修改密码</text>
        <view class="item-right">
          <text class="item-arrow">></text>
        </view>
      </view>
    </view>
    
    <!-- 通知设置 -->
    <view class="settings-section">
      <view class="section-title">通知设置</view>
      <view class="settings-item">
        <text class="item-label">接收订单通知</text>
        <switch :checked="notifyOrder" color="#C69C6D" @change="notifyOrder = $event.detail.value" />
      </view>
      <view class="settings-item">
        <text class="item-label">接收活动通知</text>
        <switch :checked="notifyActivity" color="#C69C6D" @change="notifyActivity = $event.detail.value" />
      </view>
      <view class="settings-item">
        <text class="item-label">积分变动提醒</text>
        <switch :checked="notifyPoints" color="#C69C6D" @change="notifyPoints = $event.detail.value" />
      </view>
    </view>
    
    <!-- 通用设置 -->
    <view class="settings-section">
      <view class="section-title">通用</view>
      <view class="settings-item">
        <text class="item-label">清除缓存</text>
        <view class="item-right" @click="clearCache">
          <text class="item-value">{{ cacheSize }}</text>
          <text class="item-arrow">></text>
        </view>
      </view>
      <view class="settings-item">
        <text class="item-label">当前版本</text>
        <view class="item-right">
          <text class="item-value">v1.0.0</text>
        </view>
      </view>
    </view>
    
    <!-- 关于 -->
    <view class="settings-section">
      <view class="section-title">关于</view>
      <view class="settings-item">
        <text class="item-label">用户协议</text>
        <view class="item-right">
          <text class="item-arrow">></text>
        </view>
      </view>
      <view class="settings-item">
        <text class="item-label">隐私政策</text>
        <view class="item-right">
          <text class="item-arrow">></text>
        </view>
      </view>
      <view class="settings-item">
        <text class="item-label">意见反馈</text>
        <view class="item-right">
          <text class="item-arrow">></text>
        </view>
      </view>
    </view>
    
    <!-- 退出登录 -->
    <view class="logout-section" v-if="isLoggedIn">
      <view class="logout-btn" @click="handleLogout">退出登录</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userPhone = computed(() => userStore.userInfo.phone)

// 通知设置
const notifyOrder = ref(true)
const notifyActivity = ref(true)
const notifyPoints = ref(true)

// 缓存大小
const cacheSize = ref('12.5MB')

// 清除缓存
const clearCache = () => {
  uni.showModal({
    title: '清除缓存',
    content: '确定要清除缓存吗？',
    success: (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '清除中...' })
        setTimeout(() => {
          uni.hideLoading()
          cacheSize.value = '0MB'
          uni.showToast({ title: '清除成功', icon: 'success' })
        }, 1000)
      }
    }
  })
}

// 退出登录
const handleLogout = () => {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.showToast({ title: '已退出登录', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 1000)
      }
    }
  })
}

// 页面跳转
const goToPage = (url) => {
  uni.navigateTo({ url })
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: $spacing-xl;
}

// 设置区块
.settings-section {
  background: $bg-white;
  margin-bottom: $spacing-sm;
}

.section-title {
  font-size: $font-size-sm;
  color: $text-placeholder;
  padding: $spacing-md $spacing-md $spacing-xs;
}

.settings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md;
  border-bottom: 1rpx solid $border-color;
  
  &:last-child {
    border-bottom: none;
  }
  
  .item-label {
    font-size: $font-size-md;
    color: $text-primary;
  }
  
  .item-right {
    display: flex;
    align-items: center;
    
    .item-value {
      font-size: $font-size-md;
      color: $text-secondary;
      margin-right: $spacing-sm;
    }
    
    .item-arrow {
      font-size: $font-size-md;
      color: $text-placeholder;
    }
  }
}

// 退出登录
.logout-section {
  padding: $spacing-xl $spacing-md;
  
  .logout-btn {
    text-align: center;
    padding: $spacing-md;
    background: $bg-white;
    color: $error-color;
    font-size: $font-size-md;
    border-radius: $border-radius-md;
  }
}
</style>
