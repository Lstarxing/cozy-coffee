const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export function getImageUrl(url, fallback = '') {
  if (!url) return fallback
  if (url.startsWith('http')) return url
  return `${API_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}
