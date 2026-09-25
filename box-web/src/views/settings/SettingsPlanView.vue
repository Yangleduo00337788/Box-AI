<template>
  <div class="settings-page">
    <h1 class="settings-page__title">套餐与额度</h1>
    <p class="settings-page__desc">用量按企业统计。工作空间包含同事创建的空间，不只是你已加入的那些。</p>

    <t-tabs v-model="tab" @change="onTabChange">
      <t-tab-panel value="usage" label="用量" :destroy-on-hide="false">
        <t-loading :loading="quotaLoading" size="small">
          <section v-if="quota" class="settings-card plan-card">
            <div class="plan-head">
              <div>
                <p class="plan-head__label">当前套餐</p>
                <h2 class="plan-head__name">{{ quota.planName }}</h2>
              </div>
              <t-tag theme="primary" variant="light">{{ quota.period }}</t-tag>
            </div>
            <div class="donut-grid">
              <quota-donut-chart
                v-for="item in quotaItems"
                :key="item.label"
                :label="item.label"
                :used="item.used"
                :total="item.total"
                :hint="item.hint"
                :active="tab === 'usage'"
              />
            </div>
            <div class="bar-wrap">
              <h3 class="bar-wrap__title">占用对比</h3>
              <div ref="usageBarRef" class="bar-chart" />
            </div>
          </section>
          <t-empty v-else description="暂无额度数据" />
        </t-loading>
      </t-tab-panel>
      <t-tab-panel value="billing" label="账单" :destroy-on-hide="false">
        <t-loading :loading="billingLoading" size="small">
          <section v-if="overview" class="settings-card plan-card">
            <t-descriptions :column="2" layout="horizontal">
              <t-descriptions-item label="账期">{{ overview.period }}</t-descriptions-item>
              <t-descriptions-item label="当前套餐">{{ overview.planName }}</t-descriptions-item>
              <t-descriptions-item label="参考月费">
                {{ overview.currency }} {{ overview.planPriceMonthly }}
              </t-descriptions-item>
              <t-descriptions-item :label="overview.paymentEnabled ? '预估应付' : '参考应付（未开通支付）'">
                {{ overview.currency }} {{ overview.estimatedAmount }}
              </t-descriptions-item>
            </t-descriptions>
            <div class="bar-wrap">
              <h3 class="bar-wrap__title">本月用量</h3>
              <div ref="billingBarRef" class="bar-chart" />
            </div>
            <h3 class="bar-wrap__title">发票记录</h3>
            <t-table row-key="id" :data="invoices" :columns="invoiceColumns" size="small" />
          </section>
          <t-empty v-else description="暂无账单数据" />
        </t-loading>
      </t-tab-panel>
    </t-tabs>

    <div class="plan-upgrade-entry">
      <p>需要更换套餐？</p>
      <t-button theme="primary" variant="outline" @click="upgradeVisible = true">立即升级</t-button>
    </div>

    <plan-upgrade-dialog
      v-model:visible="upgradeVisible"
      :current-plan-id="quota?.planId"
      @success="onUpgradeSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PrimaryTableCol, TabValue } from 'tdesign-vue-next'
import {
  fetchBillingOverview,
  fetchInvoices,
  type BillingInvoiceVO,
  type BillingOverviewVO,
} from '@/api/billing'
import { fetchQuota, type QuotaSnapshotVO } from '@/api/quota'
import { useAuthStore } from '@/stores/auth'
import { appPreferences } from '@/composables/useAppPreferences'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import QuotaDonutChart from '@/components/QuotaDonutChart.vue'
import PlanUpgradeDialog from '@/components/PlanUpgradeDialog.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const quotaLoading = ref(false)
const billingLoading = ref(false)
const quota = ref<QuotaSnapshotVO | null>(null)
const overview = ref<BillingOverviewVO | null>(null)
const invoices = ref<BillingInvoiceVO[]>([])
const upgradeVisible = ref(false)
const tab = ref<'usage' | 'billing'>(
  route.query.tab === 'billing' || route.query.tab === 'plans' ? 'billing' : 'usage',
)
const usageBarRef = ref<HTMLElement | null>(null)
const billingBarRef = ref<HTMLElement | null>(null)

let usageBar: ECharts | null = null
let billingBar: ECharts | null = null

