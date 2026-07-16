import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

import {
  buildQuadraticRoute,
  normalizedToViewBox,
  MAP_VIEWBOX,
} from '../src/utils/coffeeMap.js'
import {
  createMapCamera,
  projectMapPoint,
  projectPoint,
  isPointInsideSafeArea,
  DESKTOP_MAP_SAFE_AREA,
  MOBILE_MAP_SAFE_AREA,
  MAP_CAMERA_BREAKPOINT,
  MAP_CAMERA_MEDIA_QUERY,
  MAP_CAMERA_PRESETS,
} from '../src/utils/coffeeMapCamera.js'
import { cameraToSvgMatrix } from '../src/utils/coffeeMapSvgRenderer.js'
import {
  COFFEE_ORIGINS,
  COFFEE_JOURNEY,
  HANGZHOU_POINT,
} from '../src/data/coffeeOrigins.js'
import { WORLD_COUNTRY_PATHS } from '../src/data/worldCountryPaths.js'

const HANGZHOU_LABEL_WIDTH_RATIO = Object.freeze({
  desktop: 0.18,
  mobile: 0.25,
})

test('defines eight unique origins that all finish their journey in Hangzhou', () => {
  assert.equal(COFFEE_ORIGINS.length, 8)
  assert.equal(new Set(COFFEE_ORIGINS.map(({ id }) => id)).size, 8)
  assert.equal(
    new Set(COFFEE_ORIGINS.map(({ countryCode }) => countryCode)).size,
    8,
  )
  assert.ok(COFFEE_ORIGINS.every(({ destination }) => destination === 'hangzhou'))
  assert.deepEqual(COFFEE_JOURNEY.at(-1), {
    id: 'hangzhou',
    type: 'summary',
    name: '杭州',
    englishName: 'Hangzhou',
    region: 'Cozy Coffee Roastery',
    story: '世界八处产区，最终在杭州完成属于 Cozy Coffee 的风味语言。',
    process: ['Roasting', 'Blending', 'Cup'],
  })
})

test('keeps normalized origins and converted map points inside their bounds', () => {
  for (const { origin } of COFFEE_ORIGINS) {
    assert.ok(origin.x >= 0 && origin.x <= 1)
    assert.ok(origin.y >= 0 && origin.y <= 1)

    const converted = normalizedToViewBox(origin)
    assert.ok(converted.x >= 0 && converted.x <= MAP_VIEWBOX.width)
    assert.ok(converted.y >= 0 && converted.y <= MAP_VIEWBOX.height)
  }
})

test('builds the first origin route from its exact map point to Hangzhou', () => {
  const firstOrigin = COFFEE_ORIGINS[0]
  const start = normalizedToViewBox(firstOrigin.origin)
  const end = normalizedToViewBox(HANGZHOU_POINT)
  const route = buildQuadraticRoute(
    firstOrigin.origin,
    HANGZHOU_POINT,
    firstOrigin.routeBend,
  )

  assert.match(
    route,
    /^M -?\d+(?:\.\d+)? -?\d+(?:\.\d+)? Q -?\d+(?:\.\d+)? -?\d+(?:\.\d+)? -?\d+(?:\.\d+)? -?\d+(?:\.\d+)?$/,
  )
  assert.ok(route.startsWith(`M ${start.x} ${start.y} Q `))
  assert.ok(route.endsWith(` ${end.x} ${end.y}`))
})

test('derives an immutable camera from focus, anchor and zoom', () => {
  const camera = createMapCamera({
    focus: HANGZHOU_POINT,
    ...MAP_CAMERA_PRESETS.desktop,
  })
  const hangzhou = projectPoint(HANGZHOU_POINT, camera)

  assert.ok(Object.isFrozen(camera))
  assert.ok(Object.isFrozen(camera.anchor))
  assert.equal(camera.zoom, 1)
  assert.ok(hangzhou.xRatio >= 0.72 && hangzhou.xRatio <= 0.75)
  assert.ok(hangzhou.yRatio >= 0.31 && hangzhou.yRatio <= 0.37)
})

