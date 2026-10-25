import { buildAssistantReply } from '../engine/chatSimulator'
import { tryGenericRest } from '../engine/genericRest'
import { matchRoute, stripApiPrefix } from '../match'
import { fail, ok, type ApiResult } from '../result'
import { nextId } from '../id'
import { clone, nowIso, parseQuery } from '../util'
import { loadJson, persistKeys, saveJson } from '../world/persist'

export const DEMO_CONSUMER_TOKEN = 'box-demo-consumer-token'

export const consumerPermissions = [
  'agent:create', 'agent:read', 'agent:update', 'agent:delete', 'agent:publish',
  'workflow:create', 'workflow:read', 'workflow:update', 'workflow:execute', 'workflow:delete',
  'knowledge:create', 'knowledge:read', 'knowledge:update', 'knowledge:upload', 'knowledge:delete',
  'tool:create', 'tool:update', 'tool:delete', 'tool:execute',
  'model:create', 'model:update', 'member:manage', 'role:manage', 'api_key:manage', 'audit:read',
]

const ts = nowIso()

const demoCitation = {
  index: 1,
  chunkId: 1,
  documentId: 8001,
  documentName: '产品 FAQ.pdf',
  content: '企业版治理能力包括 RBAC、审计与套餐额度（演示）。',
  score: 0.92,
}

function normalizeChatRole(role: unknown): 'USER' | 'ASSISTANT' {
  const r = String(role || '').toUpperCase()
  return r === 'USER' ? 'USER' : 'ASSISTANT'
}

function normalizeChatMessage(raw: Record<string, unknown>): Record<string, unknown> {
  return {
    ...raw,
    role: normalizeChatRole(raw.role),
    contentType: String(raw.contentType || 'TEXT').toUpperCase(),
  }
}

function normalizeMessageList(list: Array<Record<string, unknown>>) {
  return list.map((item) => normalizeChatMessage(item))
}

export const consumerAuth = {
  token: DEMO_CONSUMER_TOKEN,
  user: {
    id: 10001,
    username: 'demo',
    email: 'demo@box.ai',
    nickname: '演示用户',
    avatarUrl: '',
    userType: 'TENANT_USER',
  },
  tenant: { id: 2001, name: 'Box 演示租户', slug: 'box-demo', tenantType: 'ENTERPRISE' as const },
  workspaces: [
    { id: 3001, name: '产品研发', slug: 'product', roleCode: 'TENANT_ADMIN', description: '默认演示空间', status: 1 },
    { id: 3002, name: '客服支持', slug: 'support', roleCode: 'MEMBER', status: 1 },
  ],
  currentWorkspaceId: 3001,
}

export const agents = [
  {
    id: 4001,
    name: '客服助手',
    description: '绑定产品 FAQ 知识库，支持引用溯源',
    status: 'PUBLISHED',
    draftVersion: 3,
    publishedVersion: 3,
    modelSource: 'PLATFORM',
    platformModelId: 5001,
    platformModelName: 'gpt-4.1',
    modelName: 'gpt-4.1',
    systemPrompt: '你是 Acme 公司客服助手，回答需简洁，并引用知识库。',
    temperature: 0.3,
    streamEnabled: true,
    memoryEnabled: true,
    memoryWindowSize: 20,
    createdBy: 10001,
    createdAt: ts,
    updatedAt: ts,
  },
  {
    id: 4002,
    name: '数据分析',
    description: '演示工具调用与工作流绑定',
    status: 'DRAFT',
    draftVersion: 1,
    publishedVersion: 0,
    modelSource: 'PLATFORM',
    platformModelId: 5001,
    platformModelName: 'gpt-4.1',
    systemPrompt: '帮助用户解读业务指标（演示数据）。',
    temperature: 0.5,
    streamEnabled: true,
    memoryEnabled: false,
    createdBy: 10001,
    createdAt: ts,
    updatedAt: ts,
  },
]

export const agentBindings = {
  knowledge: [{ agentId: 4001, knowledgeBaseId: 8101, knowledgeBaseName: '产品 FAQ', priority: 1 }],
  tools: [{ agentId: 4001, toolId: 8301, toolName: '查询订单', toolKey: 'http_order', enabled: true }],
  mcp: [{ agentId: 4002, mcpServerId: 8401, serverName: 'CRM MCP', enabled: true }],
  workflows: [{ agentId: 4002, workflowId: 8201, workflowName: '客服分流', enabled: true }],
  subAgents: [] as Array<Record<string, unknown>>,
}

export const conversations = [
  {
    id: 6001,
    agentId: 4001,
    agentName: '客服助手',
    projectId: 1,
    title: '企业版能力咨询',
    status: 'ACTIVE',
    messageCount: 2,
    lastMessageAt: ts,
    createdAt: ts,
    updatedAt: ts,
  },
]

export const messagesByConversation: Record<number, Array<Record<string, unknown>>> = {
  6001: [
    {
      id: 7001,
      role: 'USER',
      content: '企业版支持哪些治理能力？',
      contentType: 'TEXT',
      sequenceNo: 1,
      createdAt: ts,
    },
    {
      id: 7002,
      role: 'ASSISTANT',
      content: '企业版包含多工作空间 RBAC、审计日志、套餐额度与全链路 Trace（演示数据）。',
      contentType: 'TEXT',
      sequenceNo: 2,
      createdAt: ts,
      metadataJson: JSON.stringify({
        citations: [demoCitation],
      }),
    },
  ],
}

export const projects = [{ id: 1, name: '默认项目', conversationCount: 1, createdAt: ts, updatedAt: ts }]

export const knowledgeBases = [
  {
    id: 8101,
    name: '产品 FAQ',
    description: '演示知识库',
    documentCount: 3,
    chunkCount: 42,
    status: 'ACTIVE',
    createdAt: ts,
    updatedAt: ts,
  },
]

export const knowledgeDocuments: Record<number, Array<Record<string, unknown>>> = {
  8101: [
    {
      id: 8001,
      knowledgeBaseId: 8101,
      name: '产品 FAQ',
      fileName: '产品 FAQ.pdf',
      fileType: 'pdf',
      fileSize: 245760,
      chunkCount: 42,
      status: 'READY',
      progress: 100,
      createdAt: ts,
      updatedAt: ts,
    },
    {
      id: 8002,
      knowledgeBaseId: 8101,
      name: '使用指南',
      fileName: '使用指南.md',
      fileType: 'md',
      fileSize: 12040,
      chunkCount: 12,
      status: 'READY',
      createdAt: ts,
      updatedAt: ts,
    },
    {
      id: 8003,
      knowledgeBaseId: 8101,
      name: '套餐对比',
      fileName: '套餐对比.csv',
      fileType: 'csv',
      fileSize: 4096,
      chunkCount: 0,
      status: 'PROCESSING',
      progress: 65,
      createdAt: ts,
      updatedAt: ts,
    },
  ],
}

export const workflows = [
  {
    id: 8201,
    name: '客服分流',
    description: 'Switch + 知识库 + HTTP',
    status: 'PUBLISHED',
    draftVersionNo: 2,
    publishedVersionNo: 2,
    definitionJson: JSON.stringify({
      nodes: [
        { id: 'start', type: 'start', label: '开始', position: { x: 0, y: 80 } },
        { id: 'kb', type: 'knowledge', label: '知识库检索', position: { x: 200, y: 40 } },
        { id: 'llm', type: 'llm', label: 'LLM', position: { x: 400, y: 80 } },
        { id: 'switch', type: 'switch', label: '分支', position: { x: 600, y: 80 } },
        { id: 'end', type: 'end', label: '结束', position: { x: 800, y: 80 } },
      ],
      edges: [
        { id: 'e1', source: 'start', target: 'kb' },
        { id: 'e2', source: 'kb', target: 'llm' },
        { id: 'e3', source: 'llm', target: 'switch' },
        { id: 'e4', source: 'switch', target: 'end' },
      ],
      variables: [{ name: 'user_input', type: 'string' }],
    }),
    createdAt: ts,
    updatedAt: ts,
  },
]

