<template>
  <div class="audit-page admin-page">
    <page-header title="审计日志" desc="查看全平台操作记录，包括资源变更、登录与关键治理动作。" />

    <t-card :bordered="false" class="admin-card">
      <div class="admin-toolbar">
        <t-input v-model="filters.action" clearable placeholder="操作，如 agent.create" style="width: 200px" />
        <t-input v-model="filters.resourceType" clearable placeholder="资源类型，如 AGENT" style="width: 180px" />
        <t-date-range-picker
          v-model="dateRange"
          enable-time-picker
          allow-input
          clearable
          format="YYYY-MM-DD HH:mm:ss"
          value-type="YYYY-MM-DD HH:mm:ss"
          placeholder="时间范围"
          style="width: 360px"
        />
        <t-button theme="primary" :loading="loading" @click="onSearch">查询</t-button>
      </div>
      <t-table
        row-key="id"
        :data="logs"
        :columns="columns"
        :loading="loading"
        hover
        :pagination="pagination"
        @page-change="onPageChange"
        @row-click="onRowClick"
      >
        <template #empty>
          <t-empty description="暂无审计记录" />
        </template>
      </t-table>
    </t-card>

    <t-drawer v-model:visible="detailVisible" header="审计详情" size="480px">
      <t-descriptions v-if="activeLog" :column="1" bordered>
        <t-descriptions-item label="时间">{{ formatDateTime(activeLog.createdAt) }}</t-descriptions-item>
        <t-descriptions-item label="操作">{{ activeLog.action }}</t-descriptions-item>
        <t-descriptions-item label="资源类型">{{ activeLog.resourceType || '-' }}</t-descriptions-item>
        <t-descriptions-item label="资源">{{ activeLog.resourceName || activeLog.resourceId || '-' }}</t-descriptions-item>
        <t-descriptions-item label="工作空间">{{ activeLog.workspaceId ?? '-' }}</t-descriptions-item>
        <t-descriptions-item label="用户">{{ userLabel(activeLog) }}</t-descriptions-item>
        <t-descriptions-item label="IP">{{ activeLog.ipAddress || '-' }}</t-descriptions-item>
        <t-descriptions-item label="Trace ID">{{ activeLog.traceId || '-' }}</t-descriptions-item>
        <t-descriptions-item label="结果">{{ activeLog.result }}</t-descriptions-item>
        <t-descriptions-item label="详情">
          <pre class="audit-detail">{{ formatDetail(activeLog.detail) }}</pre>
        </t-descriptions-item>
      </t-descriptions>
    </t-drawer>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { Tag } from 'tdesign-vue-next'
import type { PageInfo, PrimaryTableCol, TableRowData } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import { listAuditLogs, type AuditLogVO } from '@/api/audit'
import { formatDateTime } from '@/utils/datetime'

const loading = ref(false)
const logs = ref<AuditLogVO[]>([])
const dateRange = ref<string[]>([])
const detailVisible = ref(false)
const activeLog = ref<AuditLogVO | null>(null)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})
const filters = reactive({
  action: '',
  resourceType: '',
})

function userLabel(row: AuditLogVO) {
  if (row.userNickname && row.userEmail) return `${row.userNickname}（${row.userEmail}）`
  return row.userNickname || row.userEmail || (row.userId != null ? `#${row.userId}` : '-')
}

const columns: PrimaryTableCol<AuditLogVO>[] = [
  {
    colKey: 'createdAt',
    title: '时间',
    width: 180,
    cell: (_, { row }) => formatDateTime(row.createdAt),
  },
  { colKey: 'action', title: '操作', width: 160 },
  { colKey: 'resourceType', title: '资源类型', width: 120 },
  { colKey: 'resourceName', title: '资源', ellipsis: true, minWidth: 160 },
  {
    colKey: 'user',
    title: '用户',
    minWidth: 180,
    cell: (_, { row }) => userLabel(row),
  },
  { colKey: 'workspaceId', title: '工作空间', width: 110 },
  { colKey: 'ipAddress', title: 'IP', width: 130 },
  { colKey: 'traceId', title: 'Trace ID', ellipsis: true, width: 160 },
  {
    colKey: 'result',
    title: '结果',
    width: 90,
    cell: (_, { row }) =>
      h(Tag, { theme: row.result === 'SUCCESS' ? 'success' : 'danger', variant: 'light' }, () => row.result),
  },
]

function formatDetail(detail?: string) {
  if (!detail) return '无'
  try {
    return JSON.stringify(JSON.parse(detail), null, 2)
  } catch {
    return detail
  }
}

async function loadLogs() {
  loading.value = true
  try {
    const { data } = await listAuditLogs({
      action: filters.action || undefined,
      resourceType: filters.resourceType || undefined,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1],
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    const page = data.data
    logs.value = page?.records || []
    pagination.total = page?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pagination.current = 1
  void loadLogs()
}

function onPageChange(pageInfo: PageInfo) {
  pagination.current = pageInfo.current
  pagination.pageSize = pageInfo.pageSize
  void loadLogs()
}

function onRowClick(context: { row: TableRowData }) {
  activeLog.value = context.row as AuditLogVO
  detailVisible.value = true
}

onMounted(loadLogs)
</script>

<style scoped>
.audit-detail {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.5;
  color: var(--td-text-color-secondary);
}
</style>
