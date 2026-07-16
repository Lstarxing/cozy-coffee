<template>
  <div class="coffee-map" :class="{ 'is-summary': isSummary }" aria-hidden="true">
    <svg viewBox="0 0 1000 500" preserveAspectRatio="xMidYMid meet">
      <defs>
        <linearGradient id="coffee-belt-gradient" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stop-color="var(--cozy-primary)" stop-opacity="0" />
          <stop offset=".18" stop-color="var(--cozy-primary)" stop-opacity=".72" />
          <stop offset=".82" stop-color="var(--cozy-primary)" stop-opacity=".72" />
          <stop offset="1" stop-color="var(--cozy-primary)" stop-opacity="0" />
        </linearGradient>
      </defs>

      <g class="map-background-layer">
        <rect width="1000" height="500" class="map-background" />
      </g>

      <g class="map-decoration-layer">
        <ellipse cx="500" cy="264" rx="510" ry="61" class="coffee-belt-glow" />
        <text x="32" y="226" class="coffee-belt-label">COFFEE BELT</text>
      </g>

      <g class="map-camera-layer" :transform="cameraMatrix">
        <g class="map-layer map-layer--countries">
          <path
            v-for="country in WORLD_COUNTRY_PATHS"
            :id="country.id"
            :key="country.id"
            :d="country.d"
            class="map-country"
            :class="{ 'is-active': country.code === activeOrigin?.countryCode }"
          />
        </g>

        <g class="map-layer map-layer--routes">
          <path
            v-for="origin in origins"
            :key="`route-${origin.id}`"
            :d="routeFor(origin)"
            pathLength="1"
            class="origin-route"
            :class="routeClass(origin.id)"
            :style="routeStyle(origin.id)"
          />
        </g>

        <g class="map-layer map-layer--points">
          <circle
            v-for="origin in origins"
            :key="`point-${origin.id}`"
            :cx="pointFor(origin).x"
            :cy="pointFor(origin).y"
            r="5"
            class="origin-point"
            :class="{ 'is-active': activeOrigin?.id === origin.id, 'is-visited': visitedOrigins.has(origin.id), 'is-summary': isSummary }"
            :style="routeStyle(origin.id)"
          />
        </g>

        <g class="map-layer map-layer--hangzhou-point">
          <circle :cx="hangzhouPoint.x" :cy="hangzhouPoint.y" r="23" class="hangzhou-pulse" />
          <circle :cx="hangzhouPoint.x" :cy="hangzhouPoint.y" r="15" class="hangzhou-ring" />
          <circle :cx="hangzhouPoint.x" :cy="hangzhouPoint.y" r="8" class="hangzhou-point" />
        </g>
      </g>

      <g class="map-label-layer">
        <text
          v-if="activeOrigin && activeOriginLabelPoint"
          :x="activeOriginLabelPoint.x + activeOriginLabelPlacement.dx"
          :y="activeOriginLabelPoint.y + activeOriginLabelPlacement.dy"
          :text-anchor="activeOriginLabelPlacement.anchor"
          class="origin-label"
        >{{ activeOrigin.englishName }}</text>
        <text
          :x="hangzhouLabelPoint.x + 19"
          :y="hangzhouLabelPoint.y - 5"
          class="hangzhou-label"
        >杭州 / Hangzhou</text>
        <text
          :x="hangzhouLabelPoint.x + 19"
          :y="hangzhouLabelPoint.y + 13"
          class="hangzhou-roastery"
        >Cozy Coffee Roastery</text>
        <text
          v-if="isSummary"
          :x="hangzhouLabelPoint.x + 19"
          :y="hangzhouLabelPoint.y + 35"
          class="map-ending-label"
        >Eight Origins. One Cup.</text>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useResponsiveMapCamera } from '@/composables/useResponsiveMapCamera'
import { WORLD_COUNTRY_PATHS } from '@/data/worldCountryPaths'
import { HANGZHOU_POINT } from '@/data/coffeeOrigins'
import { buildQuadraticRoute, normalizedToViewBox } from '@/utils/coffeeMap'
import { projectPoint } from '@/utils/coffeeMapCamera'
import { cameraToSvgMatrix } from '@/utils/coffeeMapSvgRenderer'

const props = defineProps({
  origins: { type: Array, required: true },
  activeOrigin: { type: Object, default: null },
  visitedOrigins: { type: Set, required: true },
  isSummary: { type: Boolean, default: false }
})

const { camera } = useResponsiveMapCamera()
const DEFAULT_ORIGIN_LABEL_PLACEMENT = Object.freeze({ dx: 16, dy: -16, anchor: 'start' })
const ORIGIN_LABEL_PLACEMENTS = Object.freeze({
  yunnan: Object.freeze({ dx: 0, dy: 28, anchor: 'middle' })
})
const cameraMatrix = computed(() => cameraToSvgMatrix(camera.value))
const hangzhouPoint = computed(() => normalizedToViewBox(HANGZHOU_POINT))
const hangzhouLabelPoint = computed(() => projectPoint(HANGZHOU_POINT, camera.value))
const activeOriginLabelPoint = computed(() =>
  props.activeOrigin
    ? projectPoint(props.activeOrigin.origin, camera.value)
    : null
)
const activeOriginLabelPlacement = computed(() =>
  ORIGIN_LABEL_PLACEMENTS[props.activeOrigin?.id] || DEFAULT_ORIGIN_LABEL_PLACEMENT
)