export const tools = [
  {
    id: 8301,
    name: '查询订单',
    toolKey: 'http_order',
    toolType: 'HTTP',
    status: 'ACTIVE',
    description: 'GET 订单状态（演示）',
    createdAt: ts,
    updatedAt: ts,
  },
]

export const mcpServers = [
  {
    id: 8401,
    name: 'CRM MCP',
    serverUrl: 'https://demo.box.ai/mcp/crm',
    status: 'ACTIVE',
    toolCount: 4,
    lastSyncAt: ts,
    createdAt: ts,
    updatedAt: ts,
  },
]

export const providers = [
  { id: 5101, providerCode: 'openai', providerName: 'OpenAI（平台池）', providerType: 'OPENAI', status: 1 },
]

export const models = [
  {
    id: 5001,
    providerId: 5101,
    providerName: 'OpenAI（平台池）',
    modelCode: 'gpt-4.1',
    modelName: 'GPT-4.1',
    modelType: 'CHAT',
    supportStreaming: true,
    supportToolCalling: true,
    status: 1,
  },
]

export const modelApiKeys = [
  { id: 1, providerId: 5101, providerName: 'OpenAI（BYOK）', credentialName: '演示密钥', maskedApiKey: 'sk-••••demo', status: 1 },
]

export const pluginCategories = [
  { value: 'productivity', label: '效率', desc: '办公与自动化' },
  { value: 'data', label: '数据', desc: '查询与分析' },
  { value: 'integration', label: '集成', desc: 'HTTP / MCP' },
]

export const marketPlugins = [
  {
    id: 9001,
    pluginCode: 'http_probe',
    category: 'integration',
    title: 'HTTP 探活',
    description: '官方演示插件，用于健康检查',
    installCount: 128,
    installed: true,
    resourceType: 'TOOL',
    resourceId: 8301,
    sourceType: 'OFFICIAL',
    status: 'ACTIVE',
    reviewStatus: 'APPROVED',
  },
  {
    id: 9002,
    pluginCode: 'wf_support',
    category: 'productivity',
    title: '客服分流工作流',
    description: '含 Switch 与知识库节点',
    installCount: 56,
    installed: true,
    resourceType: 'WORKFLOW',
    resourceId: 8201,
    sourceType: 'OFFICIAL',
    status: 'ACTIVE',
    reviewStatus: 'APPROVED',
  },
  {
    id: 9003,
    pluginCode: 'kb_faq',
    category: 'data',
    title: '产品 FAQ 知识库',
    description: '预置 RAG 样例',
    installCount: 210,
    installed: false,
    resourceType: 'KNOWLEDGE',
    resourceId: 8101,
    sourceType: 'OFFICIAL',
    status: 'ACTIVE',
    reviewStatus: 'APPROVED',
  },
]

export const agentTemplates = [
  { id: 9101, name: '客服助手模板', description: '含知识库绑定示例', category: 'customer', installCount: 320, enabled: true },
]

export const executions = Array.from({ length: 12 }).map((_, i) => ({
  id: 99001 + i,
  executionNo: `EX-${99001 + i}`,
  executionType: i % 3 === 0 ? 'WORKFLOW' : 'AGENT_CHAT',
  agentId: i % 2 === 0 ? 4001 : 4002,
  conversationId: i % 3 !== 0 ? 6001 + (i % 3) : undefined,
  userId: 10001,
  status: i % 7 === 0 ? 'FAILED' : 'SUCCESS',
  durationMs: 1200 + i * 180,
  totalTokens: 256 + i * 64,
  createdAt: ts,
  startedAt: ts,
  finishedAt: ts,
  errorMessage: i % 7 === 0 ? '演示：模型超时' : undefined,
}))

export const notifications = [
  { id: 1, key: 'demo.welcome', title: '欢迎使用 Box 演示环境', content: '所有数据为前端模拟，可执行 __BOX_DEMO_RESET__() 恢复种子。', level: 'INFO', read: false, createdAt: ts },
  { id: 2, key: 'quota.80', title: '额度提醒', content: '本月 Token 已使用约 78%，可在设置中查看套餐。', level: 'WARNING', read: false, createdAt: ts },
  { id: 3, key: 'agent.published', title: 'Agent 已发布', content: '「客服助手」新版本已发布到工作空间。', level: 'SUCCESS', read: true, createdAt: ts },
  { id: 4, key: 'kb.ready', title: '知识库处理完成', content: '文档「产品 FAQ.pdf」已就绪，可开始检索。', level: 'INFO', read: true, createdAt: ts },
  { id: 5, key: 'invite.pending', title: '成员邀请', content: '你有一条待处理的工作空间邀请（演示）。', level: 'INFO', read: false, createdAt: ts },
]

export const auditLogs = Array.from({ length: 22 }).map((_, i) => ({
  id: i + 1,
  actorUserId: 10001,
  actorNickname: '演示用户',
  action: ['agent:publish', 'knowledge:upload', 'workflow:run', 'api_key:create', 'member:invite'][i % 5],
  resourceType: ['AGENT', 'KNOWLEDGE', 'WORKFLOW', 'API_KEY', 'MEMBER'][i % 5],
  resourceId: 4000 + (i % 5),
  resourceName: ['客服助手', '产品 FAQ', '客服分流', '集成密钥', '新成员'][i % 5],
  ipAddress: `10.0.0.${10 + (i % 40)}`,
  createdAt: ts,
}))

export const apiKeys = [
  {
    id: 1,
    name: '演示集成密钥',
    prefix: 'box_demo_',
    status: 'ACTIVE',
    lastUsedAt: ts,
    createdAt: ts,
  },
]

export const roles = [
  { id: 1, name: '管理员', code: 'TENANT_ADMIN', builtIn: true, permissionCodes: consumerPermissions },
  { id: 2, name: '编辑', code: 'EDITOR', builtIn: true, permissionCodes: consumerPermissions.filter((c) => !c.includes('delete')) },
]

export const plans = [
  { id: 1, code: 'personal', name: '个人版', monthlyTokens: 500000, maxMembers: 1 },
  { id: 2, code: 'team', name: '团队版', monthlyTokens: 5000000, maxMembers: 20 },
]

export function quotaSnapshot() {
  return {
    tenantId: 2001,
    planId: 2,
    planName: '团队版',
    period: '2026-10',
    quotaAiCalls: 10000,
    quotaTokens: 5000000,
    quotaMembers: 20,
    quotaWorkspaces: 5,
    quotaKnowledgeBases: 20,
    usedAiCalls: 1240,
    usedTokens: 890000,
    usedMembers: 8,
    usedWorkspaces: 2,
    usedKnowledgeBases: 1,
    remainingAiCalls: 8760,
    remainingTokens: 4110000,
    remainingMembers: 12,
    remainingWorkspaces: 3,
    remainingKnowledgeBases: 19,
  }
}

function findAgent(id: string) {
  return agents.find((a) => String(a.id) === id)
}

