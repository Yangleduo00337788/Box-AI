<template>
  <div class="analytics-page admin-page">
    <page-header title="平台分析" desc="全平台对话执行、Token 消耗与租户用量。">
      <template #actions>
        <t-radio-group v-model="periodDays" variant="default-filled" size="small">
          <t-radio-button :value="1">今天</t-radio-button>
          <t-radio-button :value="7">7 天</t-radio-button>
          <t-radio-button :value="30">30 天</t-radio-button>
        </t-radio-group>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small" class="analytics-body">
      <div class="stat-grid">
        <t-card v-for="item in stats" :key="item.label" :bordered="false" class="stat-card">
          <t-statistic :title="item.label" :value="item.value" />
        </t-card>
      </div>

      <div class="chart-row">
        <t-card :bordered="false" title="执行量趋势" class="chart-card">
          <div ref="execRef" class="chart-el" />
        </t-card>
        <t-card :bordered="false" title="成功率趋势" class="chart-card">
          <div ref="successRef" class="chart-el" />
        </t-card>
      </div>

      <t-card :bordered="false" :title="`租户用量 TOP（${overview?.usagePeriod || '本月'}）`" class="table-card">
        <t-table row-key="tenantId" :data="overview?.topTenants || []" :columns="tenantColumns" hover>
          <template #empty>
            <t-empty description="本月暂无用量数据" />
          </template>
        </t-table>
      </t-card>
    </t-loading>

    <t-drawer v-model:visible="detailVisible" :header="detailTitle" size="560px" :footer="false">
      <t-loading :loading="detailLoading" size="small">
        <t-descriptions v-if="tenantDetail" :column="2" bordered>
          <t-descriptions-item label="执行次数">{{ tenantDetail.totalExecutions }}</t-descriptions-item>
          <t-descriptions-item label="成功率">{{ tenantDetail.successRate.toFixed(1) }}%</t-descriptions-item>
          <t-descriptions-item label="Token 消耗" :span="2">
            {{ Number(tenantDetail.totalTokens || 0).toLocaleString() }}
          </t-descriptions-item>
        </t-descriptions>
        <t-card v-if="tenantDetail" :bordered="false" title="错误分布" class="error-card">
          <t-table
            row-key="errorCode"
            :data="tenantDetail.modelErrors || []"
            :columns="errorColumns"
            size="small"
            hover
          >
            <template #empty>
              <t-empty description="暂无失败记录" />
            </template>
          </t-table>
        </t-card>
      </t-loading>
    </t-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Link } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import PageHeader from '@box/ui/components/PageHeader.vue'
import {
  fetchPlatformAnalyticsOverview,
  fetchPlatformAnalyticsTrends,
  fetchTenantAnalyticsDetail,
  type PlatformAnalyticsOverviewVO,
  type PlatformTopTenantVO,
  type TenantAnalyticsDetailVO,
} from '@/api/analytics'
import { useAppearanceStore } from '@/stores/appearance'

const appearance = useAppearanceStore()
const loading = ref(false)
const periodDays = ref(7)
const overview = ref<PlatformAnalyticsOverviewVO | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const tenantDetail = ref<TenantAnalyticsDetailVO | null>(null)
const activeTenant = ref<PlatformTopTenantVO | null>(null)
const execRef = ref<HTMLDivElement | null>(null)
const successRef = ref<HTMLDivElement | null>(null)
let execChart: ECharts | null = null
let successChart: ECharts | null = null

const stats = computed(() => {
  const data = overview.value
  if (!data) return []
  return [
    { label: '近窗口执行次数', value: data.periodExecutionCount },
    { label: '成功率 %', value: Number(data.successRate.toFixed(1)) },
    { label: '平均延迟 ms', value: data.avgLatencyMs },
    { label: '窗口 Token', value: data.periodTokens },
    { label: `本月 AI 调用`, value: data.monthAiCalls },
    { label: '本月 Token', value: data.monthTokens },
    { label: '租户数', value: data.tenantCount },
  ]
})