function calcPercent(used: number, total: number) {
  if (!total) return 0
  return Math.min(100, Math.round((used / total) * 100))
}

const quotaItems = computed(() => {
  if (!quota.value) return []
  const q = quota.value
  return [
    { label: 'AI 调用', used: q.usedAiCalls, total: q.quotaAiCalls },
    { label: 'Token', used: q.usedTokens, total: q.quotaTokens },
    { label: '成员', used: q.usedMembers, total: q.quotaMembers, hint: '企业内可邀请的成员' },
    {
      label: '企业工作空间',
      used: q.usedWorkspaces,
      total: q.quotaWorkspaces,
      hint: `你已加入 ${auth.workspaces.length} 个`,
    },
    { label: '知识库', used: q.usedKnowledgeBases, total: q.quotaKnowledgeBases },
  ]
})

function cssVar(name: string, fallback: string) {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

function barColor(percent: number) {
  if (percent >= 90) return cssVar('--td-error-color', '#e34d59')
  if (percent >= 75) return cssVar('--td-warning-color', '#ed7b2f')
  return cssVar('--box-ink', '#222222')
}

function renderUsageBar() {
  if (!usageBarRef.value || !quotaItems.value.length) return
  if (!usageBar) {
    usageBar = echarts.init(usageBarRef.value)
  }
  const ink = cssVar('--box-ink', '#222')
  const muted = cssVar('--box-muted', '#8b8b8b')
  usageBar.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (raw: unknown) => {
        const params = raw as { name: string; value: number }[]
        const item = params[0]
        const source = quotaItems.value.find((row) => row.label === item.name)
        if (!source) return `${item.name}: ${item.value}%`
        return `${item.name}<br/>已用 ${source.used.toLocaleString()} / ${source.total.toLocaleString()}（${item.value}%）`
      },
    },
    grid: { left: 96, right: 28, top: 8, bottom: 8 },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { color: muted, formatter: '{value}%' },
      splitLine: { lineStyle: { color: cssVar('--box-border', '#eee') } },
    },
    yAxis: {
      type: 'category',
      data: quotaItems.value.map((item) => item.label),
      axisLabel: { color: ink },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    series: [
      {
        type: 'bar',
        barWidth: 14,
        data: quotaItems.value.map((item) => {
          const value = calcPercent(item.used, item.total)
          return { value, itemStyle: { color: barColor(value), borderRadius: 7 } }
        }),
      },
    ],
  })
}

function renderBillingBar() {
  if (!billingBarRef.value || !overview.value) return
  if (!billingBar) {
    billingBar = echarts.init(billingBarRef.value)
  }
  const ink = cssVar('--box-ink', '#222')
  const muted = cssVar('--box-muted', '#8b8b8b')
  const track = cssVar('--td-bg-color-secondarycontainer', '#eee')
  const rows = [
    {
      label: 'AI 调用',
      used: overview.value.usedAiCalls,
      total: overview.value.quotaAiCalls || 0,
    },
    {
      label: 'Token',
      used: overview.value.usedTokens,
      total: overview.value.quotaTokens || 0,
    },
  ].map((item) => {
    const percent = item.total ? Math.min(100, (item.used / item.total) * 100) : 0
    return { ...item, percent, remain: Math.max(0, 100 - percent) }
  })
  billingBar.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (raw: unknown) => {
        const params = raw as { name: string }[]
        const row = rows.find((item) => item.label === params[0]?.name)
        if (!row) return ''
        const totalText = row.total ? row.total.toLocaleString() : '不限'
        return `${row.label}<br/>已用 ${row.used.toLocaleString()} / ${totalText}`
      },
    },
    legend: { data: ['已用', '剩余'], textStyle: { color: muted }, right: 0 },
    grid: { left: 72, right: 16, top: 28, bottom: 8 },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { color: muted, formatter: '{value}%' },
      splitLine: { lineStyle: { color: cssVar('--box-border', '#eee') } },
    },
    yAxis: {
      type: 'category',
      data: rows.map((item) => item.label),
      axisLabel: { color: ink },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    series: [
      {
        name: '已用',
        type: 'bar',
        stack: 'quota',
        barWidth: 18,
        data: rows.map((item) => Number(item.percent.toFixed(1))),
        itemStyle: { color: ink, borderRadius: [7, 0, 0, 7] },
      },
      {
        name: '剩余',
        type: 'bar',
        stack: 'quota',
        data: rows.map((item) => Number(item.remain.toFixed(1))),
        itemStyle: { color: track, borderRadius: [0, 7, 7, 0] },
      },
    ],
  })
}