function agentSubPath(path: string, m: string, body: unknown): ApiResult<unknown> | null {
  const pub = matchRoute('/agents/:id/publish', path)
  if (pub && m === 'GET') {
    const agent = findAgent(pub.id)
    return ok({
      publishedVersion: agent?.publishedVersion ?? 0,
      publishedAt: agent?.publishedVersion ? ts : null,
      changelog: '演示发布记录',
    })
  }
  if (pub && m === 'POST') {
    const agent = findAgent(pub.id)
    if (agent) {
      agent.status = 'PUBLISHED'
      agent.publishedVersion = agent.draftVersion
      agent.updatedAt = nowIso()
    }
    return ok({ publishedVersion: agent?.publishedVersion ?? 1, publishedAt: nowIso() })
  }
  const unpub = matchRoute('/agents/:id/unpublish', path)
  if (unpub && m === 'POST') {
    const agent = findAgent(unpub.id)
    if (agent) agent.status = 'DRAFT'
    return ok({ publishedVersion: 0 })
  }
  const versions = matchRoute('/agents/:id/versions', path)
  if (versions && m === 'GET' && !path.includes('compare')) {
    return ok([
      { id: 3, versionNo: 3, status: 'PUBLISHED', createdAt: ts, createdBy: 10001, changelog: '优化 Prompt' },
      { id: 2, versionNo: 2, status: 'ARCHIVED', createdAt: ts, createdBy: 10001 },
    ])
  }
  if (versions && path.includes('compare') && m === 'GET') {
    return ok({ leftVersionNo: 2, rightVersionNo: 3, diffSummary: '演示：System Prompt 有变更' })
  }
  const knowledgeItem = matchRoute('/agents/:id/knowledge/:knowledgeBaseId', path)
  if (knowledgeItem && m === 'DELETE') return ok(null)
  const knowledge = matchRoute('/agents/:id/knowledge', path)
  if (knowledge) {
    const list = agentBindings.knowledge.filter((b) => String(b.agentId) === knowledge.id)
    if (m === 'GET') return ok(clone(list))
    if (m === 'POST') {
      const row = { id: nextId('bind'), agentId: Number(knowledge.id), ...(body as object) }
      agentBindings.knowledge.push(row as never)
      return ok(row)
    }
    if (m === 'DELETE') return ok(null)
  }
  const toolItem = matchRoute('/agents/:id/tools/:toolId', path)
  if (toolItem && !path.includes('/test') && (m === 'PUT' || m === 'DELETE')) return ok(null)
  const toolsRoute = matchRoute('/agents/:id/tools', path)
  if (toolsRoute && !path.includes('/test') && !path.includes('/confirm')) {
    const list = agentBindings.tools.filter((b) => String(b.agentId) === toolsRoute.id)
    if (m === 'GET') return ok(clone(list))
    if (m === 'POST' || m === 'PUT') return ok({ ...list[0], ...(body as object) })
    if (m === 'DELETE') return ok(null)
  }
  if (matchRoute('/agents/:id/tools/:toolId/test', path) && m === 'POST') {
    return ok({ toolKey: 'http_order', toolName: '查询订单', output: '{"orderId":"DEMO-001","status":"SHIPPED"}' })
  }
  const mcpItem = matchRoute('/agents/:id/mcp/:mcpServerId', path)
  if (mcpItem && m === 'DELETE') return ok(null)
  const mcpRoute = matchRoute('/agents/:id/mcp', path)
  if (mcpRoute) {
    const list = agentBindings.mcp.filter((b) => String(b.agentId) === mcpRoute.id)
    if (m === 'GET') return ok(clone(list))
    if (m === 'POST') return ok({ ...list[0], ...(body as object) })
    if (m === 'DELETE') return ok(null)
  }
  const wfItem = matchRoute('/agents/:id/workflows/:workflowId', path)
  if (wfItem && m === 'DELETE') return ok(null)
  const wfRoute = matchRoute('/agents/:id/workflows', path)
  if (wfRoute) {
    const list = agentBindings.workflows.filter((b) => String(b.agentId) === wfRoute.id)
    if (m === 'GET') return ok(clone(list))
    if (m === 'POST') return ok({ ...list[0], ...(body as object) })
    if (m === 'DELETE') return ok(null)
  }
  const embed = matchRoute('/agents/:id/embed-config', path)
  if (embed) {
    const cfg = { enabled: true, allowedDomains: ['localhost', 'demo.box.ai'], theme: 'light' }
    if (m === 'GET') return ok(cfg)
    if (m === 'PUT') return ok({ ...cfg, ...(body as object) })
  }
  if (matchRoute('/agents/:id/embed-domain/verify', path) && m === 'POST') {
    return ok({ enabled: true, allowedDomains: ['demo.box.ai'], verified: true })
  }
  const memoryItem = matchRoute('/agents/:id/long-term-memories/:memoryId', path)
  if (memoryItem && m === 'DELETE') return ok(null)
  const memories = matchRoute('/agents/:id/long-term-memories', path)
  if (memories && m === 'GET') {
    return ok([{ id: 1, content: '用户偏好邮件联系（演示记忆）', createdAt: ts }])
  }
  const versionRestore = matchRoute('/agents/:id/versions/:versionId/restore', path)
  if (versionRestore && m === 'POST') return ok(null)
  const versionArchive = matchRoute('/agents/:id/versions/:versionId/archive', path)
  if (versionArchive && m === 'POST') {
    return ok({ id: Number(versionArchive.versionId), versionNo: 2, status: 'ARCHIVED', createdAt: ts })
  }
  if (versions && m === 'POST' && !path.includes('compare')) {
    return ok({ id: nextId('ver'), versionNo: 4, status: 'DRAFT', createdAt: nowIso(), changelog: '演示新版本' })
  }
  if (matchRoute('/published/agents/:id/embed-config', path) && m === 'GET') {
    return ok({ enabled: true, allowedDomains: ['localhost'], theme: 'light', welcomeMessage: '你好' })
  }
  if (/^\/agents\/\d+\/prompt$/.test(path) && m === 'PUT') {
    const id = path.split('/')[2]
    const agent = findAgent(id)
    if (agent && (body as { systemPrompt?: string }).systemPrompt) {
      agent.systemPrompt = (body as { systemPrompt: string }).systemPrompt
      agent.updatedAt = nowIso()
    }
    return agent ? ok(clone(agent)) : fail('Agent 不存在', 404)
  }
  if (/^\/agents\/\d+\/(model|memory|config)$/.test(path) && m === 'PUT') {
    const id = path.split('/')[2]
    const agent = findAgent(id)
    if (agent) Object.assign(agent, body as object, { updatedAt: nowIso() })
    return agent ? ok(clone(agent)) : fail('Agent 不存在', 404)
  }
  const agentChat = matchRoute('/agents/:id/chat', path)
  if (agentChat && m === 'POST') {
    const message = (body as { message?: string })?.message || ''
    const reply = buildAssistantReply(message)
    return ok({
      content: reply,
      citations: [demoCitation],
      executionId: 99001,
    })
  }
  const publishedChat = matchRoute('/published/agents/:id/chat', path)
  if (publishedChat && m === 'POST') {
    const message = (body as { message?: string })?.message || ''
    const reply = buildAssistantReply(message)
    return ok({
      content: reply,
      citations: [demoCitation],
      executionId: 99001,
    })
  }
  if (matchRoute('/agents/:id/tools/confirm', path) && m === 'POST') {
    return ok({ content: '工具已确认执行（演示）', executionId: 99002 })
  }
  const subAgentItem = matchRoute('/agents/:id/sub-agents/:subAgentId', path)
  if (subAgentItem && m === 'DELETE') return ok(null)
  const subAgents = matchRoute('/agents/:id/sub-agents', path)
  if (subAgents) {
    const list = agentBindings.subAgents.filter((b) => String(b.agentId) === subAgents.id)
    if (m === 'GET') return ok(clone(list))
    if (m === 'POST') {
      const row = { id: nextId('sub'), agentId: Number(subAgents.id), ...(body as object) }
      agentBindings.subAgents.push(row)
      return ok(row)
    }
    if (m === 'DELETE') return ok(null)
  }
  const duplicateRoute = matchRoute('/agents/:id/duplicate', path)
  if (duplicateRoute && m === 'POST') {
    const src = findAgent(duplicateRoute.id)
    if (!src) return fail('Agent 不存在', 404)
    const copy = {
      ...clone(src),
      id: nextId('agent'),
      name: `${src.name} 副本`,
      status: 'DRAFT',
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }
    agents.push(copy as never)
    return ok(copy)
  }
  const archiveRoute = matchRoute('/agents/:id/archive', path)
  if (archiveRoute && m === 'POST') {
    const agent = findAgent(archiveRoute.id)
    if (agent) agent.status = 'ARCHIVED'
    return agent ? ok(clone(agent)) : fail('Agent 不存在', 404)
  }
  return null
}

