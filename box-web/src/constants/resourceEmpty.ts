export interface ResourceManageEmptyConfig {
  description: string
  createLabel: string
  marketLabel: string
  hint: string
}

export const RESOURCE_MANAGE_EMPTY: Record<string, ResourceManageEmptyConfig> = {
  workflows: {
    description: '还没有工作流',
    createLabel: '从空白创建',
    marketLabel: '浏览工作流模板',
    hint: '创建后本工作空间成员共用。也可前往插件市场安装官方模板',
  },
  knowledge: {
    description: '暂无知识库',
    createLabel: '新建知识库',
    marketLabel: '浏览知识库模板',
    hint: '创建后本工作空间成员共用。上传文档建立索引，或安装官方模板',
  },
  tools: {
    description: '暂无工具',
    createLabel: '新建工具',
    marketLabel: '浏览工具模板',
    hint: '创建后本工作空间成员共用。可手动配置 HTTP 工具，或安装官方模板',
  },
  mcp: {
    description: '暂无 MCP Server',
    createLabel: '添加 MCP Server',
    marketLabel: '浏览 MCP 模板',
    hint: '创建后本工作空间成员共用。可连接外部 MCP，或安装官方模板',
  },
}
