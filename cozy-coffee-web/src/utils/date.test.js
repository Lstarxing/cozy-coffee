import { describe, expect, it } from 'vitest'
import { formatDate, formatDateTime } from './date'

describe('formatDate', () => {
  it('formats a valid date as YYYY-MM-DD', () => {
    expect(formatDate('2026-08-19T10:30:00')).toBe('2026-08-19')
  })

  it('pads month and day with zeros', () => {
    expect(formatDate('2026-01-05T00:00:00')).toBe('2026-01-05')
  })

  it('returns "-" for empty or invalid input', () => {
    expect(formatDate(null)).toBe('-')
    expect(formatDate('')).toBe('-')
    expect(formatDate('not-a-date')).toBe('-')
    expect(formatDate(undefined)).toBe('-')
  })
})

describe('formatDateTime', () => {
  it('formats date and time as YYYY-MM-DD HH:mm', () => {
    expect(formatDateTime('2026-08-19T09:05:00')).toBe('2026-08-19 09:05')
  })

  it('returns "-" for empty or invalid input', () => {
    expect(formatDateTime(null)).toBe('-')
    expect(formatDateTime('garbage')).toBe('-')
    expect(formatDateTime(undefined)).toBe('-')
  })
})
