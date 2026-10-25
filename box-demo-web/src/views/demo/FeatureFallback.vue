<script setup lang="ts">
import { computed } from 'vue'
import { adminNav, consumerNav, type DemoFeature } from '@/data/demoRegistry'
import { screenshotImage } from '@/utils/assets'

const props = defineProps<{
  realm: 'web' | 'admin'
  section: string
}>()

const feature = computed((): DemoFeature | undefined => {
  const list = props.realm === 'admin' ? adminNav : consumerNav
  const path = `/demo/${props.realm}/${props.section}`
  return list.find((f) => f.path === path)
})

const shotSrc = computed(() => {
  const name = feature.value?.screenshot
  if (!name) return ''
  return screenshotImage(name)
})

const tableCols = computed(() =>
  (feature.value?.adminColumns ?? []).map((c) => ({
    colKey: c.colKey,
    title: c.title,
    width: c.width,
  })),
)
</script>

<template>
  <div v-if="feature" class="demo-page">
    <header class="demo-page__header">
      <h1>{{ feature.title }}</h1>
      <p>{{ feature.summary }}</p>
      <t-space v-if="feature.highlights?.length" size="small" style="margin-top: 12px">
        <t-tag v-for="h in feature.highlights" :key="h" variant="light">{{ h }}</t-tag>
      </t-space>
    </header>

    <t-card v-if="feature.kind === 'admin-table' && feature.adminRows" title="模拟数据" style="margin-bottom: 16px">
      <t-table :data="feature.adminRows" :columns="tableCols" row-key="colKey" size="medium" />
    </t-card>

    <div v-if="shotSrc" class="demo-shot">
      <img :src="shotSrc" :alt="feature.title" loading="lazy" />
    </div>
    <t-alert v-else theme="info" title="界面预览">
      该模块在完整产品中与「{{ feature.title }}」路由一致；可结合左侧其它已配图模块了解整体体验。
    </t-alert>
  </div>
  <div v-else class="demo-page">
    <t-card title="未找到演示模块">
      <p>请从左侧菜单选择功能。</p>
      <t-button theme="primary" @click="$router.push('/demo/web/chat')">前往对话工作台</t-button>
    </t-card>
  </div>
</template>
