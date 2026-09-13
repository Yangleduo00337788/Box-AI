import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'

const APP_HOSTS = new Set(['localhost', '127.0.0.1'])

async function resolveEmbedAgentId(host: string): Promise<number | null> {
  try {
    const response = await fetch(`/api/v1/published/embed/resolve?host=${encodeURIComponent(host)}`)
    const payload = await response.json()
    if (payload?.code === 0 && payload.data?.agentId) {
      return Number(payload.data.agentId)
    }
  } catch {
    // ignore
  }
  return null
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true, title: '注册' },
    },
    {
      path: '/forgot-password',
      component: () => import('@/views/ForgotPasswordView.vue'),
      meta: { public: true, title: '忘记密码' },
    },
    {
      path: '/embed/agents/:id',
      component: () => import('@/views/EmbedAgentView.vue'),
      meta: { public: true, title: '智能体对话' },
    },
    {
      path: '/embed',
      component: () => import('@/views/EmbedAgentView.vue'),
      meta: { public: true, title: '智能体对话' },
    },
    {
      path: '/legal/:doc',
      component: () => import('@/views/LegalDocumentView.vue'),
      meta: { public: true, title: '法律条款' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        { path: '', redirect: '/chat' },
        {
          path: 'forbidden',
          component: () => import('@/views/ForbiddenView.vue'),
          meta: { title: '无权限' },
        },
        {
          path: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '概览' },
        },
        { path: 'conversations', redirect: '/chat' },
        { path: 'chat/logs', redirect: '/chat' },
        {
          path: 'chat/:id',
          component: () => import('@/views/ChatView.vue'),
          meta: { title: '对话' },
        },
        {
          path: 'chat',
          component: () => import('@/views/ChatView.vue'),
          meta: { title: '新任务' },
        },
        { path: 'agents', redirect: '/chat' },
        { path: 'agents/:id/builder', name: 'agent-builder', component: () => import('@/views/AgentBuilderView.vue'), meta: { title: 'Agent Builder' } },
        { path: 'workflows', component: () => import('@/views/WorkflowsView.vue'), meta: { title: '工作流' } },
        {
          path: 'workflows/:id/editor',
          name: 'workflow-editor',
          component: () => import('@/views/WorkflowEditorView.vue'),
          meta: { title: '工作流编辑器' },
        },
        { path: 'knowledge', component: () => import('@/views/KnowledgeView.vue'), meta: { title: '知识库' } },
        { path: 'tools', component: () => import('@/views/ToolsView.vue'), meta: { title: '工具' } },
        { path: 'mcp', component: () => import('@/views/McpView.vue'), meta: { title: 'MCP' } },
        { path: 'plugin-market', component: () => import('@/views/PluginMarketView.vue'), meta: { title: '插件市场' } },
        { path: 'models', component: () => import('@/views/ModelsView.vue'), meta: { title: '模型' } },
        { path: 'team', component: () => import('@/views/TeamView.vue'), meta: { title: '团队', permission: 'member:manage' } },
        { path: 'market', component: () => import('@/views/MarketView.vue'), meta: { title: '市场' } },
        { path: 'analytics', component: () => import('@/views/AnalyticsView.vue'), meta: { title: '分析' } },
        { path: 'executions', component: () => import('@/views/ExecutionsView.vue'), meta: { title: '执行记录' } },
        { path: 'debug', component: () => import('@/views/DebugConsoleView.vue'), meta: { title: 'Debug Console' } },
        {
          path: 'settings',
          component: () => import('@/layouts/SettingsLayout.vue'),
          children: [
            { path: '', redirect: '/settings/profile' },
            { path: 'profile', component: () => import('@/views/settings/SettingsProfileView.vue'), meta: { title: '个人信息' } },
            { path: 'appearance', component: () => import('@/views/settings/SettingsAppearanceView.vue'), meta: { title: '外观设置' } },
            { path: 'general', component: () => import('@/views/settings/SettingsGeneralView.vue'), meta: { title: '通用设置' } },
            { path: 'security', component: () => import('@/views/settings/SettingsSecurityView.vue'), meta: { title: '账号与安全' } },
            { path: 'api-keys', component: () => import('@/views/settings/SettingsApiKeysView.vue'), meta: { title: 'API 密钥', permission: 'api_key:manage' } },
            { path: 'roles', component: () => import('@/views/settings/SettingsRolesView.vue'), meta: { title: '角色与权限', permission: 'role:manage' } },
            { path: 'audit-logs', component: () => import('@/views/settings/SettingsAuditLogsView.vue'), meta: { title: '审计日志', permission: 'audit:read' } },
            { path: 'quota', component: () => import('@/views/settings/SettingsQuotaView.vue'), meta: { title: '额度管理' } },
            { path: 'billing', component: () => import('@/views/settings/SettingsBillingView.vue'), meta: { title: '账单概览' } },
            { path: 'capacity', component: () => import('@/views/settings/SettingsCapacityView.vue'), meta: { title: '容量管理' } },
            { path: 'about', component: () => import('@/views/settings/SettingsAboutView.vue'), meta: { title: '关于盒子' } },
            { path: 'legal', redirect: '/legal/privacy' },
          ],
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const host = window.location.hostname
  if ((to.path === '/' || to.path === '/chat') && !APP_HOSTS.has(host)) {
    const agentId = await resolveEmbedAgentId(host)
    if (agentId) {
      return { path: '/embed', query: to.query }
    }
  }
  const auth = useAuthStore()
  if (to.meta.public) {
    return true
  }
  if (!auth.token) {
    return '/login'
  }
  if (!auth.user) {
    try {
      await auth.hydrate()
    } catch {
      auth.logout()
      return '/login'
    }
  }
  const permissionStore = usePermissionStore()
  if (!permissionStore.loaded) {
    try {
      await permissionStore.load()
    } catch {
      // 权限加载失败时不阻断页面，按钮级守卫仍可用
    }
  }
  const required = to.meta.permission
  if (typeof required === 'string' && !permissionStore.can(required)) {
    return '/forbidden'
  }
  return true
})

export default router
