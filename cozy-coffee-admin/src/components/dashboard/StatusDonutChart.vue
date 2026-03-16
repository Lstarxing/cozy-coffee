<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array, // [{ status, count }]
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null

// Status Colors
const statusColors = {
  pending: '#3B82F6',    // Blue
  preparing: '#F59E0B',  // Amber
  processing: '#F59E0B', 
  shipped: '#10B981',    // Green
  completed: '#10B981', 
  cancelled: '#9CA3AF'   // Gray
}

const statusLabels = {
  pending: '待处理',
  preparing: '制作中',
  processing: '配货中',
  shipped: '已发货',
  completed: '已完成',
  cancelled: '已取消'
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
  window.addEventListener('resize', handleResize)
}

const handleResize = () => {
  chartInstance && chartInstance.resize()
}

const updateChart = () => {
  if (!chartInstance) return
  
  const chartData = props.data.map(item => ({
    name: statusLabels[item.status] || item.status,
    value: item.count,
    itemStyle: { color: statusColors[item.status] || '#CBD5E1' }
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      appendToBody: true,
      confine: true
    },
    legend: {
      orient: 'vertical',
      right: 0,
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { fontSize: 12, color: '#4B5563' }
    },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '14',
            fontWeight: 'bold'
          }
        },
        labelLine: { show: false },
        data: chartData
      }
    ]
  }
  
  chartInstance.setOption(option)
}

watch(() => props.data, () => {
  nextTick(() => updateChart())
}, { deep: true })

onMounted(() => initChart())
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance && chartInstance.dispose()
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
}
</style>
