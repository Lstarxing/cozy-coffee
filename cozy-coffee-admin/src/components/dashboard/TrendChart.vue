<template>
  <!-- 关键：TrendChart 不再自带 min-height，也不再尝试决定高度，只填充父容器 -->
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chartInstance = null

const handleResize = () => {
  chartInstance && chartInstance.resize()
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
  window.addEventListener('resize', handleResize)
}

const updateChart = () => {
  if (!chartInstance) return

  const xAxisData = props.data.map((item) => item.time)
  const coffeeData = props.data.map((item) => item.coffeeRevenue || 0)
  const orderData = props.data.map((item) => item.coffeeOrders || 0)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line' },
      appendToBody: true, // 关键：tooltip 挂载到 body，避免被 contain/overflow 裁剪
      confine: true
    },
    legend: {
      data: ['咖啡营收', '咖啡订单'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData,
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisLabel: { color: '#6B7280' }
    },
    yAxis: [
      {
        type: 'value',
        name: '营收',
        position: 'left',
        splitLine: { lineStyle: { type: 'dashed', color: '#F3F4F6' } },
        axisLabel: { color: '#6B7280' }
      },
      {
        type: 'value',
        name: '订单量',
        position: 'right',
        splitLine: { show: false },
        axisLabel: { color: '#6B7280' }
      }
    ],
    series: [
      {
        name: '咖啡营收',
        type: 'line',
        smooth: true,
        showSymbol: false,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139, 94, 60, 0.3)' },
            { offset: 1, color: 'rgba(139, 94, 60, 0.0)' }
          ])
        },
        itemStyle: { color: '#8B5E3C' },
        data: coffeeData
      },
      {
        name: '咖啡订单',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        showSymbol: false,
        itemStyle: { color: '#F59E0B' },
        data: orderData
      }
    ]
  }

  chartInstance.setOption(option, true)
}

watch(
  () => props.data,
  () => {
    nextTick(() => updateChart())
  },
  { deep: true }
)

onMounted(() => {
  initChart()
})

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