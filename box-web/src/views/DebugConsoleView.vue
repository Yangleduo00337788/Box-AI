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

    <t-loading :loading="loading" size="small" class="debug-console__list">
      <t-table
        row-key="id"
        :data="executions"
        :columns="columns"
        bordered
        stripe
        hover
        size="small"
        :active-row-keys="detail ? [detail.id] : []"
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

    <section v-if="detail" class="debug-console__inspector">
      <header class="debug-console__inspector-head">
        <div>
          <h3>{{ detail.executionNo }}</h3>
          <p>
            <t-tag :theme="statusTheme(detail.status)" variant="light" size="small">{{ statusLabel(detail.status) }}</t-tag>
            <span>{{ detail.durationMs ?? '—' }} ms</span>
            <span>{{ detail.startedAt || '—' }}</span>
          </p>
        </div>
      </header>

      <div class="debug-console__columns">
        <section class="debug-console__column">
          <h4>输入</h4>
          <pre v-if="detail.inputJson">{{ formatJson(detail.inputJson) }}</pre>
          <t-empty v-else description="无输入数据" size="small" />
        </section>

        <section class="debug-console__column debug-console__column--trace">
          <h4>Execution 树</h4>
          <trace-span-tree v-if="traceSpans.length" :spans="traceSpans" />
          <t-empty v-else description="暂无 Trace 数据" size="small" />
        </section>

        <section class="debug-console__column">
          <h4>输出</h4>
          <pre v-if="detail.outputJson">{{ formatJson(detail.outputJson) }}</pre>
          <t-empty v-else-if="!detail.errorMessage" description="无输出数据" size="small" />
          <div v-if="detail.errorMessage" class="detail-block--error">
            <h5>错误</h5>
            <pre>{{ detail.errorMessage }}</pre>
          </div>
        </section>
      </div>
    </section>
    <t-empty v-else class="debug-console__placeholder" description="选择上方执行记录查看 Input / Trace / Output" />
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

.debug-console__list {
  margin-bottom: 16px;
}

.debug-console__placeholder {
  margin-top: 24px;
}

.debug-console__inspector {
  border: 1px solid var(--td-component-border);
  border-radius: 12px;
  padding: 16px;
  background: var(--td-bg-color-container);
}

.debug-console__inspector-head h3 {
  margin: 0 0 8px;
}

.debug-console__inspector-head p {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  color: var(--td-text-color-secondary);
  font-size: 13px;
}

.debug-console__columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
  align-items: start;
}

.debug-console__column {
  min-height: 360px;
  border: 1px solid var(--td-component-border);
  border-radius: 10px;
  padding: 12px;
  background: var(--td-bg-color-secondarycontainer);
}

.debug-console__column h4,
.debug-console__column h5 {
  margin: 0 0 12px;
  font-size: 14px;
}

.debug-console__column pre {
  margin: 0;
  padding: 10px;
  border-radius: 8px;
  background: var(--td-bg-color-container);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 480px;
  overflow: auto;
}

.debug-console__column--trace {
  max-height: 520px;
  overflow: auto;
}

.detail-block--error pre {
  color: var(--td-error-color);
}

@media (max-width: 1200px) {
  .debug-console__columns {
    grid-template-columns: 1fr;
  }
}
</style>
