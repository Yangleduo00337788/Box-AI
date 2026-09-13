<template>
  <div>
    <page-header v-if="!compact" title="分析" desc="查看工作空间内的智能体、对话与资源使用情况">
      <template #actions>
        <t-radio-group v-model="periodDays" variant="default-filled" size="small" @change="loadData">
          <t-radio-button :value="1">今天</t-radio-button>
          <t-radio-button :value="7">7 天</t-radio-button>
          <t-radio-button :value="30">30 天</t-radio-button>
        </t-radio-group>
        <t-button variant="outline" @click="onViewExecutions">查看执行记录</t-button>
      </template>
    </page-header>
    <div v-else class="dialog-toolbar">
      <t-radio-group v-model="periodDays" variant="default-filled" size="small" @change="loadData">
        <t-radio-button :value="1">今天</t-radio-button>
        <t-radio-button :value="7">7 天</t-radio-button>
        <t-radio-button :value="30">30 天</t-radio-button>
      </t-radio-group>
      <t-button variant="outline" @click="onViewExecutions">查看执行记录</t-button>
    </div>

    <t-loading :loading="loading" size="small">
      <div v-if="overview" class="stats-grid">
        <article v-for="item in stats" :key="item.label" class="stat-card">
          <span class="stat-card__label">{{ item.label }}</span>
          <strong class="stat-card__value">{{ item.value }}</strong>
          <span v-if="item.hint" class="stat-card__hint">{{ item.hint }}</span>
        </article>
      </div>
    </t-loading>

    <section v-if="trends?.points?.length" class="section charts-grid">
      <article class="chart-card">
        <h3 class="section__title">执行量趋势</h3>
        <div ref="executionChartRef" class="chart-box" />
      </article>
      <article class="chart-card">
        <h3 class="section__title">成功率趋势</h3>
        <div ref="successChartRef" class="chart-box" />
      </article>
      <article class="chart-card">
        <h3 class="section__title">平均延迟趋势</h3>
        <div ref="latencyChartRef" class="chart-box" />
      </article>
      <article v-if="overview?.topAgents?.length" class="chart-card">
        <h3 class="section__title">热门智能体</h3>
        <div ref="topAgentsChartRef" class="chart-box" />
      </article>
    </section>

    <section v-if="overview?.topAgents?.length" class="section">
      <h3 class="section__title">热门智能体（近 {{ overview.periodDays }} 天）</h3>
      <t-table row-key="agentId" :data="overview.topAgents" :columns="topAgentColumns" bordered stripe size="small">
        <template #successRate="{ row }">{{ row.successRate.toFixed(1) }}%</template>
        <template #op="{ row }">
          <t-button variant="text" theme="primary" @click="router.push(`/agents/${row.agentId}/builder`)">打开</t-button>
        </template>
      </t-table>
    </section>

    <section v-if="overview?.quota" class="section">
      <h3 class="section__title">资源使用</h3>
      <div class="usage-grid">
        <article v-for="item in usageItems" :key="item.label" class="usage-card">
          <div class="usage-card__head">
            <span>{{ item.label }}</span>
            <span>{{ item.percent }}%</span>
          </div>
          <t-progress :percentage="item.percent" :status="item.status" />
          <p class="usage-card__meta">{{ item.used }} / {{ item.total }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import PageHeader from '@/components/PageHeader.vue'
import {
  fetchAnalyticsOverview,
  fetchAnalyticsTrends,
  type AnalyticsOverviewVO,
  type AnalyticsTrendsVO,
} from '@/api/analytics'

const router = useRouter()
const props = defineProps<{ compact?: boolean }>()
const emit = defineEmits<{
  'view-executions': []
}>()
const loading = ref(false)
const overview = ref<AnalyticsOverviewVO | null>(null)
const trends = ref<AnalyticsTrendsVO | null>(null)
const periodDays = ref(7)

const executionChartRef = ref<HTMLElement | null>(null)
const successChartRef = ref<HTMLElement | null>(null)
const latencyChartRef = ref<HTMLElement | null>(null)
const topAgentsChartRef = ref<HTMLElement | null>(null)

let executionChart: ECharts | null = null
let successChart: ECharts | null = null
let latencyChart: ECharts | null = null
let topAgentsChart: ECharts | null = null

const stats = computed(() => {
  if (!overview.value) return []
  return [
    { label: '智能体', value: overview.value.agentCount, hint: '当前工作空间' },
    { label: '对话', value: overview.value.conversationCount, hint: '历史会话' },
    { label: '执行', value: overview.value.periodExecutionCount, hint: `近 ${overview.value.periodDays} 天` },
    { label: '成功率', value: `${overview.value.successRate.toFixed(1)}%`, hint: '执行成功占比' },
    { label: '平均延迟', value: overview.value.avgLatencyMs ? `${overview.value.avgLatencyMs} ms` : '—', hint: '执行耗时' },
    { label: '总执行', value: overview.value.executionCount, hint: '累计记录' },
    { label: '知识库', value: overview.value.knowledgeBaseCount, hint: 'RAG 资源' },
    { label: '工具', value: overview.value.toolCount, hint: 'HTTP Tool' },
    { label: '工作流', value: overview.value.workflowCount, hint: '编排数量' },
    { label: 'MCP', value: overview.value.mcpServerCount, hint: '外部能力' },
    { label: '套餐', value: overview.value.quota?.planName || '—', hint: overview.value.quota?.period || '' },
  ]
})

function calcPercent(used: number, total: number) {
  if (!total) return 0
  return Math.min(100, Math.round((used / total) * 100))
}

function calcStatus(percent: number) {
  if (percent >= 90) return 'error'
  if (percent >= 75) return 'warning'
  return 'active'
}

const usageItems = computed(() => {
  const quota = overview.value?.quota
  if (!quota) return []
  return [
    { label: 'Token', used: quota.usedTokens, total: quota.quotaTokens },
    { label: 'AI 调用', used: quota.usedAiCalls, total: quota.quotaAiCalls },
    { label: '成员', used: quota.usedMembers, total: quota.quotaMembers },
    { label: '工作空间', used: quota.usedWorkspaces, total: quota.quotaWorkspaces },
  ].map((item) => {
    const percent = calcPercent(item.used, item.total)
    return {
      ...item,
      percent,
      status: calcStatus(percent),
      used: item.used.toLocaleString(),
      total: item.total.toLocaleString(),
    }
  })
})

const topAgentColumns = [
  { colKey: 'agentName', title: '智能体' },
  { colKey: 'executionCount', title: '执行次数', width: 100 },
  { colKey: 'successRate', title: '成功率', width: 100 },
  { colKey: 'op', title: '操作', width: 80 },
]

function formatDateLabel(date: string) {
  const parts = date.split('-')
  return parts.length === 3 ? `${parts[1]}/${parts[2]}` : date
}

function onViewExecutions() {
  if (props.compact) {
    emit('view-executions')
    return
  }
  router.push('/executions')
}

function renderCharts() {
  const points = trends.value?.points || []
  const dates = points.map((item) => formatDateLabel(item.date))

  if (executionChartRef.value) {
    executionChart?.dispose()
    executionChart = echarts.init(executionChartRef.value)
    executionChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 16, top: 24, bottom: 28 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: points.map((item) => item.executionCount), itemStyle: { color: '#0052d9' } }],
    })
  }

  if (successChartRef.value) {
    successChart?.dispose()
    successChart = echarts.init(successChartRef.value)
    successChart.setOption({
      tooltip: { trigger: 'axis', valueFormatter: (value: number) => `${value.toFixed(1)}%` },
      grid: { left: 40, right: 16, top: 24, bottom: 28 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', max: 100 },
      series: [{ type: 'line', smooth: true, data: points.map((item) => item.successRate), itemStyle: { color: '#2ba471' } }],
    })
  }

  if (latencyChartRef.value) {
    latencyChart?.dispose()
    latencyChart = echarts.init(latencyChartRef.value)
    latencyChart.setOption({
      tooltip: { trigger: 'axis', valueFormatter: (value: number) => `${value} ms` },
      grid: { left: 48, right: 16, top: 24, bottom: 28 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, data: points.map((item) => item.avgLatencyMs), itemStyle: { color: '#e37318' } }],
    })
  }

  const topAgents = overview.value?.topAgents || []
  if (topAgentsChartRef.value && topAgents.length) {
    topAgentsChart?.dispose()
    topAgentsChart = echarts.init(topAgentsChartRef.value)
    topAgentsChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 100, right: 16, top: 16, bottom: 28 },
      xAxis: { type: 'value', minInterval: 1 },
      yAxis: { type: 'category', data: topAgents.map((item) => item.agentName) },
      series: [{ type: 'bar', data: topAgents.map((item) => item.executionCount), itemStyle: { color: '#7c4dff' } }],
    })
  }
}

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, trendsRes] = await Promise.all([
      fetchAnalyticsOverview(periodDays.value),
      fetchAnalyticsTrends(periodDays.value),
    ])
    overview.value = overviewRes.data.data
    trends.value = trendsRes.data.data
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

watch(periodDays, () => {
  loadData()
})

onMounted(async () => {
  await loadData()
  window.setTimeout(() => {
    executionChart?.resize()
    successChart?.resize()
    latencyChart?.resize()
    topAgentsChart?.resize()
  }, 80)
})

onBeforeUnmount(() => {
  executionChart?.dispose()
  successChart?.dispose()
  latencyChart?.dispose()
  topAgentsChart?.dispose()
})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 28px;
}

.stat-card {
  padding: 20px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
}

.stat-card__label {
  display: block;
  margin-bottom: 8px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.stat-card__value {
  font: var(--td-font-title-large);
  color: var(--box-ink);
}

.stat-card__hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--box-muted);
}

.section {
  margin-bottom: 28px;
}

.section__title {
  margin: 0 0 12px;
  font: var(--td-font-title-medium);
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-card {
  padding: 16px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}

.chart-box {
  width: 100%;
  height: 280px;
}

.usage-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.usage-card {
  padding: 18px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}

.usage-card__head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.usage-card__meta {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--box-muted);
}

.dialog-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}

@media (max-width: 960px) {
  .stats-grid,
  .usage-grid,
  .charts-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}
</style>
