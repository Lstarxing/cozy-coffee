import { normalizedToViewBox } from './coffeeMap.js'

export const MAP_RENDER_VIEWPORT = Object.freeze({
  width: 1000,
  height: 500,
})

export const MAP_CAMERA_BREAKPOINT = 820
export const MAP_CAMERA_MEDIA_QUERY = `(max-width: ${MAP_CAMERA_BREAKPOINT}px)`

export const MAP_CAMERA_PRESETS = Object.freeze({
  desktop: Object.freeze({
    anchor: Object.freeze({ x: 0.74, y: 0.34 }),
    zoom: 1,
  }),
  mobile: Object.freeze({
    anchor: Object.freeze({ x: 0.66, y: 0.34 }),
    zoom: 1,
  }),
})

export const DESKTOP_MAP_SAFE_AREA = Object.freeze({
  left: 0.12,
  right: 0.92,
  top: 0.08,
  bottom: 0.92,
})

export const MOBILE_MAP_SAFE_AREA = Object.freeze({
  left: 0.08,
  right: 0.94,
  top: 0.08,
  bottom: 0.92,
})

const round = value => Number(value.toFixed(2))

/**
 * @typedef {Readonly<{
 *   anchor: Readonly<{ x: number, y: number }>,
 *   zoom: number,
 *   translateX: number,
 *   translateY: number
 * }>} MapCamera
 */

/** @returns {MapCamera} */
export function createMapCamera({ focus, anchor, zoom = 1 }) {
  const focusPoint = normalizedToViewBox(focus)

  return Object.freeze({
    anchor: Object.freeze({ ...anchor }),
    zoom,
    translateX: round(
      anchor.x * MAP_RENDER_VIEWPORT.width - focusPoint.x * zoom,
    ),
    translateY: round(
      anchor.y * MAP_RENDER_VIEWPORT.height - focusPoint.y * zoom,
    ),
  })
}

export function projectMapPoint(point, camera) {
  const x = point.x * camera.zoom + camera.translateX
  const y = point.y * camera.zoom + camera.translateY

  return {
    x: round(x),
    y: round(y),
    xRatio: x / MAP_RENDER_VIEWPORT.width,
    yRatio: y / MAP_RENDER_VIEWPORT.height,
  }
}

export function projectPoint(point, camera) {
  return projectMapPoint(normalizedToViewBox(point), camera)
}

export const isPointInsideSafeArea = (point, safeArea) =>
  point.xRatio >= safeArea.left &&
  point.xRatio <= safeArea.right &&
  point.yRatio >= safeArea.top &&
  point.yRatio <= safeArea.bottom
