# CozyCoffee Global Origin Journey Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current three-image Origins collage with an accessible sticky world Coffee Belt journey covering eight origins whose SVG routes converge independently on the Cozy Coffee roastery in Hangzhou.

**Architecture:** `OriginsJourney.vue` owns the chapter observer and the single journey state, passes explicit props to a pure `CoffeeBeltMap.vue` renderer and data-driven `OriginChapter.vue`, and imports all content from `coffeeOrigins.js`. World country paths are generated once during development from public-domain Natural Earth data; the production bundle contains only static SVG path data and native Vue/CSS behavior—no D3, GSAP, or scroll listener at runtime.

**Tech Stack:** Vue 3 `<script setup>`, native `IntersectionObserver`, inline SVG, CSS/SVG transitions, Node `node:test`, dev-only `d3-geo` + `topojson-client` + `world-atlas`, Python Pillow for the static noise texture.

---

## File map

**Create:**

- `cozy-coffee-web/src/data/coffeeOrigins.js` — eight origin records, summary chapter, Hangzhou point, chapter order
- `cozy-coffee-web/src/utils/coffeeMap.js` — normalized-coordinate conversion and deterministic Quadratic Bézier generation
- `cozy-coffee-web/src/data/worldCountryPaths.js` — generated static SVG country path data
- `cozy-coffee-web/src/components/home/CoffeeBeltMap.vue` — pure layered SVG renderer
- `cozy-coffee-web/src/components/home/OriginChapter.vue` — semantic data-driven chapter renderer
- `cozy-coffee-web/src/components/home/OriginsJourney.vue` — sticky layout, observer, active/visited state
- `cozy-coffee-web/src/assets/images/noise.png` — subtle static paper texture
- `cozy-coffee-web/scripts/generate-world-map.mjs` — reproducible development-only map generator
- `cozy-coffee-web/scripts/generate-noise.py` — reproducible texture generator
- `cozy-coffee-web/scripts/coffee-map.test.mjs` — route/data contract tests

**Modify:**

- `cozy-coffee-web/package.json` — map generator/test scripts and dev-only generator dependencies
- `cozy-coffee-web/src/views/Home.vue` — replace old Origins markup/styles with `OriginsJourney`; update Hero subtitle and transition copy

**Do not modify:** login/register, member center, admin, backend, or `cozy-coffee-mobile`.

---

### Task 1: Lock the origin data and route mathematics with tests

**Files:**

- Create: `cozy-coffee-web/scripts/coffee-map.test.mjs`
- Create: `cozy-coffee-web/src/utils/coffeeMap.js`
- Create: `cozy-coffee-web/src/data/coffeeOrigins.js`

- [ ] **Step 1: Write the failing route/data contract test**

Create `cozy-coffee-web/scripts/coffee-map.test.mjs`:

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildQuadraticRoute,
  normalizedToViewBox,
  MAP_VIEWBOX
} from '../src/utils/coffeeMap.js'
import {
  COFFEE_ORIGINS,
  COFFEE_JOURNEY,
  HANGZHOU_POINT
} from '../src/data/coffeeOrigins.js'

test('defines eight unique origins that each terminate in Hangzhou', () => {
  assert.equal(COFFEE_ORIGINS.length, 8)
  assert.equal(new Set(COFFEE_ORIGINS.map(origin => origin.id)).size, 8)
  assert.equal(new Set(COFFEE_ORIGINS.map(origin => origin.countryCode)).size, 8)
  assert.deepEqual(
    COFFEE_ORIGINS.map(origin => origin.destination),
    Array(8).fill('hangzhou')
  )
  assert.equal(COFFEE_JOURNEY.at(-1).id, 'hangzhou')
  assert.equal(COFFEE_JOURNEY.at(-1).type, 'summary')
})

test('keeps normalized points inside the SVG viewBox', () => {
  for (const origin of COFFEE_ORIGINS) {
    assert.ok(origin.origin.x >= 0 && origin.origin.x <= 1)
    assert.ok(origin.origin.y >= 0 && origin.origin.y <= 1)
    const point = normalizedToViewBox(origin.origin)
    assert.ok(point.x >= 0 && point.x <= MAP_VIEWBOX.width)
    assert.ok(point.y >= 0 && point.y <= MAP_VIEWBOX.height)
  }
})

test('generates a quadratic route with the exact origin and Hangzhou endpoints', () => {
  const origin = COFFEE_ORIGINS[0]
  const start = normalizedToViewBox(origin.origin)
  const end = normalizedToViewBox(HANGZHOU_POINT)
  const path = buildQuadraticRoute(origin.origin, HANGZHOU_POINT, origin.routeBend)

  assert.match(path, /^M [-\d.]+ [-\d.]+ Q [-\d.]+ [-\d.]+ [-\d.]+ [-\d.]+$/)
  assert.ok(path.startsWith(`M ${start.x} ${start.y}`))
  assert.ok(path.endsWith(`${end.x} ${end.y}`))
})

