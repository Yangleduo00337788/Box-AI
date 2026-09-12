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
    hint: '可从空白开始编排，或前往插件市场安装模板后在此编辑',
  },
  knowledge: {
    description: '暂无知识库',
    createLabel: '新建知识库',
    marketLabel: '浏览知识库模板',
    hint: '创建知识库后上传文档建立索引，也可安装模板快速开始',
  },
  tools: {
    description: '暂无工具',
    createLabel: '新建工具',
    marketLabel: '浏览工具模板',
    hint: '手动配置 HTTP 工具，或安装模板后在此调整参数',
  },
  mcp: {
    description: '暂无 MCP Server',
    createLabel: '添加 MCP Server',
    marketLabel: '浏览 MCP 模板',
    hint: '连接外部 MCP 服务，或安装模板后在此管理同步',
  },
}
