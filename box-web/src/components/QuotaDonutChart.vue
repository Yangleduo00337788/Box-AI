<template>
  <article class="quota-donut">
    <div ref="chartEl" class="quota-donut__chart" />
    <div class="quota-donut__meta">
      <div class="quota-donut__label">{{ label }}</div>
      <div class="quota-donut__value">{{ formatNumber(used) }} / {{ formatNumber(total) }}</div>
      <p v-if="hint" class="quota-donut__hint">{{ hint }}</p>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { appPreferences } from '@/composables/useAppPreferences'

const props = defineProps<{
  label: string
  used: number
  total: number
  hint?: string
  active?: boolean
}>()

const chartEl = ref<HTMLElement | null>(null)
let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const percent = computed(() => {
  if (!props.total) return 0
  return Math.min(100, Math.round((props.used / props.total) * 100))
})

function formatNumber(value: number) {
  return Number(value || 0).toLocaleString()
}

function cssVar(name: string, fallback: string) {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

function usedColor() {
  const ratio = percent.value
  if (ratio >= 90) return cssVar('--td-error-color', '#e34d59')
  if (ratio >= 75) return cssVar('--td-warning-color', '#ed7b2f')
  return cssVar('--box-ink', '#222222')
}

function hasSize() {
  const el = chartEl.value
  return Boolean(el && el.clientWidth >= 80 && el.clientHeight >= 80)
}

function render() {
  const el = chartEl.value
  if (!el || !hasSize()) return
  if (!chart) {
    chart = echarts.init(el, undefined, {
      width: el.clientWidth,
      height: el.clientHeight,
    })
  } else {
    chart.resize({ width: el.clientWidth, height: el.clientHeight })
  }
  const used = Math.max(0, props.used)
  const remaining = Math.max(0, props.total - used)
  const ink = cssVar('--box-ink', '#222222')
  const track = cssVar('--td-bg-color-secondarycontainer', '#eee')
  chart.setOption({
    animation: false,
    tooltip: {
      trigger: 'item',
      confine: true,
      appendTo: 'body',
      formatter: (raw: unknown) => {
        const item = raw as { name: string; value: number; percent: number }
        return `${item.name} ${Number(item.value || 0).toLocaleString()}（${Number(item.percent || 0).toFixed(1)}%）`
      },
    },
    series: [
      {
        type: 'pie',
        radius: ['62%', '80%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        stillShowZeroSum: false,
        silent: false,
        label: {
          show: true,
          position: 'center',
          formatter: `${percent.value}%`,
          fontSize: 16,
          fontWeight: 600,
          color: ink,
        },
        emphasis: {
          scale: false,
          label: {
            show: true,
            formatter: `${percent.value}%`,
            fontSize: 16,
            fontWeight: 600,
            color: ink,
          },
        },
        labelLine: { show: false },
        data: [
          { value: used, name: '已用', itemStyle: { color: usedColor() } },
          { value: remaining > 0 ? remaining : used > 0 ? 0 : 1, name: '剩余', itemStyle: { color: track } },
        ],
      },
    ],
  })
}

function resize() {
  render()
}

watch(
  () => [props.used, props.total, props.label, appPreferences.theme, props.active],
  () => render(),
)

onMounted(() => {
  render()
  if (chartEl.value) {
    resizeObserver = new ResizeObserver(() => render())
    resizeObserver.observe(chartEl.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})

defineExpose({ resize })
</script>

<style scoped>
.quota-donut {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 148px;
  overflow: hidden;
}

.quota-donut__chart {
  position: relative;
  width: 140px;
  height: 140px;
  overflow: hidden;
}

.quota-donut__meta {
  text-align: center;
  max-width: 100%;
}

.quota-donut__label {
  font-size: 13px;
  color: var(--box-ink);
}

.quota-donut__value {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 500;
  color: var(--box-ink);
}

.quota-donut__hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--box-muted);
}
</style>
