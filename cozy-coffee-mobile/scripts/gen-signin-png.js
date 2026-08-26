// 一键生成签到图标 PNG — 精确复刻 web 端 SigninWidget 的 SVG path
// 咖啡豆：圆形外轮廓 + C 形内弧挖空 → bean-active(奶油黄 #FBEEA8) / bean-muted(浅棕灰 #BCAAA4)
// 礼盒：stroke 线框礼物，只有一张(奶油黄 #FBEEA8)，底座渐变棕矩形由 CSS 控制
const sharp = require('sharp')
const path = require('path')

const OUT = path.resolve(__dirname, '../src/static/images/signin')

// web 端 bean 的 SVG path（fill=currentColor）
const BEAN_PATH =
  'M12,2 C6.5,2 2,6.5 2,12 C2,17.5 6.5,22 12,22 C17.5,22 22,17.5 22,12 C2,6.5 17.5,2 12,2 Z ' +
  'M12.5,5 C12.5,5 14,8 14,12 C14,16 12.5,19 12.5,19 C11,19 8,16 8,12 C8,8 11,5 12.5,5 Z'

// web 端 gift 的 SVG path（stroke=currentColor）
function giftSvg(color) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <polyline points="20 12 20 22 4 22 4 12"></polyline>
    <rect x="2" y="7" width="20" height="5"></rect>
    <line x1="12" y1="22" x2="12" y2="7"></line>
    <path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"></path>
    <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"></path>
  </svg>`
}

function beanSvg(color) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24" fill="${color}">
    <path d="${BEAN_PATH}" fill-rule="evenodd"/>
  </svg>`
}

async function gen(name, svg) {
  const buf = Buffer.from(svg)
  await sharp(buf)
    .resize(96, 96, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toFile(path.join(OUT, name))
  console.log('wrote', name)
}

;(async () => {
  await gen('bean-active.png', beanSvg('#FBEEA8'))
  await gen('bean-muted.png',  beanSvg('#BCAAA4'))
  await gen('gift.png',        giftSvg('#FBEEA8'))
})()