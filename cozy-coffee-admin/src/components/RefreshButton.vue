<template>
  <div class="refresh-control">
    <el-button 
      :icon="Refresh" 
      :loading="loading" 
      @click="handleRefresh"
      circle
      :title="lastUpdateText"
    />
    <span v-if="showTime && lastUpdateTime" class="last-update-time">
      {{ lastUpdateText }}
    </span>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  showTime: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['refresh'])

const lastUpdateTime = ref(null)

const lastUpdateText = computed(() => {
  if (!lastUpdateTime.value) return '点击刷新'
  const time = new Date(lastUpdateTime.value)
  return `最后更新: ${time.toLocaleTimeString()}`
})

// 监听 loading 变化，结束时更新时间
watch(() => props.loading, (newVal, oldVal) => {
  if (oldVal === true && newVal === false) {
    lastUpdateTime.value = Date.now()
  }
})

const handleRefresh = () => {
  emit('refresh')
}

// 组件挂载时设置初始时间
lastUpdateTime.value = Date.now()
</script>

<style scoped>
.refresh-control {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.last-update-time {
  font-size: 12px;
  color: #909399;
}
</style>