test('includes the content fields needed by every origin chapter', () => {
  for (const origin of COFFEE_ORIGINS) {
    assert.equal(origin.type, 'origin')
    assert.ok(origin.name)
    assert.ok(origin.englishName)
    assert.ok(origin.region)
    assert.ok(origin.altitude)
    assert.ok(origin.process)
    assert.ok(origin.varieties.length >= 1)
    assert.equal(origin.flavors.length, 4)
    assert.ok(origin.role)
  }
})
```

- [ ] **Step 2: Run the test to verify RED**

Run:

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-web
node scripts/coffee-map.test.mjs
```

Expected: FAIL with `ERR_MODULE_NOT_FOUND` for `src/utils/coffeeMap.js` or `src/data/coffeeOrigins.js`.

- [ ] **Step 3: Implement the route math utility**

Create `cozy-coffee-web/src/utils/coffeeMap.js`:

```js
export const MAP_VIEWBOX = Object.freeze({ width: 1000, height: 500 })

function round(value) {
  return Number(value.toFixed(2))
}

export function normalizedToViewBox(point) {
  return {
    x: round(point.x * MAP_VIEWBOX.width),
    y: round(point.y * MAP_VIEWBOX.height)
  }
}

export function buildQuadraticRoute(origin, destination, bend = -0.16) {
  const start = normalizedToViewBox(origin)
  const end = normalizedToViewBox(destination)
  const dx = end.x - start.x
  const dy = end.y - start.y
  const distance = Math.hypot(dx, dy) || 1
  const midpoint = { x: (start.x + end.x) / 2, y: (start.y + end.y) / 2 }
  const normal = { x: -dy / distance, y: dx / distance }
  const control = {
    x: round(midpoint.x + normal.x * distance * bend),
    y: round(midpoint.y + normal.y * distance * bend)
  }

  return `M ${start.x} ${start.y} Q ${control.x} ${control.y} ${end.x} ${end.y}`
}
```

- [ ] **Step 4: Add the eight origins and the summary chapter**

Create `cozy-coffee-web/src/data/coffeeOrigins.js`:

