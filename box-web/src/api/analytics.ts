import http, { type Result } from './http'
import type { QuotaSnapshotVO } from './quota'

export interface AnalyticsOverviewVO {
  agentCount: number
  conversationCount: number
  workspaceCount: number
  executionCount: number
  knowledgeBaseCount: number
  toolCount: number
  workflowCount: number
  mcpServerCount: number
  quota: QuotaSnapshotVO
}

export function fetchAnalyticsOverview() {
  return http.get<Result<AnalyticsOverviewVO>>('/analytics/overview')
}
