<template>
  <div class="debug-console">
    <page-header title="Debug Console" desc="统一查看 Agent / Workflow 执行记录、输入输出与 Trace 树" />

    <t-form layout="inline" class="debug-console__filters" @submit.prevent="loadExecutions">
      <t-form-item label="类型">
        <t-select v-model="filters.executionType" clearable :options="typeOptions" style="width: 140px" />
      </t-form-item>
      <t-form-item label="状态">
        <t-select v-model="filters.status" clearable :options="statusOptions" style="width: 140px" />
      </t-form-item>
      <t-form-item label="Agent ID">
        <t-input-number v-model="filters.agentId" :min="1" theme="column" />
      </t-form-item>
      <t-form-item label="会话 ID">
        <t-input-number v-model="filters.conversationId" :min="1" theme="column" />
      </t-form-item>
      <t-form-item>
        <t-button theme="primary" type="submit" :loading="loading">查询</t-button>
      </t-form-item>
    </t-form>

    <div class="debug-console__layout">
      <t-loading :loading="loading" size="small" class="debug-console__list">
        <t-table
          row-key="id"
          :data="executions"
          :columns="columns"
          bordered
          stripe
          hover
          size="small"
          @row-click="selectExecution"
        >
          <template #status="{ row }">
            <t-tag :theme="statusTheme(row.status)" variant="light" size="small">{{ statusLabel(row.status) }}</t-tag>
          </template>
          <template #type="{ row }">
            {{ row.executionType === 'WORKFLOW' ? '工作流' : '智能体' }}
          </template>
          <template #empty>
            <t-empty description="暂无执行记录" />
          </template>
        </t-table>
      </t-loading>

      <aside class="debug-console__detail">
        <template v-if="detail">
          <h3>执行详情</h3>
          <t-descriptions :column="1" bordered size="small">
            <t-descriptions-item label="编号">{{ detail.executionNo }}</t-descriptions-item>
            <t-descriptions-item label="状态">{{ statusLabel(detail.status) }}</t-descriptions-item>
            <t-descriptions-item label="耗时">{{ detail.durationMs ?? '—' }} ms</t-descriptions-item>
            <t-descriptions-item label="开始">{{ detail.startedAt || '—' }}</t-descriptions-item>
          </t-descriptions>
          <section v-if="detail.inputJson" class="detail-block">
            <h4>输入</h4>
            <pre>{{ formatJson(detail.inputJson) }}</pre>
          </section>
          <section v-if="detail.outputJson" class="detail-block">
            <h4>输出</h4>
            <pre>{{ formatJson(detail.outputJson) }}</pre>
          </section>
          <section v-if="detail.errorMessage" class="detail-block detail-block--error">
            <h4>错误</h4>
            <pre>{{ detail.errorMessage }}</pre>
          </section>
          <section v-if="traceSpans.length" class="detail-block">
            <h4>Trace 树</h4>
            <trace-span-tree :spans="traceSpans" />
          </section>
        </template>
        <t-empty v-else description="选择左侧执行记录查看详情" />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import TraceSpanTree from '@/components/TraceSpanTree.vue'
import { extractApiError } from '@/api/apiError'
import {
  getExecution,
  getExecutionTrace,
  listExecutions,
  type ExecutionVO,
  type TraceSpanVO,
} from '@/api/execution'

const loading = ref(false)
const executions = ref<ExecutionVO[]>([])
const detail = ref<ExecutionVO | null>(null)
const traceSpans = ref<TraceSpanVO[]>([])

const filters = reactive({
  executionType: '',
  status: '',
  agentId: undefined as number | undefined,
  conversationId: undefined as number | undefined,
})

const typeOptions = [
  { label: '智能体', value: 'AGENT' },
  { label: '工作流', value: 'WORKFLOW' },
]

const statusOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '运行中', value: 'RUNNING' },
]

const columns: PrimaryTableCol<ExecutionVO>[] = [
  { colKey: 'executionNo', title: '编号', ellipsis: true },
  { colKey: 'type', title: '类型', width: 90 },
  { colKey: 'status', title: '状态', width: 90 },
  { colKey: 'durationMs', title: '耗时', width: 90 },
  { colKey: 'startedAt', title: '开始时间', width: 170 },
]

function statusLabel(status: string) {
  if (status === 'SUCCESS' || status === 'SUCCEEDED') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'RUNNING') return '运行中'
  return status
}

function statusTheme(status: string) {
  if (status === 'SUCCESS' || status === 'SUCCEEDED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'default'
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
    const { data } = await listExecutions({
      limit: 100,
      executionType: filters.executionType || undefined,
      status: filters.status || undefined,
      agentId: filters.agentId || undefined,
      conversationId: filters.conversationId || undefined,
    })
    executions.value = data.data || []
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '加载执行记录失败'))
  } finally {
    loading.value = false
  }
}

async function selectExecution(ctx: { row: ExecutionVO }) {
  detail.value = ctx.row
  traceSpans.value = []
  try {
    const [detailRes, traceRes] = await Promise.all([
      getExecution(ctx.row.id),
      getExecutionTrace(ctx.row.id),
    ])
    detail.value = detailRes.data.data || ctx.row
    traceSpans.value = traceRes.data.data?.spans || []
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '加载 Trace 失败'))
  }
}

onMounted(loadExecutions)
</script>

<style scoped>
.debug-console__filters {
  margin-bottom: 16px;
}

.debug-console__layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.9fr);
  gap: 16px;
  align-items: start;
}

.debug-console__detail {
  border: 1px solid var(--td-component-border);
  border-radius: 12px;
  padding: 16px;
  min-height: 480px;
  background: var(--td-bg-color-container);
}

.debug-console__detail h3,
.debug-console__detail h4 {
  margin: 0 0 12px;
}

.detail-block {
  margin-top: 16px;
}

.detail-block pre {
  margin: 0;
  padding: 10px;
  border-radius: 8px;
  background: var(--td-bg-color-secondarycontainer);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-block--error pre {
  color: var(--td-error-color);
}
</style>
