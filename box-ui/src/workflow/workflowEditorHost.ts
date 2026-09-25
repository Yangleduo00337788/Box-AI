export interface WorkflowEditorPlatformModel {
  id: number
  modelName: string
  providerName?: string
  status: number
}

export interface WorkflowEditorNamedResource {
  id: number
  name: string
}

export interface WorkflowEditorMcpServer {
  id: number
  name: string
  status: number
  toolCatalogJson?: string
}

/** C 端工作空间 / B 端官方上架共用：由宿主应用注入资源加载与（可选）持久化保存 */
export interface WorkflowEditorHost {
  loadKnowledgeBases(): Promise<Array<{ label: string; value: number }>>
  loadHttpTools(): Promise<Array<{ label: string; value: number }>>
  loadMcpServers(): Promise<WorkflowEditorMcpServer[]>
  loadWorkflows(): Promise<WorkflowEditorNamedResource[]>
  loadAgents(): Promise<WorkflowEditorNamedResource[]>
  /** 独立工作流编辑器页使用；插件 manifest 嵌入模式可不实现 */
  saveDefinition?(workflowId: number, definitionJson: string): Promise<void>
}

export const emptyWorkflowEditorHost: WorkflowEditorHost = {
  async loadKnowledgeBases() {
    return []
  },
  async loadHttpTools() {
    return []
  },
  async loadMcpServers() {
    return []
  },
  async loadWorkflows() {
    return []
  },
  async loadAgents() {
    return []
  },
}
