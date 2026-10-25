import { nowIso } from '../util'

const ts = nowIso()

export const seedPlans = [
  { id: 1, code: 'personal', name: '个人版', status: 'ACTIVE', maxMembers: 1, monthlyTokens: 500_000, monthlyAiCalls: 5_000, priceCents: 0 },
  { id: 2, code: 'team', name: '团队版', status: 'ACTIVE', maxMembers: 20, monthlyTokens: 5_000_000, monthlyAiCalls: 50_000, priceCents: 29_900 },
  { id: 3, code: 'enterprise', name: '企业版', status: 'ACTIVE', maxMembers: 200, monthlyTokens: 50_000_000, monthlyAiCalls: 500_000, priceCents: 199_900 },
]

export const seedTenants = [
  { id: 2001, name: 'Acme 科技', slug: 'acme-tech', tenantType: 'ENTERPRISE', planId: 3, planName: '企业版', contactEmail: 'ops@acme.demo', status: 1, ownerId: 10001, createdAt: ts },
  { id: 2002, name: '星云零售', slug: 'nebula-retail', tenantType: 'ENTERPRISE', planId: 2, planName: '团队版', contactEmail: 'it@nebula.demo', status: 1, ownerId: 10002, createdAt: ts },
  { id: 2003, name: '蓝海教育', slug: 'blueedu', tenantType: 'ENTERPRISE', planId: 2, planName: '团队版', contactEmail: 'admin@blueedu.demo', status: 1, ownerId: 10003, createdAt: ts },
  { id: 2004, name: '个人创作者 A', slug: 'creator-a', tenantType: 'PERSONAL', planId: 1, planName: '个人版', contactEmail: 'a@creator.demo', status: 1, ownerId: 10004, createdAt: ts },
  { id: 2005, name: '个人创作者 B', slug: 'creator-b', tenantType: 'PERSONAL', planId: 1, planName: '个人版', contactEmail: 'b@creator.demo', status: 1, ownerId: 10005, createdAt: ts },
  { id: 2006, name: '智造工厂', slug: 'smart-factory', tenantType: 'ENTERPRISE', planId: 3, planName: '企业版', contactEmail: 'mes@factory.demo', status: 1, ownerId: 10006, createdAt: ts },
  { id: 2007, name: '云帆金融', slug: 'yunfan-finance', tenantType: 'ENTERPRISE', planId: 3, planName: '企业版', contactEmail: 'risk@yunfan.demo', status: 0, ownerId: 10007, createdAt: ts },
  { id: 2008, name: '试验租户', slug: 'sandbox', tenantType: 'PERSONAL', planId: 1, planName: '个人版', contactEmail: 'sandbox@demo', status: 0, ownerId: 10008, createdAt: ts },
]

export const seedPlatformUsers = [
  { id: 10001, email: 'demo@box.ai', nickname: '演示管理员', userType: 'TENANT_USER', status: 1, tenantName: 'Acme 科技', lastLoginAt: ts, createdAt: ts },
  { id: 10002, email: 'ops@acme.demo', nickname: 'Acme 运维', userType: 'TENANT_USER', status: 1, tenantName: 'Acme 科技', lastLoginAt: ts, createdAt: ts },
  { id: 10003, email: 'pm@nebula.demo', nickname: '星云产品', userType: 'TENANT_USER', status: 1, tenantName: '星云零售', lastLoginAt: ts, createdAt: ts },
  { id: 10004, email: 'edu@blue.demo', nickname: '教务老师', userType: 'TENANT_USER', status: 1, tenantName: '蓝海教育', lastLoginAt: ts, createdAt: ts },
  { id: 10005, email: 'creator@demo', nickname: '创作者', userType: 'TENANT_USER', status: 1, tenantName: '个人创作者 A', lastLoginAt: ts, createdAt: ts },
  { id: 10006, email: 'mes@factory.demo', nickname: '产线工程师', userType: 'TENANT_USER', status: 1, tenantName: '智造工厂', lastLoginAt: ts, createdAt: ts },
  { id: 10007, email: 'risk@yunfan.demo', nickname: '风控专员', userType: 'TENANT_USER', status: 0, tenantName: '云帆金融', lastLoginAt: ts, createdAt: ts },
  { id: 10008, email: 'guest@demo', nickname: '访客账号', userType: 'TENANT_USER', status: 0, tenantName: '试验租户', lastLoginAt: ts, createdAt: ts },
  { id: 10009, email: 'support@box.ai', nickname: '平台客服', userType: 'PLATFORM_ADMIN', platformAdminRole: 'SUPPORT', status: 1, tenantName: '—', lastLoginAt: ts, createdAt: ts },
  { id: 10010, email: 'audit@box.ai', nickname: '审计账号', userType: 'PLATFORM_ADMIN', platformAdminRole: 'AUDITOR', status: 1, tenantName: '—', lastLoginAt: ts, createdAt: ts },
  { id: 10011, email: 'sales@box.ai', nickname: '商务拓展', userType: 'PLATFORM_ADMIN', platformAdminRole: 'SALES', status: 1, tenantName: '—', lastLoginAt: ts, createdAt: ts },
  { id: 10012, email: 'dev@box.ai', nickname: '平台研发', userType: 'PLATFORM_ADMIN', platformAdminRole: 'ENGINEER', status: 1, tenantName: '—', lastLoginAt: ts, createdAt: ts },
]

