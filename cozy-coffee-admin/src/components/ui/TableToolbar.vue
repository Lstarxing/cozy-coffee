<template>
  <div class="table-toolbar">
    <div class="toolbar-left">
      <slot></slot>
    </div>
    <div class="toolbar-right">
      <span v-if="lastUpdated" class="last-updated">最后更新: {{ lastUpdated }}</span>
      <el-tooltip content="刷新列表" placement="top">
        <el-button circle size="small" @click="$emit('refresh')">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip content="密度设置" placement="top" v-if="showDensity">
         <el-dropdown trigger="click" @command="handleDensity">
          <el-button circle size="small">
            <el-icon><Operation /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="default">默认</el-dropdown-item>
              <el-dropdown-item command="small">紧凑</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { Refresh, Operation } from '@element-plus/icons-vue'

defineProps({
  lastUpdated: {
    type: String,
    default: ''
  },
  showDensity: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['refresh', 'density-change'])

const handleDensity = (size) => {
  emit('density-change', size)
}
</script>

<style scoped lang="scss">
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
  padding: 0 4px;

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .last-updated {
    font-size: 12px;
    color: #9CA3AF;
  }
}
</style>
