<template>
  <div class="settings-page">
    <h1 class="settings-page__title">审计日志</h1>
    <p class="settings-page__desc">查看工作空间内的登录、资源变更与关键操作记录。</p>

    <div class="settings-card">
      <t-form layout="inline" class="audit-filters" @submit.prevent="loadLogs">
        <t-form-item label="操作">
          <t-input v-model="filters.action" clearable placeholder="如 agent.create" style="width: 180px" />
        </t-form-item>
        <t-form-item label="资源类型">
          <t-input v-model="filters.resourceType" clearable placeholder="如 AGENT" style="width: 140px" />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" type="submit" :loading="loading">查询</t-button>
        </t-form-item>
      </t-form>

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
  { colKey: 'userId', title: '用户 ID', width: 90 },
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
</script>

<style scoped>
.audit-filters {
  margin-bottom: 16px;
}
</style>
