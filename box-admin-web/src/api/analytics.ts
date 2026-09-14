import http, { type Result } from './http'

export interface PlatformTopTenantVO {
  tenantId: number
  tenantName: string
  aiCalls: number
  tokens: number
}

export interface PlatformAnalyticsOverviewVO {
  periodDays: number
  usagePeriod: string
  tenantCount: number
  periodExecutionCount: number
  totalExecutionCount: number
  successRate: number
  avgLatencyMs: number
  periodTokens: number
  periodAiCalls: number
  monthTokens: number
  monthAiCalls: number
  topTenants: PlatformTopTenantVO[]
}

export interface AnalyticsTrendPointVO {
  date: string
  executionCount: number
  successRate: number
  avgLatencyMs: number
}

export interface AnalyticsTrendsVO {
  periodDays: number
  points: AnalyticsTrendPointVO[]
}

export function fetchPlatformAnalyticsOverview(days = 7) {
  return http.get<Result<PlatformAnalyticsOverviewVO>>('/analytics/overview', { params: { days } })
}

export function fetchPlatformAnalyticsTrends(days = 7) {
  return http.get<Result<AnalyticsTrendsVO>>('/analytics/trends', { params: { days } })
}
