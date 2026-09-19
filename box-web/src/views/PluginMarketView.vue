<template>
  <div class="plugin-market">
    <page-header title="插件市场" desc="发现插件与技能，扩展当前工作空间的工具、知识库、工作流和 MCP">
      <template #actions>
        <t-space>
          <t-button variant="outline" @click="router.push('/market')">智能体市场</t-button>
          <t-dropdown
            v-if="manageOptions.length"
            :options="manageOptions"
            trigger="click"
            @click="onManageSelect"
          >
            <t-button variant="outline">
              管理
              <template #suffix><t-icon name="chevron-down" /></template>
            </t-button>
          </t-dropdown>
        </t-space>
      </template>
    </page-header>

    <div class="plugin-market__layout">
      <aside class="plugin-market__nav" aria-label="市场分类">
        <p class="plugin-market__nav-label">发现</p>
        <button
          type="button"
          class="plugin-market__nav-item"
          :class="{ 'is-active': isNavActive('all') }"
          @click="selectDiscover('all')"
        >
          <t-icon name="app" />
          <span>全部插件</span>
          <em>{{ pluginCount }}</em>
        </button>
        <button
          type="button"
          class="plugin-market__nav-item"
          :class="{ 'is-active': isNavActive('skills') }"
          @click="selectDiscover('skills')"
        >
          <t-icon name="education" />
          <span>技能</span>
          <em>{{ skillCount }}</em>
        </button>

        <p class="plugin-market__nav-label">分类</p>
        <template v-if="categoriesLoading">
          <span v-for="n in 4" :key="n" class="plugin-market__nav-skel" />
        </template>
        <button
          v-for="item in pluginCategories"
          v-else
          :key="item.value"
          type="button"
          class="plugin-market__nav-item"
          :class="{ 'is-active': isNavActive(item.value) }"
          :title="item.desc || item.label"
          @click="selectCategory(item.value)"
        >
          <t-icon :name="iconOf(item.value)" />
          <span>{{ item.label }}</span>
          <em>{{ countOf(item.value) }}</em>
        </button>

        <p class="plugin-market__nav-label">我的</p>
        <button
          type="button"
          class="plugin-market__nav-item"
          :class="{ 'is-active': isNavActive('installed') }"
          @click="selectInstalled"
        >
          <t-icon name="check-circle" />
          <span>已安装</span>
          <em>{{ installedCount }}</em>
        </button>
      </aside>

      <div class="plugin-market__main">
        <div class="plugin-market__toolbar">
          <t-input
            v-model="keyword"
            placeholder="搜索插件、技能或能力说明"
            clearable
            class="plugin-market__search"
          >
            <template #prefix-icon>
              <t-icon name="search" />
            </template>
          </t-input>
          <span class="plugin-market__count">{{ resultCountLabel }}</span>
        </div>

        <template v-if="loading">
          <div class="plugin-market__list" aria-busy="true">
            <div v-for="n in 6" :key="n" class="plugin-card plugin-card--skeleton" />
          </div>
        </template>

        <template v-else>
          <section
            v-for="section in catalogSections"
            :key="section.key"
            class="plugin-market__section"
          >
            <div v-if="section.title" class="plugin-market__section-head">
              <h2 class="plugin-market__section-title">{{ section.title }}</h2>
              <p v-if="section.desc" class="plugin-market__section-desc">{{ section.desc }}</p>
            </div>
            <div v-if="section.items.length" class="plugin-market__list">
              <article
                v-for="item in section.items"
                :key="`${section.key}-${item.id}`"
                class="plugin-card"
                :class="{ 'is-installed': item.installed }"
                @click="openDetail(item)"
              >
                <span class="plugin-card__icon" :class="`plugin-card__icon--${toneOf(item.category)}`">
                  <t-icon :name="iconOf(item.category)" size="22px" />
                </span>
                <div class="plugin-card__body">
                  <div class="plugin-card__title-row">
                    <h3 class="plugin-card__title">{{ item.title }}</h3>
                    <t-tag v-if="item.installed" size="small" variant="light" theme="success">已安装</t-tag>
                  </div>
                  <p class="plugin-card__desc">{{ item.description || '暂无描述' }}</p>
                  <div class="plugin-card__meta">
                    <span>{{ categoryLabel(item.category) || '扩展' }}</span>
                  </div>
                </div>
                <div class="plugin-card__actions" @click.stop>
                  <t-button
                    v-if="!item.installed"
                    theme="primary"
                    variant="outline"
                    size="small"
                    :loading="actingId === item.id"
                    @click="handleInstall(item)"
                  >
                    安装
                  </t-button>
                  <template v-else>
                    <t-button
                      v-if="item.targetPath"
                      theme="primary"
                      size="small"
                      @click="openInstalled(item)"
                    >
                      打开
                    </t-button>
                    <t-button
                      variant="outline"
                      theme="default"
                      size="small"
                      :loading="actingId === item.id"
                      @click="handleInstall(item)"
                    >
                      移除
                    </t-button>
                  </template>
                </div>
              </article>
            </div>
          </section>

          <div v-if="!categoriesLoading && !filteredPlugins.length" class="plugin-market__empty">
            <t-empty :description="emptyDescription">
              <template #action>
                <t-button v-if="hasActiveFilters" variant="outline" @click="resetFilters">清除筛选</t-button>
              </template>
            </t-empty>
          </div>
        </template>
      </div>
    </div>

    <t-drawer
      v-model:visible="detailVisible"
      size="520px"
      destroy-on-close
      :close-btn="true"
    >
      <template #header>
        <div v-if="detailItem" class="plugin-detail__header">
          <span class="plugin-card__icon" :class="`plugin-card__icon--${toneOf(detailItem.category)}`">
            <t-icon :name="iconOf(detailItem.category)" size="24px" />
          </span>
          <div class="plugin-detail__heading">
            <strong>{{ detailItem.title }}</strong>
            <span>{{ categoryLabel(detailItem.category) || '扩展' }}</span>
          </div>
        </div>
        <span v-else>扩展详情</span>
      </template>
      <div v-if="detailItem" class="plugin-detail">
        <p class="plugin-detail__desc">{{ detailItem.description || '暂无描述' }}</p>
        <dl class="plugin-detail__facts">
          <div>
            <dt>能力类型</dt>
            <dd>{{ categoryLabel(detailItem.category) || '—' }}</dd>
          </div>
          <div>
            <dt>安装范围</dt>
            <dd>当前工作空间</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ detailItem.installed ? '已安装，可在对应资源页使用' : '未安装' }}</dd>
          </div>
        </dl>
        <p class="plugin-detail__hint">安装后会在工作空间创建对应资源，可随时从市场移除。</p>
      </div>
      <template #footer>
        <t-space v-if="detailItem">
          <t-button
            v-if="!detailItem.installed"
            theme="primary"
            :loading="actingId === detailItem.id"
            @click="handleInstall(detailItem)"
          >
            安装到工作空间
          </t-button>
          <template v-else>
            <t-button v-if="detailItem.targetPath" theme="primary" @click="openInstalled(detailItem)">
              打开
            </t-button>
            <t-button
              variant="outline"
              theme="warning"
              :loading="actingId === detailItem.id"
              @click="handleInstall(detailItem)"
            >
              移除
            </t-button>
          </template>
        </t-space>
      </template>
    </t-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'
