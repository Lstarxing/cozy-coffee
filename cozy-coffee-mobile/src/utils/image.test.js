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

  it('prepends API base to relative paths', () => {
    // VITE_API_BASE_URL from .env.development = http://127.0.0.1:8080/api
    const result = getImageUrl('/uploads/products/latte.png')
    expect(result).toContain('/uploads/products/latte.png')
    expect(result).not.toBe('/uploads/products/latte.png')
  })
})