export const seedAgentTemplates = [
  { id: 1, name: '客服助手', description: 'FAQ + 订单工具', category: 'customer', status: 'ACTIVE', reviewStatus: 'APPROVED', installCount: 1280, createdAt: ts },
  { id: 2, name: '销售顾问', description: '线索跟进与报价', category: 'sales', status: 'ACTIVE', reviewStatus: 'APPROVED', installCount: 860, createdAt: ts },
  { id: 3, name: '知识库问答', description: 'RAG 标准模板', category: 'knowledge', status: 'ACTIVE', reviewStatus: 'APPROVED', installCount: 2100, createdAt: ts },
  { id: 4, name: '数据分析', description: '指标解读', category: 'analytics', status: 'ACTIVE', reviewStatus: 'PENDING', installCount: 320, createdAt: ts },
  { id: 5, name: '工单分流', description: '工作流 + Switch', category: 'workflow', status: 'DRAFT', reviewStatus: 'PENDING', installCount: 45, createdAt: ts },
  { id: 6, name: 'HR 入职', description: '政策问答', category: 'hr', status: 'ACTIVE', reviewStatus: 'APPROVED', installCount: 540, createdAt: ts },
]

export const seedAdminPlugins = [
  { id: 1, pluginCode: 'http_probe', category: 'integration', title: 'HTTP 探活', description: '定时探测接口可用性', sourceType: 'OFFICIAL', reviewStatus: 'APPROVED', status: 'ACTIVE', sortOrder: 1, installCount: 920 },
  { id: 2, pluginCode: 'crm_sync', category: 'integration', title: 'CRM 同步', description: '同步客户到外部 CRM', sourceType: 'OFFICIAL', reviewStatus: 'APPROVED', status: 'ACTIVE', sortOrder: 2, installCount: 640 },
  { id: 3, pluginCode: 'slack_notify', category: 'notification', title: 'Slack 通知', description: '消息推送到 Slack', sourceType: 'PARTNER', reviewStatus: 'APPROVED', status: 'ACTIVE', sortOrder: 3, installCount: 410 },
  { id: 4, pluginCode: 'feishu_bot', category: 'notification', title: '飞书机器人', description: '飞书群机器人', sourceType: 'PARTNER', reviewStatus: 'APPROVED', status: 'ACTIVE', sortOrder: 4, installCount: 780 },
  { id: 5, pluginCode: 'pdf_export', category: 'productivity', title: 'PDF 导出', description: '对话导出 PDF', sourceType: 'OFFICIAL', reviewStatus: 'APPROVED', status: 'ACTIVE', sortOrder: 5, installCount: 1200 },
  { id: 6, pluginCode: 'chart_gen', category: 'analytics', title: '图表生成', description: '根据数据生成图表', sourceType: 'COMMUNITY', reviewStatus: 'PENDING', status: 'ACTIVE', sortOrder: 6, installCount: 88 },
  { id: 7, pluginCode: 'web_search', category: 'search', title: '联网搜索', description: 'Bing 搜索增强', sourceType: 'OFFICIAL', reviewStatus: 'APPROVED', status: 'ACTIVE', sortOrder: 7, installCount: 1560 },
  { id: 8, pluginCode: 'code_runner', category: 'dev', title: '代码沙箱', description: '安全执行 Python', sourceType: 'OFFICIAL', reviewStatus: 'REJECTED', status: 'INACTIVE', sortOrder: 8, installCount: 12 },
]

export const seedOpsPlacements = [
  { id: 1, slot: 'GLOBAL_ALERT', title: '双 11 活动', content: '团队版限时 8 折', enabled: true, audience: 'CONSUMER', updatedAt: ts },
  { id: 2, slot: 'DASHBOARD_BANNER', title: '新功能', content: '工作流调试支持 SSE', enabled: true, audience: 'CONSUMER', updatedAt: ts },
  { id: 3, slot: 'LOGIN_SIDEBAR', title: '安全合规', content: '企业版支持审计导出', enabled: true, audience: 'CONSUMER', updatedAt: ts },
  { id: 4, slot: 'GLOBAL_ALERT', title: '管理端维护', content: '本周六 02:00 升级', enabled: false, audience: 'ADMIN', updatedAt: ts },
  { id: 5, slot: 'MARKET_TOP', title: '插件大赛', content: '投稿赢取流量扶持', enabled: true, audience: 'CONSUMER', updatedAt: ts },
]

