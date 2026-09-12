import { DialogPlugin } from 'tdesign-vue-next'
import { extractApiError, extractDependencyConflict, type ResourceDependency } from '@/api/apiError'

const RELATION_LABELS: Record<string, string> = {
  agent: '智能体',
  knowledge: '知识库',
  tool: '工具',
  workflow: '工作流',
  publish: '发布记录',
  sub_agent: '子智能体',
}

function formatDependency(dep: ResourceDependency) {
  const typeLabel = RELATION_LABELS[dep.resourceType] || dep.resourceType
  const name = dep.resourceName || `#${dep.resourceId}`
  if (dep.relation === 'published') {
    return `${typeLabel}「${name}」已发布`
  }
  return `${typeLabel}「${name}」`
}

export function showDependencyConflictDialog(dependencies: ResourceDependency[], resourceLabel: string) {
  const lines = dependencies.map((dep) => `· ${formatDependency(dep)}`).join('\n')
  DialogPlugin.alert({
    header: `无法删除${resourceLabel}`,
    body: `存在以下依赖关系，请先解除后再删除：\n\n${lines}`,
    confirmBtn: '我知道了',
  })
}

export async function confirmResourceDelete(options: {
  header: string
  body: string
  resourceLabel: string
  onDelete: () => Promise<void>
  onSuccess?: () => void | Promise<void>
}) {
  const dialog = DialogPlugin.confirm({
    header: options.header,
    body: options.body,
    confirmBtn: '删除',
    cancelBtn: '取消',
    theme: 'warning',
    onConfirm: async () => {
      try {
        await options.onDelete()
        dialog.hide()
        await options.onSuccess?.()
      } catch (error) {
        const dependencies = extractDependencyConflict(error)
        if (dependencies?.length) {
          dialog.hide()
          showDependencyConflictDialog(dependencies, options.resourceLabel)
          return
        }
        DialogPlugin.alert({
          header: '删除失败',
          body: extractApiError(error, '删除失败'),
          confirmBtn: '关闭',
        })
      }
    },
  })
}
