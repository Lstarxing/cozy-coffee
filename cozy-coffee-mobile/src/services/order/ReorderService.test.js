import { describe, expect, it, vi } from 'vitest'
import { createReorderCartLine, restoreOrderToCart } from './ReorderService'

describe('ReorderService', () => {
  it('restores current products with the original recoverable specifications', async () => {
    const cartStore = { addItem: vi.fn() }
    const order = {
      items: [{
        productId: 7,
        productName: '旧名称',
        quantity: 2,
        cupSize: 'LARGE',
        temperature: 'COLD',
        sugarLevel: 'LESS',
        coffeeStrength: 'STRONG',
        optionsJson: JSON.stringify({ skuId: 'sku-7', milkType: 'OAT' })
      }]
    }
    const menuApi = vi.fn().mockResolvedValue({
      data: [{ id: 7, name: '当前名称', price: 20, category: 'latte', status: 'active' }]
    })

    const result = await restoreOrderToCart({ order, cartStore, menuApi })

    expect(result.restoredQuantity).toBe(2)
    expect(result.invalidItems).toHaveLength(0)
    expect(cartStore.addItem).toHaveBeenCalledWith(expect.objectContaining({
      productId: '7',
      name: '当前名称',
      skuId: 'sku-7',
      cupSize: 'LARGE',
      temperature: 'COLD',
      sugarLevel: 'LESS',
      coffeeStrength: 'STRONG',
      milkType: 'OAT',
      price: 31,
      quantity: 2
    }), 2)
  })

  it('skips offline products and adjusts specifications no longer supported', async () => {
    const cartStore = { addItem: vi.fn() }
    const order = {
      items: [
        { productId: 1, productName: '已下架', quantity: 1 },
        { productId: 2, quantity: 1, cupSize: 'LARGE', temperature: 'HOT', sugarLevel: 'NONE' }
      ]
    }
    const menuApi = vi.fn().mockResolvedValue({ data: [
      { id: 2, name: '限定冷饮', price: 16, status: 'active', sizeType: 'DEFAULT', tempType: 'COLD_ONLY', sugarType: 'NO_SUGAR_ONLY',
        allowedSizes: ['STANDARD'], allowedTemps: ['COLD'], allowedSugars: [] }
    ] })

    const result = await restoreOrderToCart({ order, cartStore, menuApi })

    expect(result.invalidItems).toHaveLength(1)
    expect(result.adjustedItems).toHaveLength(1)
    expect(cartStore.addItem).toHaveBeenCalledWith(expect.objectContaining({
      cupSize: 'STANDARD',
      temperature: 'COLD',
      sugarLevel: ''
    }), 1)
  })

  it('tolerates malformed historical options JSON', () => {
    const restored = createReorderCartLine(
      { productId: 3, quantity: 1, optionsJson: '{bad json' },
      { id: 3, name: '美式', price: 12, category: 'coffee' }
    )

    expect(restored.line.milkType).toBe('WHOLE')
    expect(restored.line.productId).toBe('3')
  })

  it('maps legacy web option names and removes milk from products that no longer support it', () => {
    const restored = createReorderCartLine(
      { temperature: 'iced', sugarLevel: 'full', optionsJson: JSON.stringify({ milkType: 'OAT' }) },
      { id: 4, name: '手冲', price: 28, category: 'soe', tempType: 'HOT_COLD' }
    )

    expect(restored.line.temperature).toBe('COLD')
    expect(restored.line.sugarLevel).toBe('STANDARD')
    expect(restored.line.milkType).toBe('')
    expect(restored.adjusted).toBe(true)
  })
})
