import { checkCart as checkCartApi, createOrder as createOrderApi, getOrderDetail, acceptOrder as acceptOrderApi, confirmOrder as confirmOrderApi } from '@/api/order'

export function toOrderItems(lines = []) {
  return lines.map(line => ({
    productId: /^\d+$/.test(String(line.productId)) ? Number(line.productId) : line.productId,
    quantity: Number(line.quantity || 1),
    cupSize: line.cupSize || 'STANDARD',
    sugarLevel: line.sugarLevel || 'STANDARD',
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
  constructor(api = { checkCart: checkCartApi, createOrder: createOrderApi, getOrderDetail, acceptOrder: acceptOrderApi, confirmOrder: confirmOrderApi }) {
    this.api = api
  }

  async checkCart(context) {
    const response = await this.api.checkCart({
      items: toOrderItems(context.items),
      couponCode: resolveCouponCode(context),
      addonCouponCodes: context.addonCouponCodes || [],
      diningMethod: context.diningMethod || 'TAKEOUT',
      deliveryAddressId: context.deliveryAddressId || null,
      storeId: context.storeId,
      pickupTime: context.pickupTime
    })
    return response?.data ?? response
  }

  async create(context, idempotencyKey) {
    const addr = context.deliveryAddress || {}
    const response = await this.api.createOrder({
      items: toOrderItems(context.items),
      couponCode: resolveCouponCode(context),
      addonCouponCodes: context.addonCouponCodes || [],
      diningMethod: context.diningMethod || 'TAKEOUT',
      deliveryAddressId: context.deliveryAddressId || null,
      storeId: context.storeId,
      pickupTime: context.pickupTime,
      remark: context.remark || '',
      receiverName: addr.name || '',
      receiverPhone: context.phone || addr.phone || '',
      receiverAddress: [addr.region, addr.detail].filter(Boolean).join(' '),
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

  async accept(orderId) {
    const response = await this.api.acceptOrder(orderId)
    return response?.data ?? response
  }

  async confirm(orderId) {
    const response = await this.api.confirmOrder(orderId)
    return response?.data ?? response
  }
}