function workflowSubPath(path: string, m: string, body: unknown): ApiResult<unknown> | null {
  const wfMatch = path.match(/^\/workflows\/(\d+)/)
  if (!wfMatch) return null
  const item = workflows.find((w) => String(w.id) === wfMatch[1])
  if (path.endsWith('/definition') && m === 'PUT') {
    if (item && (body as { definitionJson?: string })?.definitionJson) {
      item.definitionJson = (body as { definitionJson: string }).definitionJson
      item.updatedAt = nowIso()
    }
    return item ? ok(clone(item)) : fail('工作流不存在', 404)
  }
  if (path.endsWith('/validate') && m === 'POST') {
    return ok({ valid: true, errors: [] })
  }
  if (path.endsWith('/publish') && m === 'GET') {
    return ok({ publishedVersionNo: item?.publishedVersionNo ?? 0, publishedAt: ts })
  }
  if (path.endsWith('/publish') && m === 'POST') {
    if (item) {
      item.status = 'PUBLISHED'
      item.publishedVersionNo = item.draftVersionNo
    }
    return ok({ publishedVersionNo: item?.publishedVersionNo ?? 1, publishedAt: nowIso() })
  }
  if (path.endsWith('/debug') && m === 'POST') {
    return ok({
      output: '工作流调试完成（演示）',
      logs: ['start', 'knowledge', 'llm', 'end'],
      durationMs: 1200,
    })
  }
  if (m === 'GET' && /^\/workflows\/\d+$/.test(path)) {
    return item ? ok(clone(item)) : fail('工作流不存在', 404)
  }
  if (m === 'DELETE' && /^\/workflows\/\d+$/.test(path)) return ok(null)
  return null
}

