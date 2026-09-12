import http, { type Result } from './http'

export interface BillingOverviewVO {
  period: string
  planName: string
  planPriceMonthly: number
  usedAiCalls: number
  usedTokens: number
  quotaAiCalls?: number
  quotaTokens?: number
  estimatedAmount: number
  currency: string
}

export function fetchBillingOverview() {
  return http.get<Result<BillingOverviewVO>>('/billing/overview')
}
