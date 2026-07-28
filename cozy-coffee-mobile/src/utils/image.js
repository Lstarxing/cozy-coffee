const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export function getImageUrl(url) {
  if (!url) return '/static/images/default-product.png'
  if (url.startsWith('http')) return url
  return `${API_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}
