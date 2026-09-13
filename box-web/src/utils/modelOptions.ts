import type { PlatformModelVO } from '@/api/platform'

export function groupedPlatformModelOptions(models: PlatformModelVO[]) {
  const groups = new Map<string, { label: string; value: number }[]>()
  for (const item of models.filter((model) => model.status === 1)) {
    const group = item.providerName || '其他服务商'
    const list = groups.get(group) ?? []
    list.push({ label: item.modelName, value: item.id })
    groups.set(group, list)
  }
  return [...groups.entries()].map(([group, children]) => ({ group, children }))
}