const pointFor = origin => normalizedToViewBox(origin.origin)
const routeFor = origin => buildQuadraticRoute(origin.origin, HANGZHOU_POINT, origin.routeBend)
const routeClass = id => ({
  'is-active': props.activeOrigin?.id === id,
  'is-visited': props.visitedOrigins.has(id),
  'is-summary': props.isSummary
})
const routeStyle = id => ({ '--route-index': props.origins.findIndex(origin => origin.id === id) })
</script>

<style scoped>
.coffee-map {
  position: relative;
  width: 100%;
  overflow: hidden;
  box-sizing: border-box;
  background: var(--cozy-bg) url('../../assets/images/noise.png') repeat;
  background-blend-mode: multiply;
}

svg { display: block; width: 100%; height: auto; }
.map-background { fill: var(--cozy-bg); fill-opacity: .93; }
.coffee-belt-glow { fill: url('#coffee-belt-gradient'); opacity: .12; filter: blur(17px); }
.coffee-belt-label { fill: var(--cozy-primary); opacity: .72; font: 650 11px var(--font-sans); letter-spacing: .14em; }
.map-country { fill: color-mix(in oklch, var(--cozy-primary) 4%, var(--cozy-surface)); stroke: color-mix(in oklch, var(--cozy-ink) 18%, var(--cozy-border)); stroke-width: .85; transition: fill .45s ease, opacity .45s ease; }
.map-country.is-active { fill: color-mix(in oklch, var(--cozy-primary) 48%, var(--cozy-surface)); }
.coffee-map.is-summary .map-country { opacity: .42; }
.origin-route { fill: none; stroke: var(--cozy-primary); stroke-width: 1; opacity: 0; stroke-dasharray: 1; stroke-dashoffset: 1; will-change: opacity, stroke-dashoffset; transition: opacity .42s ease, stroke-dashoffset .9s cubic-bezier(.22, 1, .36, 1); }
.origin-route.is-visited { opacity: .16; stroke-width: 1; stroke-dashoffset: 0; }
.origin-route.is-active { opacity: 1; stroke-width: 2.25; stroke-dashoffset: 0; }
.origin-route.is-summary { opacity: .72; stroke-width: 1.6; stroke-dashoffset: 0; transition: opacity .72s cubic-bezier(.22, 1, .36, 1), stroke-width .72s cubic-bezier(.22, 1, .36, 1), stroke-dashoffset .9s cubic-bezier(.22, 1, .36, 1); transition-delay: calc(var(--route-index) * 65ms); }
.origin-point { fill: var(--cozy-border); stroke: var(--cozy-bg); stroke-width: 2; transform-box: fill-box; transform-origin: center; transition: fill .45s ease, transform .45s ease; }
.origin-point.is-visited { fill: color-mix(in oklch, var(--cozy-primary) 38%, var(--cozy-border)); }
.origin-point.is-active { fill: var(--cozy-primary); transform: scale(1.22); animation: origin-pulse 2s ease-in-out infinite; }
.origin-point.is-summary { fill: color-mix(in oklch, var(--cozy-primary) 76%, var(--cozy-border)); transform: scale(1.08); transition-delay: calc(var(--route-index) * 65ms); }
.origin-label,
.hangzhou-label,
.hangzhou-roastery {
  font-family: var(--font-sans);
  paint-order: stroke fill;
  stroke: var(--cozy-bg);
  stroke-linejoin: round;
}
.origin-label { fill: var(--cozy-primary); stroke-width: 4px; font-size: 13px; font-weight: 700; }
.hangzhou-point { fill: var(--cozy-primary); stroke: var(--cozy-bg); stroke-width: 3; }
.hangzhou-ring { fill: color-mix(in oklch, var(--cozy-primary) 8%, transparent); stroke: var(--cozy-primary); stroke-width: 1.5; opacity: .66; }
.hangzhou-pulse { fill: none; stroke: var(--cozy-primary); stroke-width: 1.35; opacity: .38; transform-box: fill-box; transform-origin: center; animation: hangzhou-pulse 2s ease-in-out infinite; }
.hangzhou-label { fill: var(--cozy-ink); stroke-width: 5px; font-size: 14px; font-weight: 750; }
.hangzhou-roastery { fill: var(--cozy-muted); stroke-width: 4px; font-size: 10.5px; }
.map-ending-label { fill: var(--cozy-primary); stroke: var(--cozy-bg); stroke-width: 4px; paint-order: stroke fill; font: 650 11px var(--font-sans); letter-spacing: .04em; opacity: 0; animation: summary-copy-in .65s .55s cubic-bezier(.22, 1, .36, 1) forwards; }
.coffee-map.is-summary .hangzhou-ring { opacity: .92; stroke-width: 2; }
.coffee-map.is-summary .hangzhou-pulse { animation: hangzhou-summary-breathe 1.35s .35s cubic-bezier(.22, 1, .36, 1) forwards; }

@keyframes origin-pulse { 50% { transform: scale(1.1); } }
@keyframes hangzhou-pulse { 50% { transform: scale(1.35); opacity: .08; } }
@keyframes hangzhou-summary-breathe {
  0% { transform: scale(1); opacity: .38; }
  58% { transform: scale(1.62); opacity: .1; }
  100% { transform: scale(1.24); opacity: .28; }
}
@keyframes summary-copy-in {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: .78; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .map-country, .origin-route, .origin-point { transition: none; }
  .origin-route.is-summary { transition-delay: 0s; }
  .origin-point.is-summary { transition-delay: 0s; }
  .origin-point.is-active, .hangzhou-pulse, .map-ending-label { animation: none; }
  .map-ending-label { opacity: .78; }
}
</style>