import type { DropdownOption } from 'tdesign-vue-next'
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
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import { PLUGIN_CATEGORY_MANAGE_PATHS } from '@/constants/resourceRoutes'

const ALL_CATEGORY = ''
const SKILLS_CATEGORY = 'skills'
const CATEGORY_META: Record<string, { icon: string; tone: string }> = {
  workflows: { icon: 'tree-square-dot', tone: 'ink' },
  knowledge: { icon: 'folder', tone: 'sky' },
  tools: { icon: 'tools', tone: 'stone' },
  mcp: { icon: 'server', tone: 'violet' },
  skills: { icon: 'education', tone: 'teal' },
}

type MarketTab = 'plugins' | 'skills'
type DiscoverView = 'all' | 'skills'
type CatalogSection = {
  key: string
  title: string
  desc: string
  items: PluginCatalogVO[]
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const categoriesLoading = ref(false)
const actingId = ref<number | null>(null)
const plugins = ref<PluginCatalogVO[]>([])
const categories = ref<PluginCategoryVO[]>([])
const keyword = ref('')
const detailItem = ref<PluginCatalogVO | null>(null)
const detailVisible = ref(false)

const pluginCategories = computed(() =>
  categories.value.filter((item) => item.value !== SKILLS_CATEGORY),
)

const skillCategory = computed(() =>
  categories.value.find((item) => item.value === SKILLS_CATEGORY),
)

const activeTab = computed<MarketTab>(() => {
  if (route.query.tab === 'skills' || route.query.category === SKILLS_CATEGORY) return 'skills'
  return 'plugins'
})

const activeCategory = computed(() => {
  const category = route.query.category
  if (activeTab.value === 'skills') return SKILLS_CATEGORY
  if (typeof category === 'string' && pluginCategories.value.some((item) => item.value === category)) {
    return category
  }
  return ALL_CATEGORY
})

const installFilter = computed(() => (route.query.filter === 'installed' ? 'installed' : 'all'))

const discoverView = computed<DiscoverView>(() => {
  if (activeTab.value === 'skills') return 'skills'
  return 'all'
})

const manageOptions = computed<DropdownOption[]>(() =>
  Object.entries(PLUGIN_CATEGORY_MANAGE_PATHS)
    .filter(([category]) => categories.value.some((item) => item.value === category))
    .map(([category, path]) => ({
      content: `我的${categories.value.find((item) => item.value === category)?.label || category}`,
      value: path,
    })),
)

const pluginCount = computed(
  () => plugins.value.filter((item) => item.category !== SKILLS_CATEGORY).length,
)

const skillCount = computed(
  () => plugins.value.filter((item) => item.category === SKILLS_CATEGORY).length,
)

const installedCount = computed(() => plugins.value.filter((item) => item.installed).length)

const hasActiveFilters = computed(
  () =>
    Boolean(keyword.value.trim()) ||
    installFilter.value !== 'all' ||
    Boolean(activeCategory.value) ||
    discoverView.value !== 'all',
)

const filteredPlugins = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return plugins.value.filter((item) => {
    if (installFilter.value === 'installed') {
      if (!item.installed) return false
      if (activeTab.value === 'skills' && item.category !== SKILLS_CATEGORY) return false
    } else if (activeTab.value === 'skills') {
      if (item.category !== SKILLS_CATEGORY) return false
    } else if (item.category === SKILLS_CATEGORY) {
      return false
    } else if (activeCategory.value && item.category !== activeCategory.value) {
      return false
    }
    if (!q) return true
    const haystack = `${item.title} ${item.description || ''} ${categoryLabel(item.category)}`.toLowerCase()
    return haystack.includes(q)
  })
})

