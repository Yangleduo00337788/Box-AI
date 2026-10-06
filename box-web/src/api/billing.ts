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
  paymentProvider?: string
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
  audience?: 'PERSONAL' | 'TEAM' | string
  overagePolicy?: string
  byokEnabled?: number
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
  paymentChannel?: string
  paymentUrl?: string | null
  requiresClientConfirm?: boolean
  checkoutFormAction?: string | null
  checkoutForm?: Record<string, string> | null
}

export interface PaymentRecordVO {
  id: number
  invoiceId?: number
  amount?: number
  currency?: string
  channel?: string
  status: string
  paidAt?: string
}

export function fetchBillingOverview() {
  return http.get<Result<BillingOverviewVO>>('/billing/overview')
}

export function subscribePlan(planId: number) {
  return http.post<Result<CreateSubscriptionOrderVO>>('/billing/subscribe', { planId })
}

export function fetchPayment(paymentId: number) {
  return http.get<Result<PaymentRecordVO>>(`/billing/payments/${paymentId}`)
}

export function fetchInvoices() {
  return http.get<Result<BillingInvoiceVO[]>>('/billing/invoices')
}

export function listPlans() {
  return http.get<Result<PlanVO[]>>('/plans')
}
