<template>
  <div class="dashboard-page admin-page">
    <page-header title="工作台" desc="平台资源、用量趋势与快捷入口。">
      <template #actions>
        <t-space :size="8">
          <t-button
            v-for="item in entries"
            :key="item.to"
            variant="outline"
            size="small"
            @click="router.push(item.to)"
          >
            <template #icon>
              <t-icon :name="item.icon" />
            </template>
            {{ item.label }}
          </t-button>
        </t-space>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small" class="dashboard-body">
      <div class="stat-grid">
        <t-card
          v-for="item in stats"
          :key="item.label"
          :bordered="false"
          class="stat-card"
          @click="router.push(item.to)"
        >
          <t-statistic :title="item.label" :value="item.value" />
        </t-card>
      </div>

      <div class="chart-row">
        <t-card :bordered="false" title="资源分布" class="chart-card">
          <div ref="barRef" class="chart-el" />
        </t-card>
        <t-card :bordered="false" title="租户构成" class="chart-card">
          <div ref="pieRef" class="chart-el" />
        </t-card>
      </div>

      <t-card v-if="canOpen('/tenants')" :bordered="false" title="最近租户" class="table-card">
        <t-table row-key="id" :data="recentTenants" :columns="tenantColumns" hover size="medium">
          <template #empty>
            <t-empty description="暂无租户" />
          </template>
        </t-table>
      </t-card>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Tag } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import PageHeader from '@box/ui/components/PageHeader.vue'
import { fetchAgentTemplates, type AgentTemplateVO } from '@/api/agentTemplate'
import { fetchPlugins, type AdminPluginCatalogVO } from '@/api/plugin'
import { fetchPlans } from '@/api/plan'
import { fetchPlatformModels, fetchPlatformProviders } from '@/api/platform'
import { useAppearanceStore } from '@/stores/appearance'
import { fetchTenants, type TenantVO } from '@/api/tenant'
import { formatDateTime } from '@/utils/datetime'
import { canAccessAdminRoute } from '@/constants/rbac'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const appearance = useAppearanceStore()
const loading = ref(false)
const tenants = ref<TenantVO[]>([])
const plugins = ref<AdminPluginCatalogVO[]>([])
const templates = ref<AgentTemplateVO[]>([])
const planCount = ref(0)
const modelCount = ref(0)
const providerCount = ref(0)

const barRef = ref<HTMLDivElement | null>(null)
const pieRef = ref<HTMLDivElement | null>(null)
let barChart: ECharts | null = null
let pieChart: ECharts | null = null

function canOpen(path: string) {
  return canAccessAdminRoute(auth.platformRole, path)
}

const stats = computed(() =>
  [
    { label: '租户', value: tenants.value.length, to: '/tenants' },
    { label: '启用租户', value: tenants.value.filter((item) => item.status === 1).length, to: '/tenants' },
    { label: '套餐', value: planCount.value, to: '/plans' },
    { label: '模型服务商', value: providerCount.value, to: '/platform-models' },
    { label: '平台模型', value: modelCount.value, to: '/platform-models' },
    { label: '上架插件', value: plugins.value.length, to: '/plugin-catalog' },
    { label: '智能体模板', value: templates.value.length, to: '/agent-templates' },
  ].filter((item) => canOpen(item.to)),
)

const entries = computed(() =>
  [
    { to: '/analytics', label: '分析', icon: 'chart' },
    { to: '/tenants', label: '租户', icon: 'usergroup' },
    { to: '/users', label: '用户', icon: 'user' },
    { to: '/platform-tools', label: '工具', icon: 'tools' },
    { to: '/platform-mcp', label: 'MCP', icon: 'server' },
    { to: '/plugin-catalog', label: '插件', icon: 'layers' },
    { to: '/audit-logs', label: '审计', icon: 'history' },
  ].filter((item) => canOpen(item.to)),
)

const recentTenants = computed(() => tenants.value.slice(0, 8))

const tenantColumns: PrimaryTableCol<TenantVO>[] = [
  { colKey: 'name', title: '名称', minWidth: 160 },
  { colKey: 'slug', title: 'Slug', width: 140 },
  {
    colKey: 'tenantType',
    title: '类型',
    width: 90,
    cell: (_, { row }) =>
      h(Tag, { theme: row.tenantType === 'PERSONAL' ? 'default' : 'primary', variant: 'light' }, () =>
        row.tenantType === 'PERSONAL' ? '个人' : '企业',
      ),
  },
  { colKey: 'planName', title: '套餐', minWidth: 140 },
  {
    colKey: 'status',
    title: '状态',
    width: 90,
    cell: (_, { row }) =>
      h(Tag, { theme: row.status === 1 ? 'success' : 'warning', variant: 'light' }, () =>
        row.status === 1 ? '正常' : '停用',
      ),
  },
  { colKey: 'createdAt', title: '创建时间', width: 180, cell: (_, { row }) => formatDateTime(row.createdAt) },
]