```js
export const HANGZHOU_POINT = Object.freeze({ x: 0.82, y: 0.43 })

export const COFFEE_ORIGINS = Object.freeze([
  {
    id: 'ethiopia', type: 'origin', countryCode: 'ET',
    name: '埃塞俄比亚', englishName: 'Ethiopia', region: '耶加雪菲与西达摩',
    origin: { x: 0.57, y: 0.49 }, destination: 'hangzhou', routeBend: -0.2,
    altitude: '1,800–2,200 m', process: '水洗 / 日晒', varieties: ['Heirloom'],
    flavors: ['茉莉', '蓝莓', '柑橘', '红茶'], role: '花香层次',
    story: '被视为阿拉比卡咖啡的故乡，丰富的遗传多样性让花香、果香与茶感自然并存。'
  },
  {
    id: 'kenya', type: 'origin', countryCode: 'KE',
    name: '肯尼亚', englishName: 'Kenya', region: '涅里与基里尼亚加',
    origin: { x: 0.58, y: 0.55 }, destination: 'hangzhou', routeBend: -0.14,
    altitude: '1,500–2,100 m', process: '双重水洗', varieties: ['SL28', 'SL34'],
    flavors: ['黑加仑', '莓果', '葡萄柚', '酒香'], role: '莓果骨架',
    story: '高海拔与双重水洗塑造鲜明酸质，为拼配提供清晰、明亮而有张力的果香骨架。'
  },
  {
    id: 'brazil', type: 'origin', countryCode: 'BR',
    name: '巴西', englishName: 'Brazil', region: '米纳斯吉拉斯',
    origin: { x: 0.34, y: 0.66 }, destination: 'hangzhou', routeBend: -0.24,
    altitude: '900–1,300 m', process: '日晒 / 半日晒', varieties: ['Catuai', 'Mundo Novo'],
    flavors: ['坚果', '巧克力', '焦糖', '低酸'], role: '浓缩基底',
    story: '稳定的坚果、可可与焦糖甜感构成浓缩基底，让杯中结构保持醇厚与平衡。'
  },
  {
    id: 'colombia', type: 'origin', countryCode: 'CO',
    name: '哥伦比亚', englishName: 'Colombia', region: '安第斯山脉',
    origin: { x: 0.28, y: 0.56 }, destination: 'hangzhou', routeBend: -0.18,
    altitude: '1,200–2,000 m', process: '水洗', varieties: ['Caturra', 'Castillo'],
    flavors: ['焦糖', '红莓', '坚果', '平衡酸质'], role: '平衡主体',
    story: '安第斯山脉的多样微气候把甜感与酸质放在同一条清晰的风味轴线上。'
  },
  {
    id: 'guatemala', type: 'origin', countryCode: 'GT',
    name: '危地马拉', englishName: 'Guatemala', region: '安提瓜火山产区',
    origin: { x: 0.23, y: 0.48 }, destination: 'hangzhou', routeBend: -0.12,
    altitude: '1,300–2,000 m', process: '水洗', varieties: ['Bourbon', 'Caturra'],
    flavors: ['黑巧克力', '香料', '坚果', '柑橘'], role: '香料层次',
    story: '火山土壤与昼夜温差积累出扎实甜感，并在尾韵中留下细致的香料层次。'
  },
  {
    id: 'panama', type: 'origin', countryCode: 'PA',
    name: '巴拿马', englishName: 'Panama', region: '波奎特',
    origin: { x: 0.25, y: 0.53 }, destination: 'hangzhou', routeBend: -0.08,
    altitude: '1,500–2,100 m', process: '水洗 / 日晒', varieties: ['Geisha'],
    flavors: ['茉莉', '佛手柑', '热带水果', '蜂蜜'], role: '芳香高点',
    story: '瑰夏把花香、柑橘与蜂蜜般甜感推向高点，为风味体系带来轻盈而明确的芳香记忆。'
  },
  {
    id: 'indonesia', type: 'origin', countryCode: 'ID',
    name: '印度尼西亚', englishName: 'Indonesia', region: '苏门答腊',
    origin: { x: 0.76, y: 0.63 }, destination: 'hangzhou', routeBend: 0.2,
    altitude: '1,200–1,700 m', process: '湿刨', varieties: ['Ateng', 'Tim Tim'],
    flavors: ['草本', '香料', '黑巧克力', '醇厚'], role: '醇厚深度',
    story: '湿刨处理带来低沉香料感与厚重质地，为浅亮风味补上深度与余韵。'
  },
  {
    id: 'yunnan', type: 'origin', countryCode: 'CN',
    name: '中国云南', englishName: 'Yunnan, China', region: '保山与普洱',
    origin: { x: 0.78, y: 0.5 }, destination: 'hangzhou', routeBend: 0.12,
    altitude: '1,200–1,900 m', process: '水洗 / 日晒', varieties: ['Catimor'],
    flavors: ['坚果', '焦糖', '红茶', '柑橘'], role: '东方表达',
    story: '高原季风与不断进步的处理实验，让云南成为 Cozy Coffee 风味语言中的东方表达。'
  }
])

export const JOURNEY_SUMMARY = Object.freeze({
  id: 'hangzhou', type: 'summary', name: '杭州', englishName: 'Hangzhou',
  region: 'Cozy Coffee Roastery',
  story: '世界八处产区，最终在杭州完成属于 Cozy Coffee 的风味语言。',
  process: ['Roasting', 'Blending', 'Cup']
})

export const COFFEE_JOURNEY = Object.freeze([...COFFEE_ORIGINS, JOURNEY_SUMMARY])
```

- [ ] **Step 5: Run the contract test to verify GREEN**

Run: `node scripts/coffee-map.test.mjs`

Expected: 4 tests pass, 0 fail.

- [ ] **Step 6: Commit the data/math slice**

```powershell
git add cozy-coffee-web/scripts/coffee-map.test.mjs cozy-coffee-web/src/utils/coffeeMap.js cozy-coffee-web/src/data/coffeeOrigins.js
git commit -m "feat: define coffee origin journey data"
```

---

### Task 2: Generate the static Natural Earth world map and paper texture

**Files:**

- Modify: `cozy-coffee-web/package.json`
- Create: `cozy-coffee-web/scripts/generate-world-map.mjs`
- Create: `cozy-coffee-web/scripts/generate-noise.py`
- Generate: `cozy-coffee-web/src/data/worldCountryPaths.js`
- Generate: `cozy-coffee-web/src/assets/images/noise.png`
- Modify: `cozy-coffee-web/scripts/coffee-map.test.mjs`

- [ ] **Step 1: Extend the failing test with the generated-map contract**

Append to `coffee-map.test.mjs`:

```js
import { WORLD_COUNTRY_PATHS } from '../src/data/worldCountryPaths.js'

test('generated world map contains the eight highlighted ISO country codes', () => {
  assert.ok(WORLD_COUNTRY_PATHS.length > 150)
  const codes = new Set(WORLD_COUNTRY_PATHS.map(country => country.code))
  for (const code of ['ET', 'KE', 'BR', 'CO', 'GT', 'PA', 'ID', 'CN']) {
    assert.ok(codes.has(code), `missing ${code}`)
  }
  assert.ok(WORLD_COUNTRY_PATHS.every(country => country.d.startsWith('M')))
})
```

- [ ] **Step 2: Run the test to verify RED**

Run: `node scripts/coffee-map.test.mjs`

Expected: FAIL because `worldCountryPaths.js` does not exist.

- [ ] **Step 3: Add development-only generator dependencies and scripts**

Run:

