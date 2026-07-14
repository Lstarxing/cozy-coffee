import { describe, expect, it } from 'vitest'
import { createCartLineKey, normalizeCartOptions } from './cartLineKey'
import { migrateCartStorage } from './cartMigrations'

describe('cart line identity', () => {
  const base = {
    productId: 12,
    skuId: null,
    cupSize: 'medium',
    temperature: 'hot',
    sugarLevel: 'half sugar',
    milkType: 'whole',
    coffeeStrength: 'normal'
  }

  it('uses a versioned stable canonical key', () => {
    expect(createCartLineKey(base)).toMatch(/^v1:/)
    expect(createCartLineKey(base)).toBe(createCartLineKey({ ...base }))
    expect(normalizeCartOptions(base).skuId).toBe('')
  })

  it('normalizes enums and includes milk type', () => {
    expect(createCartLineKey(base)).toBe(createCartLineKey({ ...base, cupSize: 'MEDIUM', sugarLevel: 'half-sugar' }))
    expect(createCartLineKey(base)).not.toBe(createCartLineKey({ ...base, milkType: 'oat' }))
  })

  it('separates different specifications', () => {
    expect(createCartLineKey(base)).not.toBe(createCartLineKey({ ...base, temperature: 'ice' }))
  })

  it('migrates safe legacy lines and discards ambiguous synthetic ids', () => {
    const result = migrateCartStorage(JSON.stringify([
      { id: 1, name: 'Latte', price: 20, quantity: 2 },
      { id: '1_123456_0', name: 'Legacy spec', price: 24, quantity: 1 }
    ]))
    expect(result.items).toHaveLength(1)
    expect(result.items[0].productId).toBe('1')
    expect(result.discardedItems).toHaveLength(1)
  })
})
