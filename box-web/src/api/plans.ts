import http, { type Result } from './http'
import type { PlanVO } from './billing'

export function fetchPlans() {
  return http.get<Result<PlanVO[]>>('/plans')
}

export type { PlanVO }
