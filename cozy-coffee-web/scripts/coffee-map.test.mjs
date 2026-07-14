import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildQuadraticRoute,
  normalizedToViewBox,
  MAP_VIEWBOX,
} from '../src/utils/coffeeMap.js'
import {
  COFFEE_ORIGINS,
  COFFEE_JOURNEY,
  HANGZHOU_POINT,
} from '../src/data/coffeeOrigins.js'

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
    firstOrigin.bend,
  )

  assert.match(
    route,
    /^M -?\d+(?:\.\d+)? -?\d+(?:\.\d+)? Q -?\d+(?:\.\d+)? -?\d+(?:\.\d+)? -?\d+(?:\.\d+)? -?\d+(?:\.\d+)?$/,
  )
  assert.ok(route.startsWith(`M ${start.x} ${start.y} Q `))
  assert.ok(route.endsWith(` ${end.x} ${end.y}`))
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
