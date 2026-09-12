<template>
  <div class="billing-page">
    <h2 class="settings-title">账单概览</h2>
    <p class="settings-desc">查看当前套餐与本月用量估算（按套餐月费计费）。</p>
    <t-loading :loading="loading">
      <t-descriptions v-if="overview" :column="1" bordered>
        <t-descriptions-item label="账期">{{ overview.period }}</t-descriptions-item>
        <t-descriptions-item label="当前套餐">{{ overview.planName }}</t-descriptions-item>
        <t-descriptions-item label="套餐月费">
          {{ overview.currency }} {{ overview.planPriceMonthly }}
        </t-descriptions-item>
        <t-descriptions-item label="本月 AI 调用">
          {{ overview.usedAiCalls }} / {{ overview.quotaAiCalls ?? '不限' }}
        </t-descriptions-item>
        <t-descriptions-item label="本月 Token">
          {{ overview.usedTokens }} / {{ overview.quotaTokens ?? '不限' }}
        </t-descriptions-item>
        <t-descriptions-item label="预估应付">
          {{ overview.currency }} {{ overview.estimatedAmount }}
        </t-descriptions-item>
      </t-descriptions>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchBillingOverview, type BillingOverviewVO } from '@/api/billing'

const loading = ref(false)
const overview = ref<BillingOverviewVO | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await fetchBillingOverview()
    overview.value = data.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.billing-page {
  max-width: 720px;
}
</style>
