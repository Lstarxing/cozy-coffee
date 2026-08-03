/**
 * CozyCoffee Origin Map Generator v2
 *
 * Editorial magazine layout: left 38% brand text zone, right 62% world map.
 * Real Natural Earth data + d3-geo projection. Cubic bezier routes arc upward.
 * No text in SVG — labels rendered by Vue DOM overlay.
 *
 * Output: origin-map.svg + origin-map-background.png
 * Canvas: 1875 × 1000 (1.875:1)
 */

const topojson = require('topojson-client');
const d3 = require('d3-geo');
const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

// ── Canvas ──────────────────────────────────────────────
const W = 1875;
const H = 1000;
const SPLIT = 0.38;            // left 38% brand zone
const MAP_LEFT = W * SPLIT;    // ~712
const MAP_PAD = { top: 80, right: 60, bottom: 60, left: 20 };

// ── Brand colors ────────────────────────────────────────
const C = {
  bg:       '#FBF9F7',
  landFill: '#F3EEE8',
  landStroke:'#D8CEC3',
  route:    '#8B5E46',
  nodeFill: '#8B5E46',
  hubOuter: '#753A22',
  hubInner: '#753A22',
};

// ── Load real world land data ────────────────────────────
const landTopo = require('world-atlas/land-110m.json');
const landGeo = topojson.feature(landTopo, landTopo.objects.land);

// ── Projection: map fits inside the right 62% panel ──────
const projection = d3.geoEquirectangular()
  .fitExtent([
    [MAP_LEFT + MAP_PAD.left, MAP_PAD.top],
    [W - MAP_PAD.right, H - MAP_PAD.bottom]
  ], landGeo);

const geoPath = d3.geoPath(projection);

function project(lat, lng) {
  const p = projection([lng, lat]);
  if (!p) throw new Error(`Projection failed for ${lat}, ${lng}`);
  return { x: p[0], y: p[1] };
}

// ── Data ─────────────────────────────────────────────────
const origins = [
  { id: 'ethiopia',   lat: 9.145,    lng: 40.489   },
  { id: 'kenya',      lat: -0.0236,  lng: 37.9062  },
  { id: 'brazil',     lat: -14.235,  lng: -51.925  },
  { id: 'colombia',   lat: 4.5709,   lng: -74.2973 },
  { id: 'guatemala',  lat: 15.7835,  lng: -90.2308 },
  { id: 'panama',     lat: 8.538,    lng: -80.7821 },
  { id: 'indonesia',  lat: -0.7893,  lng: 113.9213 },
  { id: 'yunnan',     lat: 25.0453,  lng: 101.833  },
];

const hangzhou = { lat: 30.2741, lng: 120.1551 };

const hz = project(hangzhou.lat, hangzhou.lng);
const points = origins.map(o => {
  const p = project(o.lat, o.lng);
  return { ...o, x: p.x, y: p.y };
});

// ── Cubic bezier route (arches UPWARD) ──────────────────
function cubicRoute(ox, oy) {
  const dx = hz.x - ox;
  const dy = hz.y - oy;
  // control points pull the curve above both endpoints
  const arcY = Math.min(oy, hz.y) - 120;
  const cx1 = ox + dx * 0.25;
  const cy1 = arcY;
  const cx2 = ox + dx * 0.75;
  const cy2 = arcY;
  return `M${ox.toFixed(1)},${oy.toFixed(1)} C${cx1.toFixed(1)},${cy1.toFixed(1)} ${cx2.toFixed(1)},${cy2.toFixed(1)} ${hz.x.toFixed(1)},${hz.y.toFixed(1)}`;
}

// ── Build SVG ────────────────────────────────────────────
function buildSvg() {
  const parts = [];

  parts.push(`<?xml version="1.0" encoding="UTF-8"?>`);
  parts.push(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">`);

  // Background
  parts.push(`<rect width="${W}" height="${H}" fill="${C.bg}"/>`);

  // World landmasses (shifted into right panel)
  parts.push(`<g id="landmasses">`);
  parts.push(`<path d="${geoPath(landGeo)}" fill="${C.landFill}" stroke="${C.landStroke}" stroke-width="0.8" stroke-opacity="0.25"/>`);
  parts.push(`</g>`);

  // Routes: Yunnan special, others standard
  parts.push(`<g id="routes" fill="none" stroke="${C.route}">`);
  for (const p of points) {
    const isYunnan = p.id === 'yunnan';
    const sw = isYunnan ? '2' : '1';
    const op = isYunnan ? '0.55' : '0.25';
    parts.push(`<path d="${cubicRoute(p.x, p.y)}" stroke-width="${sw}" stroke-opacity="${op}"/>`);
  }
  parts.push(`</g>`);

  // Origin nodes
  parts.push(`<g id="origin-nodes">`);
  for (const p of points) {
    parts.push(`<circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="4" fill="${C.nodeFill}"/>`);
    parts.push(`<circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="1.5" fill="#FFFFFF" fill-opacity="0.6"/>`);
  }
  parts.push(`</g>`);

  // Hangzhou hub
  parts.push(`<g id="hangzhou-hub">`);
  parts.push(`<circle cx="${hz.x.toFixed(1)}" cy="${hz.y.toFixed(1)}" r="16" fill="none" stroke="${C.hubOuter}" stroke-width="1.5"/>`);
  parts.push(`<circle cx="${hz.x.toFixed(1)}" cy="${hz.y.toFixed(1)}" r="6" fill="${C.hubInner}"/>`);
  parts.push(`<circle cx="${hz.x.toFixed(1)}" cy="${hz.y.toFixed(1)}" r="2" fill="#FFFFFF" fill-opacity="0.7"/>`);
  parts.push(`</g>`);

  parts.push(`</svg>`);
  return parts.join('\n');
}

// ── Main ─────────────────────────────────────────────────
const outDir = path.resolve(__dirname, '..', 'cozy-coffee-mobile', 'src', 'static', 'images');

if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}

const svg = buildSvg();
const svgPath = path.join(outDir, 'origin-map.svg');
fs.writeFileSync(svgPath, svg, 'utf-8');
console.log(`✓ SVG written → ${svgPath} (${(Buffer.byteLength(svg) / 1024).toFixed(1)} KB)`);

// Convert to PNG
sharp(Buffer.from(svg))
  .png({ quality: 100, compressionLevel: 6 })
  .toFile(path.join(outDir, 'origin-map-background.png'))
  .then(info => {
    console.log(`✓ PNG written → ${path.join(outDir, 'origin-map-background.png')} (${(info.size / 1024).toFixed(1)} KB, ${info.width}×${info.height})`);
  })
  .catch(err => {
    console.error('PNG conversion failed:', err.message);
    console.log('SVG is still available at:', svgPath);
  });
