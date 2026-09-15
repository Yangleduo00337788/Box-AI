<template>
  <div class="settings-page">
    <h1 class="settings-page__title">审计日志</h1>
    <p class="settings-page__desc">查看工作空间内的登录、资源变更与关键操作记录。</p>

    <div class="settings-card">
      <div class="audit-filters">
        <label class="audit-filters__item">
          <span>操作</span>
          <t-input v-model="filters.action" clearable placeholder="如 agent.create" />
        </label>
        <label class="audit-filters__item">
          <span>资源类型</span>
          <t-input v-model="filters.resourceType" clearable placeholder="如 AGENT" />
        </label>
        <t-button theme="primary" :loading="loading" @click="loadLogs">查询</t-button>
      </div>

      <t-table
        row-key="id"
        :data="logs"
        :columns="columns"
        :loading="loading"
        bordered
        stripe
        size="small"
        :pagination="pagination"
        @page-change="onPageChange"
      >
        <template #result="{ row }">
          <t-tag :theme="row.result === 'SUCCESS' ? 'success' : 'danger'" variant="light" size="small">
            {{ row.result }}
          </t-tag>
        </template>
        <template #empty>
          <t-empty description="暂无审计记录" />
        </template>
      </t-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import { extractApiError } from '@/api/apiError'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import { listAuditLogs, type AuditLogVO } from '@/api/audit'

const loading = ref(false)
const logs = ref<AuditLogVO[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const filters = reactive({
  action: '',
  resourceType: '',
})

const columns: PrimaryTableCol<AuditLogVO>[] = [
  { colKey: 'createdAt', title: '时间', width: 170 },
  { colKey: 'action', title: '操作', width: 140 },
  { colKey: 'resourceType', title: '资源类型', width: 110 },
  { colKey: 'resourceName', title: '资源', ellipsis: true },
  {
    colKey: 'user',
    title: '用户',
    width: 180,
    ellipsis: true,
    cell: (_, { row }) => row.userNickname || row.userEmail || (row.userId != null ? `#${row.userId}` : '-'),
  },
  { colKey: 'ipAddress', title: 'IP', width: 120 },
  { colKey: 'traceId', title: 'Trace ID', ellipsis: true, width: 140 },
  { colKey: 'result', title: '结果', width: 90 },
]

async function loadLogs() {
  loading.value = true
  try {
    const { data } = await listAuditLogs({
      action: filters.action || undefined,
      resourceType: filters.resourceType || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    const page = data.data
    logs.value = page?.records || []
    pagination.total = page?.total || 0
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '加载审计日志失败'))
  } finally {
    loading.value = false
  }
}

function onPageChange(pageInfo: { current: number; pageSize: number }) {
  pagination.current = pageInfo.current
  pagination.pageSize = pageInfo.pageSize
  loadLogs()
}

onMounted(loadLogs)
useReloadOnWorkspaceChange(loadLogs)
</script>

<style scoped>
.audit-filters {
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}

.audit-filters__item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  flex: 1;
  font-size: 12px;
  color: var(--box-muted);
}

.audit-filters__item :deep(.t-input) {
  width: 100%;
}

.audit-filters > .t-button {
  flex-shrink: 0;
}
</style>