function renderCharts() {
  const palette = appearance.brandPreset.chartColors
  const brand = palette[0]
  const muted = '#8b8b8b'
  if (barRef.value) {
    barChart?.dispose()
    barChart = echarts.init(barRef.value)
    const values = [
      tenants.value.length,
      planCount.value,
      providerCount.value,
      modelCount.value,
      plugins.value.length,
      templates.value.length,
    ]
    barChart.setOption({
      color: palette,
      tooltip: { trigger: 'axis' },
      grid: { left: 36, right: 16, top: 16, bottom: 28 },
      xAxis: {
        type: 'category',
        data: ['租户', '套餐', '服务商', '模型', '插件', '模板'],
        axisLabel: { color: muted },
        axisLine: { lineStyle: { color: '#e7e7e7' } },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { color: '#f0f0f0' } },
        axisLabel: { color: muted },
      },
      series: [
        {
          type: 'bar',
          barMaxWidth: 28,
          data: values.map((value, index) => ({
            value,
            itemStyle: { color: palette[index % palette.length], borderRadius: [4, 4, 0, 0] },
          })),
        },
      ],
    })
  }

  if (pieRef.value) {
    pieChart?.dispose()
    pieChart = echarts.init(pieRef.value)
    const personal = tenants.value.filter((item) => item.tenantType === 'PERSONAL').length
    const enterprise = tenants.value.filter((item) => item.tenantType === 'ENTERPRISE').length
    const other = tenants.value.length - personal - enterprise
    pieChart.setOption({
      color: palette,
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, icon: 'circle' },
      series: [
        {
          type: 'pie',
          radius: ['46%', '68%'],
          center: ['50%', '44%'],
          label: { formatter: '{b}\n{c}' },
          data: [
            { name: '个人', value: personal, itemStyle: { color: palette[1] || brand } },
            { name: '企业', value: enterprise, itemStyle: { color: brand } },
            ...(other > 0 ? [{ name: '未分类', value: other, itemStyle: { color: palette[4] || '#c5c5c5' } }] : []),
          ],
        },
      ],
    })
  }
}

function resizeCharts() {
  barChart?.resize()
  pieChart?.resize()
}

async function load() {
  loading.value = true
  try {
    const [tenantRes, planRes, providerRes, modelRes, pluginRes, templateRes] = await Promise.all([
      canOpen('/tenants') ? fetchTenants() : Promise.resolve(null),
      canOpen('/plans') ? fetchPlans() : Promise.resolve(null),
      canOpen('/platform-models') ? fetchPlatformProviders() : Promise.resolve(null),
      canOpen('/platform-models') ? fetchPlatformModels() : Promise.resolve(null),
      canOpen('/plugin-catalog') ? fetchPlugins() : Promise.resolve(null),
      canOpen('/agent-templates') ? fetchAgentTemplates() : Promise.resolve(null),
    ])
    tenants.value = tenantRes?.data.data || []
    planCount.value = (planRes?.data.data || []).length
    providerCount.value = (providerRes?.data.data || []).length
    modelCount.value = (modelRes?.data.data || []).length
    plugins.value = pluginRes?.data.data || []
    templates.value = templateRes?.data.data || []
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

watch(
  () => appearance.brand,
  async () => {
    await nextTick()
    renderCharts()
  },
)

onMounted(() => {
  void load()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  gap: 12px;
}

.dashboard-page :deep(.page-header) {
  margin-bottom: 0;
}

.dashboard-page :deep(.page-header__actions) {
  max-width: 58%;
}

.dashboard-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.stat-card :deep(.t-card__body) {
  padding: 16px 18px;
}

.stat-card {
  cursor: pointer;
}

.chart-row {
  display: grid;
  grid-template-columns: 1.45fr 1fr;
  gap: 12px;
  min-height: 340px;
  flex: 1;
}

.chart-card {
  display: flex;
  flex-direction: column;
  min-height: 340px;
}

.chart-card :deep(.t-card__body) {
  flex: 1;
  min-height: 280px;
}

.chart-el {
  width: 100%;
  height: 100%;
  min-height: 280px;
}

.table-card :deep(.t-table__content) {
  min-height: 0;
}

@media (max-width: 1280px) {
  .stat-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>
