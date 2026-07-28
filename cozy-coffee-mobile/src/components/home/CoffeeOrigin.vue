<!-- CoffeeOrigin: 精品咖啡产区档案 — 多色节点 + 统一航线 + 色彩图例。 -->
<template>
  <view class="coffee-origin">
    <view class="origin-head">
      <text class="origin-title cozy-display">风味从土地开始</text>
      <text class="origin-sub">8 个产区，汇聚于杭州</text>
    </view>

    <view class="origin-map">
      <canvas
        v-if="!mapImage"
        type="2d"
        id="origin-canvas"
        class="origin-canvas"
        :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"
      />
      <image
        v-if="mapImage"
        :src="mapImage"
        mode="widthFix"
        class="origin-image"
        :style="{ width: canvasWidth + 'px' }"
      />
    </view>

    <view class="origin-legend">
      <view class="legend-item" v-for="o in originColors" :key="o.id">
        <view class="legend-dot" :style="{ background: o.color }" />
        <text class="legend-name">{{ o.name }}</text>
      </view>
    </view>

    <view class="origin-foot">
      <text class="origin-cta" @click="onExplore">探索 8 个产区 →</text>
    </view>
  </view>
</template>

<script setup>
import { onMounted, ref, getCurrentInstance, nextTick } from 'vue'
import { WORLD_COUNTRY_PATHS } from '@/data/worldCountryPaths'
import { COFFEE_ORIGINS, HANGZHOU_POINT } from '@/data/coffeeOrigins'
import { normalizedToViewBox, buildQuadraticRoute, drawSvgPath } from '@/utils/coffeeMap'

const emit = defineEmits(['explore'])
const instance = getCurrentInstance()

const VB_W = 1000
const VB_H = 500
const canvasWidth = ref(360)
const canvasHeight = ref(180)
const mapImage = ref('')

const HZ_COLOR = '#753A22'
const ROUTE_COLOR = '#E3DED8'
const MUTED = '#756A63'
const SURFACE = '#F7F5F3'

// Origin colors — data-driven, each origin gets a distinct warm/earthy tone
const originColors = [
  { id: 'ethiopia', name: 'Ethiopia', color: '#C97A3D' },
  { id: 'kenya', name: 'Kenya', color: '#D65A45' },
  { id: 'brazil', name: 'Brazil', color: '#B58A45' },
  { id: 'colombia', name: 'Colombia', color: '#8A6B45' },
  { id: 'guatemala', name: 'Guatemala', color: '#6B7C55' },
  { id: 'panama', name: 'Panama', color: '#D49B5A' },
  { id: 'indonesia', name: 'Indonesia', color: '#526C7A' },
  { id: 'yunnan', name: 'Yunnan', color: '#753A22' }
]

function originColor(id) {
  return originColors.find(o => o.id === id)?.color || HZ_COLOR
}

const hangzhouVB = normalizedToViewBox(HANGZHOU_POINT)

function mx(vbX) { return (vbX / VB_W) * canvasWidth.value }
function my(vbY) { return (vbY / VB_H) * canvasHeight.value }

onMounted(() => {
  const info = uni.getSystemInfoSync()
  const w = info.windowWidth - 28 * 2
  canvasWidth.value = w
  canvasHeight.value = Math.round(w * 0.52)

  nextTick(() => {
    setTimeout(() => {
      initCanvas(info)
    }, 300)
  })
})

function initCanvas(info) {
  const query = uni.createSelectorQuery().in(instance)
  query.select('#origin-canvas')
    .fields({ node: true, size: true })
    .exec((res) => {
      if (!res || !res[0] || !res[0].node) return
      const canvas = res[0].node
      const ctx = canvas.getContext('2d')
      const dpr = info.pixelRatio || 2
      canvas.width = canvasWidth.value * dpr
      canvas.height = canvasHeight.value * dpr
      ctx.scale(dpr, dpr)

      drawMap(ctx)

      uni.canvasToTempFilePath({
        canvas,
        fileType: 'png',
        quality: 1,
        success: (result) => { mapImage.value = result.tempFilePath },
        fail: (err) => { console.error('canvasToTempFilePath failed', err) }
      })
    })
}

