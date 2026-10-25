export type DemoKind = 'interactive' | 'screenshot' | 'admin-table'

export interface DemoFeature {
  id: string
  title: string
  summary: string
  path: string
  kind: DemoKind
  screenshot?: string
  highlights?: string[]
  adminColumns?: { colKey: string; title: string; width?: number }[]
  adminRows?: Record<string, string | number>[]
}

export const consumerNav: DemoFeature[] = [
  {
    id: 'chat',
    title: '对话工作台',
    summary: '流式 SSE、多智能体切换、工具调用与引用溯源',
    path: '/demo/web/chat',
    kind: 'interactive',
    screenshot: 'chat-workspace.png',
    highlights: ['Published 版本隔离', 'citations 事件', '对话插件加号'],
  },
  {
    id: 'agent-builder',
    title: 'Agent Builder',
    summary: 'Prompt / 模型 / 知识库 / 工具绑定与右侧调试',
    path: '/demo/web/agent-builder',
    kind: 'interactive',
    screenshot: 'agent-builder.png',
    highlights: ['版本快照', '发布上线', '变量与记忆'],
  },
  {
    id: 'workflow',
    title: '工作流编排',
    summary: '可视化节点、校验调试与作为 Agent 子能力发布',
    path: '/demo/web/workflow',
    kind: 'interactive',
    screenshot: 'workflow-editor.png',
    highlights: ['Switch 分支', 'Webhook', 'LLM / HTTP 节点'],
  },
  {
    id: 'knowledge',
    title: '知识库 RAG',
    summary: '多格式文档、处理状态机、检索与 Test Answer',
    path: '/demo/web/knowledge',
    kind: 'interactive',
    highlights: ['ES 混合检索', 'Rerank', 'Agent 绑定'],
  },
  {
    id: 'plugin-market',
    title: '插件市场',
    summary: '工具 / 工作流 / 知识库 / MCP 统一目录与「我的」管理',
    path: '/demo/web/plugin-market',
    kind: 'interactive',
    highlights: ['官方目录', '工作空间安装', '对话加号插件'],
  },
  {
    id: 'models',
    title: '模型',
    summary: '工作空间可用模型与能力标签',
    path: '/demo/web/models',
    kind: 'screenshot',
    highlights: ['平台模型池下发', '多厂商', '额度展示'],
  },
  {
    id: 'team',
    title: '团队与工作空间',
    summary: '成员邀请、RBAC、企业版升级',
    path: '/demo/web/team',
    kind: 'admin-table',
    adminColumns: [
      { colKey: 'name', title: '成员', width: 160 },
      { colKey: 'role', title: '角色', width: 120 },
      { colKey: 'workspace', title: '工作空间' },
    ],
    adminRows: [
      { name: '张明', role: '管理员', workspace: '产品研发' },
      { name: '李悦', role: '编辑', workspace: '产品研发' },
      { name: '王晨', role: '只读', workspace: '客服支持' },
    ],
  },
  {
    id: 'market',
    title: '智能体市场',
    summary: '模板与公开智能体发现',
    path: '/demo/web/market',
    kind: 'screenshot',
    highlights: ['模板一键 fork', '分类筛选'],
  },
  {
    id: 'analytics',
    title: '分析',
    summary: '对话量、Token、活跃 Agent 统计',
    path: '/demo/web/analytics',
    kind: 'screenshot',
    highlights: ['工作空间维度', '趋势图'],
  },
  {
    id: 'debug',
    title: '执行与调试',
    summary: 'Trace、Workflow / Agent 运行记录',
    path: '/demo/web/debug',
    kind: 'admin-table',
    adminColumns: [
      { colKey: 'id', title: '执行 ID', width: 140 },
      { colKey: 'type', title: '类型', width: 100 },
      { colKey: 'status', title: '状态', width: 100 },
      { colKey: 'duration', title: '耗时' },
    ],
    adminRows: [
      { id: 'run_8f2a', type: 'Agent', status: '成功', duration: '2.4s' },
      { id: 'run_91bc', type: 'Workflow', status: '成功', duration: '5.1s' },
      { id: 'run_a003', type: 'Tool', status: '等待确认', duration: '—' },
    ],
  },
  {
    id: 'settings',
    title: '设置与治理',
    summary: 'API Key、角色权限、审计、套餐额度',
    path: '/demo/web/settings',
    kind: 'interactive',
    highlights: ['API Key 仅展示一次', '审计日志', '套餐与账单入口'],
  },
  {
    id: 'embed',
    title: 'Embed 嵌入',
    summary: 'iframe / SDK 对外发布已上线 Agent',
    path: '/demo/web/embed',
    kind: 'screenshot',
    highlights: ['域名白名单', 'Embed 网关'],
  },
]