export function dispatchConsumerMock(method: string, rawUrl: string, body: unknown): ApiResult<unknown> {
  const pathOnly = stripApiPrefix(rawUrl.split('?')[0], '/api/v1')
  const m = method.toUpperCase()

  if (m === 'POST' && pathOnly === '/auth/login') return ok(clone(consumerAuth))
  if (m === 'POST' && pathOnly === '/auth/register') return ok(clone(consumerAuth))
  if (m === 'GET' && pathOnly === '/auth/me') return ok(clone(consumerAuth))
  if (m === 'PUT' && pathOnly === '/auth/current-workspace') {
    const wsId = (body as { workspaceId?: number })?.workspaceId
    if (wsId) consumerAuth.currentWorkspaceId = wsId
    return ok(clone(consumerAuth))
  }
  if (m === 'PUT' && pathOnly === '/auth/profile') {
    Object.assign(consumerAuth.user, body as object)
    return ok(clone(consumerAuth))
  }
  if (m === 'GET' && pathOnly === '/auth/preferences') return ok({ theme: 'system', sendWithEnter: true })
  if (m === 'PUT' && pathOnly === '/auth/preferences') return ok(body ?? { theme: 'system', sendWithEnter: true })
  if (m === 'POST' && pathOnly === '/auth/verification-code') return ok({ devCode: null })
  if (m === 'GET' && pathOnly === '/auth/oauth/providers') return ok([])
  if (m === 'GET' && pathOnly === '/auth/sessions') {
    return ok([{ sessionId: 'demo-session', deviceName: 'Browser', current: true, lastActiveAt: ts }])
  }
  if (matchRoute('/auth/sessions/:sessionId', pathOnly) && m === 'DELETE') return ok(null)
  if (m === 'PUT' && pathOnly === '/auth/password') return ok(null)
  if (m === 'POST' && pathOnly === '/auth/password/reset') return ok(null)
  if (m === 'POST' && pathOnly === '/auth/avatar') return ok(clone(consumerAuth))
  if (m === 'GET' && pathOnly === '/auth/enterprise/org') {
    return ok({ orgId: 'ACME-DEMO', companyName: 'Box 演示企业', status: 'ACTIVE' })
  }
  if (m === 'POST' && pathOnly === '/auth/enterprise/join') return ok(clone(consumerAuth))

  if (m === 'GET' && pathOnly === '/workspaces') return ok(clone(consumerAuth.workspaces))
  if (m === 'POST' && pathOnly === '/workspaces') {
    const ws = { id: nextId('ws'), name: (body as { name: string }).name, slug: 'ws', status: 1, roleCode: 'TENANT_ADMIN' }
    consumerAuth.workspaces.push(ws as never)
    return ok(ws)
  }
  if (m === 'GET' && pathOnly === '/workspaces/current-permissions') return ok(consumerPermissions)
  const wsOne = matchRoute('/workspaces/:id', pathOnly)
  if (wsOne && m === 'PUT') {
    const ws = consumerAuth.workspaces.find((w) => String(w.id) === wsOne.id)
    if (ws) Object.assign(ws, body as object)
    return ws ? ok(clone(ws)) : fail('工作空间不存在', 404)
  }
  if (wsOne && m === 'DELETE') {
    const idx = consumerAuth.workspaces.findIndex((w) => String(w.id) === wsOne.id)
    if (idx >= 0) consumerAuth.workspaces.splice(idx, 1)
    return ok(null)
  }

  if (m === 'GET' && pathOnly === '/published/embed/resolve') {
    const q = parseQuery(rawUrl)
    const host = (q.host || '').toLowerCase()
    const embedOnlyHosts = new Set(['embed.demo.box.ai', 'chat.demo.box.ai'])
    if (!host || !embedOnlyHosts.has(host)) {
      return ok({ agentId: null })
    }
    return ok({
      agentId: 4001,
      customDomain: host,
      embed: { enabled: true, allowedDomains: [host], theme: 'light' },
    })
  }

  if (m === 'GET' && pathOnly === '/sidebar/pins') {
    return ok({ pinnedAgentIds: [4001], pinnedConversationIds: [6001] })
  }
  if (m === 'PUT' && pathOnly === '/sidebar/pins') return ok(body ?? { pinnedAgentIds: [], pinnedConversationIds: [] })
  if (m === 'GET' && pathOnly === '/sidebar/selection') return ok({ selectedAgentId: 4001 })
  if (m === 'PUT' && pathOnly === '/sidebar/selection') {
    return ok({ selectedAgentId: (body as { selectedAgentId?: number })?.selectedAgentId ?? 4001 })
  }

  if (m === 'GET' && pathOnly === '/agents') return ok(clone(agents))
  if (m === 'POST' && pathOnly === '/agents') {
    const payload = body as { name: string; description?: string }
    const agent = {
      id: nextId('agent'),
      name: payload.name,
      description: payload.description,
      status: 'DRAFT',
      draftVersion: 1,
      publishedVersion: 0,
      modelSource: 'PLATFORM',
      platformModelId: 5001,
      platformModelName: 'gpt-4.1',
      streamEnabled: true,
      memoryEnabled: false,
      createdBy: 10001,
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }
    agents.push(agent as never)
    return ok(agent)
  }
  const agentSub = agentSubPath(pathOnly, m, body)
  if (agentSub) return agentSub
  if (/^\/agents\/\d+$/.test(pathOnly)) {
    const id = pathOnly.split('/')[2]
    const agent = findAgent(id)
    if (m === 'GET') return agent ? ok(clone(agent)) : fail('Agent 不存在', 404)
    if (m === 'PUT' || m === 'POST') {
      if (agent) Object.assign(agent, body as object, { updatedAt: nowIso() })
      return agent ? ok(clone(agent)) : fail('Agent 不存在', 404)
    }
    if (m === 'DELETE') return ok(null)
  }

  if (m === 'GET' && pathOnly === '/conversations') return ok(clone(conversations))
  if (m === 'POST' && pathOnly === '/conversations') {
    const payload = body as { agentId: number; title?: string; projectId?: number }
    const conv = {
      id: nextId('conv'),
      agentId: payload.agentId,
      agentName: findAgent(String(payload.agentId))?.name,
      projectId: payload.projectId ?? null,
      title: payload.title || '新对话',
      status: 'ACTIVE',
      messageCount: 0,
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }
    conversations.unshift(conv)
    messagesByConversation[conv.id] = []
    return ok(conv)
  }
  const convOne = matchRoute('/conversations/:id', pathOnly)
  if (convOne && !pathOnly.includes('/messages') && !pathOnly.includes('/shares') && !pathOnly.includes('/project')) {
    const conv = conversations.find((c) => String(c.id) === convOne.id)
    if (m === 'GET') return conv ? ok(clone(conv)) : fail('会话不存在', 404)
    if (m === 'PUT') {
      if (conv && (body as { title?: string }).title) conv.title = (body as { title: string }).title
      return conv ? ok(clone(conv)) : fail('会话不存在', 404)
    }
    if (m === 'DELETE') return ok(null)
  }
  const msgRoute = matchRoute('/conversations/:id/messages', pathOnly)
  if (msgRoute && m === 'GET') {
    const list = messagesByConversation[Number(msgRoute.id)] || []
    return ok(clone(normalizeMessageList(list)))
  }
  const convProject = matchRoute('/conversations/:id/project', pathOnly)
  if (convProject && m === 'PUT') {
    const conv = conversations.find((c) => String(c.id) === convProject.id)
    if (conv) conv.projectId = (body as { projectId?: number | null }).projectId ?? null
    return conv ? ok(clone(conv)) : fail('会话不存在', 404)
  }
  const convShare = matchRoute('/conversations/:id/shares', pathOnly)
  if (convShare && m === 'POST') {
    const token = `demo-share-${convShare.id}-${Date.now()}`
    return ok({ token, sharePath: `/share/conversation/${token}` })
  }
  const msgOne = matchRoute('/conversations/:id/messages/:messageId', pathOnly)
  if (msgOne && m === 'DELETE') {
    const list = messagesByConversation[Number(msgOne.id)] || []
    const idx = list.findIndex((m) => String(m.id) === msgOne.messageId)
    if (idx >= 0) list.splice(idx, 1)
    return ok(null)
  }
  const msgFeedback = matchRoute('/conversations/:id/messages/:messageId/feedback', pathOnly)
  if (msgFeedback && m === 'POST') return ok(null)
  const publicShare = matchRoute('/public/conversation-shares/:token', pathOnly)
  if (publicShare && m === 'GET') {
    const conv = conversations[0]
    const msgs = messagesByConversation[conv?.id] || []
    return ok({
      title: conv?.title || '分享对话',
      sharedAt: nowIso(),
      userName: consumerAuth.user.nickname,
      agentName: conv?.agentName || '客服助手',
      messages: msgs.map((item) => ({
        role: String(item.role).toLowerCase(),
        content: String(item.content || ''),
      })),
    })
  }
  const publicInvite = matchRoute('/public/invitations/:token', pathOnly)
  if (publicInvite && m === 'GET') {
    return ok({
      id: 1,
      email: 'invite@box.ai',
      roleCode: 'MEMBER',
      status: 'PENDING',
      expiresAt: ts,
      workspaceName: '产品研发',
    })
  }
  const publicInviteAccept = matchRoute('/public/invitations/:token/accept', pathOnly)
  if (publicInviteAccept && m === 'POST') {
    return ok({
      userId: consumerAuth.user.id,
      nickname: consumerAuth.user.nickname,
      email: consumerAuth.user.email,
      roleCode: 'MEMBER',
      status: 'ACTIVE',
    })
  }

  if (msgRoute && m === 'POST' && !pathOnly.includes('regenerate')) {
    return ok({
      userMessage: normalizeChatMessage({
        id: nextId('msg'),
        role: 'USER',
        content: '(同步演示)',
        contentType: 'TEXT',
        sequenceNo: 1,
        createdAt: nowIso(),
      }),
      assistantMessage: normalizeChatMessage({
        id: nextId('msg'),
        role: 'ASSISTANT',
        content: '请使用流式接口（演示环境已模拟 SSE）。',
        contentType: 'TEXT',
        sequenceNo: 2,
        createdAt: nowIso(),
      }),
    })
  }

  if (m === 'GET' && pathOnly === '/projects') return ok(clone(projects))
  if (m === 'POST' && pathOnly === '/projects') {
    const p = { id: nextId('proj'), name: (body as { name: string }).name, conversationCount: 0, createdAt: nowIso(), updatedAt: nowIso() }
    projects.push(p)
    return ok(p)
  }

  if (m === 'GET' && pathOnly === '/knowledge-bases') return ok(clone(knowledgeBases))
  if (m === 'POST' && pathOnly === '/knowledge-bases') {
    const kb = {
      id: nextId('kb'),
      name: (body as { name: string }).name,
      description: (body as { description?: string }).description,
      documentCount: 0,
      chunkCount: 0,
      status: 'ACTIVE',
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }
    knowledgeBases.push(kb)
    knowledgeDocuments[kb.id] = []
    return ok(kb)
  }
  const kbOne = matchRoute('/knowledge-bases/:id', pathOnly)
  if (kbOne && !pathOnly.includes('/documents') && !pathOnly.includes('/search')) {
    const kb = knowledgeBases.find((k) => String(k.id) === kbOne.id)
    if (m === 'GET') return kb ? ok(clone(kb)) : fail('知识库不存在', 404)
    if (m === 'PUT' && kb) {
      Object.assign(kb, body as object, { updatedAt: nowIso() })
      return ok(clone(kb))
    }
    if (m === 'DELETE') {
      const idx = knowledgeBases.findIndex((k) => String(k.id) === kbOne.id)
      if (idx >= 0) knowledgeBases.splice(idx, 1)
      delete knowledgeDocuments[Number(kbOne.id)]
      return ok(null)
    }
  }
  const kbDocs = matchRoute('/knowledge-bases/:id/documents', pathOnly)
  if (kbDocs) {
    const kbId = Number(kbDocs.id)
    if (!knowledgeDocuments[kbId]) knowledgeDocuments[kbId] = []
    const docs = knowledgeDocuments[kbId]
    if (m === 'GET') return ok(clone(docs))
    if (m === 'POST') {
      const doc = {
        id: nextId('doc'),
        knowledgeBaseId: kbId,
        name: '新文档',
        fileName: 'upload.demo.txt',
        fileType: 'txt',
        fileSize: 1024,
        chunkCount: 12,
        status: 'READY',
        progress: 100,
        createdAt: nowIso(),
        updatedAt: nowIso(),
      }
      docs.push(doc)
      const kb = knowledgeBases.find((k) => k.id === kbId)
      if (kb) {
        kb.documentCount = docs.length
        kb.chunkCount = (kb.chunkCount || 0) + doc.chunkCount
        kb.updatedAt = nowIso()
      }
      return ok(doc)
    }
  }
  const importUrl = matchRoute('/knowledge-bases/:id/documents/import-url', pathOnly)
  if (importUrl && m === 'POST') {
    const kbId = Number(importUrl.id)
    if (!knowledgeDocuments[kbId]) knowledgeDocuments[kbId] = []
    const doc = {
      id: nextId('doc'),
      knowledgeBaseId: kbId,
      name: '导入网页',
      fileName: (body as { url?: string })?.url || 'https://demo.box.ai',
      fileType: 'url',
      fileSize: 0,
      chunkCount: 8,
      status: 'READY',
      progress: 100,
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }
    knowledgeDocuments[kbId].push(doc)
    return ok(doc)
  }
  const docOne = matchRoute('/documents/:id', pathOnly)
  if (docOne && !pathOnly.includes('/chunks') && m === 'GET') {
    return ok({
      id: Number(docOne.id),
      knowledgeBaseId: 8101,
      name: '产品 FAQ.pdf',
      fileName: 'faq.pdf',
      fileType: 'pdf',
      fileSize: 204800,
      chunkCount: 24,
      status: 'READY',
      progress: 100,
      createdAt: ts,
      updatedAt: ts,
    })
  }
  if (matchRoute('/documents/:id/reprocess', pathOnly) && m === 'POST') {
    return ok({ status: 'PROCESSING', progress: 30 })
  }
  if (matchRoute('/documents/:id/retry', pathOnly) && m === 'POST') {
    return ok({
      id: Number(matchRoute('/documents/:id/retry', pathOnly)!.id),
      knowledgeBaseId: 8101,
      name: '产品 FAQ.pdf',
      fileName: 'faq.pdf',
      fileType: 'pdf',
      fileSize: 204800,
      chunkCount: 24,
      status: 'PROCESSING',
      progress: 20,
      createdAt: ts,
      updatedAt: nowIso(),
    })
  }
  if (docOne && m === 'DELETE') return ok(null)
  if (pathOnly.includes('/search') && pathOnly.includes('knowledge-bases') && m === 'POST') {
    return ok([{ chunkId: 1, content: '企业版支持 RBAC 与审计（演示检索）', score: 0.92, documentName: '产品 FAQ.pdf' }])
  }
  if (pathOnly.includes('/test-answer') && m === 'POST') {
    return ok({ answer: '根据知识库：企业版支持 SSO 对接与审计导出（演示）。', citations: [{ title: '产品 FAQ', documentId: 8001 }] })
  }
  const chunks = matchRoute('/documents/:id/chunks', pathOnly)
  if (chunks && m === 'GET') {
    return ok([{ id: 1, sequenceNo: 1, content: '企业版治理能力包括 RBAC…', tokenCount: 48 }])
  }

  if (m === 'GET' && pathOnly === '/workflows') return ok(clone(workflows))
  if (m === 'POST' && pathOnly === '/workflows') {
    const wf = {
      id: nextId('wf'),
      name: (body as { name: string }).name,
      description: (body as { description?: string }).description,
      status: 'DRAFT',
      draftVersionNo: 1,
      definitionJson: JSON.stringify({ nodes: [], edges: [], variables: [] }),
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }
    workflows.push(wf as never)
    return ok(wf)
  }
  const wfHandled = workflowSubPath(pathOnly, m, body)
  if (wfHandled) return wfHandled

  if (m === 'GET' && pathOnly === '/tools') return ok(clone(tools))
  if (m === 'POST' && pathOnly === '/tools') {
    const t = { id: nextId('tool'), ...(body as object), status: 'ACTIVE', createdAt: nowIso(), updatedAt: nowIso() }
    tools.push(t as never)
    return ok(t)
  }
  const toolTest = matchRoute('/tools/:id/test', pathOnly)
  if (toolTest && m === 'POST') {
    return ok({ statusCode: 200, body: '{"ok":true,"demo":true}', durationMs: 120 })
  }

  if (m === 'GET' && pathOnly === '/mcp-servers') return ok(clone(mcpServers))
  if (m === 'POST' && pathOnly.includes('/mcp-servers') && pathOnly.endsWith('/sync')) {
    return ok({ ...mcpServers[0], lastSyncAt: nowIso(), toolCount: 5 })
  }
  const mcpOne = matchRoute('/mcp-servers/:id', pathOnly)
  if (mcpOne && m === 'DELETE') return ok(null)

  if (m === 'GET' && pathOnly === '/model-providers') return ok(clone(providers))
  if (m === 'GET' && pathOnly === '/models') return ok(clone(models))
  if (m === 'GET' && pathOnly === '/model-api-keys') return ok(clone(modelApiKeys))
  if (m === 'GET' && pathOnly === '/platform/models') {
    return ok(
      clone(
        models.map((item) => ({
          id: item.id,
          providerId: item.providerId,
          providerName: item.providerName || 'OpenAI（平台池）',
          modelCode: item.modelCode,
          modelName: item.modelName,
          description: '演示平台模型',
          supportStreaming: true,
          status: 1,
        })),
      ),
    )
  }
  if (m === 'GET' && pathOnly === '/platform/capabilities') {
    return ok({ chat: true, embedding: true, rerank: true, toolCalling: true })
  }

  if (m === 'GET' && pathOnly === '/market/plugin-categories') return ok(clone(pluginCategories))
  if (m === 'GET' && pathOnly === '/market/plugins') {
    const q = parseQuery(rawUrl)
    const cat = q.category
    const list = cat ? marketPlugins.filter((p) => p.category === cat) : marketPlugins
    return ok(clone(list))
  }
  if (matchRoute('/market/plugins/:id/install', pathOnly)) {
    const id = Number(matchRoute('/market/plugins/:id/install', pathOnly)!.id)
    const p = marketPlugins.find((x) => x.id === id)
    if (p) p.installed = m === 'POST'
    return ok(null)
  }
  if (m === 'GET' && pathOnly === '/market/templates') return ok(clone(agentTemplates))
  if (matchRoute('/market/templates/:id/enable', pathOnly) && m === 'POST') {
    return ok(clone(agents[0]))
  }
  if (m === 'POST' && pathOnly === '/market/workspace-plugins') {
    const payload = body as { category: string; title: string; description?: string; manifestJson?: string }
    const row = {
      id: nextId('plugin'),
      pluginCode: `custom_${Date.now()}`,
      category: payload.category,
      title: payload.title,
      description: payload.description,
      installed: true,
      resourceType: 'PLUGIN',
      status: 'ACTIVE',
      reviewStatus: 'APPROVED',
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }
    marketPlugins.unshift(row as never)
    return ok(row)
  }

  if (m === 'GET' && pathOnly === '/tenant/quota') return ok(quotaSnapshot())
  if (m === 'GET' && pathOnly === '/tenant/members') {
    return ok([{ userId: 10001, nickname: '演示用户', email: 'demo@box.ai', roleCode: 'TENANT_ADMIN', status: 'ACTIVE' }])
  }
  if (m === 'GET' && pathOnly === '/tenant/org-access') {
    return ok({ orgId: 'ACME-DEMO', inviteCode: 'DEMO-INVITE', inviteCodeExpiresAt: ts })
  }
  if (m === 'GET' && pathOnly === '/plans') return ok(clone(plans))
  if (m === 'GET' && pathOnly === '/billing/overview') {
    return ok({ currentPlan: plans[1], subscriptionStatus: 'ACTIVE', nextBillingDate: '2026-11-01' })
  }
  if (m === 'GET' && pathOnly === '/billing/invoices') return ok([])
  if (m === 'POST' && pathOnly === '/billing/subscribe') {
    return ok({ paymentId: nextId('pay'), orderNo: `ORD-${Date.now()}`, status: 'PENDING' })
  }
  const paymentOne = matchRoute('/billing/payments/:paymentId', pathOnly)
  if (paymentOne && m === 'GET') {
    return ok({
      id: Number(paymentOne.paymentId),
      status: 'PAID',
      amountCents: 29900,
      paidAt: nowIso(),
      planName: '团队版',
    })
  }

  if (m === 'GET' && pathOnly === '/notifications/unread-count') {
    return ok({ count: notifications.filter((n) => !n.read).length })
  }
  if (m === 'GET' && pathOnly === '/notifications') return ok(clone(notifications))
  if (pathOnly.includes('/notifications/') && pathOnly.endsWith('/read') && m === 'POST') {
    const id = Number(pathOnly.split('/')[2])
    const n = notifications.find((x) => x.id === id)
    if (n) n.read = true
    return ok(null)
  }
  if (m === 'POST' && pathOnly === '/notifications/read-by-key') return ok(null)
  if (m === 'POST' && pathOnly === '/notifications/dismiss') return ok(null)
  if (m === 'POST' && pathOnly === '/notifications/read-all') {
    notifications.forEach((n) => { n.read = true })
    return ok(null)
  }

  if (m === 'GET' && pathOnly === '/system/content') {
    return ok({ aboutMarkdown: '# Box 演示环境\n\n数据均为模拟。', privacyUrl: '/legal/privacy', termsUrl: '/legal/terms' })
  }
  if (m === 'GET' && pathOnly === '/system/health') return ok({ mysql: true, redis: true, elasticsearch: true })

  if (m === 'GET' && pathOnly === '/audit-logs') {
    return ok({ records: clone(auditLogs), total: auditLogs.length, page: 1, pageSize: 20 })
  }
  if (m === 'GET' && pathOnly === '/executions') return ok(clone(executions))
  const ex = matchRoute('/executions/:id', pathOnly)
  if (ex && m === 'GET' && !pathOnly.endsWith('/trace')) {
    const e = executions.find((x) => String(x.id) === ex.id)
    return e ? ok(clone(e)) : fail('执行不存在', 404)
  }
  if (m === 'GET' && pathOnly === '/executions/latest') {
    const q = parseQuery(rawUrl)
    const convId = q.conversationId ? Number(q.conversationId) : undefined
    const hit = executions.find((e) => !convId || e.conversationId === convId) || executions[0]
    return hit ? ok(clone(hit)) : fail('暂无执行记录', 404)
  }
  if (ex && pathOnly.endsWith('/trace') && m === 'GET') {
    return ok({
      traceId: 'trace-demo',
      executionId: Number(ex.id),
      status: 'SUCCESS',
      spans: [
        { spanId: '1', spanType: 'agent', name: 'AgentChat', status: 'SUCCESS', durationMs: 1800 },
        { spanId: '2', parentSpanId: '1', spanType: 'retrieval', name: 'RAG', status: 'SUCCESS', durationMs: 320 },
      ],
    })
  }
  if (m === 'GET' && pathOnly === '/api-keys') return ok(clone(apiKeys))
  if (m === 'POST' && pathOnly === '/api-keys') {
    return ok({ id: nextId('key'), name: (body as { name: string }).name, plainKey: 'box_demo_' + Date.now(), prefix: 'box_demo_' })
  }
  const apiKeyOne = matchRoute('/api-keys/:id', pathOnly)
  if (apiKeyOne && pathOnly.endsWith('/disable') && m === 'POST') {
    return ok({ id: Number(apiKeyOne.id), name: '演示 Key', status: 'DISABLED', prefix: 'box_demo_' })
  }
  if (apiKeyOne && pathOnly.endsWith('/enable') && m === 'POST') {
    return ok({ id: Number(apiKeyOne.id), name: '演示 Key', status: 'ACTIVE', prefix: 'box_demo_' })
  }
  if (apiKeyOne && pathOnly.endsWith('/rotate') && m === 'POST') {
    return ok({
      id: Number(apiKeyOne.id),
      name: '演示 Key',
      plainKey: 'box_demo_' + Date.now(),
      prefix: 'box_demo_',
      status: 'ACTIVE',
    })
  }
  if (m === 'GET' && pathOnly === '/roles') return ok(clone(roles))
  if (m === 'GET' && pathOnly === '/roles/permissions') {
    return ok(consumerPermissions.map((code) => ({ code, name: code, description: '演示权限' })))
  }
  if (m === 'GET' && pathOnly === '/workspace/members') {
    return ok([{ userId: 10001, nickname: '演示用户', email: 'demo@box.ai', roleCode: 'TENANT_ADMIN', status: 'ACTIVE' }])
  }
  if (m === 'GET' && pathOnly === '/workspace/invitations') return ok([])
  if (m === 'POST' && pathOnly === '/workspace/members') {
    return ok({
      userId: nextId('user'),
      nickname: (body as { email?: string })?.email || '新成员',
      email: (body as { email: string }).email,
      roleCode: (body as { roleCode?: string })?.roleCode || 'MEMBER',
      status: 'ACTIVE',
    })
  }
  const wsMember = matchRoute('/workspace/members/:userId', pathOnly)
  if (wsMember && pathOnly.endsWith('/role') && m === 'PUT') {
    return ok({
      userId: Number(wsMember.userId),
      nickname: '演示用户',
      email: 'demo@box.ai',
      roleCode: (body as { roleCode: string }).roleCode,
      status: 'ACTIVE',
    })
  }
  if (wsMember && m === 'DELETE') return ok(null)
  if (m === 'POST' && pathOnly === '/workspace/invitations') {
    return ok({
      id: nextId('inv'),
      email: (body as { email: string }).email,
      roleCode: (body as { roleCode?: string })?.roleCode || 'MEMBER',
      status: 'PENDING',
      expiresAt: ts,
    })
  }
  const wsInv = matchRoute('/workspace/invitations/:id', pathOnly)
  if (wsInv && pathOnly.endsWith('/revoke') && m === 'POST') return ok(null)
  if (m === 'POST' && pathOnly === '/tenant/members') {
    return ok({
      userId: nextId('user'),
      nickname: (body as { nickname?: string })?.nickname || '新成员',
      email: (body as { email: string }).email,
      roleCode: 'MEMBER',
      status: 'ACTIVE',
    })
  }
  if (m === 'POST' && pathOnly === '/tenant/members/accounts') {
    return ok({
      userId: nextId('user'),
      nickname: (body as { nickname: string }).nickname,
      email: (body as { email: string }).email,
      roleCode: 'MEMBER',
      status: 'ACTIVE',
    })
  }
  if (matchRoute('/tenant/members/:userId/password', pathOnly) && m === 'PUT') return ok(null)
  if (matchRoute('/tenant/members/:userId/status', pathOnly) && m === 'PUT') {
    return ok({
      userId: Number(matchRoute('/tenant/members/:userId/status', pathOnly)!.userId),
      nickname: '成员',
      email: 'member@box.ai',
      roleCode: 'MEMBER',
      status: (body as { status: string }).status,
    })
  }
  if (m === 'POST' && pathOnly === '/tenant/invite-code/rotate') {
    return ok({ orgId: 'ACME-DEMO', inviteCode: `DEMO-${Date.now()}`, inviteCodeExpiresAt: ts })
  }
  if (m === 'PUT' && pathOnly === '/tenant/org-id') {
    return ok({ orgId: (body as { orgId: string }).orgId, inviteCode: 'DEMO-INVITE', inviteCodeExpiresAt: ts })
  }
  if (m === 'POST' && pathOnly === '/tenant/upgrade-enterprise') {
    return ok({ ...consumerAuth.tenant, tenantType: 'ENTERPRISE', name: (body as { companyName: string }).companyName })
  }
  if (matchRoute('/ops/placements/:id/track', pathOnly) && m === 'POST') return ok(null)
  const modelChat = matchRoute('/models/:id/chat', pathOnly)
  if (modelChat && m === 'POST') {
    const message = (body as { message?: string })?.message || ''
    return ok({ content: buildAssistantReply(message) })
  }
  if (m === 'POST' && pathOnly === '/model-api-keys') {
    return ok({
      id: nextId('key'),
      name: (body as { name: string }).name,
      providerId: (body as { providerId: number }).providerId,
      maskedSecret: 'sk-demo••••',
      status: 'ACTIVE',
      createdAt: nowIso(),
    })
  }
  if (m === 'POST' && pathOnly === '/market/plugin-catalog-assets') {
    return ok({ url: 'https://demo.box.ai/assets/plugin.png', assetId: nextId('asset') })
  }
  if (m === 'GET' && pathOnly === '/search') {
    const q = parseQuery(rawUrl).q?.toLowerCase() || ''
    const hits = [
      ...agents.filter((a) => a.name.toLowerCase().includes(q)).map((a) => ({ type: 'AGENT', id: a.id, title: a.name })),
      ...workflows.filter((w) => w.name.toLowerCase().includes(q)).map((w) => ({ type: 'WORKFLOW', id: w.id, title: w.name })),
    ]
    return ok(hits.slice(0, 10))
  }
  if (m === 'GET' && pathOnly === '/ops/placements') {
    return ok([{ id: 1, slot: 'GLOBAL_ALERT', title: '演示运营位', content: '欢迎体验 Box', enabled: true }])
  }

  if (m === 'GET' && pathOnly === '/analytics/overview') {
    return ok({
      agentCount: agents.length,
      conversationCount: conversations.length,
      workspaceCount: consumerAuth.workspaces.length,
      executionCount: executions.length,
      knowledgeBaseCount: knowledgeBases.length,
      toolCount: tools.length,
      workflowCount: workflows.length,
      mcpServerCount: mcpServers.length,
      quota: quotaSnapshot(),
      periodDays: 7,
      periodExecutionCount: 120,
      successRate: 0.98,
      avgLatencyMs: 820,
      recentAgents: agents.map((a) => ({ id: a.id, name: a.name, status: a.status, updatedAt: a.updatedAt })),
      recentConversations: conversations.map((c) => ({
        id: c.id,
        agentId: c.agentId,
        title: c.title,
        lastMessageAt: c.lastMessageAt,
        updatedAt: c.updatedAt,
      })),
      recentWorkflows: workflows.map((w) => ({ id: w.id, name: w.name, status: w.status, updatedAt: w.updatedAt })),
      topAgents: [{ agentId: 4001, agentName: '客服助手', executionCount: 80, successRate: 0.99 }],
    })
  }
  if (m === 'GET' && pathOnly === '/analytics/trends') {
    return ok({
      periodDays: 7,
      points: Array.from({ length: 7 }).map((_, i) => ({
        date: `2026-10-${19 + i}`,
        executionCount: 12 + i * 2,
        successRate: 0.98,
        avgLatencyMs: 800 + i * 10,
      })),
    })
  }

  if (m === 'POST' && pathOnly === '/assets/images') {
    return ok({ url: 'https://demo.box.ai/assets/placeholder.png', assetId: nextId('asset') })
  }

  const generic = tryGenericRest(m, pathOnly, body, {
    collections: {
      agents: agents as Array<Record<string, unknown>>,
      workflows: workflows as Array<Record<string, unknown>>,
      knowledgeBases: knowledgeBases as Array<Record<string, unknown>>,
      tools: tools as Array<Record<string, unknown>>,
      mcpServers: mcpServers as Array<Record<string, unknown>>,
      providers: providers as Array<Record<string, unknown>>,
      models: models as Array<Record<string, unknown>>,
      modelApiKeys: modelApiKeys as Array<Record<string, unknown>>,
      conversations: conversations as Array<Record<string, unknown>>,
      projects: projects as Array<Record<string, unknown>>,
      apiKeys: apiKeys as Array<Record<string, unknown>>,
      roles: roles as Array<Record<string, unknown>>,
      executions: executions as Array<Record<string, unknown>>,
      notifications: notifications as Array<Record<string, unknown>>,
      marketPlugins: marketPlugins as Array<Record<string, unknown>>,
      agentTemplates: agentTemplates as Array<Record<string, unknown>>,
      workspaces: consumerAuth.workspaces as Array<Record<string, unknown>>,
    },
  })
  if (generic) {
    commitConsumerState()
    return generic
  }

  if (m === 'POST' || m === 'PUT' || m === 'DELETE') {
    commitConsumerState()
    return ok(body ?? null)
  }
  return ok([])
}

