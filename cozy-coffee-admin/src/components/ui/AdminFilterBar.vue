<template>
  <div class="admin-filter-bar">
    <div class="filter-inputs">
      <slot></slot>
    </div>
    <div class="filter-actions">
      <el-button type="primary" @click="$emit('search')">查询</el-button>
      <el-button @click="$emit('reset')">重置</el-button>
    </div>
  </div>
</template>

<script setup>
defineEmits(['search', 'reset'])
</script>

<style scoped lang="scss">
.admin-filter-bar {
  background: #fff;
  padding: 16px 20px;
  border-radius: 6px;
  border: 1px solid #E5E7EB;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;

  .filter-inputs {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    flex: 1;

    :deep(.el-form-item) {
      margin-bottom: 0;
      margin-right: 0;
    }
    
    // Make inputs consistent width
    :deep(.el-input), :deep(.el-select) {
      width: 200px;
    }
    
    :deep(.el-date-editor) {
      width: 240px;
    }
  }

  .filter-actions {
    display: flex;
    gap: 8px;
  }
}
</style>
