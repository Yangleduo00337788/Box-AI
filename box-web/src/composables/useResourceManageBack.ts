import { RESOURCE_MANAGE_EMPTY } from '@/constants/resourceEmpty'
import { pluginMarketPathForCategory } from '@/constants/resourceRoutes'

export function useResourceManageBack(category: string) {
  const pluginMarketPath = pluginMarketPathForCategory(category)
  return {
    backTo: pluginMarketPath,
    backLabel: '插件市场',
    pluginMarketPath,
    empty: RESOURCE_MANAGE_EMPTY[category],
  }
}
