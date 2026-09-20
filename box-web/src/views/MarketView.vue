<template>
  <div class="agent-market">
    <page-header title="智能体市场" desc="选用平台模板，一键启用到你的工作空间">
      <template #actions>
        <t-button variant="outline" @click="router.push('/plugin-market')">插件市场</t-button>
      </template>
    </page-header>

    <div class="plugin-market__toolbar">
      <t-input
        v-model="keyword"
        placeholder="搜索模板名称、分类或模型"
        clearable
        class="plugin-market__search"
      >
        <template #prefix-icon>
          <t-icon name="search" />
        </template>
      </t-input>
      <span class="plugin-market__count">{{ countLabel }}</span>
    </div>

    <section class="plugin-market__section">
      <div class="plugin-market__section-head">
        <h2 class="plugin-market__section-title">平台模板</h2>
        <p class="plugin-market__section-desc">启用后将复制为本工作空间智能体，可继续编排、调试与发布。</p>
      </div>

      <template v-if="loading">
        <div class="plugin-market__list" aria-busy="true">
          <div v-for="n in 6" :key="n" class="plugin-card plugin-card--skeleton" />
        </div>
      </template>
      <template v-else>
        <div v-if="filtered.length" class="plugin-market__list">
          <resource-item-card
            v-for="item in filtered"
            :key="item.id"
            :title="item.name"
            :description="item.description"
            icon="chat"
            tone="violet"
          >
            <template #tags>
              <t-tag size="small" variant="light" theme="primary">平台模板</t-tag>
              <t-tag v-if="item.category" size="small" variant="light">{{ item.category }}</t-tag>
            </template>
            <template #meta>
              {{ item.platformModelName || '平台模型' }} · {{ item.installCount ?? 0 }} 次启用
            </template>
            <template #actions>
              <t-button
                theme="primary"
                size="small"
                :loading="enablingId === item.id"
                @click="enable(item)"
              >
                一键启用
              </t-button>
            </template>
          </resource-item-card>
        </div>
        <div v-else class="plugin-market__empty">
          <t-empty :description="emptyDescription" />
        </div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import ResourceItemCard from '@/components/ResourceItemCard.vue'
import { enableMarketTemplate, listMarketTemplates, type AgentTemplateVO } from '@/api/market'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import { filterResourcesByKeyword } from '@/constants/resourceManage'

const router = useRouter()
const loading = ref(false)
const enablingId = ref<number | null>(null)
const templates = ref<AgentTemplateVO[]>([])
const keyword = ref('')

const filtered = computed(() =>
  filterResourcesByKeyword(templates.value, keyword.value, (item) => [
    item.name,
    item.description,
    item.category,
    item.platformModelName,
    item.templateCode,
  ]),
)

const countLabel = computed(() => `${filtered.value.length} 个模板`)

const emptyDescription = computed(() => {
  if (keyword.value.trim()) return '没有匹配的模板'
  return '暂无上架模板'
})

async function loadTemplates() {
  loading.value = true
  try {
    const { data } = await listMarketTemplates()
    templates.value = data.data || []
  } finally {
    loading.value = false
  }
}

async function enable(item: AgentTemplateVO) {
  enablingId.value = item.id
  try {
    const { data } = await enableMarketTemplate(item.id)
    MessagePlugin.success(`「${item.name}」已启用`)
    if (data.data?.id) {
      router.push(`/agents/${data.data.id}/builder`)
    }
  } finally {
    enablingId.value = null
  }
}

onMounted(loadTemplates)
useReloadOnWorkspaceChange(loadTemplates)
</script>
