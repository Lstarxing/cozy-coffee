<template>
  <div class="chart-card">
    <div class="card-header">
      <div class="title">{{ title }}</div>
      <div class="actions">
        <slot name="actions"></slot>
      </div>
    </div>

    <!-- 关键：body 使用固定高度，内部 flex 让图表填满 -->
    <div
      class="card-body"
      :style="{ height: computedBodyHeight }"
      v-loading="loading"
    >
      <div v-if="!loading && empty" class="empty-state">
        <el-empty description="暂无数据" :image-size="60" />
      </div>

      <div v-else class="chart-content">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: String,
  loading: Boolean,
  empty: Boolean,
  /**
   * 图表内容区域高度（不含 header）
   * - 趋势图：360
   * - 饼图/分布：320
   * - 排行榜：360
   */
  bodyHeight: {
    type: [Number, String],
    default: 320
  }
})

const computedBodyHeight = computed(() => {
  return typeof props.bodyHeight === 'number' ? `${props.bodyHeight}px` : props.bodyHeight
})
</script>

<style scoped lang="scss">
.chart-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  
  /* 关键：隔离层叠上下文，防止 echarts canvas 覆盖外部区域 */
  position: relative;
  isolation: isolate;
  z-index: 0;
  
  /* 确保盒模型正确 */
  box-sizing: border-box;

  .card-header {
    padding: 12px 16px;
    border-bottom: 1px solid #f3f4f6;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;

    .title {
      font-size: 15px;
      font-weight: 600;
      color: #111827;
    }

    .actions {
      display: flex;
      gap: 8px;
      align-items: center;
    }
  }

  /* 关键：body 固定高度，内部 flex 填充 */
  .card-body {
    padding: 12px 16px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    
    /* 关键：限制绘制边界，防止 canvas 溢出 */
    contain: layout paint;
    overflow: hidden; /* 裁剪溢出的 canvas */

    .empty-state {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .chart-content {
      flex: 1;
      min-height: 0; /* 防止 flex 子项撑开 */
      width: 100%;
      position: relative;
    }
  }
}
</style>