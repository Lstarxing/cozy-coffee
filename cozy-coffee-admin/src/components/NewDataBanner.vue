<template>
  <transition name="slide-down">
    <div v-if="visible" class="new-data-banner">
      <el-icon class="banner-icon"><Bell /></el-icon>
      <span class="banner-text">{{ message }}</span>
      <el-button type="primary" size="small" @click="handleRefresh">
        刷新
      </el-button>
      <el-button text size="small" @click="handleDismiss" class="dismiss-btn">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>
  </transition>
</template>

<script setup>
import { Bell, Close } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  message: {
    type: String,
    default: '有新数据，点击刷新'
  }
})

const emit = defineEmits(['refresh', 'dismiss'])

const handleRefresh = () => {
  emit('refresh')
}

const handleDismiss = () => {
  emit('dismiss')
}
</script>

<style scoped>
.new-data-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #409eff15 0%, #67c23a15 100%);
  border: 1px solid #409eff40;
  border-radius: 8px;
  margin-bottom: 16px;
}

.banner-icon {
  color: #409eff;
  font-size: 18px;
}

.banner-text {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.dismiss-btn {
  color: #909399;
}

.dismiss-btn:hover {
  color: #606266;
}

/* 动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
