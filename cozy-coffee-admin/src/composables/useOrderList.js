import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getOrders, getOrderCounts } from '@/api'
import sseService from '@/api/sse'

export function useOrderList() {
  // -- reactive state --
  const loading = ref(false)
  const orders = ref([])
  const hasNewData = ref(false)
  const lastUpdated = ref('')
  const nowTs = ref(Date.now())
  const orderServiceUnavailable = ref(false)

  const filters = reactive({
    keyword: '',
    status: '',
    dateRange: null
  })

  const orderCounts = ref({
    total: 0,
    pending: 0,
    preparing: 0
  })

  const currentPage = ref(1)
  const pageSize = ref(10)

  // -- module-level (non-reactive) --
  let providerProbeCounter = 0
  let lastUnavailableHintAt = 0

  // -- timers --
  let secondTicker = null
  let pollingTimer = null
  let delayedRefreshTimer = null
  let expireSyncTimer = null

  // -- computed --
  const paginatedOrders = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return orders.value.slice(start, start + pageSize.value)
  })

  // =============================================
  // Service degradation helpers
  // =============================================
  const getErrorMessage = (e) => {
    return e?.response?.data?.message || e?.message || ''
  }

  const isOrderProviderUnavailable = (e) => {
    const msg = getErrorMessage(e)
    return msg.includes('No provider available') && msg.includes('OrderService')
  }

  const markProviderUnavailable = () => {
    orderServiceUnavailable.value = true
    const now = Date.now()
    if (now - lastUnavailableHintAt > 15000) {
      ElMessage.warning('订单服务暂不可用，已自动降频重试')
      lastUnavailableHintAt = now
    }
  }

  const markProviderRecovered = () => {
    if (orderServiceUnavailable.value) {
      orderServiceUnavailable.value = false
      providerProbeCounter = 0
      ElMessage.success('订单服务已恢复')
    }
  }

  // =============================================
  // Countdown / expiry helpers
  // =============================================
  const getExpireMs = (row) => {
    if (!row) return null
    if (row.expireAt) {
      const ts = new Date(row.expireAt).getTime()
      if (!Number.isNaN(ts)) return ts
    }
    if (row.createdAt) {
      const createdTs = new Date(row.createdAt).getTime()
      if (!Number.isNaN(createdTs)) {
        return createdTs + 15 * 60 * 1000
      }
    }
    return null
  }

  const getRemainingSeconds = (row) => {
    const expireMs = getExpireMs(row)
    if (!expireMs) return null
    const seconds = Math.floor((expireMs - nowTs.value) / 1000)
    return seconds > 0 ? seconds : 0
  }

  const isTimedOutPending = (row) => {
    return row?.status === 'pending' && getRemainingSeconds(row) === 0
  }

  const normalizeVisibleOrders = (list) => {
    if (!Array.isArray(list)) return []
    if (filters.status === 'pending') {
      return list.filter((o) => !isTimedOutPending(o))
    }
    return list
  }

  const formatCountdown = (row) => {
    const remaining = getRemainingSeconds(row)
    if (remaining == null) return '即将超时'
    if (remaining <= 0) return '即将自动取消'
    const minutes = Math.floor(remaining / 60)
    const seconds = remaining % 60
    return `剩余 ${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }

  const isExpiringSoon = (row) => {
    const remaining = getRemainingSeconds(row)
    return remaining != null && remaining <= 30
  }

  const getDisplayStatus = (row) => {
    if (!row) return ''
    if (isTimedOutPending(row)) {
      return 'cancelled'
    }
    return row.status
  }

  // =============================================
  // Data loading
  // =============================================
  const loadOrders = async ({ force = false, silent = false, fresh = false } = {}) => {
    if (orderServiceUnavailable.value && !force) {
      return
    }
    loading.value = true
    try {
      const params = {
        keyword: filters.keyword,
        status: filters.status,
        startDate: filters.dateRange ? filters.dateRange[0] : null,
        endDate: filters.dateRange ? filters.dateRange[1] : null,
        noCache: fresh ? true : undefined
      }

      if (!filters.dateRange && !filters.keyword) {
        loadOrderCounts({ silent: true })
      }

      const res = await getOrders(params)
      let list = res.data || []

      list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

      orders.value = normalizeVisibleOrders(list)
      lastUpdated.value = dayjs().format('HH:mm:ss')
      markProviderRecovered()
    } catch (e) {
      console.error(e)
      if (isOrderProviderUnavailable(e)) {
        markProviderUnavailable()
      } else if (!silent) {
        ElMessage.error('加载订单失败')
      }
    } finally {
      loading.value = false
    }
  }

  const loadOrderCounts = async ({ silent = false } = {}) => {
    if (orderServiceUnavailable.value) {
      return
    }
    try {
      const res = await getOrderCounts()
      if (res.success) {
        orderCounts.value = res.data
        const total = Object.values(res.data || {})
          .reduce((sum, val) => sum + (Number(val) || 0), 0)
        orderCounts.value.total = total
      }
    } catch (e) {
      if (isOrderProviderUnavailable(e)) {
        markProviderUnavailable()
      } else if (!silent) {
        console.warn('Failed to load counts', e)
      }
    }
  }

  // =============================================
  // Filter / search actions
  // =============================================
  const handleSearch = () => { loadOrders() }

  const resetFilters = () => {
    filters.keyword = ''
    filters.status = ''
    filters.dateRange = null
    loadOrders()
  }

  const handleQuickFilter = (status) => {
    filters.status = status
    loadOrders()
  }

  const handleNewDataRefresh = () => {
    hasNewData.value = false
    loadOrders()
  }

  // =============================================
  // Date formatting
  // =============================================
  const formatDate = (d) => d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '-'

  // =============================================
  // SSE subscription
  // =============================================
  let unsubscribeSse = null

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      loadOrders({ force: true, silent: true })
    }
  }

  // =============================================
  // Lifecycle (timers + SSE + visibility)
  // =============================================
  onMounted(() => {
    loadOrders({ force: true })

    secondTicker = window.setInterval(() => {
      nowTs.value = Date.now()
    }, 1000)

    expireSyncTimer = window.setInterval(() => {
      const hasZeroPending = orders.value.some(
        (o) => o.status === 'pending' && getRemainingSeconds(o) === 0
      )
      if (hasZeroPending) {
        orders.value = normalizeVisibleOrders(orders.value)
        loadOrders({ silent: true, fresh: true })
      }
    }, 2000)

    pollingTimer = window.setInterval(() => {
      if (orderServiceUnavailable.value) {
        providerProbeCounter += 1
        if (providerProbeCounter % 3 !== 0) {
          return
        }
        loadOrders({ force: true, silent: true })
        return
      }
      loadOrders({ silent: true })
    }, 8000)

    document.addEventListener('visibilitychange', handleVisibilityChange)

    unsubscribeSse = sseService.on('new_order', () => {
      hasNewData.value = true
      loadOrders({ force: orderServiceUnavailable.value, silent: true, fresh: true })
      if (delayedRefreshTimer) {
        window.clearTimeout(delayedRefreshTimer)
      }
      delayedRefreshTimer = window.setTimeout(() => {
        loadOrders({ force: orderServiceUnavailable.value, silent: true, fresh: true })
      }, 1200)
    })
  })

  onUnmounted(() => {
    if (unsubscribeSse) unsubscribeSse()
    if (secondTicker) window.clearInterval(secondTicker)
    if (expireSyncTimer) window.clearInterval(expireSyncTimer)
    if (pollingTimer) window.clearInterval(pollingTimer)
    if (delayedRefreshTimer) window.clearTimeout(delayedRefreshTimer)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  // =============================================
  // Public API
  // =============================================
  return {
    // reactive state
    orders,
    orderCounts,
    loading,
    hasNewData,
    lastUpdated,
    orderServiceUnavailable,
    nowTs,
    filters,
    currentPage,
    pageSize,
    // computed
    paginatedOrders,
    // data loading
    loadOrders,
    loadOrderCounts,
    // filter actions
    handleSearch,
    resetFilters,
    handleQuickFilter,
    handleNewDataRefresh,
    // helpers
    getExpireMs,
    getRemainingSeconds,
    isTimedOutPending,
    normalizeVisibleOrders,
    formatCountdown,
    isExpiringSoon,
    getDisplayStatus,
    formatDate
  }
}
