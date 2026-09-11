import http, { type Result } from './http'

export interface PlanVO {
  id: number
  code: string
  name: string
  description?: string
  priceMonthly: number
  quotaAiCalls: number
  quotaTokens: number
  quotaMembers: number
  quotaWorkspaces: number
  status: number
  createdAt?: string
}

export interface CreatePlanRequest {
  code: string
  name: string
  description?: string
  priceMonthly: number
  quotaAiCalls: number
  quotaTokens: number
  quotaMembers: number
  quotaWorkspaces: number
}

export interface UpdatePlanRequest {
  name: string
  description?: string
  priceMonthly: number
  quotaAiCalls: number
  quotaTokens: number
  quotaMembers: number
  quotaWorkspaces: number
  status: number
}

export function fetchPlans() {
  return http.get<Result<PlanVO[]>>('/plans')
}

export function createPlan(payload: CreatePlanRequest) {
  return http.post<Result<PlanVO>>('/plans', payload)
}

export function updatePlan(id: number, payload: UpdatePlanRequest) {
  return http.put<Result<PlanVO>>(`/plans/${id}`, payload)
}
