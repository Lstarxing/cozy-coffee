import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createApp } from 'vue'

// 网络与 SSE 的替身必须先于 import 建立（vi.mock 会被提升，故用 vi.hoisted 避免 TDZ）
const { getOrders, getOrderCounts } = vi.hoisted(() => ({
  getOrders: vi.fn(),
  getOrderCounts: vi.fn()
}))
vi.mock('@/api', () => ({ getOrders, getOrderCounts }))
vi.mock('@/api/sse', () => ({ default: { on: () => () => {} } }))

import { useOrderList } from '@/composables/useOrderList'

/**
 * 守护「显式刷新必须回源」这件事。
 *
 * 管理端订单列表在网关侧有 30s(±8s) 的 Redis 缓存。若刷新也走缓存，用户点「刷新」看到的是
 * 最多 38 秒前的状态，只能等 8s 轮询把缓存熬过期 —— 现场表现就是
 * 「手动刷新不更新，过十几秒自己变了」。所以 fresh=true 必须映射成 noCache=true。
 */
describe('useOrderList 的缓存语义', () => {
  let mountedApp = null

  beforeEach(() => {
    getOrders.mockReset()
    getOrderCounts.mockReset()
    getOrders.mockResolvedValue({ data: [] })
    getOrderCounts.mockResolvedValue({ success: true, data: {} })
  })

  // 必须真挂一个组件实例：否则 onMounted/onUnmounted 不会注册，
  // 既会打 Vue 警告，也会让轮询定时器留着泄漏到别的用例。
  afterEach(() => {
    if (mountedApp) {
      mountedApp.unmount()
      mountedApp = null
    }
  })

  const mountComposable = () => {
    let composable
    mountedApp = createApp({
      setup() {
        composable = useOrderList()
        return () => null
      }
    })
    mountedApp.mount(document.createElement('div'))
    return composable
  }

  // 挂载时 onMounted 会强制加载一次，故断言只取最后一次调用
  it('显式刷新（fresh）带 noCache=true 回源', async () => {
    const { loadOrders } = mountComposable()

    await loadOrders({ fresh: true })

    expect(getOrders.mock.calls.at(-1)[0].noCache).toBe(true)
  })

  it('轮询（silent）不下发 noCache，走网关缓存', async () => {
    const { loadOrders } = mountComposable()

    await loadOrders({ silent: true })

    expect(getOrders.mock.calls.at(-1)[0].noCache).toBeUndefined()
  })
})
