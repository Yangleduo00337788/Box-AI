<template>
  <t-dialog
    v-model:visible="visible"
    width="1080px"
    top="4vh"
    placement="center"
    mode="modal"
    attach="body"
    :z-index="6000"
    :footer="false"
    :header="false"
    :close-btn="false"
    destroy-on-close
    class="plan-upgrade-dialog"
    @opened="onOpened"
  >
    <template v-if="visible">
      <header class="plan-upgrade__header">
        <div class="plan-upgrade__brand">
          <span class="plan-upgrade__brand-mark" aria-hidden="true">B</span>
          <h2 class="plan-upgrade__title">Box 付费套餐</h2>
        </div>
        <div class="plan-upgrade__segment" role="tablist">
          <button
            type="button"
            class="plan-upgrade__segment-item"
            :class="{ 'is-active': audience === 'PERSONAL' }"
            @click="audience = 'PERSONAL'"
          >
            个人工作
          </button>
          <button
            type="button"
            class="plan-upgrade__segment-item"
            :class="{ 'is-active': audience === 'TEAM' }"
            @click="audience = 'TEAM'"
          >
            团队协作
          </button>
        </div>
        <button type="button" class="plan-upgrade__close" aria-label="关闭" @click="visible = false">
          <t-icon name="close" />
        </button>
      </header>

      <t-loading :loading="loading" size="small">
        <div v-if="visiblePlans.length" class="plan-upgrade__body">
            <div class="plan-upgrade__cards">
              <article
                v-for="plan in visiblePlans"
                :key="plan.id"
                class="plan-upgrade__plan-card"
                :class="{
                  'is-selected': plan.id === selectedPlanId,
                  'is-current': plan.id === currentPlanId,
                }"
                @click="selectedPlanId = plan.id"
              >
                <div class="plan-upgrade__plan-head">
                  <h3 class="plan-upgrade__plan-name" :title="plan.name">{{ plan.name }}</h3>
                  <span v-if="plan.id === currentPlanId" class="plan-upgrade__plan-tag">当前</span>
                </div>

                <div class="plan-upgrade__price-row">
                  <span class="plan-upgrade__currency">¥</span>
                  <span class="plan-upgrade__price">{{ Number(plan.priceMonthly || 0).toLocaleString() }}</span>
                  <span class="plan-upgrade__price-suffix">/ 月</span>
                </div>
                <p v-if="plan.description" class="plan-upgrade__price-note" :title="plan.description">
                  {{ plan.description }}
                </p>
                <p v-else class="plan-upgrade__price-note plan-upgrade__price-note--empty">&nbsp;</p>

                <button
                  type="button"
                  class="plan-upgrade__cta"
                  :disabled="plan.id === currentPlanId"
                  :class="{ 'is-loading': subscribingId === plan.id }"
                  @click.stop="onSubscribePlan(plan)"
                >
                  {{
                    plan.id === currentPlanId ? '当前套餐' : paymentEnabled ? '订阅并支付' : '订阅（模拟支付）'
                  }}
                </button>

                <div class="plan-upgrade__quota-line">
                  <span>{{ formatQuotaLimit(plan.quotaTokens) }} Token / 月</span>
                  <span v-if="quotaMultiplier(plan)" class="plan-upgrade__quota-badge">
                    相对 {{ baselinePlan?.name }} {{ quotaMultiplier(plan) }}
                  </span>
                </div>

                <ul class="plan-upgrade__features">
                  <li v-for="row in PLAN_CARD_ROWS" :key="row.key">
                    <span class="plan-upgrade__check">✓</span>
                    <span>{{ row.label }}：{{ row.text(plan) }}</span>
                  </li>
                </ul>
              </article>
            </div>

            <section class="plan-upgrade__compare-page">
              <h4 class="plan-upgrade__compare-title">功能对比</h4>
              <p class="plan-upgrade__compare-sub">查看不同套餐之间权益差异</p>
            <div class="plan-upgrade__table-wrap">
              <table class="plan-upgrade__table">
                <thead>
                  <tr>
                    <th scope="col" class="plan-upgrade__table-corner">套餐版本</th>
                    <th
                      v-for="plan in visiblePlans"
                      :key="plan.id"
                      scope="col"
                      :class="{ 'is-selected': plan.id === selectedPlanId }"
                      :title="plan.name"
                    >
                      {{ plan.name }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <template v-for="group in PLAN_COMPARE_GROUPS" :key="group.title">
                    <tr class="plan-upgrade__group-row">
                      <th :colspan="visiblePlans.length + 1">{{ group.title }}</th>
                    </tr>
                    <tr v-for="row in group.rows" :key="row.key">
                      <th scope="row">{{ row.label }}</th>
                      <td
                        v-for="plan in visiblePlans"
                        :key="`${row.key}-${plan.id}`"
                        :class="{ 'is-selected': plan.id === selectedPlanId }"
                        :title="row.text(plan)"
                      >
                        {{ row.text(plan) }}
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
            </section>
        </div>
        <t-empty
          v-else-if="!loading"
          class="plan-upgrade__empty"
          :description="plans.length ? '该分类暂无套餐' : '暂无可用套餐'"
        />
      </t-loading>
    </template>
  </t-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { PlanVO } from '@/api/billing'
import {
  PLAN_CARD_ROWS,
  PLAN_COMPARE_GROUPS,
  formatQuotaLimit,
  planAudience,
  planQuotaMultiplier,
  usePlanUpgrade,
} from '@/composables/usePlanUpgrade'

const visible = defineModel<boolean>('visible', { default: false })

const props = defineProps<{
  currentPlanId?: number | null
}>()

const emit = defineEmits<{
  success: []
}>()

const { plans, loading, overview, subscribingId, loadPlans, subscribe } = usePlanUpgrade()
const selectedPlanId = ref<number | null>(null)
const audience = ref<'PERSONAL' | 'TEAM'>('PERSONAL')

const currentPlanId = computed(() => props.currentPlanId ?? null)
const paymentEnabled = computed(() => overview.value?.paymentEnabled === true)
const visiblePlans = computed(() => plans.value.filter((p) => planAudience(p) === audience.value))
const baselinePlan = computed(() => visiblePlans.value[0])

function quotaMultiplier(plan: PlanVO) {
  const base = baselinePlan.value
  if (!base || plan.id === base.id) return null
  return planQuotaMultiplier(plan, base)
}

watch(
  () => plans.value,
  (list) => {
    const current = list.find((p) => p.id === currentPlanId.value)
    if (current) audience.value = planAudience(current)
    syncSelected()
  },
)

watch(audience, () => {
  syncSelected()
})

function syncSelected() {
  const list = visiblePlans.value
  if (!list.length) {
    selectedPlanId.value = null
    return
  }
  if (selectedPlanId.value && list.some((p) => p.id === selectedPlanId.value)) return
  const current = currentPlanId.value
  if (current && list.some((p) => p.id === current)) {
    selectedPlanId.value = current
    return
  }
  selectedPlanId.value = list[0].id
}

function onOpened() {
  void loadPlans()
}

async function onSubscribePlan(plan: PlanVO) {
  selectedPlanId.value = plan.id
  if (plan.id === currentPlanId.value) return
  const result = await subscribe(plan.id)
  if (result?.redirected) return
  if (result?.success) {
    visible.value = false
    emit('success')
  }
}
</script>

<style scoped>
.plan-upgrade__header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  padding: 4px 0 16px;
}