const tenantColumns: PrimaryTableCol<PlatformTopTenantVO>[] = [
  {
    colKey: 'tenantName',
    title: '租户',
    minWidth: 180,
    cell: (_, { row }) =>
      h(Link, { theme: 'primary', hover: 'color', onClick: () => openTenantDetail(row) }, () => row.tenantName),
  },
  { colKey: 'aiCalls', title: 'AI 调用', width: 120 },
  {
    colKey: 'tokens',
    title: 'Token',
    width: 140,
    cell: (_, { row }) => Number(row.tokens || 0).toLocaleString(),
  },
]

const errorColumns: PrimaryTableCol<{ errorCode: string; count: number; totalExecutions: number }>[] = [
  { colKey: 'errorCode', title: '错误码', minWidth: 160 },
  { colKey: 'count', title: '次数', width: 80 },
  {
    colKey: 'totalExecutions',
    title: '占比',
    width: 100,
    cell: (_, { row }) =>
      row.totalExecutions ? `${((row.count / row.totalExecutions) * 100).toFixed(1)}%` : '-',
  },
]

const detailTitle = computed(() =>
  activeTenant.value ? `租户分析 · ${activeTenant.value.tenantName}` : '租户分析',
)

async function openTenantDetail(row: PlatformTopTenantVO) {
  activeTenant.value = row
  tenantDetail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    const { data } = await fetchTenantAnalyticsDetail(row.tenantId)
    tenantDetail.value = data.data
  } finally {
    detailLoading.value = false
  }
}

function renderCharts(dates: string[], executions: number[], rates: number[]) {
  const palette = appearance.brandPreset.chartColors
  const muted = '#8b8b8b'
  if (execRef.value) {
    execChart?.dispose()
    execChart = echarts.init(execRef.value)
    execChart.setOption({
      color: palette,
      tooltip: { trigger: 'axis' },
      grid: { left: 36, right: 16, top: 24, bottom: 28 },
      xAxis: { type: 'category', data: dates, axisLabel: { color: muted } },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { color: muted } },
      series: [{ type: 'line', smooth: true, areaStyle: { opacity: 0.12 }, data: executions }],
    })
  }
  if (successRef.value) {
    successChart?.dispose()
    successChart = echarts.init(successRef.value)
    successChart.setOption({
      color: [palette[1] || palette[0]],
      tooltip: { trigger: 'axis' },
      grid: { left: 36, right: 16, top: 24, bottom: 28 },
      xAxis: { type: 'category', data: dates, axisLabel: { color: muted } },
      yAxis: { type: 'value', max: 100, axisLabel: { color: muted, formatter: '{value}%' } },
      series: [{ type: 'line', smooth: true, data: rates }],
    })
  }
}

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, trendsRes] = await Promise.all([
      fetchPlatformAnalyticsOverview(periodDays.value),
      fetchPlatformAnalyticsTrends(periodDays.value),
    ])
    overview.value = overviewRes.data.data
    const points = trendsRes.data.data?.points || []
    await nextTick()
    renderCharts(
      points.map((item) => item.date.slice(5)),
      points.map((item) => item.executionCount),
      points.map((item) => Number(item.successRate.toFixed(1))),
    )
  } finally {
    loading.value = false
  }
}

function resizeCharts() {
  execChart?.resize()
  successChart?.resize()
}

watch(periodDays, () => {
  void loadData()
})

watch(
  () => appearance.brand,
  async () => {
    await loadData()
  },
)

onMounted(() => {
  void loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  execChart?.dispose()
  successChart?.dispose()
})
</script>

<style scoped>
.analytics-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  min-height: 280px;
}

.chart-el {
  width: 100%;
  height: 260px;
}

@media (max-width: 1280px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .chart-row {
    grid-template-columns: 1fr;
  }
}

.error-card {
  margin-top: 16px;
}
</style>
