import { fail, ok, type ApiResult } from '../result'
import { nextId } from '../id'
import { clone, nowIso } from '../util'

type Collection = Array<Record<string, unknown>>

export interface GenericRestContext {
  collections: Record<string, Collection>
}

/** 将常见 REST 路径映射到内存集合名 */
const PATH_TO_COLLECTION: Record<string, string> = {
  '/agents': 'agents',
  '/workflows': 'workflows',
  '/knowledge-bases': 'knowledgeBases',
  '/tools': 'tools',
  '/mcp-servers': 'mcpServers',
  '/billing/invoices': 'billingInvoices',
  '/model-providers': 'providers',
  '/models': 'models',
  '/model-api-keys': 'modelApiKeys',
  '/conversations': 'conversations',
  '/projects': 'projects',
  '/api-keys': 'apiKeys',
  '/roles': 'roles',
  '/executions': 'executions',
  '/notifications': 'notifications',
  '/market/plugins': 'marketPlugins',
  '/market/workspace-plugins': 'marketPlugins',
  '/market/templates': 'agentTemplates',
  '/workspaces': 'workspaces',
  '/tenants': 'tenants',
  '/plans': 'plans',
  '/agent-templates': 'agentTemplates',
  '/plugins': 'adminPlugins',
  '/plugin-categories': 'pluginCategories',
  '/ops/placements': 'opsPlacements',
  '/message-feedbacks': 'messageFeedbacks',
  '/platform/providers': 'platformProviders',
  '/platform/models': 'platformModels',
}

function getCollection(ctx: GenericRestContext, name: string): Collection {
  if (!ctx.collections[name]) {
    ctx.collections[name] = []
  }
  return ctx.collections[name]
}

export function tryGenericRest(
  method: string,
  path: string,
  body: unknown,
  ctx: GenericRestContext,
): ApiResult<unknown> | null {
  const m = method.toUpperCase()

  for (const [prefix, collectionName] of Object.entries(PATH_TO_COLLECTION)) {
    if (path === prefix) {
      const list = getCollection(ctx, collectionName)
      if (m === 'GET') return ok(clone(list))
      if (m === 'POST') {
        const row = {
          id: nextId(collectionName),
          ...(typeof body === 'object' && body ? body : {}),
          createdAt: nowIso(),
          updatedAt: nowIso(),
          status: (body as { status?: string })?.status ?? 'ACTIVE',
        }
        list.unshift(row)
        return ok(clone(row))
      }
    }

    const detail = path.match(new RegExp(`^${prefix.replace(/\//g, '\\/')}/([^/]+)$`))
    if (detail) {
      const id = detail[1]
      const list = getCollection(ctx, collectionName)
      const index = list.findIndex((row) => String(row.id) === id)
      if (m === 'GET') {
        const item = list[index]
        return item ? ok(clone(item)) : fail('资源不存在', 404)
      }
      if (m === 'PUT' || m === 'PATCH') {
        if (index < 0) return fail('资源不存在', 404)
        list[index] = { ...list[index], ...(body as object), updatedAt: nowIso() }
        return ok(clone(list[index]))
      }
      if (m === 'DELETE') {
        if (index >= 0) list.splice(index, 1)
        return ok(null)
      }
    }
  }

  return null
}
