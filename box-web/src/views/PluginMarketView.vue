<template>
  <div class="plugin-market">
    <page-header
      title="插件市场"
      desc="按分类浏览工作流、知识库、工具与 MCP 插件，一键安装到你的工作空间"
    />

    <t-loading :loading="categoriesLoading" size="small">
      <div v-if="categories.length" class="plugin-market__tabs" role="tablist" aria-label="插件分类">
        <button
          v-for="item in categories"
          :key="item.value"
          type="button"
          role="tab"
          class="category-tab"
          :class="{ 'category-tab--active': activeCategory === item.value }"
          :aria-selected="activeCategory === item.value"
          @click="selectCategory(item.value)"
        >
          <span>{{ item.label }}</span>
        </button>
      </div>
    </t-loading>

    <div v-if="activeCategoryMeta" class="plugin-market__intro">
      <h2 class="plugin-market__intro-title">{{ activeCategoryMeta.label }}</h2>
      <p class="plugin-market__intro-desc">{{ activeCategoryMeta.desc }}</p>
    </div>

    <t-loading :loading="loading" size="small">
      <div v-if="plugins.length" class="plugin-market__grid">
        <article v-for="item in plugins" :key="item.id" class="plugin-card">
          <div class="plugin-card__head">
            <t-tag size="small" variant="light">{{ activeCategoryMeta?.label || item.category }}</t-tag>
          </div>
          <h3 class="plugin-card__title">{{ item.title }}</h3>
          <p class="plugin-card__desc">{{ item.description || '暂无描述' }}</p>
          <t-button
            variant="outline"
            block
            :loading="actingId === item.id"
            :theme="item.installed ? 'default' : 'primary'"
            @click="handleInstall(item)"
          >
            {{ item.installed ? '已安装' : '安装到工作空间' }}
          </t-button>
        </article>
      </div>
      <t-empty v-else description="该分类暂无插件" />
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import {
  fetchPluginCategories,
  installPlugin,
  listPlugins,
  uninstallPlugin,
  type PluginCatalogVO,
  type PluginCategoryVO,
} from '@/api/plugin'
import { extractApiError } from '@/api/apiError'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const categoriesLoading = ref(false)
const actingId = ref<number | null>(null)
const plugins = ref<PluginCatalogVO[]>([])
const categories = ref<PluginCategoryVO[]>([])

const activeCategory = computed(() => {
  const category = route.query.category
  if (typeof category === 'string' && categories.value.some((item) => item.value === category)) {
    return category
  }
  return categories.value[0]?.value || ''
})

const activeCategoryMeta = computed(() =>
  categories.value.find((item) => item.value === activeCategory.value),
)

async function loadCategories() {
  categoriesLoading.value = true
  try {
    const { data } = await fetchPluginCategories()
    categories.value = data.data || []
    if (
      typeof route.query.category === 'string' &&
      route.query.category &&
      !categories.value.some((item) => item.value === route.query.category)
    ) {
      router.replace({
        path: '/plugin-market',
        query: categories.value[0] ? { category: categories.value[0].value } : {},
      })
    }
  } catch (error) {
    categories.value = []
    MessagePlugin.error(extractApiError(error, '加载分类失败'))
  } finally {
    categoriesLoading.value = false
  }
}

async function loadPlugins() {
  if (!activeCategory.value) {
    plugins.value = []
    return
  }
  loading.value = true
  try {
    const { data } = await listPlugins(activeCategory.value)
    plugins.value = data.data || []
  } catch (error) {
    plugins.value = []
    MessagePlugin.error(extractApiError(error, '加载插件失败'))
  } finally {
    loading.value = false
  }
}

function selectCategory(category: string) {
  if (category === activeCategory.value) return
  router.replace({ path: '/plugin-market', query: { category } })
}

async function handleInstall(item: PluginCatalogVO) {
  actingId.value = item.id
  try {
    if (item.installed) {
      await uninstallPlugin(item.id)
      MessagePlugin.success(`已移除「${item.title}」`)
    } else {
      await installPlugin(item.id)
      MessagePlugin.success(`已安装「${item.title}」`)
    }
    await loadPlugins()
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '操作失败'))
  } finally {
    actingId.value = null
  }
}

onMounted(async () => {
  await loadCategories()
  await loadPlugins()
})

watch(
  () => route.query.category,
  () => {
    loadPlugins()
  },
)
</script>

<style scoped>
.plugin-market__tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  overflow-x: auto;
  scrollbar-width: none;
}

.plugin-market__tabs::-webkit-scrollbar {
  display: none;
}

.category-tab {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  height: 36px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--box-muted);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.category-tab:hover {
  background: rgba(0, 0, 0, 0.04);
  color: var(--box-ink);
}

.category-tab--active {
  background: rgba(0, 0, 0, 0.04);
  color: var(--box-ink);
  font-weight: 500;
}

.plugin-market__intro {
  margin-bottom: 20px;
}

.plugin-market__intro-title {
  margin: 0 0 4px;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.plugin-market__intro-desc {
  margin: 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.plugin-market__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.plugin-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 168px;
  padding: 16px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-md);
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
}

.plugin-card__head {
  display: flex;
  align-items: center;
}

.plugin-card__title {
  margin: 0;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.plugin-card__desc {
  flex: 1;
  margin: 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  line-height: 1.55;
}

.plugin-card :deep(.t-button) {
  border-radius: 8px;
}
</style>
