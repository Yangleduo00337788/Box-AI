<template>
  <div class="billing-invoices-page admin-page">
    <page-header title="账单对账" desc="平台订阅账单与超量费用汇总，供财务核对。" />

    <t-card :bordered="false" class="admin-card">
      <t-table row-key="id" :data="invoices" :columns="columns" :loading="loading" hover>
        <template #empty>
          <t-empty description="暂无账单记录" />
        </template>
      </t-table>
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import { Tag } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import { fetchAdminInvoices, type BillingInvoiceVO } from '@/api/billing'
import { formatDateTime } from '@/utils/datetime'

const loading = ref(false)
const invoices = ref<BillingInvoiceVO[]>([])

const statusTheme: Record<string, 'default' | 'success' | 'warning' | 'danger'> = {
  PAID: 'success',
  OPEN: 'warning',
  VOID: 'default',
}

const statusLabel: Record<string, string> = {
  PAID: '已支付',
  OPEN: '待支付',
  VOID: '作废',
}

const columns: PrimaryTableCol<BillingInvoiceVO>[] = [
  { colKey: 'invoiceNo', title: '账单号', minWidth: 160 },
  { colKey: 'tenantName', title: '租户', minWidth: 140, cell: (_, { row }) => row.tenantName || '-' },
  { colKey: 'period', title: '账期', width: 100 },
  { colKey: 'planName', title: '套餐', minWidth: 120 },
  {
    colKey: 'subtotal',
    title: '套餐费',
    width: 100,
    cell: (_, { row }) => `¥${Number(row.subtotal || 0).toFixed(2)}`,
  },
  {
    colKey: 'overageAmount',
    title: '超量费',
    width: 100,
    cell: (_, { row }) => `¥${Number(row.overageAmount || 0).toFixed(2)}`,
  },
  {
    colKey: 'totalAmount',
    title: '合计',
    width: 100,
    cell: (_, { row }) => `¥${Number(row.totalAmount || 0).toFixed(2)}`,
  },
  {
    colKey: 'status',
    title: '状态',
    width: 90,
    cell: (_, { row }) =>
      h(Tag, { theme: statusTheme[row.status] || 'default', variant: 'light' }, () =>
        statusLabel[row.status] || row.status,
      ),
  },
  {
    colKey: 'paidAt',
    title: '支付时间',
    width: 180,
    cell: (_, { row }) => formatDateTime(row.paidAt),
  },
  {
    colKey: 'createdAt',
    title: '创建时间',
    width: 180,
    cell: (_, { row }) => formatDateTime(row.createdAt),
  },
]

async function loadInvoices() {
  loading.value = true
  try {
    const { data } = await fetchAdminInvoices()
    invoices.value = data.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadInvoices)
</script>
