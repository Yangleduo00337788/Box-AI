<template>
  <div class="resource-empty">
    <t-empty :description="config.description">
      <template #action>
        <t-space>
          <t-button theme="primary" @click="emit('create')">{{ config.createLabel }}</t-button>
          <t-button variant="outline" @click="goMarket">{{ config.marketLabel }}</t-button>
        </t-space>
      </template>
    </t-empty>
    <p class="resource-empty__hint">{{ config.hint }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { RESOURCE_MANAGE_EMPTY } from '@/constants/resourceEmpty'
import { pluginMarketPathForCategory } from '@/constants/resourceRoutes'

const props = defineProps<{
  category: string
}>()

const emit = defineEmits<{
  create: []
}>()

const router = useRouter()

const config = computed(() => {
  return RESOURCE_MANAGE_EMPTY[props.category] ?? {
    description: '暂无资源',
    createLabel: '新建',
    marketLabel: '浏览模板',
    hint: '创建后本工作空间成员共用，或前往插件市场安装模板',
  }
})

function goMarket() {
  router.push(pluginMarketPathForCategory(props.category))
}
</script>

<style scoped>
.resource-empty {
  padding: 48px 16px;
  text-align: center;
}

.resource-empty__hint {
  max-width: 420px;
  margin: 12px auto 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  line-height: 1.6;
}
</style>
