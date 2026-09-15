import http, { type Result } from './http'

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

export function fetchAdminInvoices() {
  return http.get<Result<BillingInvoiceVO[]>>('/billing/invoices')
}
