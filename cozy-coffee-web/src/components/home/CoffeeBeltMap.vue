<template>
  <div class="coffee-map" aria-hidden="true">
    <svg viewBox="0 0 1000 500" preserveAspectRatio="xMidYMid meet">
      <defs>
        <linearGradient id="coffee-belt-gradient" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stop-color="var(--cozy-primary)" stop-opacity="0" />
          <stop offset=".18" stop-color="var(--cozy-primary)" stop-opacity=".72" />
          <stop offset=".82" stop-color="var(--cozy-primary)" stop-opacity=".72" />
          <stop offset="1" stop-color="var(--cozy-primary)" stop-opacity="0" />
        </linearGradient>
      </defs>

      <g class="map-layer map-layer--background">
        <rect width="1000" height="500" class="map-background" />
      </g>

      <g class="map-layer map-layer--coffee-belt">
        <ellipse cx="500" cy="264" rx="510" ry="61" class="coffee-belt-glow" />
        <text x="108" y="226" class="coffee-belt-label">COFFEE BELT</text>
      </g>

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
          :class="{ 'is-active': activeOrigin?.id === origin.id, 'is-visited': visitedOrigins.has(origin.id) }"
        />
      </g>

      <g class="map-layer map-layer--labels">
        <text
          v-if="activeOrigin"
          :x="pointFor(activeOrigin).x + 16"
          :y="pointFor(activeOrigin).y - 16"
          class="origin-label"
        >{{ activeOrigin.englishName }}</text>
      </g>

      <g class="map-layer map-layer--hangzhou">
        <circle :cx="hangzhou.x" :cy="hangzhou.y" r="23" class="hangzhou-pulse" />
        <circle :cx="hangzhou.x" :cy="hangzhou.y" r="15" class="hangzhou-ring" />
        <circle :cx="hangzhou.x" :cy="hangzhou.y" r="8" class="hangzhou-point" />
        <text :x="hangzhou.x + 19" :y="hangzhou.y - 5" class="hangzhou-label">杭州 / Hangzhou</text>
        <text :x="hangzhou.x + 19" :y="hangzhou.y + 13" class="hangzhou-roastery">Cozy Coffee Roastery</text>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { WORLD_COUNTRY_PATHS } from '@/data/worldCountryPaths'
import { HANGZHOU_POINT } from '@/data/coffeeOrigins'
import { buildQuadraticRoute, normalizedToViewBox } from '@/utils/coffeeMap'

const props = defineProps({
  origins: { type: Array, required: true },
  activeOrigin: { type: Object, default: null },
  visitedOrigins: { type: Set, required: true },
  isSummary: { type: Boolean, default: false }
})

const hangzhou = computed(() => normalizedToViewBox(HANGZHOU_POINT))
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
.origin-route { fill: none; stroke: var(--cozy-primary); stroke-width: 1; opacity: 0; stroke-dasharray: 1; stroke-dashoffset: 1; will-change: opacity, stroke-dashoffset; transition: opacity .42s ease, stroke-dashoffset .9s cubic-bezier(.22, 1, .36, 1); }
.origin-route.is-visited { opacity: .16; stroke-width: 1; stroke-dashoffset: 0; }
.origin-route.is-active { opacity: 1; stroke-width: 2.25; stroke-dashoffset: 0; }
.origin-route.is-summary { opacity: .56; stroke-width: 1.45; stroke-dashoffset: 0; transition-delay: calc(var(--route-index) * 65ms); }
.origin-point { fill: var(--cozy-border); stroke: var(--cozy-bg); stroke-width: 2; transform-box: fill-box; transform-origin: center; transition: fill .45s ease, transform .45s ease; }
.origin-point.is-visited { fill: color-mix(in oklch, var(--cozy-primary) 38%, var(--cozy-border)); }
.origin-point.is-active { fill: var(--cozy-primary); transform: scale(1.22); animation: origin-pulse 2s ease-in-out infinite; }
.origin-label, .hangzhou-label, .hangzhou-roastery { font-family: var(--font-sans); }
.origin-label { fill: var(--cozy-primary); font-size: 13px; font-weight: 700; }
.hangzhou-point { fill: var(--cozy-primary); stroke: var(--cozy-bg); stroke-width: 3; }
.hangzhou-ring { fill: color-mix(in oklch, var(--cozy-primary) 8%, transparent); stroke: var(--cozy-primary); stroke-width: 1.5; opacity: .66; }
.hangzhou-pulse { fill: none; stroke: var(--cozy-primary); stroke-width: 1.35; opacity: .38; transform-box: fill-box; transform-origin: center; animation: hangzhou-pulse 2s ease-in-out infinite; }
.hangzhou-label { fill: var(--cozy-ink); font-size: 14px; font-weight: 750; }
.hangzhou-roastery { fill: var(--cozy-muted); font-size: 10.5px; }

@keyframes origin-pulse { 50% { transform: scale(1.1); } }
@keyframes hangzhou-pulse { 50% { transform: scale(1.35); opacity: .08; } }

@media (prefers-reduced-motion: reduce) {
  .map-country, .origin-route, .origin-point { transition: none; }
  .origin-route.is-summary { transition-delay: 0s; }
  .origin-point.is-active, .hangzhou-pulse { animation: none; }
}
</style>
