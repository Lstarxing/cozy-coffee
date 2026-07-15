export const MAP_VIEWBOX = Object.freeze({ width: 1000, height: 500 })

const round = (value) => Number(value.toFixed(2))

export const normalizedToViewBox = (point) => ({
  x: round(point.x * MAP_VIEWBOX.width),
  y: round(point.y * MAP_VIEWBOX.height),
})

export const buildQuadraticRoute = (origin, destination, bend = -0.16) => {
  const start = normalizedToViewBox(origin)
  const end = normalizedToViewBox(destination)
  const dx = end.x - start.x
  const dy = end.y - start.y
  const distance = Math.hypot(dx, dy) || 1
  const midpoint = {
    x: (start.x + end.x) / 2,
    y: (start.y + end.y) / 2,
  }
  const normal = {
    x: -dy / distance,
    y: dx / distance,
  }
  const control = {
    x: round(midpoint.x + normal.x * distance * bend),
    y: round(midpoint.y + normal.y * distance * bend),
  }

  return `M ${start.x} ${start.y} Q ${control.x} ${control.y} ${end.x} ${end.y}`
}
