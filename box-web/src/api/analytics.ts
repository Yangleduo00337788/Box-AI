import http, { type Result } from './http'
import type { QuotaSnapshotVO } from './quota'

export interface RecentAgentVO {
  id: number
  name: string
  status: string
  updatedAt: string
}

export interface RecentConversationVO {
  id: number
  agentId: number
  title?: string
  lastMessageAt?: string
  updatedAt: string
}

export interface RecentWorkflowVO {
  id: number
  name: string
  status: string
  updatedAt: string
}

export interface TopAgentVO {
  agentId: number
  agentName: string
  executionCount: number
  successRate: number
}

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
  periodDays: number
  periodExecutionCount: number
  successRate: number
  avgLatencyMs: number
  recentAgents: RecentAgentVO[]
  recentConversations: RecentConversationVO[]
  recentWorkflows: RecentWorkflowVO[]
  topAgents: TopAgentVO[]
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

export function fetchAnalyticsOverview(days = 7) {
  return http.get<Result<AnalyticsOverviewVO>>('/analytics/overview', { params: { days } })
}

export function fetchAnalyticsTrends(days = 7) {
  return http.get<Result<AnalyticsTrendsVO>>('/analytics/trends', { params: { days } })
}
