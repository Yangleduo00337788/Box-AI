import type { WorkflowEditorHost } from '@box/ui/workflow/workflowEditorHost'
import { emptyWorkflowEditorHost } from '@box/ui/workflow/workflowEditorHost'

/**
 * B 端官方插件上架：工作流定义写入 manifest，不调用租户内 saveDefinition。
 * 节点配置里的 Agent / 知识库 / 工具 / 子工作流下拉依赖工作空间上下文，管理端无租户会话时为空；
 * 与 C 端相同的画布与 LLM 平台模型仍可用。含资源引用的官方包建议在 C 端工作空间用「新建插件」编排（创建后会进入全屏编辑器）。
 */
export const boxAdminWorkflowEditorHost: WorkflowEditorHost = {
  ...emptyWorkflowEditorHost,
}
