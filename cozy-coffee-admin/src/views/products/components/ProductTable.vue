<template>
  <el-card shadow="never" class="table-card">
    <TableToolbar :last-updated="lastUpdated" @refresh="$emit('refresh')" />

    <el-table
      v-loading="loading"
      :data="data"
      size="small"
      header-cell-class-name="table-header"
    >
      <!-- Product Info Column -->
      <slot name="productInfo" />
      <!-- Price Column -->
      <slot name="price" />
      <!-- Category Column -->
      <slot name="category" />
      <!-- Stock Column (optional) -->
      <slot name="stock" />
      <!-- Status Column -->
      <slot name="status" />
      <!-- Actions Column -->
      <slot name="actions" />
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
      />
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import TableToolbar from '@/components/ui/TableToolbar.vue'

const props = defineProps({
  loading: { type: Boolean, default: false },
  data: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  currentPage: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  lastUpdated: { type: String, default: '' }
})

const emit = defineEmits(['refresh', 'update:currentPage', 'update:pageSize'])

const currentPage = computed({
  get: () => props.currentPage,
  set: (val) => emit('update:currentPage', val)
})

const pageSize = computed({
  get: () => props.pageSize,
  set: (val) => emit('update:pageSize', val)
})
</script>

<style scoped lang="scss">
.table-card {
  border: 1px solid #E5E7EB;
  border-radius: 6px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