const catalogSections = computed<CatalogSection[]>(() => {
  if (!filteredPlugins.value.length) return []
  return [{ key: 'list', title: listTitle.value, desc: listDesc.value, items: sortItems(filteredPlugins.value) }]
})

const listTitle = computed(() => {
  if (keyword.value.trim()) return '搜索结果'
  if (installFilter.value === 'installed') return '已安装'
  if (activeTab.value === 'skills') return skillCategory.value?.label || '技能'
  if (activeCategory.value) return categoryLabel(activeCategory.value)
  return ''
})

const listDesc = computed(() => {
  if (keyword.value.trim() || installFilter.value === 'installed') return ''
  if (activeCategory.value) {
    return pluginCategories.value.find((item) => item.value === activeCategory.value)?.desc || ''
  }
  if (activeTab.value === 'skills') return skillCategory.value?.desc || '可复用的技能包，安装后在对话中调用'
  return ''
})

const resultCountLabel = computed(() => {
  const total = filteredPlugins.value.length
  return `${total} 个扩展`
})

const emptyDescription = computed(() => {
  if (hasActiveFilters.value) return '没有匹配的扩展'
  return '该分类暂无扩展'
})

function sortItems(items: PluginCatalogVO[]) {
  return [...items].sort((left, right) => left.title.localeCompare(right.title, 'zh-CN'))
}

function isNavActive(key: string) {
  if (key === 'installed') return installFilter.value === 'installed'
  if (installFilter.value === 'installed') return false
  if (key === 'skills') return activeTab.value === 'skills'
  if (activeTab.value === 'skills') return false
  if (key === 'all') return !activeCategory.value && discoverView.value === 'all'
  return activeCategory.value === key
}