```powershell
npm.cmd install --save-dev d3-geo@^3.1.1 topojson-client@^3.1.0 world-atlas@^2.0.2
```

Add these scripts to `package.json`:

```json
"test:coffee-map": "node scripts/coffee-map.test.mjs",
"generate:coffee-map": "node scripts/generate-world-map.mjs",
"generate:noise": "python scripts/generate-noise.py"
```

These packages must appear only in `devDependencies`; no production Vue file may import them.

- [ ] **Step 4: Create the deterministic world-path generator**

Create `scripts/generate-world-map.mjs`:

```js
import { writeFile } from 'node:fs/promises'
import { geoNaturalEarth1, geoPath } from 'd3-geo'
import { feature } from 'topojson-client'
import world from 'world-atlas/countries-110m.json' with { type: 'json' }

const numericToIso = {
  '076': 'BR', '156': 'CN', '170': 'CO', '231': 'ET',
  '320': 'GT', '360': 'ID', '404': 'KE', '591': 'PA'
}

const collection = feature(world, world.objects.countries)
const projection = geoNaturalEarth1().fitExtent([[18, 24], [982, 476]], collection)
const renderPath = geoPath(projection)

const countries = collection.features
  .map(country => {
    const numeric = String(country.id).padStart(3, '0')
    return {
      id: `country-${numeric}`,
      code: numericToIso[numeric] || numeric,
      d: renderPath(country)
    }
  })
  .filter(country => country.d)

const output = `// Generated by scripts/generate-world-map.mjs. Do not edit by hand.\n` +
  `export const WORLD_COUNTRY_PATHS = Object.freeze(${JSON.stringify(countries, null, 2)})\n`

await writeFile(new URL('../src/data/worldCountryPaths.js', import.meta.url), output, 'utf8')
console.log(`Generated ${countries.length} country paths`)
```

- [ ] **Step 5: Generate the low-opacity PNG noise asset**

Create `scripts/generate-noise.py`:

```python
from pathlib import Path
from random import Random
from PIL import Image

random = Random(20260714)
size = 128
image = Image.new('RGBA', (size, size))
pixels = []

for _ in range(size * size):
    shade = random.randint(70, 185)
    alpha = random.randint(5, 16)
    pixels.append((shade, shade, shade, alpha))

image.putdata(pixels)
output = Path(__file__).resolve().parents[1] / 'src' / 'assets' / 'images' / 'noise.png'
output.parent.mkdir(parents=True, exist_ok=True)
image.save(output, optimize=True)
print(f'Generated {output}')
```

Run:

```powershell
npm.cmd run generate:coffee-map
npm.cmd run generate:noise
```

Expected: generator reports more than 150 country paths and creates a 128×128 RGBA PNG.

- [ ] **Step 6: Run tests and verify the generators are reproducible**

Run:

```powershell
npm.cmd run test:coffee-map
npm.cmd run generate:coffee-map
git diff --exit-code -- src/data/worldCountryPaths.js
```

Expected: 5 tests pass; the second generation produces no diff.

- [ ] **Step 7: Commit generated static assets and development tooling**

```powershell
git add cozy-coffee-web/package.json cozy-coffee-web/package-lock.json cozy-coffee-web/scripts/generate-world-map.mjs cozy-coffee-web/scripts/generate-noise.py cozy-coffee-web/scripts/coffee-map.test.mjs cozy-coffee-web/src/data/worldCountryPaths.js cozy-coffee-web/src/assets/images/noise.png
git commit -m "build: generate static coffee belt map assets"
```

---

### Task 3: Build the pure layered SVG renderer

**Files:**

- Create: `cozy-coffee-web/src/components/home/CoffeeBeltMap.vue`

- [ ] **Step 1: Implement the props-only renderer**

Create `CoffeeBeltMap.vue` with this component contract:

```vue
<template>
  <div class="coffee-map" aria-hidden="true">
    <svg viewBox="0 0 1000 500" preserveAspectRatio="xMidYMid meet">
      <g class="map-layer map-layer--background">
        <rect width="1000" height="500" class="map-background" />
      </g>

      <g class="map-layer map-layer--coffee-belt">
        <rect x="0" y="205" width="1000" height="118" rx="59" class="coffee-belt-glow" />
        <text x="44" y="220" class="coffee-belt-label">COFFEE BELT</text>
      </g>

      <g class="map-layer map-layer--countries">
        <path
          v-for="country in WORLD_COUNTRY_PATHS"
          :key="country.id"
          :id="country.id"
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
          :x="pointFor(activeOrigin).x + 12"
          :y="pointFor(activeOrigin).y - 10"
          class="origin-label"
        >{{ activeOrigin.englishName }}</text>
      </g>

      <g class="map-layer map-layer--hangzhou">
        <circle :cx="hangzhou.x" :cy="hangzhou.y" r="18" class="hangzhou-pulse" />
        <circle :cx="hangzhou.x" :cy="hangzhou.y" r="8" class="hangzhou-point" />
        <text :x="hangzhou.x + 14" :y="hangzhou.y - 4" class="hangzhou-label">杭州 / Hangzhou</text>
        <text :x="hangzhou.x + 14" :y="hangzhou.y + 13" class="hangzhou-roastery">Cozy Coffee Roastery</text>
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
</script>

