<template>
  <span v-if="items.length" class="box-chat-message-plugins">
    <template v-for="(item, index) in items" :key="item.id">
      <button type="button" class="box-chat-plugin-inline" @click="openPlugin(item)">
        <t-icon :name="pluginCategoryIcon(item.category)" class="box-chat-plugin-inline__icon" />
        <span>{{ pillLabel(item) }}</span>
      </button>
      <span v-if="index < items.length - 1" class="box-chat-plugin-inline__sep"> </span>
    </template>
  </span>
</template>

<script setup lang="ts">
import '@/styles/box-chat-bubble.css'
import { useRouter } from 'vue-router'
import type { MessagePluginMeta } from '@/utils/messageMetadata'
import { pluginCategoryIcon, pluginPillLabel } from '@/constants/pluginCatalogMeta'
import { pluginMarketPathForCategory } from '@/constants/resourceRoutes'

defineProps<{
  items: MessagePluginMeta[]
}>()

const router = useRouter()

function pillLabel(item: MessagePluginMeta) {
  return pluginPillLabel({ title: item.title })
}

function openPlugin(item: MessagePluginMeta) {
  const path = pluginMarketPathForCategory(item.category)
  if (item.category === 'skills') {
    router.push({ path: '/plugin-market', query: { tab: 'skills' } })
    return
  }
  router.push(path)
}
</script>

<style scoped>
.box-chat-message-plugins {
  display: inline-flex;
  align-items: center;
  margin: 0;
}

.box-chat-plugin-inline__sep {
  white-space: pre;
}
</style>
