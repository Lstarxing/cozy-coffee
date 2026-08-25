const API_BASE = import.meta.env.VITE_API_BASE_URL || ''
const IMAGE_BASE = import.meta.env.VITE_IMAGE_BASE_URL || ''

const DEFAULT_FALLBACK = '/images/menu/floral.svg'

// 图片统一基址：DB 存相对路径（/images/...），前端统一拼接 IMAGE_BASE。
// 本地托管时 IMAGE_BASE=http://localhost:8080（网关 classpath /images）；
// 上线配好 OSS 后改 .env 里 VITE_IMAGE_BASE_URL 为 OSS 域名即可，代码零改动。
export function getImageUrl(url, fallback = DEFAULT_FALLBACK) {
  if (!url) return fallback
  if (typeof url === 'string' && url.startsWith('http')) return url
  return `${IMAGE_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}

export function handleImageError(e, fallback = DEFAULT_FALLBACK) {
  if (e.target.dataset.fallbackApplied) return
  e.target.dataset.fallbackApplied = 'true'
  e.target.srcset = ''
  e.target.src = fallback
}
