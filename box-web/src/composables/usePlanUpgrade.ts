import { ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  confirmPayment,
  fetchBillingOverview,
  fetchPayment,
  listPlans,
  subscribePlan,
  type BillingOverviewVO,
  type PlanVO,
} from '@/api/billing'
import { formatQuotaNumber } from '@/composables/useTenantQuota'

/** C 端仅展示、不可自助订阅；由 B 端分配或销售开通 */
export const PLAN_CODE_ENTERPRISE = 'team_enterprise'

export function isSelfServicePlan(plan: Pick<PlanVO, 'code'>) {
  return plan.code !== PLAN_CODE_ENTERPRISE
}

export const OVERAGE_POLICY_LABEL: Record<string, string> = {
  REJECT: '超额拒绝',
  DEGRADE: '超额降级',
  METERED: '按量计费',
}

export function formatPlanOverage(policy?: string) {
  if (!policy) return '—'
  return OVERAGE_POLICY_LABEL[policy] || policy
}

/** 配额 0 在后端表示不限 */
export function formatQuotaLimit(value?: number | null) {
  const n = Number(value ?? 0)
  if (!n) return '不限'
  return n.toLocaleString()
}

export function planLooksTeam(plan: Pick<PlanVO, 'audience' | 'code' | 'name'>) {
  const blob = `${plan.code || ''} ${plan.name || ''}`
  return /enterprise|team|org|企业|团队/i.test(blob)
}

export function planAudience(plan: PlanVO): 'PERSONAL' | 'TEAM' {
  if (plan.audience === 'TEAM') return 'TEAM'
  if (planLooksTeam(plan)) return 'TEAM'
  return 'PERSONAL'
}

export interface PlanCompareGroup {
  title: string
  rows: Array<{ key: string; label: string; text: (plan: PlanVO) => string }>
}

export const PLAN_COMPARE_GROUPS: PlanCompareGroup[] = [
  {
    title: '基本信息',
    rows: [
      { key: 'name', label: '套餐名称', text: (p) => p.name || '—' },
      { key: 'code', label: '套餐编码', text: (p) => p.code || '—' },
      { key: 'desc', label: '说明', text: (p) => p.description?.trim() || '—' },
    ],
  },
  {
    title: '价格',
    rows: [{ key: 'price', label: '月费（元）', text: (p) => Number(p.priceMonthly || 0).toLocaleString() }],
  },
  {
    title: '用量配额',
    rows: [
      { key: 'tokens', label: 'Token / 月', text: (p) => formatQuotaLimit(p.quotaTokens) },
      { key: 'ai', label: 'AI 调用 / 月', text: (p) => formatQuotaLimit(p.quotaAiCalls) },
    ],
  },
  {
    title: '协作配额',
    rows: [
      { key: 'members', label: '成员席位', text: (p) => formatQuotaLimit(p.quotaMembers) },
      { key: 'workspaces', label: '工作空间', text: (p) => formatQuotaLimit(p.quotaWorkspaces) },
      { key: 'knowledge', label: '知识库', text: (p) => formatQuotaLimit(p.quotaKnowledgeBases) },
    ],
  },
  {
    title: '能力',
    rows: [
      { key: 'overage', label: '超额策略', text: (p) => formatPlanOverage(p.overagePolicy) },
      { key: 'byok', label: '允许自带密钥', text: (p) => (p.byokEnabled ? '是' : '否') },
    ],
  },
]

export const PLAN_CARD_ROWS = [
  { key: 'ai', label: 'AI 调用 / 月', text: (p: PlanVO) => formatQuotaLimit(p.quotaAiCalls) },
  { key: 'members', label: '成员席位', text: (p: PlanVO) => formatQuotaLimit(p.quotaMembers) },
  { key: 'workspaces', label: '工作空间', text: (p: PlanVO) => formatQuotaLimit(p.quotaWorkspaces) },
  { key: 'knowledge', label: '知识库', text: (p: PlanVO) => formatQuotaLimit(p.quotaKnowledgeBases) },
  { key: 'overage', label: '超额策略', text: (p: PlanVO) => formatPlanOverage(p.overagePolicy) },
  { key: 'byok', label: '允许自带密钥', text: (p: PlanVO) => (p.byokEnabled ? '是' : '否') },
]