function countOf(category: string) {
  return plugins.value.filter((item) => item.category === category).length
}

function iconOf(category: string) {
  return CATEGORY_META[category]?.icon || 'app'
}

function toneOf(category: string) {
  return CATEGORY_META[category]?.tone || 'ink'
}

function categoryLabel(value: string) {
  return categories.value.find((item) => item.value === value)?.label || ''
}

function onManageSelect(option: DropdownOption) {
  const path = String(option.value || '')
  if (path) router.push(path)
}

function marketQuery(tab: MarketTab, category: string, extra: Record<string, string> = {}) {
  const query: Record<string, string> = { ...extra }
  if (tab === 'skills') {
    query.tab = 'skills'
    if (skillCategory.value) query.category = SKILLS_CATEGORY
    return query
  }
  if (category) query.category = category
  return query
}

function selectDiscover(view: DiscoverView) {
  keyword.value = ''
  if (view === 'skills') {
    router.replace({ path: '/plugin-market', query: marketQuery('skills', ALL_CATEGORY) })
    return
  }
  router.replace({ path: '/plugin-market' })
}

function selectCategory(category: string) {
  if (category === activeCategory.value && installFilter.value === 'all' && activeTab.value === 'plugins') return
  keyword.value = ''
  router.replace({ path: '/plugin-market', query: marketQuery('plugins', category) })
}

function selectInstalled() {
  keyword.value = ''
  const query = activeTab.value === 'skills' ? marketQuery('skills', ALL_CATEGORY, { filter: 'installed' }) : { filter: 'installed' }
  router.replace({ path: '/plugin-market', query })
}

async function loadCategories() {
  categoriesLoading.value = true
  try {
    const { data } = await fetchPluginCategories()
    categories.value = data.data || []
    if (
      typeof route.query.category === 'string' &&
      route.query.category &&
      route.query.category !== SKILLS_CATEGORY &&
      !categories.value.some((item) => item.value === route.query.category)
    ) {
      router.replace({ path: '/plugin-market' })
    }
  } catch (error) {
    categories.value = []
    MessagePlugin.error(extractApiError(error, '加载分类失败'))
  } finally {
    categoriesLoading.value = false
  }
}

async function loadPlugins() {
  loading.value = true
  try {
    const { data } = await listPlugins()
    plugins.value = data.data || []
  } catch (error) {
    plugins.value = []
    MessagePlugin.error(extractApiError(error, '加载插件失败'))
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  router.replace({ path: '/plugin-market' })
}

function openDetail(item: PluginCatalogVO) {
  detailItem.value = item
  detailVisible.value = true
}

function openInstalled(item: PluginCatalogVO) {
  if (item.targetPath) {
    router.push(item.targetPath)
  }
}

async function handleInstall(item: PluginCatalogVO) {
  if (item.installed) {
    const dialog = DialogPlugin.confirm({
      header: '移除扩展',
      body: `确定从工作空间移除「${item.title}」吗？`,
      confirmBtn: '移除',
      cancelBtn: '取消',
      theme: 'warning',
      onConfirm: async () => {
        dialog.hide()
        await runInstallAction(item)
      },
    })
    return
  }
  await runInstallAction(item)
}

async function runInstallAction(item: PluginCatalogVO) {
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

watch(plugins, (list) => {
  if (!detailItem.value) return
  const latest = list.find((item) => item.id === detailItem.value?.id)
  if (latest) detailItem.value = latest
})

onMounted(async () => {
  await loadCategories()
  await loadPlugins()
})
useReloadOnWorkspaceChange(async () => {
  await loadCategories()
  await loadPlugins()
})
</script>

<style scoped>
.plugin-market__layout {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

.plugin-market__nav {
  position: sticky;
  top: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 0 12px;
}

.plugin-market__nav-label {
  margin: 12px 0 6px;
  padding: 0 10px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  letter-spacing: 0.04em;
}

.plugin-market__nav-label:first-child {
  margin-top: 0;
}

.plugin-market__nav-skel {
  display: block;
  height: 32px;
  margin: 2px 0;
  border-radius: 8px;
  background: var(--td-gray-color-2);
}

.plugin-market__nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin: 0;
  padding: 7px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--box-ink);
  font: var(--td-font-body-medium);
  text-align: left;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.plugin-market__nav-item :deep(.t-icon) {
  flex-shrink: 0;
  color: var(--box-muted);
}

.plugin-market__nav-item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-market__nav-item em {
  margin-left: auto;
  font-style: normal;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.plugin-market__nav-item:hover {
  background: var(--box-hover);
}

.plugin-market__nav-item.is-active {
  background: var(--box-active);
  font-weight: 600;
}

.plugin-market__nav-item.is-active :deep(.t-icon) {
  color: var(--box-ink);
}

.plugin-market__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 12px;
  margin-bottom: 20px;
}

.plugin-market__search {
  width: min(420px, 100%);
}

.plugin-market__count {
  margin-left: auto;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.plugin-market__section + .plugin-market__section {
  margin-top: 28px;
}

.plugin-market__section-head {
  margin-bottom: 12px;
}

.plugin-market__section-title {
  margin: 0;
  font: var(--td-font-title-medium);
  color: var(--box-ink);
}

.plugin-market__section-desc {
  margin: 4px 0 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.plugin-market__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 12px;
}

.plugin-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
  min-height: 108px;
  padding: 16px 18px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-md);
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
}

.plugin-card:hover {
  border-color: var(--td-gray-color-4);
  box-shadow: var(--box-shadow-soft);
  transform: translateY(-1px);
}

.plugin-card--skeleton {
  min-height: 108px;
  cursor: default;
  background: var(--td-gray-color-1);
  animation: plugin-card-shimmer 1.2s ease-in-out infinite;
}

.plugin-card__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.plugin-card__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.plugin-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  flex-shrink: 0;
}