async function renderCharts() {
  await nextTick()
  if (tab.value === 'usage') {
    renderUsageBar()
    usageBar?.resize()
  } else {
    renderBillingBar()
    billingBar?.resize()
  }
}

const invoiceColumns: PrimaryTableCol<BillingInvoiceVO>[] = [
  { colKey: 'invoiceNo', title: '发票号', minWidth: 140 },
  { colKey: 'period', title: '账期', width: 90 },
  { colKey: 'totalAmount', title: '金额', width: 100 },
  { colKey: 'status', title: '状态', width: 90 },
]

function onTabChange(value: TabValue) {
  const next = value === 'billing' ? 'billing' : 'usage'
  tab.value = next
  const query = next === 'usage' ? {} : { tab: next }
  void router.replace({ query })
  if (next === 'billing') void loadInvoices()
  void renderCharts()
}

async function loadQuota() {
  quotaLoading.value = true
  try {
    const { data } = await fetchQuota()
    quota.value = data.data
  } catch {
    quota.value = null
  } finally {
    quotaLoading.value = false
  }
}

async function loadBilling() {
  billingLoading.value = true
  try {
    const [{ data: overviewRes }, { data: invoiceRes }] = await Promise.all([
      fetchBillingOverview(),
      fetchInvoices(),
    ])
    overview.value = overviewRes.data
    invoices.value = invoiceRes.data || []
  } catch {
    overview.value = null
    invoices.value = []
  } finally {
    billingLoading.value = false
  }
}

async function loadInvoices() {
  try {
    const { data } = await fetchInvoices()
    invoices.value = data.data || []
  } catch {
    invoices.value = []
  }
}

async function loadAll() {
  await Promise.all([loadQuota(), loadBilling()])
  await renderCharts()
}

async function onUpgradeSuccess() {
  await loadAll()
  tab.value = 'usage'
}

function handleResize() {
  usageBar?.resize()
  billingBar?.resize()
}

watch(
  () => route.query.tab,
  (value) => {
    if (value === 'plans') {
      upgradeVisible.value = true
      void router.replace({ path: route.path, query: {} })
      return
    }
    tab.value = value === 'billing' ? 'billing' : 'usage'
    void renderCharts()
  },
)

watch(
  () => [quota.value, overview.value, appPreferences.theme],
  () => {
    void renderCharts()
  },
)

function handlePaymentReturn() {
  const payment = route.query.payment
  if (payment === 'success') {
    MessagePlugin.success('支付已完成，套餐已更新')
    tab.value = 'billing'
    void router.replace({ path: route.path, query: { tab: 'billing' } })
  } else if (payment === 'cancel') {
    MessagePlugin.warning('已取消支付')
    upgradeVisible.value = true
    void router.replace({ path: route.path, query: {} })
  }
}

onMounted(() => {
  handlePaymentReturn()
  if (route.query.tab === 'plans' || route.query.upgrade === '1') {
    upgradeVisible.value = true
    void router.replace({ path: route.path, query: {} })
  }
  void loadAll()
  window.addEventListener('resize', handleResize)
})
useReloadOnWorkspaceChange(loadAll)

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  usageBar?.dispose()
  billingBar?.dispose()
})
</script>

<style scoped>
.plan-card {
  margin-top: 8px;
}

.plan-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.plan-head__label {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--box-muted);
}

.plan-head__name {
  margin: 0;
  font: var(--td-font-title-medium);
}

.donut-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(148px, 1fr));
  justify-items: center;
  gap: 16px 12px;
  overflow: hidden;
}

.bar-wrap {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--box-border);
}

.bar-wrap__title {
  margin: 0 0 8px;
  font: var(--td-font-title-small);
}

.bar-chart {
  width: 100%;
  height: 220px;
}

.plan-upgrade-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
  padding: 14px 16px;
  border: 1px solid var(--box-border);
  border-radius: 10px;
  background: var(--td-bg-color-container);
}

.plan-upgrade-entry p {
  margin: 0;
  font-size: 13px;
  color: var(--box-muted);
}
</style>