export function planQuotaMultiplier(plan: PlanVO, baseline: PlanVO | undefined) {
  if (!baseline?.quotaTokens || baseline.quotaTokens <= 0) return null
  if (!plan.quotaTokens) return null
  const ratio = plan.quotaTokens / baseline.quotaTokens
  if (ratio <= 1.05) return null
  const text = ratio >= 10 ? ratio.toFixed(0) : ratio.toFixed(1).replace(/\.0$/, '')
  return `${text} 倍`
}

export { formatQuotaNumber }

export function paymentSubscribeLabel(overview: BillingOverviewVO | null | undefined) {
  if (!overview?.paymentEnabled) return '订阅（模拟支付）'
  if (overview.paymentProvider === 'alipay') return '支付宝支付'
  if (overview.paymentProvider === 'stripe') return 'Stripe 支付'
  return '订阅并支付'
}

function postCheckoutForm(action: string, fields: Record<string, string>) {
  const form = document.createElement('form')
  form.method = 'POST'
  form.action = action
  form.acceptCharset = 'UTF-8'
  form.style.display = 'none'
  Object.entries(fields).forEach(([name, value]) => {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = name
    input.value = value
    form.appendChild(input)
  })
  document.body.appendChild(form)
  form.submit()
}

export async function waitForPaymentSuccess(paymentId: number, attempts = 12) {
  for (let i = 0; i < attempts; i++) {
    const { data } = await fetchPayment(paymentId)
    if (data.data?.status === 'SUCCEEDED') return true
    await new Promise((resolve) => setTimeout(resolve, 1500))
  }
  return false
}

export function usePlanUpgrade() {
  const plans = ref<PlanVO[]>([])
  const loading = ref(false)
  const overview = ref<BillingOverviewVO | null>(null)
  const subscribingId = ref<number | null>(null)

  async function loadPlans() {
    loading.value = true
    try {
      const [plansRes, billingRes] = await Promise.all([listPlans(), fetchBillingOverview()])
      plans.value = (plansRes.data.data || [])
        .filter((item) => item.status === 1)
        .sort((a, b) => {
          if (a.code === PLAN_CODE_ENTERPRISE) return 1
          if (b.code === PLAN_CODE_ENTERPRISE) return -1
          return Number(a.priceMonthly) - Number(b.priceMonthly)
        })
      overview.value = billingRes.data.data
    } catch {
      plans.value = []
      overview.value = null
    } finally {
      loading.value = false
    }
  }

  async function subscribe(planId: number) {
    subscribingId.value = planId
    try {
      const { data } = await subscribePlan(planId)
      const order = data.data
      if (order?.checkoutFormAction && order.checkoutForm && Object.keys(order.checkoutForm).length) {
        postCheckoutForm(order.checkoutFormAction, order.checkoutForm)
        return { redirected: true as const }
      }
      if (order?.paymentChannel === 'ALIPAY') {
        MessagePlugin.error('无法打开支付宝收银台，请重试')
        return { redirected: false as const, success: false as const }
      }
      if (order?.paymentUrl) {
        window.location.href = order.paymentUrl
        return { redirected: true as const }
      }
      if (order?.requiresClientConfirm && order?.paymentId) {
        await confirmPayment(order.paymentId)
      }
      MessagePlugin.success('套餐已更新')
      await loadPlans()
      return { redirected: false as const, success: true as const }
    } finally {
      subscribingId.value = null
    }
  }

  return {
    plans,
    loading,
    overview,
    subscribingId,
    loadPlans,
    subscribe,
  }
}