.plan-upgrade__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.plan-upgrade__brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, #7b6cff 0%, #c9a8ff 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.plan-upgrade__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.plan-upgrade__segment {
  display: inline-flex;
  padding: 3px;
  border-radius: 999px;
  background: #f0f0f0;
}

.plan-upgrade__segment-item {
  padding: 6px 18px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #666;
  font-size: 13px;
  cursor: pointer;
}

.plan-upgrade__segment-item.is-active {
  background: #fff;
  color: #1a1a1a;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.plan-upgrade__close {
  justify-self: end;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #888;
  cursor: pointer;
}

.plan-upgrade__close:hover {
  background: #f3f3f3;
}

.plan-upgrade__cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.plan-upgrade__plan-card {
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 18px 16px 16px;
  border: 1px solid #ececec;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
}

.plan-upgrade__plan-card.is-selected {
  border-color: #1a1a1a;
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.06);
}

.plan-upgrade__plan-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  min-height: 40px;
}

.plan-upgrade__plan-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.plan-upgrade__plan-tag {
  flex-shrink: 0;
  padding: 2px 6px;
  border-radius: 4px;
  background: #1a1a1a;
  color: #fff;
  font-size: 11px;
  line-height: 1.2;
  writing-mode: horizontal-tb;
}

.plan-upgrade__price-row {
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin-bottom: 6px;
}

