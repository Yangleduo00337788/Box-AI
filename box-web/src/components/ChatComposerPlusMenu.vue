<template>
  <div class="composer-plus-menu" role="menu" aria-label="添加内容" @click.stop>
    <button type="button" class="composer-plus-menu__item" role="menuitem" @click="emit('upload')">
      <t-icon name="attach" class="composer-plus-menu__leading" />
      <span>上传附件</span>
    </button>

    <div
      class="composer-plus-menu__submenu"
      @mouseenter="pluginPanelOpen = true"
      @mouseleave="pluginPanelOpen = false"
    >
      <button type="button" class="composer-plus-menu__item" role="menuitem" :class="{ 'is-active': pluginPanelOpen }">
        <t-icon name="app" class="composer-plus-menu__leading" />
        <span>插件</span>
        <t-icon name="chevron-right" class="composer-plus-menu__arrow" />
      </button>

      <div v-show="pluginPanelOpen" class="composer-plus-menu__flyout" role="menu">
        <div class="composer-plus-menu__flyout-panel">
          <div v-if="loading" class="composer-plus-menu__state">
            <t-loading size="small" />
          </div>
          <template v-else-if="plugins.length">
            <button
              v-for="item in plugins"
              :key="item.id"
              type="button"
              class="composer-plus-menu__plugin"
              :class="{ 'is-selected': (selectedIds || []).includes(item.id) }"
              role="menuitem"
              @click="onPickPlugin(item)"
            >
              <span class="composer-plus-menu__plugin-icon" :class="`composer-plus-menu__plugin-icon--${toneOf(item.category)}`">
                <t-icon :name="iconOf(item.category)" size="14px" />
              </span>
              <span class="composer-plus-menu__plugin-body">
                <span class="composer-plus-menu__plugin-title">{{ item.title }}</span>
                <span class="composer-plus-menu__plugin-meta">
                  <t-tag size="small" variant="light" :theme="pluginSourceTheme(item.sourceType)">
                    {{ pluginSourceLabel(item.sourceType) }}
                  </t-tag>
                  <span v-if="categoryLabel(item.category)">{{ categoryLabel(item.category) }}</span>
                </span>
              </span>
              <t-icon v-if="(selectedIds || []).includes(item.id)" name="check" class="composer-plus-menu__check" />
            </button>
          </template>
          <div v-else class="composer-plus-menu__state">
            <span>暂无可用技能 / 工具 / MCP</span>
            <t-button variant="text" theme="primary" size="small" @click="emit('open-market')">去插件市场</t-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { PluginCatalogVO } from '@/api/plugin'
import { fetchPluginCategories, type PluginCategoryVO } from '@/api/plugin'
import {
  PLUGIN_CATEGORY_META,
  pluginCategoryIcon,
  pluginSourceLabel,
  pluginSourceTheme,
} from '@/constants/pluginCatalogMeta'

defineProps<{
  plugins: PluginCatalogVO[]
  selectedIds?: number[]
  loading?: boolean
}>()

const emit = defineEmits<{
  upload: []
  'pick-plugin': [plugin: PluginCatalogVO]
  'open-market': []
}>()

const pluginPanelOpen = ref(false)
const categories = ref<PluginCategoryVO[]>([])

function iconOf(category: string) {
  return pluginCategoryIcon(category)
}

function toneOf(category: string) {
  return PLUGIN_CATEGORY_META[category]?.tone || 'ink'
}

function categoryLabel(value: string) {
  return categories.value.find((item) => item.value === value)?.label || ''
}

function onPickPlugin(item: PluginCatalogVO) {
  emit('pick-plugin', item)
}

onMounted(async () => {
  try {
    const { data } = await fetchPluginCategories()
    categories.value = data.data || []
  } catch {
    categories.value = []
  }
})
</script>

<style scoped>
.composer-plus-menu {
  position: absolute;
  bottom: calc(100% + 6px);
  left: 0;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 148px;
  padding: 4px;
  border: 1px solid var(--box-border);
  border-radius: 10px;
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
}

.composer-plus-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin: 0;
  padding: 6px 8px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--box-ink);
  font: var(--td-font-body-small);
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
}

.composer-plus-menu__item:hover,
.composer-plus-menu__item.is-active {
  background: var(--box-hover);
}

.composer-plus-menu__leading {
  font-size: 16px;
  color: var(--box-muted);
  flex-shrink: 0;
}

.composer-plus-menu__arrow {
  margin-left: auto;
  font-size: 14px;
  color: var(--box-muted);
}

.composer-plus-menu__submenu {
  position: relative;
}

.composer-plus-menu__flyout {
  position: absolute;
  left: calc(100% - 6px);
  top: 0;
  z-index: 21;
  padding-left: 10px;
}

.composer-plus-menu__flyout-panel {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: min(236px, 70vw);
  max-height: min(240px, 42vh);
  padding: 4px;
  overflow: auto;
  border: 1px solid var(--box-border);
  border-radius: 10px;
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
}

.composer-plus-menu__state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 8px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.composer-plus-menu__plugin {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 5px 6px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
}

.composer-plus-menu__plugin:hover,
.composer-plus-menu__plugin.is-selected {
  background: var(--box-hover);
}

.composer-plus-menu__plugin-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  flex-shrink: 0;
}

.composer-plus-menu__plugin-icon--ink {
  background: #eef0f2;
  color: #1f2329;
}

.composer-plus-menu__plugin-icon--sky {
  background: #e8f1ff;
  color: #2563eb;
}

.composer-plus-menu__plugin-icon--stone {
  background: #f3eee8;
  color: #57534e;
}

.composer-plus-menu__plugin-icon--violet {
  background: #f3e8ff;
  color: #7c3aed;
}

.composer-plus-menu__plugin-icon--teal {
  background: #e6f6f3;
  color: #0f766e;
}

.composer-plus-menu__plugin-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.composer-plus-menu__plugin-title {
  font: var(--td-font-body-small);
  color: var(--box-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.composer-plus-menu__plugin-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  line-height: 16px;
  color: var(--box-muted);
}

.composer-plus-menu__check {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--td-brand-color);
}
</style>