<style scoped>
.coffee-map {
  position: relative;
  width: 100%;
  background: var(--cozy-bg) url('../../assets/images/noise.png') repeat;
  background-blend-mode: multiply;
}

svg { display: block; width: 100%; height: auto; }
.map-background { fill: var(--cozy-bg); fill-opacity: .98; }
.coffee-belt-glow { fill: var(--cozy-primary); opacity: .05; filter: blur(13px); }
.coffee-belt-label { fill: var(--cozy-muted); opacity: .55; font: 500 10px var(--font-sans); letter-spacing: .12em; }
.map-country { fill: var(--cozy-surface); stroke: var(--cozy-border); stroke-width: .7; transition: fill .35s ease, opacity .35s ease; }
.map-country.is-active { fill: color-mix(in oklch, var(--cozy-primary) 34%, var(--cozy-surface)); }
.origin-route { fill: none; stroke: var(--cozy-primary); stroke-width: 1.5; opacity: 0; stroke-dasharray: 1; stroke-dashoffset: 1; transition: opacity .3s ease, stroke-dashoffset .7s ease; }
.origin-route.is-visited { opacity: .16; stroke-dashoffset: 0; }
.origin-route.is-active { opacity: .82; stroke-dashoffset: 0; }
.origin-route.is-summary { opacity: .52; stroke-dashoffset: 0; }
.origin-point { fill: var(--cozy-border); stroke: var(--cozy-bg); stroke-width: 2; transform-box: fill-box; transform-origin: center; transition: fill .3s ease, transform .3s ease; }
.origin-point.is-visited { fill: color-mix(in oklch, var(--cozy-primary) 38%, var(--cozy-border)); }
.origin-point.is-active { fill: var(--cozy-primary); animation: origin-pulse 2s ease-in-out infinite; }
.origin-label, .hangzhou-label, .hangzhou-roastery { font-family: var(--font-sans); }
.origin-label { fill: var(--cozy-primary); font-size: 12px; font-weight: 650; }
.hangzhou-point { fill: var(--cozy-primary); stroke: var(--cozy-bg); stroke-width: 3; }
.hangzhou-pulse { fill: none; stroke: var(--cozy-primary); stroke-width: 1.2; opacity: .32; transform-box: fill-box; transform-origin: center; animation: hangzhou-pulse 2s ease-in-out infinite; }
.hangzhou-label { fill: var(--cozy-ink); font-size: 12px; font-weight: 700; }
.hangzhou-roastery { fill: var(--cozy-muted); font-size: 9px; }

@keyframes origin-pulse { 50% { transform: scale(1.1); } }
@keyframes hangzhou-pulse { 50% { transform: scale(1.35); opacity: .08; } }

@media (prefers-reduced-motion: reduce) {
  .map-country, .origin-route, .origin-point { transition: none; }
  .origin-point.is-active, .hangzhou-pulse { animation: none; }
}
</style>
```

Note: the Coffee Belt uses CSS blur on a simple rectangle, not an SVG filter. If Safari profiling shows blur cost, replace it with two unblurred translucent rounded rectangles; do not add `feTurbulence` or other SVG filters.

- [ ] **Step 2: Build to catch template/compiler errors**

Run: `npm.cmd run build`

Expected: PASS; production bundle contains no `d3-geo`, `topojson-client`, or `world-atlas` imports.

- [ ] **Step 3: Commit the renderer**

```powershell
git add cozy-coffee-web/src/components/home/CoffeeBeltMap.vue
git commit -m "feat: render layered coffee belt map"
```

---

### Task 4: Build the chapter renderer and observer-owned journey shell

**Files:**

- Create: `cozy-coffee-web/src/components/home/OriginChapter.vue`
- Create: `cozy-coffee-web/src/components/home/OriginsJourney.vue`

- [ ] **Step 1: Create the semantic data-driven chapter component**

Create `OriginChapter.vue`:

```vue
<template>
  <article
    class="origin-chapter"
    :class="[`origin-chapter--${chapter.type}`, { 'is-active': active, 'is-static': staticMode }]"
    :data-origin-id="chapter.id"
    :aria-current="active ? 'step' : undefined"
  >
    <template v-if="chapter.type === 'origin'">
      <p class="origin-counter">{{ number }} / 08 · {{ chapter.englishName }}</p>
      <h3>{{ chapter.name }}</h3>
      <p class="origin-region">{{ chapter.region }}</p>
      <p class="origin-story">{{ chapter.story }}</p>
      <dl class="origin-facts">
        <div><dt>海拔</dt><dd>{{ chapter.altitude }}</dd></div>
        <div><dt>处理法</dt><dd>{{ chapter.process }}</dd></div>
        <div><dt>代表品种</dt><dd>{{ chapter.varieties.join(' · ') }}</dd></div>
        <div><dt>风味角色</dt><dd>{{ chapter.role }}</dd></div>
      </dl>
      <ul class="origin-flavors" aria-label="代表风味">
        <li v-for="flavor in chapter.flavors" :key="flavor">{{ flavor }}</li>
      </ul>
      <p class="origin-route-caption">{{ chapter.name }} → 杭州烘焙中心</p>
    </template>

    <template v-else>
      <p class="origin-counter">09 / 09 · HANGZHOU</p>
      <h3>{{ chapter.name }}</h3>
      <p class="origin-region">{{ chapter.region }}</p>
      <p class="origin-story origin-story--summary">{{ chapter.story }}</p>
      <ol class="roastery-process" aria-label="杭州烘焙流程">
        <li v-for="step in chapter.process" :key="step">{{ step }}</li>
      </ol>
    </template>
  </article>