export const seedMessageFeedbacks = Array.from({ length: 14 }).map((_, i) => ({
  id: i + 1,
  tenantId: seedTenants[i % seedTenants.length].id,
  conversationId: 6000 + i,
  messageId: 7000 + i,
  rating: i % 3 === 0 ? 'UP' : 'DOWN',
  comment: ['回答很准', '引用来源不全', '希望支持导出', '工具调用慢', '体验不错'][i % 5],
  status: i % 4 === 0 ? 'OPEN' : i % 4 === 1 ? 'REPLIED' : 'CLOSED',
  createdAt: ts,
}))

export const seedAuditLogs = Array.from({ length: 28 }).map((_, i) => ({
  id: i + 1,
  actorEmail: ['admin@box.ai', 'ops@acme.demo', 'audit@box.ai'][i % 3],
  action: ['tenant:update_plan', 'user:disable', 'plugin:review', 'model:import', 'tenant:create'][i % 5],
  resourceType: ['TENANT', 'USER', 'PLUGIN', 'MODEL', 'TENANT'][i % 5],
  resourceId: 2000 + (i % 8),
  detail: `演示审计记录 #${i + 1}`,
  createdAt: ts,
}))

export const seedSystemConfigs = [
  { key: 'mail.enabled', value: 'true', description: '邮件服务' },
  { key: 'mail.from', value: 'noreply@box.ai', description: '发件地址' },
  { key: 'oauth.github.enabled', value: 'false', description: 'GitHub OAuth' },
  { key: 'oauth.google.enabled', value: 'true', description: 'Google OAuth' },
  { key: 'signup.enabled', value: 'true', description: '开放注册' },
  { key: 'demo.banner', value: 'true', description: '演示横幅' },
]

export const seedBillingInvoices = [
  { id: 1, tenantId: 2001, amountCents: 199_900, status: 'PAID', period: '2026-09', createdAt: ts },
  { id: 2, tenantId: 2002, amountCents: 29_900, status: 'PAID', period: '2026-09', createdAt: ts },
  { id: 3, tenantId: 2006, amountCents: 199_900, status: 'PENDING', period: '2026-10', createdAt: ts },
  { id: 4, tenantId: 2003, amountCents: 29_900, status: 'FAILED', period: '2026-08', createdAt: ts },
]

export const seedPlatformProviders = [
  { id: 1, providerCode: 'openai', providerName: 'OpenAI', providerType: 'OPENAI', status: 'ACTIVE' },
  { id: 2, providerCode: 'anthropic', providerName: 'Anthropic', providerType: 'ANTHROPIC', status: 'ACTIVE' },
  { id: 3, providerCode: 'deepseek', providerName: 'DeepSeek', providerType: 'OPENAI_COMPAT', status: 'ACTIVE' },
]

export const seedPlatformModels = [
  { id: 1, providerId: 1, modelCode: 'gpt-4.1', modelName: 'GPT-4.1', modelType: 'CHAT', status: 'ACTIVE', supportStreaming: true, supportToolCalling: true },
  { id: 2, providerId: 1, modelCode: 'text-embedding-3-large', modelName: 'Embedding 3 Large', modelType: 'EMBEDDING', status: 'ACTIVE', supportStreaming: false, supportToolCalling: false },
  { id: 3, providerId: 2, modelCode: 'claude-sonnet-4', modelName: 'Claude Sonnet 4', modelType: 'CHAT', status: 'ACTIVE', supportStreaming: true, supportToolCalling: true },
  { id: 4, providerId: 3, modelCode: 'deepseek-chat', modelName: 'DeepSeek Chat', modelType: 'CHAT', status: 'ACTIVE', supportStreaming: true, supportToolCalling: true },
  { id: 5, providerId: 1, modelCode: 'gpt-4.1-mini', modelName: 'GPT-4.1 Mini', modelType: 'CHAT', status: 'ACTIVE', supportStreaming: true, supportToolCalling: true },
]

export const seedPluginCategories = [
  { value: 'integration', label: '集成', desc: 'HTTP / Webhook / CRM' },
  { value: 'notification', label: '通知', desc: 'IM / 邮件 / 短信' },
  { value: 'analytics', label: '分析', desc: '报表与可视化' },
  { value: 'productivity', label: '效率', desc: '导出 / 文档' },
]
