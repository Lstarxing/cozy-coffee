const IMAGE_BASE = import.meta.env.VITE_IMAGE_BASE_URL || ''

// 图片统一基址：DB 存相对路径（/uploads /images），前端统一拼接 IMAGE_BASE。
// 本地托管时 IMAGE_BASE=http://localhost:8080（网关 classpath /images + /uploads）；
// 上线配好 OSS 后改 .env 里 VITE_IMAGE_BASE_URL 为 OSS 域名即可，代码零改动。
export function getImageUrl(url, fallback = '') {
  if (!url) return fallback
  if (url.startsWith('http')) return url
  return `${IMAGE_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}