.plan-upgrade__currency {
  font-size: 16px;
  font-weight: 600;
}

.plan-upgrade__price {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.03em;
}

.plan-upgrade__price-suffix {
  margin-left: 4px;
  font-size: 13px;
  color: #666;
}

.plan-upgrade__price-note {
  margin: 0 0 14px;
  min-height: 36px;
  font-size: 12px;
  line-height: 1.45;
  color: #888;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.plan-upgrade__cta {
  width: 100%;
  margin-bottom: 14px;
  padding: 10px 12px;
  border: none;
  border-radius: 10px;
  background: #1a1a1a;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.plan-upgrade__cta:hover:not(:disabled) {
  opacity: 0.88;
}

.plan-upgrade__cta:disabled {
  background: #f0f0f0;
  color: #aaa;
  cursor: not-allowed;
}

.plan-upgrade__cta.is-loading {
  opacity: 0.65;
  pointer-events: none;
}

.plan-upgrade__quota-line {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #ececec;
  font-size: 13px;
  font-weight: 600;
}

.plan-upgrade__quota-badge {
  font-size: 11px;
  font-weight: 500;
  color: #6b5cff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-upgrade__features {
  margin: 0;
  padding: 0;
  list-style: none;
}

.plan-upgrade__features li {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  line-height: 1.45;
  color: #555;
}

.plan-upgrade__check {
  color: #c4c4c4;
}

.plan-upgrade__compare-page {
  padding-top: 28px;
}

.plan-upgrade__compare-title {
  margin: 0 0 6px;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
}

.plan-upgrade__compare-sub {
  margin: 0 0 16px;
  text-align: center;
  font-size: 13px;
  color: #4d6bfe;
}

.plan-upgrade__table-wrap {
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid #eee;
}

.plan-upgrade__table {
  width: 100%;
  min-width: 640px;
  border-collapse: collapse;
  font-size: 13px;
}

.plan-upgrade__table th,
.plan-upgrade__table td {
  padding: 12px 10px;
  text-align: center;
  border-bottom: 1px solid #f0f0f0;
}

.plan-upgrade__table td {
  white-space: normal;
  word-break: break-word;
  max-width: 180px;
}

.plan-upgrade__group-row th {
  text-align: left !important;
  background: #f7f7f7 !important;
  font-weight: 600;
  color: #1a1a1a;
  padding-left: 16px;
}

.plan-upgrade__table thead th {
  background: #f7f7f7;
  font-weight: 600;
}

.plan-upgrade__table-corner,
.plan-upgrade__table tbody th[scope='row'] {
  text-align: left;
  color: #888;
  font-weight: 500;
  min-width: 120px;
  padding-left: 16px;
}

.plan-upgrade__table .is-selected {
  background: #faf9ff;
  font-weight: 600;
}

.plan-upgrade__empty {
  padding: 48px 0;
}
</style>

<style>
.plan-upgrade-dialog .t-dialog {
  max-width: calc(100vw - 32px);
  border-radius: 16px;
  overflow: hidden;
}

.plan-upgrade-dialog .t-dialog__header {
  display: none;
}

.plan-upgrade-dialog .t-dialog__close {
  display: none;
}

.plan-upgrade-dialog .t-dialog__body {
  padding: 12px 20px 24px !important;
  background: #fff;
  max-height: calc(94vh - 24px);
  overflow-y: auto;
}
</style>
