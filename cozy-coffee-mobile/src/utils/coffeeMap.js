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
  const midpoint = { x: (start.x + end.x) / 2, y: (start.y + end.y) / 2 }
  const normal = { x: -dy / distance, y: dx / distance }
  const control = {
    x: round(midpoint.x + normal.x * distance * bend),
    y: round(midpoint.y + normal.y * distance * bend),
  }
  return { start, control, end }
}

export function drawSvgPath(ctx, d) {
  const parts = d.match(/[MLQZmlqz]|[\d.-]+/g)
  if (!parts) return
  let i = 0
  let cmd = 'M'
  while (i < parts.length) {
    const token = parts[i]
    if (/^[MLQZmlqz]$/.test(token)) { cmd = token.toUpperCase(); i++; continue }
    const num = () => { const v = parseFloat(parts[i]); i++; return v }
    switch (cmd) {
      case 'M': { const x = num(); const y = num(); ctx.moveTo(x, y); cmd = 'L'; break }
      case 'L': { const x = num(); const y = num(); ctx.lineTo(x, y); break }
      case 'Q': { const cx = num(); const cy = num(); const x = num(); const y = num(); ctx.quadraticCurveTo(cx, cy, x, y); break }
      case 'Z': { ctx.closePath(); i++; break }
      default: i++
    }
  }
}
