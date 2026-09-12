import http, { type Result } from './http'

export interface QuotaSnapshotVO {
  tenantId: number
  planId: number
  planName: string
  period: string
  quotaAiCalls: number
  quotaTokens: number
  quotaMembers: number
  quotaWorkspaces: number
  quotaKnowledgeBases: number
  usedAiCalls: number
  usedTokens: number
  usedMembers: number
  usedWorkspaces: number
  usedKnowledgeBases: number
  remainingAiCalls: number | null
  remainingTokens: number | null
  remainingMembers: number | null
  remainingWorkspaces: number | null
  remainingKnowledgeBases: number | null
}

export function fetchQuota() {
  return http.get<Result<QuotaSnapshotVO>>('/tenant/quota')
}