</template>

<script setup>
defineProps({
  chapter: { type: Object, required: true },
  number: { type: String, required: true },
  active: { type: Boolean, default: false },
  staticMode: { type: Boolean, default: false }
})
</script>

<style scoped>
.origin-chapter { min-height: 60vh; display: flex; flex-direction: column; justify-content: center; padding-block: 64px; border-top: 1px solid var(--cozy-border); opacity: .38; transition: opacity .3s ease; }
.origin-chapter.is-active, .origin-chapter.is-static { opacity: 1; }
.origin-counter { margin: 0 0 28px; color: var(--cozy-muted); font-size: 12px; letter-spacing: .08em; }
.origin-chapter h3 { margin: 0; font-size: clamp(2rem, 3.4vw, 3rem); line-height: 1.15; font-weight: 600; }
.origin-region { margin: 8px 0 0; color: var(--cozy-primary); font-weight: 600; }
.origin-story { margin: 24px 0 0; color: var(--cozy-muted); font-size: 16px; line-height: 1.8; }
.origin-story--summary { color: var(--cozy-ink); font-size: clamp(1.35rem, 2.4vw, 2rem); }
.origin-facts { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 32px 0 0; padding-block: 24px; border-block: 1px solid var(--cozy-border); }
.origin-facts dt { color: var(--cozy-muted); font-size: 12px; }
.origin-facts dd { margin: 6px 0 0; line-height: 1.55; }
.origin-flavors { display: flex; flex-wrap: wrap; gap: 8px; margin: 24px 0 0; padding: 0; list-style: none; }
.origin-flavors li { padding: 7px 11px; border-radius: 999px; color: var(--cozy-muted); background: var(--cozy-surface); font-size: 13px; }
.origin-route-caption { margin: 28px 0 0; color: var(--cozy-primary); font-size: 13px; font-weight: 600; }
.roastery-process { display: flex; flex-wrap: wrap; gap: 10px 28px; margin: 32px 0 0; padding: 0; list-style: none; }
.roastery-process li:not(:last-child)::after { content: '→'; margin-left: 28px; color: var(--cozy-muted); }

@media (max-width: 820px) {
  .origin-chapter { min-height: 58svh; padding-block: 56px; }
}

@media (max-width: 520px) {
  .origin-facts { grid-template-columns: 1fr; }
  .origin-flavors li { background: transparent; border: 1px solid var(--cozy-border); }
}

@media (prefers-reduced-motion: reduce) {
  .origin-chapter { transition: none; }
}
</style>
```

- [ ] **Step 2: Create the single-state-owner journey shell**

Create `OriginsJourney.vue`:

```vue
<template>
  <section ref="journeyRoot" id="origins" class="origins-journey" aria-labelledby="origins-title">
    <div class="origins-heading origins-shell">
      <h2 id="origins-title">风味从土地开始</h2>
      <p class="origins-slogan">Eight Origins. One Cup.</p>
      <p>沿着赤道咖啡带，探索八处产地如何在杭州汇聚成 Cozy Coffee 的风味语言。</p>
    </div>

    <div class="origins-layout origins-shell">
      <div class="origins-map-sticky">
        <CoffeeBeltMap
          :origins="COFFEE_ORIGINS"
          :active-origin="activeOrigin"
          :visited-origins="visitedOrigins"
          :is-summary="activeChapter?.type === 'summary'"
        />
        <p class="journey-progress" aria-live="polite">
          {{ activeChapter?.type === 'summary' ? '八条航线已汇聚杭州' : `当前探索：${activeOrigin?.name || COFFEE_ORIGINS[0].name}` }}
        </p>
      </div>

      <div class="origin-chapters">
        <OriginChapter
          v-for="(chapter, index) in COFFEE_JOURNEY"
          :key="chapter.id"
          :chapter="chapter"
          :number="String(index + 1).padStart(2, '0')"
          :active="activeChapterId === chapter.id"
          :static-mode="!observerSupported"
        />
      </div>
    </div>

    <div class="origins-to-menu origins-shell">
      <p>最终，成为今天杯中的六种表达。</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import CoffeeBeltMap from './CoffeeBeltMap.vue'
