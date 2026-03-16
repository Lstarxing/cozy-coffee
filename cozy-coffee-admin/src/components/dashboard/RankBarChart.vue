<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array, // [{ name, value }]
    default: () => []
  },
  metric: String // amount | count | points
})

const chartRef = ref(null)
let chartInstance = null

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
  
  // Reverse for horizontal bar to show top at top
  const sortedData = [...props.data].reverse()
  const yData = sortedData.map(item => {
    // Truncate name if too long
    return item.name.length > 8 ? item.name.substring(0, 8) + '...' : item.name
  })
  const xData = sortedData.map(item => item.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      appendToBody: true,
      confine: true
    },
    grid: {
      left: '3%',
      right: '10%',
      bottom: '3%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      show: false
    },
    yAxis: {
      type: 'category',
      data: yData,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#4B5563' }
    },
    series: [
      {
        name: 'Top',
        type: 'bar',
        barWidth: '60%',
        data: xData,
        itemStyle: { 
            color: '#8B5E3C', 
            borderRadius: [0, 4, 4, 0] 
        },
        label: {
            show: true,
            position: 'right',
            color: '#6B7280',
            formatter: '{c}'
        }
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
