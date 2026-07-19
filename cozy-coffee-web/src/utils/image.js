const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

const DEFAULT_FALLBACK = '/images/menu/floral.svg'

export function getImageUrl(url, fallback = DEFAULT_FALLBACK) {
  if (!url) return fallback
  if (url.startsWith('http')) return url
  return `${API_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}

export function handleImageError(e, fallback = DEFAULT_FALLBACK) {
  if (e.target.dataset.fallbackApplied) return
  e.target.dataset.fallbackApplied = 'true'
  e.target.srcset = ''
  e.target.src = fallback
}
