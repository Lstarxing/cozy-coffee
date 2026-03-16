<template>
  <span class="copy-text" :class="{ 'icon-only': iconOnly }" @click.stop="handleCopy">
    <span v-if="!iconOnly" class="text-content">{{ text }}</span>
    <el-icon class="copy-icon"><CopyDocument /></el-icon>
  </span>
</template>

<script setup>
import { CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  text: {
    type: [String, Number],
    required: true
  },
  iconOnly: {
    type: Boolean,
    default: false
  }
})

const handleCopy = async () => {
  if (!props.text) return
  try {
    await navigator.clipboard.writeText(String(props.text))
    ElMessage.success('复制成功')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped lang="scss">
.copy-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  
  .text-content {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 13px;
  }

  .copy-icon {
    font-size: 12px;
    color: #9CA3AF;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover {
    .text-content {
      color: var(--el-color-primary);
    }
    .copy-icon {
      opacity: 1;
      color: var(--el-color-primary);
    }
  }
  
  // iconOnly 模式：图标始终可见
  &.icon-only {
    .copy-icon {
      opacity: 0.6;
    }
    &:hover .copy-icon {
      opacity: 1;
    }
  }
}
</style>
