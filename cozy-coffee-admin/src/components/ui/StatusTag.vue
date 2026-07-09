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
import { ORDER_STATUS_MAP } from '@/constants/order'
import { REDEMPTION_STATUS_MAP } from '@/constants/redemption'
import { PRODUCT_STATUS_MAP } from '@/constants/product'

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

const MAPPINGS = {
  order: ORDER_STATUS_MAP,
  redemption: REDEMPTION_STATUS_MAP,
  product: PRODUCT_STATUS_MAP,
  common: {
    active: { label: '启用', tagType: 'success' },
    disabled: { label: '禁用', tagType: 'danger' }
  }
}

const computedConfig = computed(() => {
  const map = MAPPINGS[props.type] || MAPPINGS.common
  const key = (props.status || '').toLowerCase()
  return map[key] || { label: props.status || '-', tagType: 'info' }
})

const tagType = computed(() => computedConfig.value.tagType)
const computedLabel = computed(() => computedConfig.value.label)
</script>

<style scoped>
.status-tag {
  border: none;
  font-weight: 500;
}
</style>
