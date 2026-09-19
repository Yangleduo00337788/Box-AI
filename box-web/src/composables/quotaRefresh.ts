type QuotaRefreshListener = () => void

const listeners = new Set<QuotaRefreshListener>()

/** 订阅额度刷新（避免 http 与 quota 模块循环依赖） */
export function subscribeQuotaRefresh(listener: QuotaRefreshListener) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function requestQuotaRefresh() {
  for (const listener of listeners) {
    listener()
  }
}