import OriginChapter from './OriginChapter.vue'
import { COFFEE_JOURNEY, COFFEE_ORIGINS } from '@/data/coffeeOrigins'

const activeChapterId = ref(COFFEE_JOURNEY[0].id)
const visitedOrigins = ref(new Set())
const journeyRoot = ref(null)
const observerSupported = ref(true)
let chapterObserver = null

const activeChapter = computed(() =>
  COFFEE_JOURNEY.find(chapter => chapter.id === activeChapterId.value) || COFFEE_JOURNEY[0]
)
const activeOrigin = computed(() =>
  activeChapter.value.type === 'origin' ? activeChapter.value : null
)

function activateChapter(id) {
  activeChapterId.value = id
  const chapter = COFFEE_ORIGINS.find(origin => origin.id === id)
  if (chapter) visitedOrigins.value = new Set(visitedOrigins.value).add(id)
}

onMounted(() => {
  const chapters = journeyRoot.value?.querySelectorAll('[data-origin-id]') || []
  if (!('IntersectionObserver' in window)) {
    observerSupported.value = false
    return
  }

  chapterObserver = new IntersectionObserver(
    entries => {
      const visible = entries
        .filter(entry => entry.isIntersecting)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0]
      if (visible) activateChapter(visible.target.dataset.originId)
    },
    { rootMargin: '-34% 0px -42% 0px', threshold: [0, 0.25, 0.5, 0.75] }
  )
  chapters.forEach(chapter => chapterObserver.observe(chapter))
})

onUnmounted(() => chapterObserver?.disconnect())
</script>
```

- [ ] **Step 3: Add scoped layout and typography styles**

Append to `OriginsJourney.vue`:

```vue
<style scoped>
.origins-journey { padding-block: 128px 0; background: var(--cozy-bg); color: var(--cozy-ink); }
.origins-shell { width: min(1180px, calc(100% - 48px)); margin-inline: auto; }
.origins-heading { padding-bottom: 72px; }
.origins-heading h2 { margin: 0; font-size: clamp(2rem, 4vw, 3.25rem); line-height: 1.15; font-weight: 600; }
.origins-slogan { margin: 14px 0 0; color: var(--cozy-primary); font-size: 15px; font-weight: 650; letter-spacing: -.01em; }
.origins-heading > p:last-child { max-width: 34em; margin: 18px 0 0; color: var(--cozy-muted); font-size: 17px; line-height: 1.75; }
.origins-layout { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(300px, .8fr); gap: 64px; align-items: start; }
.origins-map-sticky { position: sticky; top: calc(var(--nav-height) + 28px); min-width: 0; }
.journey-progress { margin: 16px 0 0; color: var(--cozy-muted); font-size: 13px; }
.origin-chapters { min-width: 0; }
.origins-to-menu { padding-block: 72px 128px; }
.origins-to-menu p { max-width: 16em; margin: 0 auto; font-size: clamp(1.5rem, 3vw, 2.4rem); line-height: 1.45; text-align: center; }

@media (max-width: 820px) {
  .origins-journey { padding-top: 96px; }
  .origins-layout { display: block; width: 100%; }
  .origins-map-sticky { top: var(--nav-height); z-index: 2; height: 40svh; padding: 8px 16px 0; background: var(--cozy-bg); border-bottom: 1px solid var(--cozy-border); }
  .origins-map-sticky :deep(.coffee-map) { height: calc(40svh - 44px); display: grid; align-items: center; }
  .journey-progress { margin: 4px 0 0; text-align: center; }
  .origin-chapters { width: min(100% - 32px, 620px); margin-inline: auto; }
}