function snapshotConsumerState() {
  return {
    consumerAuth,
    agents,
    conversations,
    messagesByConversation,
    projects,
    knowledgeBases,
    knowledgeDocuments,
    workflows,
    tools,
    mcpServers,
    providers,
    models,
    modelApiKeys,
    marketPlugins,
    agentTemplates,
    executions,
    notifications,
    auditLogs,
    apiKeys,
    roles,
  }
}

function restoreConsumerState(saved: ReturnType<typeof snapshotConsumerState>) {
  Object.assign(consumerAuth, saved.consumerAuth)
  agents.splice(0, agents.length, ...(saved.agents as typeof agents))
  conversations.splice(0, conversations.length, ...(saved.conversations as typeof conversations))
  Object.keys(messagesByConversation).forEach((k) => delete messagesByConversation[Number(k)])
  Object.assign(messagesByConversation, saved.messagesByConversation)
  projects.splice(0, projects.length, ...(saved.projects as typeof projects))
  knowledgeBases.splice(0, knowledgeBases.length, ...(saved.knowledgeBases as typeof knowledgeBases))
  Object.keys(knowledgeDocuments).forEach((k) => delete knowledgeDocuments[Number(k)])
  Object.assign(knowledgeDocuments, saved.knowledgeDocuments)
  workflows.splice(0, workflows.length, ...(saved.workflows as typeof workflows))
  tools.splice(0, tools.length, ...(saved.tools as typeof tools))
  mcpServers.splice(0, mcpServers.length, ...(saved.mcpServers as typeof mcpServers))
  providers.splice(0, providers.length, ...(saved.providers as typeof providers))
  models.splice(0, models.length, ...(saved.models as typeof models))
  modelApiKeys.splice(0, modelApiKeys.length, ...(saved.modelApiKeys as typeof modelApiKeys))
  marketPlugins.splice(0, marketPlugins.length, ...(saved.marketPlugins as typeof marketPlugins))
  agentTemplates.splice(0, agentTemplates.length, ...(saved.agentTemplates as typeof agentTemplates))
  executions.splice(0, executions.length, ...(saved.executions as typeof executions))
  notifications.splice(0, notifications.length, ...(saved.notifications as typeof notifications))
  auditLogs.splice(0, auditLogs.length, ...(saved.auditLogs as typeof auditLogs))
  apiKeys.splice(0, apiKeys.length, ...(saved.apiKeys as typeof apiKeys))
  roles.splice(0, roles.length, ...(saved.roles as typeof roles))
}

