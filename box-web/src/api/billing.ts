import http, { type Result } from './http'

export interface BillingOverviewVO {
  period: string
  planName: string
  planPriceMonthly: number
  usedAiCalls: number
  usedTokens: number
  quotaAiCalls?: number
  quotaTokens?: number
  overageAiCalls?: number
  overageTokens?: number
  overagePolicy?: string
  estimatedAmount: number
  currency: string
  paymentEnabled?: boolean
}

export interface BillingInvoiceVO {
  id: number
  tenantId?: number
  tenantName?: string
  invoiceNo: string
  period: string
  planName: string
  subtotal: number
  overageAmount: number
  totalAmount: number
  currency: string
  status: string
  paidAt?: string
  createdAt?: string
}

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
  quotaKnowledgeBases?: number
  overagePolicy?: string
  status: number
}

export interface CreateSubscriptionOrderVO {
  subscriptionId: number
  invoiceId: number
  paymentId: number
  invoiceNo: string
  amount: number
  currency: string
  paymentStatus: string
}

export function fetchBillingOverview() {
  return http.get<Result<BillingOverviewVO>>('/billing/overview')
}

export function subscribePlan(planId: number) {
  return http.post<Result<CreateSubscriptionOrderVO>>('/billing/subscribe', { planId })
}

export function confirmPayment(paymentId: number) {
  return http.post<Result<unknown>>(`/billing/payments/${paymentId}/confirm`)
}

export function fetchInvoices() {
  return http.get<Result<BillingInvoiceVO[]>>('/billing/invoices')
}

export function listPlans() {
  return http.get<Result<PlanVO[]>>('/plans')
}
