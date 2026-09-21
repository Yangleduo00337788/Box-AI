import { computed, ref } from 'vue'
import { listPlugins, type PluginCatalogVO } from '@/api/plugin'
import { isComposerPluginCategory, isPluginInstalled } from '@/constants/pluginCatalogMeta'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'

export function useWorkspaceInstalledPlugins() {
  const loading = ref(false)
  const plugins = ref<PluginCatalogVO[]>([])

  const installed = computed(() =>
    plugins.value
      .filter(isPluginInstalled)
      .sort((left, right) => left.title.localeCompare(right.title, 'zh-CN')),
  )

  const composerInstalled = computed(() =>
    installed.value
      .filter((item) => isComposerPluginCategory(item.category))
      .sort((left, right) => left.title.localeCompare(right.title, 'zh-CN')),
  )

  async function load() {
    loading.value = true
    try {
      const { data } = await listPlugins()
      plugins.value = data.data || []
    } catch {
      plugins.value = []
    } finally {
      loading.value = false
    }
  }

  useReloadOnWorkspaceChange(load)

  return {
    loading,
    plugins,
    installed,
    composerInstalled,
    load,
  }
}
