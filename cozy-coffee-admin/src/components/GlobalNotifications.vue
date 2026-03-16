<template>
  <teleport to="body">
    <!-- 新订单通知 -->
    <transition-group name="notification" tag="div" class="notification-container">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        :class="['notification', `notification-${notification.type}`]"
        @click="handleNotificationClick(notification)"
      >
        <div class="notification-icon">
          <el-icon :size="24">
            <Bell v-if="notification.type === 'order'" />
            <ShoppingBag v-if="notification.type === 'redemption'" />
            <Warning v-if="notification.type === 'warning'" />
          </el-icon>
        </div>
        <div class="notification-content">
          <div class="notification-title">{{ notification.title }}</div>
          <div class="notification-message">{{ notification.message }}</div>
          <div class="notification-time">{{ notification.time }}</div>
        </div>
        <div class="notification-close" @click.stop="removeNotification(notification.id)">
          <el-icon><Close /></el-icon>
        </div>
      </div>
    </transition-group>
  </teleport>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Bell, ShoppingBag, Warning, Close } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import sseService from '../api/sse'

const router = useRouter()
const notifications = ref([])
let notificationId = 0

// 音效
const playNotificationSound = () => {
  const audio = new Audio('/notification.mp3')
  audio.volume = 0.5
  audio.play().catch(err => console.log('播放音效失败:', err))
}

// 添加通知
const addNotification = (notification) => {
  const id = ++notificationId
  notifications.value.push({
    id,
    ...notification,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  })

  // 播放提示音
 // playNotificationSound()

  // 5秒后自动移除
  setTimeout(() => {
    removeNotification(id)
  }, 5000)
}

// 移除通知
const removeNotification = (id) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

// 点击通知
const handleNotificationClick = (notification) => {
  if (notification.type === 'order') {
    router.push('/orders')
  } else if (notification.type === 'redemption') {
    router.push('/redemptions')
  }
  removeNotification(notification.id)
}

// SSE 事件处理
let unsubscribeNewOrder
let unsubscribeNewRedemption

onMounted(() => {
  // 监听新订单事件
  unsubscribeNewOrder = sseService.on('new_order', (data) => {
    console.log('[通知] 收到新订单', data)
    addNotification({
      type: 'order',
      title: '🎉 新咖啡订单',
      message: `订单号: ${data.data || data.entityId}`,
      entityId: data.entityId
    })
  })

  // 监听新兑换订单事件
  unsubscribeNewRedemption = sseService.on('new_redemption', (data) => {
    console.log('[通知] 收到新兑换订单', data)
    addNotification({
      type: 'redemption',
      title: '🎁 新积分兑换',
      message: `订单ID: ${data.entityId}`,
      entityId: data.entityId
    })
  })
})

onUnmounted(() => {
  if (unsubscribeNewOrder) unsubscribeNewOrder()
  if (unsubscribeNewRedemption) unsubscribeNewRedemption()
})
</script>

<style scoped>
.notification-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;
}

.notification {
  min-width: 320px;
  max-width: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  padding: 16px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  pointer-events: all;
  transition: all 0.3s ease;
  border-left: 4px solid;
}

.notification:hover {
  transform: translateX(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
}

.notification-order {
  border-left-color: #67c23a;
}

.notification-redemption {
  border-left-color: #409eff;
}

.notification-warning {
  border-left-color: #e6a23c;
}

.notification-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.notification-order .notification-icon {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.notification-redemption .notification-icon {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.notification-message {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-close {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #909399;
  transition: all 0.2s;
}

.notification-close:hover {
  background: #f5f7fa;
  color: #606266;
}

/* 动画 */
.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s ease;
}

.notification-enter-from {
  opacity: 0;
  transform: translateX(100px);
}

.notification-leave-to {
  opacity: 0;
  transform: translateX(50px) scale(0.95);
}

.notification-move {
  transition: transform 0.3s ease;
}
</style>