export function commitConsumerState() {
  saveJson(persistKeys.consumer, snapshotConsumerState())
}

const persistedConsumer = loadJson(persistKeys.consumer, null as ReturnType<typeof snapshotConsumerState> | null)
if (persistedConsumer) {
  restoreConsumerState(persistedConsumer)
}

/** 流式结束后写入会话（与前端乐观 UI 对齐） */
export function appendStreamConversationMessage(conversationId: number, userText: string, assistantText: string) {
  const list = messagesByConversation[conversationId] || (messagesByConversation[conversationId] = [])
  const seq = list.length
  list.push(
    normalizeChatMessage({
      id: nextId('msg'),
      role: 'USER',
      content: userText,
      contentType: 'TEXT',
      sequenceNo: seq + 1,
      createdAt: nowIso(),
    }),
  )
  list.push(
    normalizeChatMessage({
      id: nextId('msg'),
      role: 'ASSISTANT',
      content: assistantText,
      contentType: 'TEXT',
      sequenceNo: seq + 2,
      createdAt: nowIso(),
      metadataJson: JSON.stringify({
        citations: [demoCitation],
      }),
    }),
  )
  const conv = conversations.find((c) => c.id === conversationId)
  if (conv) {
    conv.messageCount = list.length
    conv.lastMessageAt = nowIso()
    conv.updatedAt = nowIso()
  }
  commitConsumerState()
}

export function finalizeRegenerateAssistantMessage(conversationId: number, assistantText: string) {
  const list = messagesByConversation[conversationId] || (messagesByConversation[conversationId] = [])
  const last = list[list.length - 1]
  if (last && normalizeChatRole(last.role) === 'ASSISTANT') {
    last.content = assistantText
    last.updatedAt = nowIso()
    last.metadataJson = JSON.stringify({ citations: [demoCitation] })
  } else {
    list.push(
      normalizeChatMessage({
        id: nextId('msg'),
        role: 'ASSISTANT',
        content: assistantText,
        contentType: 'TEXT',
        sequenceNo: list.length + 1,
        createdAt: nowIso(),
        metadataJson: JSON.stringify({ citations: [demoCitation] }),
      }),
    )
  }
  const conv = conversations.find((c) => c.id === conversationId)
  if (conv) {
    conv.messageCount = list.length
    conv.lastMessageAt = nowIso()
    conv.updatedAt = nowIso()
  }
  commitConsumerState()
}