test('keeps breakpoint and media query in one camera constant', () => {
  assert.equal(MAP_CAMERA_BREAKPOINT, 820)
  assert.equal(MAP_CAMERA_MEDIA_QUERY, '(max-width: 820px)')
})

test('uses separate desktop and mobile cameras around Hangzhou', () => {
  const desktopCamera = createMapCamera({
    focus: HANGZHOU_POINT,
    ...MAP_CAMERA_PRESETS.desktop,
  })
  const mobileCamera = createMapCamera({
    focus: HANGZHOU_POINT,
    ...MAP_CAMERA_PRESETS.mobile,
  })
  const desktopHangzhou = projectPoint(HANGZHOU_POINT, desktopCamera)
  const mobileHangzhou = projectPoint(HANGZHOU_POINT, mobileCamera)

  assert.ok(desktopHangzhou.xRatio >= 0.72)
  assert.ok(desktopHangzhou.xRatio <= 0.75)
  assert.ok(mobileHangzhou.xRatio >= 0.64)
  assert.ok(mobileHangzhou.xRatio <= 0.68)
  assert.notEqual(desktopCamera.translateX, mobileCamera.translateX)
})

test('keeps all origins inside each named safe area', () => {
  const cases = [
    [MAP_CAMERA_PRESETS.desktop, DESKTOP_MAP_SAFE_AREA],
    [MAP_CAMERA_PRESETS.mobile, MOBILE_MAP_SAFE_AREA],
  ]

  for (const [preset, safeArea] of cases) {
    const camera = createMapCamera({ focus: HANGZHOU_POINT, ...preset })

    for (const { englishName, origin } of COFFEE_ORIGINS) {
      assert.ok(
        isPointInsideSafeArea(projectPoint(origin, camera), safeArea),
        `${englishName} is outside the camera safe area`,
      )
    }
  }
})

test('reserves enough right-side space for the Hangzhou label', () => {
  const desktopCamera = createMapCamera({
    focus: HANGZHOU_POINT,
    ...MAP_CAMERA_PRESETS.desktop,
  })
  const mobileCamera = createMapCamera({
    focus: HANGZHOU_POINT,
    ...MAP_CAMERA_PRESETS.mobile,
  })
  const desktopHangzhou = projectPoint(HANGZHOU_POINT, desktopCamera)
  const mobileHangzhou = projectPoint(HANGZHOU_POINT, mobileCamera)

  assert.ok(
    desktopHangzhou.xRatio + HANGZHOU_LABEL_WIDTH_RATIO.desktop <=
      DESKTOP_MAP_SAFE_AREA.right,
  )
  assert.ok(
    mobileHangzhou.xRatio + HANGZHOU_LABEL_WIDTH_RATIO.mobile <=
      MOBILE_MAP_SAFE_AREA.right,
  )
})