.plugin-card__icon--ink {
  background: #eef0f2;
  color: #1f2329;
}

.plugin-card__icon--sky {
  background: #e8f1ff;
  color: #2563eb;
}

.plugin-card__icon--stone {
  background: #f3eee8;
  color: #57534e;
}

.plugin-card__icon--violet {
  background: #f3e8ff;
  color: #7c3aed;
}

.plugin-card__icon--teal {
  background: #e6f6f3;
  color: #0f766e;
}

.plugin-card__title {
  margin: 0;
  min-width: 0;
  font: var(--td-font-title-small);
  color: var(--box-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-card__desc {
  margin: 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.plugin-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.plugin-card__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: stretch;
  min-width: 76px;
}

.plugin-market__empty {
  padding: 48px 0;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-md);
  background: var(--box-surface);
}

.plugin-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.plugin-detail__header {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.plugin-detail__heading {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.plugin-detail__heading strong {
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.plugin-detail__heading span {
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.plugin-detail__desc {
  margin: 0;
  font: var(--td-font-body-medium);
  color: var(--box-ink);
  line-height: 1.7;
  white-space: pre-wrap;
}

.plugin-detail__facts {
  margin: 0;
  display: grid;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--box-radius-md);
  background: var(--box-shell);
}

.plugin-detail__facts div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px;
}

.plugin-detail__facts dt {
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.plugin-detail__facts dd {
  margin: 0;
  font: var(--td-font-body-small);
  color: var(--box-ink);
}

.plugin-detail__hint {
  margin: 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

@keyframes plugin-card-shimmer {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 1;
  }
}

@media (max-width: 1100px) {
  .plugin-market__layout {
    grid-template-columns: 1fr;
  }

  .plugin-market__nav {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 4px;
    padding: 0;
  }

  .plugin-market__nav-label {
    display: none;
  }

  .plugin-market__nav-item {
    width: auto;
  }

  .plugin-market__nav-item em {
    display: none;
  }

  .plugin-market__list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .plugin-market__search {
    width: 100%;
  }

  .plugin-market__count {
    margin-left: 0;
    width: 100%;
  }

  .plugin-card {
    grid-template-columns: 44px minmax(0, 1fr);
  }

  .plugin-card__actions {
    grid-column: 1 / -1;
    flex-direction: row;
    min-width: 0;
  }

  .plugin-card__actions :deep(.t-button) {
    flex: 1;
  }
}
</style>
