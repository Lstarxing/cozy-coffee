import { checkCart as checkCartApi, createOrder as createOrderApi, getOrderDetail } from '@/api/order'

export function toOrderItems(lines = []) {
  return lines.map(line => ({
    productId: /^\d+$/.test(String(line.productId)) ? Number(line.productId) : line.productId,
    quantity: Number(line.quantity || 1),
    cupSize: line.cupSize || 'MEDIUM',
    sugarLevel: line.sugarLevel || 'NORMAL',
    temperature: line.temperature || 'HOT',
    coffeeStrength: line.coffeeStrength || 'NORMAL',
    optionsJson: JSON.stringify({
      skuId: line.skuId || null,
      milkType: line.milkType || null
    }),
    addonsJson: line.addons ? JSON.stringify(line.addons) : undefined
  }))
}

function resolveCouponCode(context) {
  return context.couponCode || context.coupon?.couponCode || context.coupon?.code || context.selectedCouponId || undefined
}

export class OrderService {
  constructor(api = { checkCart: checkCartApi, createOrder: createOrderApi, getOrderDetail }) {
    this.api = api
  }

  async checkCart(context) {
    const response = await this.api.checkCart({
      items: toOrderItems(context.items),
      couponCode: resolveCouponCode(context),
      addonCouponCodes: context.addonCouponCodes || [],
      storeId: context.storeId,
      pickupTime: context.pickupTime
    })
    return response?.data ?? response
  }

  async create(context, idempotencyKey) {
    const response = await this.api.createOrder({
      items: toOrderItems(context.items),
      couponCode: resolveCouponCode(context),
      addonCouponCodes: context.addonCouponCodes || [],
      diningMethod: 'TAKEOUT',
      storeId: context.storeId,
      pickupTime: context.pickupTime,
      remark: context.remark || '',
      previewToken: context.preview?.previewToken || context.preview?.previewVersion
    }, {
      header: { 'Idempotency-Key': idempotencyKey }
    })
    return response?.data ?? response
  }

  async query(orderId) {
    const response = await this.api.getOrderDetail(orderId)
    return response?.data ?? response
  }
}