test('keeps SVG rendering outside the camera module', async () => {
  const cameraSource = await readFile(
    new URL('../src/utils/coffeeMapCamera.js', import.meta.url),
    'utf8',
  )
  const camera = createMapCamera({
    focus: HANGZHOU_POINT,
    ...MAP_CAMERA_PRESETS.desktop,
  })

  assert.doesNotMatch(cameraSource, /SVG|matrix\(/i)
  assert.match(cameraToSvgMatrix(camera), /^matrix\(/)
})

test('camera transform never changes route geometry', () => {
  const origin = COFFEE_ORIGINS[0]
  const camera = createMapCamera({
    focus: HANGZHOU_POINT,
    ...MAP_CAMERA_PRESETS.desktop,
  })
  const route = buildQuadraticRoute(
    origin.origin,
    HANGZHOU_POINT,
    origin.routeBend,
  )
  const numbers = route.match(/-?\d+(?:\.\d+)?/g).map(Number)
  const routeStart = projectMapPoint({ x: numbers[0], y: numbers[1] }, camera)
  const routeEnd = projectMapPoint({ x: numbers[4], y: numbers[5] }, camera)
  const expectedStart = projectPoint(origin.origin, camera)
  const expectedEnd = projectPoint(HANGZHOU_POINT, camera)

  assert.deepEqual(routeStart, expectedStart)
  assert.deepEqual(routeEnd, expectedEnd)
})

test('generated world map contains the eight highlighted ISO country codes', () => {
  assert.ok(WORLD_COUNTRY_PATHS.length > 150)
  const codes = new Set(WORLD_COUNTRY_PATHS.map(({ code }) => code))
  for (const code of ['ET', 'KE', 'BR', 'CO', 'GT', 'PA', 'ID', 'CN']) {
    assert.ok(codes.has(code), `missing ${code}`)
  }
  assert.ok(WORLD_COUNTRY_PATHS.every(({ d }) => d.startsWith('M')))
})

test('Home uses OriginsJourney and contains no legacy three-origin collage', async () => {
  const home = await readFile(new URL('../src/views/Home.vue', import.meta.url), 'utf8')
  assert.match(home, /<OriginsJourney\s*\/>/)
  assert.doesNotMatch(home, /origins-composition|origin-story--lead|三处产地/)
  assert.match(home, /全球八处产区/)
})

test('Origins journey keeps natural page scrolling with a sticky map column', async () => {
  const journey = await readFile(
    new URL('../src/components/home/OriginsJourney.vue', import.meta.url),
    'utf8',
  )
  const chapter = await readFile(
    new URL('../src/components/home/OriginChapter.vue', import.meta.url),
    'utf8',
  )

  assert.doesNotMatch(journey, /@wheel|scroll-snap-type|overflow-y:\s*auto/)
  assert.match(journey, /\.origins-left-column\s*\{[^}]*position:\s*sticky/)
  assert.match(journey, /data-origin-sentinel="top"/)
  assert.match(journey, /data-origin-sentinel="bottom"/)
  assert.match(journey, /getProgressTrackLine/)
  assert.match(journey, /scrollDirection !== direction/)
  assert.match(journey, /direction === 'down'/)
  assert.match(journey, /`0px 0px -\$\{viewportHeight - safeLineY\}px 0px`/)
  assert.match(journey, /`-\$\{safeLineY\}px 0px 0px 0px`/)
  assert.match(chapter, /data-origin-sentinel="top"/)
  assert.match(chapter, /data-origin-sentinel="bottom"/)
  assert.doesNotMatch(journey, /lastObservedScrollY|scrollingDown/)
})

test('CoffeeBeltMap separates decoration, camera and label layers', async () => {
  const map = await readFile(
    new URL('../src/components/home/CoffeeBeltMap.vue', import.meta.url),
    'utf8',
  )

  assert.match(map, /class="map-decoration-layer"/)
  assert.match(map, /class="map-camera-layer"/)
  assert.match(map, /:transform="cameraMatrix"/)
  assert.match(map, /class="map-label-layer"/)
  assert.match(map, /activeOriginLabelPoint/)
  assert.match(map, /hangzhouLabelPoint/)
  assert.match(map, /yunnan:\s*Object\.freeze\(\{ dx: 0, dy: 28, anchor: 'middle' \}\)/)
  assert.match(map, /class="map-ending-label"/)
  assert.match(map, /Eight Origins\. One Cup\./)
  assert.match(map, /\.coffee-map\.is-summary \.map-country/)
  assert.match(map, /hangzhou-summary-breathe/)
  assert.doesNotMatch(map, /viewBox="100 0 1000 500"/)
})

test('provides complete display and flavor data for every origin', () => {
  for (const origin of COFFEE_ORIGINS) {
    assert.equal(origin.type, 'origin')
    assert.ok(origin.name)
    assert.ok(origin.englishName)
    assert.ok(origin.region)
    assert.ok(origin.altitude)
    assert.ok(origin.process)
    assert.ok(Array.isArray(origin.varieties) && origin.varieties.length >= 1)
    assert.ok(Array.isArray(origin.flavors))
    assert.equal(origin.flavors.length, 4)
    assert.ok(origin.role)
  }
})
