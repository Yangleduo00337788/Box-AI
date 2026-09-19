import { computed, onMounted, onUnmounted, ref, type ComputedRef, type Ref } from 'vue'
import { fetchQuota, type QuotaSnapshotVO } from '@/api/quota'
import { subscribeQuotaRefresh } from '@/composables/quotaRefresh'

const POLL_MS = 45_000

let pollTimer: ReturnType<typeof setInterval> | undefined
let subscriberCount = 0
let unsubscribeRefresh: (() => void) | undefined

const quota = ref<QuotaSnapshotVO | null>(null)
const loading = ref(false)

export function quotaUsagePercent(used: number, total: number) {
  if (!total || total <= 0) return 0
  return Math.min(100, Math.round((used / total) * 100))
}

export function formatQuotaNumber(value: number) {
  const n = Number(value || 0)
  if (n >= 1_000_000) {
    return `${(n / 1_000_000).toFixed(n >= 10_000_000 ? 0 : 1)}M`
  }
  if (n >= 10_000) {
    return `${(n / 1_000).toFixed(n >= 100_000 ? 0 : 1)}k`
  }
  return n.toLocaleString()
}

export async function refreshTenantQuota() {
  const token = localStorage.getItem('box.token')
  if (!token) {
    quota.value = null
    return
  }
  loading.value = true
  try {
    const { data } = await fetchQuota()
    quota.value = data.data
  } catch {
    /* 未登录或网络异常时保留上次快照 */
  } finally {
    loading.value = false
  }
}

function onWindowFocus() {
  void refreshTenantQuota()
}

function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    void refreshTenantQuota()
  }
}

function startPolling() {
  subscriberCount += 1
  if (subscriberCount > 1) return
  void refreshTenantQuota()
  pollTimer = setInterval(() => void refreshTenantQuota(), POLL_MS)
  window.addEventListener('focus', onWindowFocus)
  document.addEventListener('visibilitychange', onVisibilityChange)
  unsubscribeRefresh = subscribeQuotaRefresh(() => {
    void refreshTenantQuota()
  })
}

function stopPolling() {
  subscriberCount = Math.max(0, subscriberCount - 1)
  if (subscriberCount > 0) return
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = undefined
  }
  window.removeEventListener('focus', onWindowFocus)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  unsubscribeRefresh?.()
  unsubscribeRefresh = undefined
}

export interface TenantQuotaView {
  quota: Ref<QuotaSnapshotVO | null>
  loading: Ref<boolean>
  tokenPercent: ComputedRef<number>
  aiCallsPercent: ComputedRef<number>
  tokenSummary: ComputedRef<string>
  aiCallsSummary: ComputedRef<string>
  refreshQuota: () => Promise<void>
}

/** 租户套餐额度（轮询 + 聚焦刷新 + 业务事件触发） */
export function useTenantQuota(): TenantQuotaView {
  onMounted(startPolling)
  onUnmounted(stopPolling)

  const tokenPercent = computed(() => {
    const q = quota.value
    if (!q) return 0
    return quotaUsagePercent(q.usedTokens, q.quotaTokens)
  })

  const aiCallsPercent = computed(() => {
    const q = quota.value
    if (!q) return 0
    return quotaUsagePercent(q.usedAiCalls, q.quotaAiCalls)
  })

  const tokenSummary = computed(() => {
    const q = quota.value
    if (!q) return '—'
    return `${formatQuotaNumber(q.usedTokens)} / ${formatQuotaNumber(q.quotaTokens)}`
  })

  const aiCallsSummary = computed(() => {
    const q = quota.value
    if (!q) return '—'
    return `${formatQuotaNumber(q.usedAiCalls)} / ${formatQuotaNumber(q.quotaAiCalls)}`
  })

  return {
    quota,
    loading,
    tokenPercent,
    aiCallsPercent,
    tokenSummary,
    aiCallsSummary,
    refreshQuota: refreshTenantQuota,
  }
}
