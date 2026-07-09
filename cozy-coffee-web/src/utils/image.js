const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

const DEFAULT_FALLBACK = 'https://placehold.co/400x400/F5F5F0/8D6E63?text=Coffee'

export function getImageUrl(url, fallback = DEFAULT_FALLBACK) {
  if (!url) return fallback
  if (url.startsWith('http')) return url
  return `${API_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}

export function handleImageError(e, fallback = DEFAULT_FALLBACK) {
  e.target.src = fallback
}
