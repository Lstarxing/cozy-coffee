import { describe, expect, it } from 'vitest'
import { getImageUrl } from './image'

describe('getImageUrl', () => {
  it('returns the fallback for empty url', () => {
    expect(getImageUrl('')).toBe('/images/menu/floral.svg')
  })

  it('returns the fallback for null / undefined url', () => {
    expect(getImageUrl(null)).toBe('/images/menu/floral.svg')
    expect(getImageUrl(undefined)).toBe('/images/menu/floral.svg')
  })

  it('returns a custom fallback when provided', () => {
    expect(getImageUrl('', '/custom/fallback.jpg')).toBe('/custom/fallback.jpg')
  })

  it('passes through absolute HTTP URLs unchanged', () => {
    const cdn = 'https://cdn.example.com/products/latte.png'
    expect(getImageUrl(cdn)).toBe(cdn)
  })

  it('keeps a leading-slash relative path as-is when image base is empty', () => {
    const result = getImageUrl('/uploads/products/latte.png')
    expect(result).toBe('/uploads/products/latte.png')
  })

  it('inserts a slash between API base and relative path without leading slash', () => {
    const result = getImageUrl('uploads/products/latte.png')
    expect(result).toMatch(/\/uploads\/products\/latte\.png$/)
  })
})
