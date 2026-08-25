import { IMAGE_BASE } from '@/config/image'

export function getImageUrl(url, fallback = '/static/images/default-product.png') {
  if (!url) return fallback
  if (url.startsWith('http') || url.startsWith('/static/')) return url
  return `${IMAGE_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}