export const adminNav: DemoFeature[] = [
  {
    id: 'dashboard',
    title: '运营总览',
    summary: '租户、对话、用量大盘',
    path: '/demo/admin/dashboard',
    kind: 'screenshot',
  },
  {
    id: 'tenants',
    title: '租户管理',
    summary: '个人 / 企业租户、启停与套餐',
    path: '/demo/admin/tenants',
    kind: 'screenshot',
    screenshot: 'admin-tenants.png',
  },
  {
    id: 'users',
    title: '平台用户',
    summary: '全平台账号与状态',
    path: '/demo/admin/users',
    kind: 'admin-table',
    adminColumns: [
      { colKey: 'email', title: '邮箱', width: 200 },
      { colKey: 'tenant', title: '租户' },
      { colKey: 'status', title: '状态', width: 100 },
    ],
    adminRows: [
      { email: 'ops@acme.com', tenant: 'Acme 企业', status: '正常' },
      { email: 'demo@personal.io', tenant: '个人空间', status: '正常' },
    ],
  },
  {
    id: 'plans',
    title: '套餐',
    summary: '额度、功能开关与定价',
    path: '/demo/admin/plans',
    kind: 'admin-table',
    adminColumns: [
      { colKey: 'name', title: '套餐', width: 120 },
      { colKey: 'seats', title: '席位', width: 80 },
      { colKey: 'tokens', title: '月 Token' },
    ],
    adminRows: [
      { name: '个人版', seats: 1, tokens: '500K' },
      { name: '团队版', seats: 20, tokens: '5M' },
      { name: '企业版', seats: '不限', tokens: '定制' },
    ],
  },
  {
    id: 'platform-models',
    title: '平台模型池',
    summary: '密钥托管与能力下发',
    path: '/demo/admin/platform-models',
    kind: 'admin-table',
    adminColumns: [
      { colKey: 'provider', title: '厂商', width: 120 },
      { colKey: 'model', title: '模型' },
      { colKey: 'status', title: '状态', width: 100 },
    ],
    adminRows: [
      { provider: 'OpenAI', model: 'gpt-4.1', status: '可用' },
      { provider: 'Anthropic', model: 'claude-sonnet', status: '可用' },
    ],
  },
  {
    id: 'plugin-catalog',
    title: '插件目录',
    summary: '官方工具 / MCP 上架',
    path: '/demo/admin/plugin-catalog',
    kind: 'screenshot',
  },
  {
    id: 'agent-templates',
    title: '智能体模板',
    summary: '平台模板与运营位',
    path: '/demo/admin/agent-templates',
    kind: 'screenshot',
  },
  {
    id: 'audit-logs',
    title: '审计日志',
    summary: '跨租户操作留痕',
    path: '/demo/admin/audit-logs',
    kind: 'admin-table',
    adminColumns: [
      { colKey: 'time', title: '时间', width: 160 },
      { colKey: 'actor', title: '操作者', width: 120 },
      { colKey: 'action', title: '动作' },
    ],
    adminRows: [
      { time: '2026-10-25 14:02', actor: 'platform_admin', action: '更新租户套餐 → 团队版' },
      { time: '2026-10-25 13:48', actor: 'tenant_admin', action: '发布 Agent「客服助手」' },
    ],
  },
  {
    id: 'system-config',
    title: '系统配置',
    summary: '邮件、OAuth、支付等全局项',
    path: '/demo/admin/system-config',
    kind: 'screenshot',
  },
  {
    id: 'object-storage',
    title: '对象存储',
    summary: 'MinIO / R2 连接与桶策略',
    path: '/demo/admin/object-storage',
    kind: 'screenshot',
  },
]

export function findFeature(path: string): DemoFeature | undefined {
  return [...consumerNav, ...adminNav].find((f) => f.path === path)
}
