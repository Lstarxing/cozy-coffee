import { describe, expect, it } from 'vitest'
import { getImageUrl } from './image'

describe('getImageUrl', () => {
  it('returns fallback for empty url', () => {
    const result = getImageUrl('')
    expect(result).toBe('/static/images/default-product.png')
  })

  it('returns fallback for null or undefined url', () => {
    expect(getImageUrl(null)).toBe('/static/images/default-product.png')
    expect(getImageUrl(undefined)).toBe('/static/images/default-product.png')
  })

  it('passes through absolute HTTP URLs unchanged', () => {
    const cdnUrl = 'https://cdn.example.com/products/latte.png'
    expect(getImageUrl(cdnUrl)).toBe(cdnUrl)
  })

  it('passes through /static local packaged assets unchanged', () => {
    expect(getImageUrl('/static/images/default-avatar.png')).toBe('/static/images/default-avatar.png')
  })

  it('prepends IMAGE_BASE to relative paths', () => {
    // IMAGE_BASE from config/image.js = http://127.0.0.1:8080
    const result = getImageUrl('/images/seed/coffee.png')
    expect(result).toBe('http://127.0.0.1:8080/images/seed/coffee.png')
  })
})
