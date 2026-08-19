import { describe, it, expect } from 'vitest'
import {
  ORDER_STATUS,
  ORDER_STATUS_MAP,
  DINING_METHOD,
  DINING_METHOD_MAP,
  ORDER_SPEC,
  EXPIRE_SECONDS,
  getLabel
} from '../../src/constants/order'

describe('ORDER_STATUS', () => {
  it('定义全部订单状态', () => {
    expect(ORDER_STATUS).toEqual({
      PENDING: 'pending',
      PREPARING: 'preparing',
      DELIVERING: 'delivering',
      COMPLETED: 'completed',
      CANCELLED: 'cancelled'
    })
  })
})

describe('ORDER_STATUS_MAP', () => {
  it('每个状态都有对应的中文标签', () => {
    for (const status of Object.values(ORDER_STATUS)) {
      expect(ORDER_STATUS_MAP[status]).toBeDefined()
      expect(typeof ORDER_STATUS_MAP[status].label).toBe('string')
      expect(typeof ORDER_STATUS_MAP[status].tagType).toBe('string')
    }
  })

  it('标签含义正确', () => {
    expect(ORDER_STATUS_MAP[ORDER_STATUS.PENDING].label).toBe('待支付')
    expect(ORDER_STATUS_MAP[ORDER_STATUS.PREPARING].label).toBe('制作中')
    expect(ORDER_STATUS_MAP[ORDER_STATUS.DELIVERING].label).toBe('配送中')
    expect(ORDER_STATUS_MAP[ORDER_STATUS.COMPLETED].label).toBe('已完成')
    expect(ORDER_STATUS_MAP[ORDER_STATUS.CANCELLED].label).toBe('已取消')
  })
})

describe('DINING_METHOD_MAP', () => {
  it('就餐方式映射正确', () => {
    expect(DINING_METHOD_MAP[DINING_METHOD.TAKEOUT]).toBe('自提')
    expect(DINING_METHOD_MAP[DINING_METHOD.DELIVERY]).toBe('外卖')
  })
})

describe('ORDER_SPEC', () => {
  it('温度映射覆盖枚举与常见小写形式', () => {
    expect(ORDER_SPEC.TEMP_MAP[ORDER_SPEC.TEMP.HOT]).toBe('热')
    expect(ORDER_SPEC.TEMP_MAP[ORDER_SPEC.TEMP.COLD]).toBe('冰')
    expect(ORDER_SPEC.TEMP_MAP[ORDER_SPEC.TEMP.WARM]).toBe('温')
    expect(ORDER_SPEC.TEMP_MAP.iced).toBe('冰')
    expect(ORDER_SPEC.TEMP_MAP.hot).toBe('热')
  })

  it('甜度映射覆盖枚举与常见小写形式', () => {
    expect(ORDER_SPEC.SUGAR_MAP[ORDER_SPEC.SUGAR.NONE]).toBe('无糖')
    expect(ORDER_SPEC.SUGAR_MAP[ORDER_SPEC.SUGAR.STANDARD]).toBe('标准甜')
    expect(ORDER_SPEC.SUGAR_MAP.none).toBe('无糖')
    expect(ORDER_SPEC.SUGAR_MAP.full).toBe('标准甜')
  })

  it('杯型映射覆盖枚举与常见小写形式', () => {
    expect(ORDER_SPEC.SIZE_MAP[ORDER_SPEC.SIZE.STANDARD]).toBe('标准杯')
    expect(ORDER_SPEC.SIZE_MAP[ORDER_SPEC.SIZE.LARGE]).toBe('大杯')
    expect(ORDER_SPEC.SIZE_MAP.medium).toBe('中杯')
  })

  it('浓度映射正确', () => {
    expect(ORDER_SPEC.STRENGTH_MAP[ORDER_SPEC.STRENGTH.STRONG]).toBe('加浓')
    expect(ORDER_SPEC.STRENGTH_MAP[ORDER_SPEC.STRENGTH.NORMAL]).toBe('标准浓')
  })
})

describe('EXPIRE_SECONDS', () => {
  it('取餐码过期时间为 900 秒 (15 分钟)', () => {
    expect(EXPIRE_SECONDS).toBe(900)
  })
})

describe('getLabel', () => {
  const map = { pending: '待处理', completed: '已完成' }

  it('能根据 map 取到标签', () => {
    expect(getLabel(map, 'pending')).toBe('待处理')
    expect(getLabel(map, 'completed')).toBe('已完成')
  })

  it('map 中不存在时回退为原始值', () => {
    expect(getLabel(map, 'unknown')).toBe('unknown')
  })

  it('值为空时回退为占位符', () => {
    expect(getLabel(map, '')).toBe('-')
    expect(getLabel(map, null)).toBe('-')
    expect(getLabel(map, undefined)).toBe('-')
  })
})
