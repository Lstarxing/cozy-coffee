export function getImageUrl(url) {
  if (!url) return '/static/images/default-product.png'
  if (url.startsWith('http')) return url
  return url
}
