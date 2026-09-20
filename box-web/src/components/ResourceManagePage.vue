<template>
  <div class="resource-manage-page">
    <page-header :title="meta.title" :desc="meta.desc" :back-to="backTo" :back-label="backLabel">
      <template #actions>
        <t-button v-if="canCreate" theme="primary" @click="emit('create')">
          <template #icon><t-icon name="add" /></template>
          {{ meta.createLabel }}
        </t-button>
      </template>
    </page-header>

    <nav class="resource-manage__tabs" aria-label="资源类型">
      <router-link
        v-for="item in RESOURCE_MANAGE_CATEGORIES"
        :key="item.value"
        :to="item.path"
        class="resource-manage__tab"
        :class="{ 'is-active': item.value === category }"
      >
        <t-icon :name="item.icon" />
        {{ item.label }}
      </router-link>
    </nav>

    <div class="resource-manage__toolbar">
      <t-input
        :model-value="keyword"
        :placeholder="meta.searchPlaceholder"
        clearable
        class="resource-manage__search"
        @update:model-value="onKeyword"
      >
        <template #prefix-icon>
          <t-icon name="search" />
        </template>
      </t-input>
      <t-tag variant="light" theme="primary">本空间共享</t-tag>
      <span class="resource-manage__count">{{ countLabel }}</span>
    </div>

    <t-loading :loading="loading" size="small">
      <slot />
    </t-loading>
    <slot name="dialogs" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { useResourceManageBack } from '@/composables/useResourceManageBack'
import { RESOURCE_MANAGE_CATEGORIES, resourceManageCategory } from '@/constants/resourceManage'

const props = defineProps<{
  category: string
  keyword: string
  total: number
  filtered: number
  loading?: boolean
  canCreate?: boolean
}>()

const emit = defineEmits<{
  create: []
  'update:keyword': [string]
}>()

const { backTo, backLabel } = useResourceManageBack(props.category)

const meta = computed(() => {
  return (
    resourceManageCategory(props.category) ?? {
      value: props.category,
      path: '/',
      label: props.category,
      title: props.category,
      desc: '',
      icon: 'app',
      tone: 'ink',
      createLabel: '新建',
      searchPlaceholder: '搜索',
    }
  )
})

const countLabel = computed(() => {
  if (props.keyword.trim() && props.filtered !== props.total) {
    return `${props.filtered} / ${props.total}`
  }
  return `${props.total} 项`
})

function onKeyword(value: string | number) {
  emit('update:keyword', String(value ?? ''))
}
</script>
