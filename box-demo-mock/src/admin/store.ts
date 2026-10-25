import { tryGenericRest } from '../engine/genericRest'
import { matchRoute, stripApiPrefix } from '../match'
import { fail, ok, type ApiResult } from '../result'
import { nextId } from '../id'
import {
  seedAdminPlugins,
  seedAgentTemplates,
  seedAuditLogs,
  seedBillingInvoices,
  seedMessageFeedbacks,
  seedOpsPlacements,
  seedPlatformModels,
  seedPlatformProviders,
  seedPlatformUsers,
  seedPlans,
  seedPluginCategories,
  seedSystemConfigs,
  seedTenants,
} from './seeds'
import { clone, nowIso, parseQuery } from '../util'
import { loadJson, persistKeys, saveJson } from '../world/persist'

export const DEMO_ADMIN_TOKEN = 'box-demo-admin-token'

const ts = nowIso()

export const adminAuth = {
  token: DEMO_ADMIN_TOKEN,
  user: {
    id: 1,
    username: 'admin',
    email: 'admin@box.ai',
    nickname: '平台管理员',
    userType: 'PLATFORM_ADMIN',
    platformAdminRole: 'SUPER_ADMIN',
  },
}

export const tenants = clone(seedTenants)
export const plans = clone(seedPlans)
export const agentTemplates = clone(seedAgentTemplates)
export const adminPlugins = clone(seedAdminPlugins)
export const opsPlacements = clone(seedOpsPlacements)
export const messageFeedbacks = clone(seedMessageFeedbacks)
export const auditLogs = clone(seedAuditLogs)
export const systemConfigs = clone(seedSystemConfigs)
const platformUsers = clone(seedPlatformUsers)

