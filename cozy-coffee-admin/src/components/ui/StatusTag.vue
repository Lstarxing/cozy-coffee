<template>
  <el-tag 
    :type="tagType" 
    effect="light" 
    class="status-tag"
  >
    {{ computedLabel }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    required: true
  },
  type: {
    type: String, // 'order' | 'redemption' | 'product' | 'common'
    default: 'common'
  }
})

// Mappings (use lowercase keys, normalize input)
const MAPPINGS = {
  // Coffee Shop Orders
  order: {
    pending: { label: '待处理', type: 'info' },
    paid: { label: '已支付', type: 'warning' },
    preparing: { label: '制作中', type: 'warning' },
    completed: { label: '已完成', type: 'success' },
    cancelled: { label: '已取消', type: 'info' }
  },
  // Redemptions
  redemption: {
    pending: { label: '待处理', type: 'info' },
    processing: { label: '处理中', type: 'warning' },
    shipped: { label: '已发货', type: 'primary' },
    completed: { label: '已完成', type: 'success' },
    cancelled: { label: '已取消', type: 'info' }
  },
  // Products
  product: {
    active: { label: '上架', type: 'success' },
    inactive: { label: '下架', type: 'info' },
    sold_out: { label: '售罄', type: 'warning' }
  },
  // Common Active/Disabled
  common: {
    active: { label: '启用', type: 'success' },
    disabled: { label: '禁用', type: 'danger' }
  }
}

const computedConfig = computed(() => {
  const map = MAPPINGS[props.type] || MAPPINGS.common
  const key = (props.status || '').toLowerCase()
  return map[key] || { label: props.status || '-', type: 'info' }
})

const tagType = computed(() => computedConfig.value.type)
const computedLabel = computed(() => computedConfig.value.label)
</script>

<style scoped>
.status-tag {
  border: none;
  font-weight: 500;
}
</style>
