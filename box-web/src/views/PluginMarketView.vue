<template>
  <div class="plugin-market">
    <page-header
      title="插件市场"
      desc="浏览模板并安装到工作空间；管理已有资源请点右侧按钮"
    >
      <template #actions>
        <t-space>
          <t-button
            v-if="activeManagePath"
            variant="outline"
            @click="goManageMine"
          >
            {{ manageButtonLabel }}
          </t-button>
          <t-button variant="outline" @click="router.push('/market')">智能体市场</t-button>
        </t-space>
      </template>
    </page-header>

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

    <p v-if="activeCategoryMeta" class="plugin-market__category-desc">{{ activeCategoryMeta.desc }}</p>

    <t-loading :loading="loading" size="small">
      <h2 v-if="plugins.length" class="plugin-market__section-title">推荐模板</h2>
      <div v-if="plugins.length" class="plugin-market__grid">
        <article v-for="item in plugins" :key="item.id" class="plugin-card">
          <div class="plugin-card__head">
            <t-tag size="small" variant="light">{{ activeCategoryMeta?.label || item.category }}</t-tag>
          </div>
          <h3 class="plugin-card__title">{{ item.title }}</h3>
          <p class="plugin-card__desc">{{ item.description || '暂无描述' }}</p>
          <div class="plugin-card__actions">
            <t-button
              variant="outline"
              block
              :loading="actingId === item.id"
              :theme="item.installed ? 'default' : 'primary'"
              @click="handleInstall(item)"
            >
              {{ item.installed ? '移除' : '安装到工作空间' }}
            </t-button>
            <t-button
              v-if="item.installed && item.targetPath"
              variant="text"
              block
              @click="goToResource(item.targetPath!)"
            >
              前往使用
            </t-button>
          </div>
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
import { managePathForCategory } from '@/constants/resourceRoutes'

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

const activeManagePath = computed(() => managePathForCategory(activeCategory.value))

const manageButtonLabel = computed(() => {
  const label = activeCategoryMeta.value?.label
  if (!label) return '管理我的资源'
  if (/^[A-Z]{2,}$/.test(label)) return `管理我的 ${label}`
  return `管理我的${label}`
})

function goManageMine() {
  const path = activeManagePath.value
  if (path) router.push(path)
}

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

function goToResource(path: string) {
  router.push(path)
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

.plugin-market__category-desc {
  margin: -8px 0 20px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.plugin-market__section-title {
  margin: 0 0 12px;
  font: var(--td-font-title-small);
  color: var(--box-ink);
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

.plugin-card__actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.plugin-card :deep(.t-button) {
  border-radius: 8px;
}
</style>
