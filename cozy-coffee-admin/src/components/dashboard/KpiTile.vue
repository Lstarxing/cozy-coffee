<template>
  <div class="kpi-tile" :class="{ clickable: !!to }" @click="handleClick">
    <div class="kpi-top">
      <div class="kpi-label">{{ label }}</div>
      <el-icon v-if="icon" class="kpi-icon">
        <component :is="icon" />
      </el-icon>
    </div>

    <div class="kpi-value">
      <span v-if="currency" class="kpi-currency">¥</span>
      <span class="kpi-number">{{ formattedValue }}</span>
    </div>

    <div class="kpi-footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  label: { type: String, default: '' },
  value: { type: [Number, String], default: 0 },
  currency: { type: Boolean, default: false },
  icon: { type: [Object, Function], default: null },
  to: { type: String, default: '' }
})

const router = useRouter()

const formattedValue = computed(() => {
  const v = props.value ?? 0
  // 你也可以在这里做千分位、保留两位小数等格式化
  return String(v)
})

const handleClick = () => {
  if (!props.to) return
  router.push(props.to)
}
</script>

<style scoped lang="scss">
.kpi-tile {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 16px 20px;

  /* 关键修复点：不要 height:100%（父级没高度会导致塌陷/不撑开） */
  /* height: 100%;  <-- 删除 */
  min-height: 120px; /* 给一个稳定高度，确保 row 一定撑开 */

  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.kpi-tile.clickable {
  cursor: pointer;
}

.kpi-tile.clickable:hover {
  border-color: #d1d5db;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
  transform: translateY(-1px);
}

.kpi-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kpi-label {
  font-size: 14px;
  color: #374151;
  line-height: 20px;
}

.kpi-icon {
  color: #9ca3af;
  font-size: 18px;
  flex: 0 0 auto;
}

.kpi-value {
  margin-top: 8px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.kpi-currency {
  font-size: 14px;
  color: #111827;
}

.kpi-number {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  letter-spacing: 0.2px;
}

.kpi-footer {
  margin-top: 10px;
  font-size: 12px;
  color: #9ca3af;
  line-height: 16px;
  min-height: 16px; /* footer 空时也保持布局稳定 */
}
</style>