export function dispatchAdminMock(method: string, rawUrl: string, body: unknown): ApiResult<unknown> {
  const path = stripApiPrefix(rawUrl.split('?')[0], '/api/v1/admin')
  const m = method.toUpperCase()

  if (m === 'POST' && path === '/auth/login') return ok(clone(adminAuth))
  if (m === 'GET' && path === '/auth/me') return ok(clone(adminAuth))
  if (m === 'POST' && path === '/auth/verification-code') return ok({ devCode: null })

  if (m === 'GET' && path === '/tenants') return ok(clone(tenants))
  if (m === 'POST' && path === '/tenants') {
    const t = {
      id: nextId('tenant'),
      ...(body as object),
      status: 1,
      createdAt: nowIso(),
    }
    tenants.push(t as never)
    return ok(t)
  }
  const tenantOne = matchRoute('/tenants/:id/quota', path)
  if (tenantOne && m === 'GET') {
    return ok({
      tenantId: Number(tenantOne.id),
      planId: 2,
      planName: '团队版',
      period: '2026-10',
      quotaTokens: 5000000,
      usedTokens: 890000,
      remainingTokens: 4110000,
      quotaAiCalls: 10000,
      usedAiCalls: 1240,
      remainingAiCalls: 8760,
      quotaMembers: 20,
      usedMembers: 8,
      remainingMembers: 12,
      quotaWorkspaces: 5,
      usedWorkspaces: 2,
      remainingWorkspaces: 3,
    })
  }
  if (matchRoute('/tenants/:id/workspaces', path) && m === 'GET') {
    const tid = Number(matchRoute('/tenants/:id/workspaces', path)!.id)
    return ok([
      { id: tid * 10 + 1, name: '默认工作空间', memberCount: 12, status: 1 },
      { id: tid * 10 + 2, name: '客服支持', memberCount: 8, status: 1 },
      { id: tid * 10 + 3, name: '研发测试', memberCount: 5, status: 1 },
    ])
  }
  if (matchRoute('/tenants/:id/members', path) && m === 'GET') {
    return ok([
      { userId: 10001, email: 'admin@tenant.demo', nickname: '租户管理员', roleCode: 'TENANT_ADMIN', status: 'ACTIVE' },
      { userId: 10002, email: 'member1@tenant.demo', nickname: '成员甲', roleCode: 'MEMBER', status: 'ACTIVE' },
      { userId: 10003, email: 'member2@tenant.demo', nickname: '成员乙', roleCode: 'MEMBER', status: 'ACTIVE' },
      { userId: 10004, email: 'viewer@tenant.demo', nickname: '只读访客', roleCode: 'VIEWER', status: 'ACTIVE' },
    ])
  }

  if (m === 'GET' && path === '/plans') return ok(clone(plans))
  if (m === 'POST' && path === '/plans') return ok({ id: nextId('plan'), ...(body as object), status: 'ACTIVE' })

  if (m === 'GET' && path === '/users') {
    const q = parseQuery(rawUrl)
    const page = Math.max(1, Number(q.page) || 1)
    const pageSize = Math.max(1, Number(q.pageSize) || 20)
    const keyword = (q.keyword || '').toLowerCase()
    let list = platformUsers
    if (keyword) {
      list = list.filter(
        (u) =>
          String(u.email || '').toLowerCase().includes(keyword) ||
          String(u.nickname || '').toLowerCase().includes(keyword) ||
          String((u as { tenantName?: string }).tenantName || '').toLowerCase().includes(keyword),
      )
    }
    if (q.status !== undefined && q.status !== '') {
      const st = Number(q.status)
      list = list.filter((u) => u.status === st)
    }
    const start = (page - 1) * pageSize
    const records = list.slice(start, start + pageSize)
    return ok({ records: clone(records), total: list.length, page, pageSize })
  }
  if (matchRoute('/users/:id/context', path) && m === 'GET') {
    const uid = Number(matchRoute('/users/:id/context', path)!.id)
    const user = platformUsers.find((u) => Number(u.id) === uid)
    return ok({
      userId: uid,
      email: user?.email,
      nickname: user?.nickname,
      primaryTenantId: tenants[0]?.id,
      primaryTenantName: tenants[0]?.name,
      tenantType: tenants[0]?.tenantType,
      planId: tenants[0]?.planId,
      planName: tenants[0]?.planName,
      workspaceNames: ['默认工作空间', '客服支持', '研发测试'],
      monthAiCalls: 1240 + (uid % 7) * 80,
      monthTokens: 420000 + (uid % 5) * 50000,
      recentFailureRate: 0.012 + (uid % 3) * 0.004,
    })
  }

  if (m === 'GET' && path === '/analytics/overview') {
    return ok({
      periodDays: 7,
      usagePeriod: '2026-10',
      tenantCount: tenants.length,
      periodExecutionCount: 1240,
      totalExecutionCount: 50210,
      successRate: 0.986,
      avgLatencyMs: 840,
      periodTokens: 890000,
      periodAiCalls: 1240,
      monthTokens: 2100000,
      monthAiCalls: 3100,
      topTenants: tenants.map((t) => ({
        tenantId: t.id,
        tenantName: t.name,
        aiCalls: 620,
        tokens: 420000,
      })),
    })
  }
  if (m === 'GET' && path === '/analytics/trends') {
    return ok({
      periodDays: 7,
      points: Array.from({ length: 7 }).map((_, i) => ({
        date: `2026-10-${19 + i}`,
        executionCount: 120 + i * 8,
        successRate: 0.98,
        avgLatencyMs: 800 + i * 10,
      })),
    })
  }
  if (matchRoute('/analytics/tenants/:id', path) && m === 'GET') {
    return ok({
      tenantId: Number(matchRoute('/analytics/tenants/:id', path)!.id),
      periodDays: 7,
      executionCount: 320,
      tokens: 180000,
      topAgents: [{ agentId: 4001, agentName: '客服助手', executionCount: 200 }],
    })
  }

  if (m === 'GET' && path === '/audit-logs') {
    return ok({ records: clone(auditLogs), total: auditLogs.length, page: 1, pageSize: 20 })
  }
  if (m === 'GET' && path === '/message-feedbacks') {
    return ok({ records: clone(messageFeedbacks), total: messageFeedbacks.length, page: 1, pageSize: 20 })
  }
  if (matchRoute('/message-feedbacks/:id', path) && m === 'GET') {
    const item = messageFeedbacks.find((f) => String(f.id) === matchRoute('/message-feedbacks/:id', path)!.id)
    return ok(item ? { ...item, messageContent: '企业版支持哪些治理能力？', reply: null } : null)
  }
  if (matchRoute('/message-feedbacks/:id/reply', path) && m === 'POST') {
    const fb = messageFeedbacks.find((f) => String(f.id) === matchRoute('/message-feedbacks/:id/reply', path)!.id)
    if (fb) {
      fb.status = 'REPLIED'
      ;(fb as { reply?: string }).reply = (body as { reply: string }).reply
    }
    commitAdminState()
    return ok(null)
  }
  if (m === 'POST' && path === '/notifications/read') return ok(null)
  if (m === 'POST' && path === '/notifications/read-all') return ok(null)
  if (m === 'POST' && path === '/notifications/dismiss') return ok(null)
  if (m === 'POST' && path === '/auth/password/reset') return ok(null)
  if (matchRoute('/tenants/:id/status', path) && m === 'PUT') {
    const t = tenants.find((x) => String(x.id) === matchRoute('/tenants/:id/status', path)!.id)
    if (t) t.status = (body as { status: number }).status
    commitAdminState()
    return t ? ok(clone(t)) : fail('租户不存在', 404)
  }
  if (matchRoute('/tenants/:id/plan', path) && m === 'PUT') {
    const route = matchRoute('/tenants/:id/plan', path)!
    const t = tenants.find((x) => String(x.id) === route.id)
    const planId = (body as { planId: number }).planId
    const plan = plans.find((p) => p.id === planId)
    if (t && plan) {
      t.planId = planId
      t.planName = plan.name
    }
    commitAdminState()
    return t ? ok(clone(t)) : fail('租户不存在', 404)
  }
  if (matchRoute('/tenants/:tenantId/members', path) && m === 'POST') {
    return ok({
      userId: nextId('user'),
      email: (body as { email: string }).email,
      nickname: (body as { nickname?: string })?.nickname || '新成员',
      roleCode: 'MEMBER',
      status: 'ACTIVE',
    })
  }
  if (matchRoute('/tenants/:tenantId/members/:userId/status', path) && m === 'PUT') {
    return ok({
      userId: Number(matchRoute('/tenants/:tenantId/members/:userId/status', path)!.userId),
      email: 'member@demo.box.ai',
      nickname: '成员',
      roleCode: 'MEMBER',
      status: (body as { status: string }).status,
    })
  }
  if (m === 'POST' && path === '/users') {
    return ok({
      id: nextId('user'),
      email: (body as { email: string }).email,
      nickname: (body as { nickname?: string })?.nickname || '新用户',
      status: 'ACTIVE',
      tenantName: 'Box 演示租户',
    })
  }
  if (matchRoute('/users/:id/status', path) && m === 'PUT') {
    return ok({
      id: Number(matchRoute('/users/:id/status', path)!.id),
      email: 'demo@box.ai',
      nickname: '演示用户',
      status: (body as { status: string }).status,
      tenantName: 'Box 演示租户',
    })
  }

  if (m === 'GET' && path === '/billing/invoices') {
    return ok(clone(seedBillingInvoices))
  }

  if (m === 'GET' && path === '/notifications/inbox') {
    return ok({
      items: [
        { key: 'admin.tenant.quota', title: '租户额度预警', content: 'Acme 科技 Token 使用率 82%', read: false, createdAt: ts },
        { key: 'admin.plugin.review', title: '插件待审核', content: '社区插件「图表生成」待审核', read: false, createdAt: ts },
        { key: 'admin.feedback', title: '新用户反馈', content: '14 条未处理反馈', read: true, createdAt: ts },
        { key: 'admin.release', title: '版本发布', content: '平台模型同步完成', read: true, createdAt: ts },
      ],
      unreadCount: 2,
    })
  }

  if (m === 'GET' && path === '/system/config') return ok(clone(systemConfigs))
  if (m === 'PUT' && path === '/system/config') return ok({ ...(body as object), updatedAt: nowIso() })

  if (m === 'GET' && path === '/plugin-categories') {
    return ok(clone(seedPluginCategories))
  }
  if (m === 'GET' && path === '/plugins') return ok(clone(adminPlugins))
  if (m === 'GET' && path === '/agent-templates') return ok(clone(agentTemplates))
  if (m === 'GET' && path === '/ops/placements') return ok(clone(opsPlacements))
  if (m === 'GET' && path === '/ops/placements/active') return ok(clone(opsPlacements.filter((p) => p.enabled)))

  if (m === 'GET' && path === '/platform/providers') {
    return ok(clone(seedPlatformProviders))
  }
  if (m === 'GET' && path === '/platform/models') {
    const q = parseQuery(rawUrl)
    const providerId = q.providerId ? Number(q.providerId) : undefined
    const list = providerId
      ? seedPlatformModels.filter((m) => m.providerId === providerId)
      : seedPlatformModels
    return ok(clone(list))
  }
  if (matchRoute('/platform/providers/:id/credentials', path) && m === 'GET') {
    return ok([{ id: 1, providerId: 1, name: '生产密钥', maskedSecret: 'sk-••••', status: 'ACTIVE' }])
  }
  if (matchRoute('/platform/providers/:id/upstream-models', path) && m === 'GET') {
    return ok([{ modelCode: 'gpt-4.1', modelName: 'GPT-4.1' }])
  }
  if (m === 'GET' && path === '/platform/object-storage') {
    return ok({
      backend: 'MINIO',
      endpoint: 'https://minio.demo.box.ai',
      bucket: 'box-demo',
      region: 'auto',
      publicBaseUrl: 'https://cdn.demo.box.ai',
    })
  }
  if (m === 'POST' && path === '/platform/object-storage/test') return ok({ ok: true })
  if (m === 'GET' && path === '/platform/ocr-default') return ok({ platformModelId: 1 })

  if (m === 'POST' && path === '/assets/images') {
    return ok({ url: 'https://demo.box.ai/assets/admin.png', assetId: nextId('asset') })
  }

  const generic = tryGenericRest(m, path, body, {
    collections: {
      tenants: tenants as Array<Record<string, unknown>>,
      plans: plans as Array<Record<string, unknown>>,
      agentTemplates: agentTemplates as Array<Record<string, unknown>>,
      adminPlugins: adminPlugins as Array<Record<string, unknown>>,
      opsPlacements: opsPlacements as Array<Record<string, unknown>>,
      messageFeedbacks: messageFeedbacks as Array<Record<string, unknown>>,
      platformProviders: seedPlatformProviders as Array<Record<string, unknown>>,
      platformModels: seedPlatformModels as Array<Record<string, unknown>>,
    },
  })
  if (generic) {
    commitAdminState()
    return generic
  }

  if (m === 'POST' || m === 'PUT' || m === 'DELETE') {
    commitAdminState()
    return ok(body ?? null)
  }
  return ok([])
}

function snapshotAdminState() {
  return { tenants, plans, agentTemplates, adminPlugins, opsPlacements, messageFeedbacks, auditLogs, systemConfigs }
}

function restoreAdminState(saved: ReturnType<typeof snapshotAdminState>) {
  tenants.splice(0, tenants.length, ...saved.tenants)
  plans.splice(0, plans.length, ...saved.plans)
  agentTemplates.splice(0, agentTemplates.length, ...saved.agentTemplates)
  adminPlugins.splice(0, adminPlugins.length, ...saved.adminPlugins)
  opsPlacements.splice(0, opsPlacements.length, ...saved.opsPlacements)
  messageFeedbacks.splice(0, messageFeedbacks.length, ...saved.messageFeedbacks)
  auditLogs.splice(0, auditLogs.length, ...saved.auditLogs)
  systemConfigs.splice(0, systemConfigs.length, ...saved.systemConfigs)
}

export function commitAdminState() {
  saveJson(persistKeys.admin, snapshotAdminState())
}

const persistedAdmin = loadJson(persistKeys.admin, null as ReturnType<typeof snapshotAdminState> | null)
if (persistedAdmin) {
  restoreAdminState(persistedAdmin)
}
