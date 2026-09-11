<template>
  <div>
    <page-header title="执行记录" desc="查看 Agent 与 Workflow 的运行历史、耗时与 Trace 详情" />

    <t-loading :loading="loading" size="small">
      <t-table
        row-key="id"
        :data="executions"
        :columns="columns"
        bordered
        stripe
        hover
        @row-click="openDetail"
      >
        <template #status="{ row }">
          <t-tag
            :theme="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'default'"
            variant="light"
            size="small"
          >
            {{ statusLabel(row.status) }}
          </t-tag>
        </template>
        <template #type="{ row }">
          {{ row.executionType === 'WORKFLOW' ? '工作流' : '智能体' }}
        </template>
        <template #empty>
          <t-empty description="暂无执行记录" />
        </template>
      </t-table>
    </t-loading>

    <t-drawer v-model:visible="detailVisible" header="执行详情" size="480px" :footer="false">
      <template v-if="detail">
        <t-descriptions :column="1" bordered>
          <t-descriptions-item label="编号">{{ detail.executionNo }}</t-descriptions-item>
          <t-descriptions-item label="类型">
            {{ detail.executionType === 'WORKFLOW' ? '工作流' : '智能体' }}
          </t-descriptions-item>
          <t-descriptions-item label="状态">{{ statusLabel(detail.status) }}</t-descriptions-item>
          <t-descriptions-item label="Agent ID">{{ detail.agentId ?? '—' }}</t-descriptions-item>
          <t-descriptions-item label="耗时">
            {{ detail.durationMs != null ? `${detail.durationMs} ms` : '—' }}
          </t-descriptions-item>
          <t-descriptions-item label="Token">{{ detail.totalTokens ?? '—' }}</t-descriptions-item>
          <t-descriptions-item label="开始">{{ detail.startedAt || '—' }}</t-descriptions-item>
          <t-descriptions-item label="结束">{{ detail.finishedAt || '—' }}</t-descriptions-item>
        </t-descriptions>

        <section v-if="detail.inputJson" class="detail-block">
          <h3>输入</h3>
          <pre>{{ formatJson(detail.inputJson) }}</pre>
        </section>
        <section v-if="detail.outputJson" class="detail-block">
          <h3>输出</h3>
          <pre>{{ formatJson(detail.outputJson) }}</pre>
        </section>
        <section v-if="detail.errorMessage" class="detail-block detail-block--error">
          <h3>错误</h3>
          <pre>{{ detail.errorMessage }}</pre>
        </section>
      </template>
    </t-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PrimaryTableCol, TableRowData } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import { extractApiError } from '@/api/apiError'
import { getExecution, listExecutions, type ExecutionVO } from '@/api/execution'

const loading = ref(false)
const executions = ref<ExecutionVO[]>([])
const detailVisible = ref(false)
const detail = ref<ExecutionVO | null>(null)

const columns: PrimaryTableCol<ExecutionVO>[] = [
  { colKey: 'executionNo', title: '编号', ellipsis: true, width: 180 },
  { colKey: 'type', title: '类型', width: 90 },
  { colKey: 'status', title: '状态', width: 90 },
  { colKey: 'durationMs', title: '耗时(ms)', width: 100 },
  { colKey: 'totalTokens', title: 'Token', width: 80 },
  { colKey: 'startedAt', title: '开始时间', width: 180 },
]

function statusLabel(status: string) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'RUNNING') return '运行中'
  return status
}

function formatJson(raw: string) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

async function loadExecutions() {
  loading.value = true
  try {
    const { data } = await listExecutions(100)
    executions.value = data.data || []
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '加载执行记录失败'))
  } finally {
    loading.value = false
  }
}

async function openDetail(context: { row: TableRowData }) {
  const row = context.row as ExecutionVO
  try {
    const { data } = await getExecution(row.id)
    detail.value = data.data || row
    detailVisible.value = true
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '加载详情失败'))
  }
}

onMounted(loadExecutions)
</script>

<style scoped>
.detail-block {
  margin-top: 20px;
}

.detail-block h3 {
  margin: 0 0 8px;
  font: var(--td-font-title-small);
}

.detail-block pre {
  margin: 0;
  padding: 12px;
  border-radius: 8px;
  background: #f7f8fa;
  border: 1px solid var(--box-border);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-block--error pre {
  background: #fff1f0;
  border-color: #ffccc7;
}
</style>