function drawMap(ctx) {
  const w = canvasWidth.value
  const h = canvasHeight.value
  const sw = w / VB_W
  const sh = h / VB_H

  ctx.clearRect(0, 0, w, h)
  ctx.fillStyle = '#FFFFFF'
  ctx.fillRect(0, 0, w, h)

  // Country outlines — faint, editorial
  ctx.save()
  ctx.scale(sw, sh)
  WORLD_COUNTRY_PATHS.forEach(country => {
    ctx.beginPath()
    drawSvgPath(ctx, country.d)
    ctx.closePath()
    ctx.fillStyle = SURFACE
    ctx.fill()
    ctx.strokeStyle = MUTED + '20'
    ctx.lineWidth = 0.6 / sw
    ctx.stroke()
  })
  ctx.restore()

  // Routes — all #E3DED8, unified
  ctx.save()
  ctx.scale(sw, sh)
  COFFEE_ORIGINS.forEach(origin => {
    const route = buildQuadraticRoute(origin.origin, HANGZHOU_POINT, origin.routeBend)
    ctx.beginPath()
    ctx.moveTo(route.start.x, route.start.y)
    ctx.quadraticCurveTo(route.control.x, route.control.y, route.end.x, route.end.y)
    ctx.strokeStyle = ROUTE_COLOR
    ctx.lineWidth = 0.7
    ctx.setLineDash([3, 4])
    ctx.stroke()
    ctx.setLineDash([])
  })
  ctx.restore()

  // Origin points — per-origin color, 6px radius
  const dotR = 6
  COFFEE_ORIGINS.forEach(origin => {
    const pt = normalizedToViewBox(origin.origin)
    const px = mx(pt.x)
    const py = my(pt.y)
    const color = originColor(origin.id)

    ctx.beginPath()
    ctx.arc(px, py, dotR, 0, Math.PI * 2)
    ctx.fillStyle = color
    ctx.fill()
    ctx.strokeStyle = '#FFFFFF'
    ctx.lineWidth = 1.5
    ctx.stroke()
  })

  // Hangzhou hub — double ring, 14px
  const hx = mx(hangzhouVB.x)
  const hy = my(hangzhouVB.y)

  ctx.beginPath()
  ctx.arc(hx, hy, 14, 0, Math.PI * 2)
  ctx.fillStyle = HZ_COLOR + '20'
  ctx.fill()
  ctx.strokeStyle = HZ_COLOR
  ctx.lineWidth = 1.2
  ctx.stroke()

  ctx.beginPath()
  ctx.arc(hx, hy, 6, 0, Math.PI * 2)
  ctx.fillStyle = HZ_COLOR
  ctx.fill()
  ctx.strokeStyle = '#FFFFFF'
  ctx.lineWidth = 1.5
  ctx.stroke()

  // Hangzhou label
  ctx.fillStyle = HZ_COLOR
  ctx.font = 'bold 10px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('杭州', hx, hy - 22)
}

function onExplore() {
  emit('explore')
}
</script>

<style lang="scss" scoped>
.coffee-origin {
  padding: 72rpx 28rpx;
  background: $cozy-bg;
}

.origin-head {
  margin-bottom: 36rpx;
}

.origin-title {
  display: block;
  color: $cozy-ink;
  font-size: 40rpx;
}

.origin-sub {
  display: block;
  margin-top: 8rpx;
  color: $cozy-muted;
  font-size: 22rpx;
}

.origin-map {
  width: 100%;
  min-height: 20rpx;
}

.origin-canvas,
.origin-image {
  display: block;
}

.origin-legend {
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14rpx 8rpx;
  justify-items: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  white-space: nowrap;
}

.legend-dot {
  width: 10rpx;
  height: 10rpx;
  flex: none;
  border-radius: 50%;
}

.legend-name {
  color: $cozy-muted;
  font-size: 18rpx;
}

.origin-foot {
  margin-top: 28rpx;
}

.origin-cta {
  color: $cozy-primary;
  font-size: 22rpx;
  font-weight: 650;
}
</style>