@media (max-width: 520px) {
  .origins-shell { width: min(100% - 32px, 520px); }
  .origins-heading { padding-bottom: 48px; }
}
</style>
```

- [ ] **Step 4: Build and verify there is exactly one chapter observer**

Run:

```powershell
npm.cmd run build
rg -n "new IntersectionObserver" src/components/home/OriginsJourney.vue src/components/home/CoffeeBeltMap.vue src/components/home/OriginChapter.vue
```

Expected: build passes; only `OriginsJourney.vue` contains an Origins observer.

- [ ] **Step 5: Commit the journey components**

```powershell
git add cozy-coffee-web/src/components/home/OriginChapter.vue cozy-coffee-web/src/components/home/OriginsJourney.vue
git commit -m "feat: add sticky origin journey chapters"
```

---

### Task 5: Integrate the journey into the current Warm Reserve homepage

**Files:**

- Modify: `cozy-coffee-web/src/views/Home.vue`

- [ ] **Step 1: Import and render the new journey component**

In `Home.vue`:

```js
import OriginsJourney from '@/components/home/OriginsJourney.vue'
```

Replace the entire current `<section id="origins" ...>` block with:

```vue
<OriginsJourney />
```

Update the Hero subtitle to:

```vue
<p>全球八处产区 ｜ 小批次新鲜烘焙</p>
```

The new component owns both Origins transition lines; remove the old Home-level text:

```vue
<p class="warm-transition">三处产地，成为今天杯中的六种表达。</p>
```

- [ ] **Step 2: Delete only the obsolete Origins collage CSS**

Remove these scoped selectors from `Home.vue`:

```text
.origins-composition
.origin-story
.origin-story--lead
.origin-story--portrait
.origin-story--text
.origin-story__image
.origin-story__copy
.origin-story__meta
```

Keep `.warm-transition` because Menu → Membership still uses it.

Remove the obsolete mobile rules targeting `.origins-composition` and `.origin-story--text`.

- [ ] **Step 3: Add a source-contract assertion for the homepage integration**

Append to `scripts/coffee-map.test.mjs`:

```js
import { readFile } from 'node:fs/promises'

test('Home uses OriginsJourney and contains no legacy three-origin collage', async () => {
  const home = await readFile(new URL('../src/views/Home.vue', import.meta.url), 'utf8')
  assert.match(home, /<OriginsJourney\s*\/>/)
  assert.doesNotMatch(home, /origins-composition|origin-story--lead|三处产地/)
  assert.match(home, /全球八处产区/)
})
```

- [ ] **Step 4: Run tests, detector, and build**

Run:

```powershell
npm.cmd run test:coffee-map
node C:\Users\dell\.agents\skills\impeccable\scripts\detect.mjs --json src/views/Home.vue
npm.cmd run build
```

Expected: all coffee-map tests pass; detector returns `[]`; build passes without unresolved image warnings introduced by this feature.

- [ ] **Step 5: Commit homepage integration**

```powershell
git add cozy-coffee-web/src/views/Home.vue cozy-coffee-web/scripts/coffee-map.test.mjs
git commit -m "feat: integrate global origin journey"
```

---

### Task 6: Browser QA, accessibility, and final verification

**Files:**

- Modify only if QA exposes a defect: the files created or modified in Tasks 1–5

- [ ] **Step 1: Start the local app**

Run:

```powershell
npm.cmd run dev -- --host 127.0.0.1 --port 4173
```

Expected: Vite serves `http://127.0.0.1:4173/`.

- [ ] **Step 2: Verify desktop behavior at 1440×1000**

Check all of the following in the in-app browser:

```text
- Origins is a 60/40 sticky layout
- the map stays below the 70px navigation
- chapters activate in the declared 8-origin order
- current country, point, and route are coffee brown
- previously visited routes remain visible at low opacity
- all eight routes are visible in the Hangzhou summary chapter
- Hangzhou label is separate from “Cozy Coffee Roastery”
- Menu immediately follows the two-stage closing copy
- no horizontal scrollbar or console error
```

- [ ] **Step 3: Verify tablet and mobile behavior**

Test `768×1024` and `390×844`:

```text
- map is sticky at approximately 40svh
- only the active origin and Hangzhou labels remain readable
- chapter text follows DOM order
- flavor tags wrap without clipping
- no particle animation exists
- current point pulse is subtle and does not move the map layout
```

- [ ] **Step 4: Verify reduced motion**

Emulate `prefers-reduced-motion: reduce` and confirm:

```text
- routes switch immediately without draw animation
- origin/Hangzhou pulses stop
- content remains visible and the active state still changes
```

- [ ] **Step 5: Verify keyboard and screen-reader semantics**

Confirm:

```text
- map SVG is aria-hidden
- active chapter exposes aria-current="step"
- journey progress text updates through aria-live="polite"
- every chapter has one h3 under the section h2
- no map-only content is required to understand the story
```

- [ ] **Step 6: Run the complete final command set from a clean prompt**

Run:

```powershell
npm.cmd run test:coffee-map
node scripts/homepage-membership.test.mjs
npm.cmd run build
node C:\Users\dell\.agents\skills\impeccable\scripts\detect.mjs --json src/views/Home.vue
git diff --check
git status --short
```

Expected:

```text
- coffee-map tests: all pass
- membership tests: 3 pass
- Vite build: success
- impeccable detector: []
- git diff --check: no whitespace errors
- git status lists only intentional homepage/map changes and pre-existing user-owned changes
```

- [ ] **Step 7: Commit QA fixes, if any**

If QA required changes:

```powershell
git add cozy-coffee-web/src/components/home cozy-coffee-web/src/data cozy-coffee-web/src/utils/coffeeMap.js cozy-coffee-web/src/views/Home.vue cozy-coffee-web/scripts
git commit -m "fix: polish origin journey responsiveness"
```

If QA required no changes, do not create an empty commit.
