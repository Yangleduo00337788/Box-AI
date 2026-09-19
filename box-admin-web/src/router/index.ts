import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canAccessAdminRoute } from '@/constants/rbac'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/forgot-password',
      component: () => import('@/views/ForgotPasswordView.vue'),
      meta: { public: true, title: '忘记密码' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'analytics',
          component: () => import('@/views/AnalyticsView.vue'),
          meta: { title: '平台分析' },
        },
        {
          path: 'tenants',
          component: () => import('@/views/TenantsView.vue'),
          meta: { title: '租户管理' },
        },
        {
          path: 'users',
          component: () => import('@/views/UsersView.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'plans',
          component: () => import('@/views/PlansView.vue'),
          meta: { title: '套餐管理' },
        },
        {
          path: 'billing-invoices',
          component: () => import('@/views/BillingInvoicesView.vue'),
          meta: { title: '账单对账' },
        },
        {
          path: 'ops-placements',
          component: () => import('@/views/OpsPlacementsView.vue'),
          meta: { title: '运营位' },
        },
        {
          path: 'platform-models',
          component: () => import('@/views/PlatformModelsView.vue'),
          meta: { title: '平台模型池' },
        },
        {
          path: 'agent-templates',
          component: () => import('@/views/AgentTemplatesView.vue'),
          meta: { title: '智能体市场' },
        },
        {
          path: 'plugin-catalog',
          component: () => import('@/views/PluginCatalogView.vue'),
          meta: { title: '插件市场' },
        },
        {
          path: 'platform-tools',
          component: () => import('@/views/PlatformCatalogView.vue'),
          meta: { title: '官方工具', catalogCategory: 'tools' },
        },
        {
          path: 'platform-mcp',
          component: () => import('@/views/PlatformCatalogView.vue'),
          meta: { title: '官方 MCP', catalogCategory: 'mcp' },
        },
        {
          path: 'message-feedbacks',
          component: () => import('@/views/MessageFeedbacksView.vue'),
          meta: { title: '消息反馈' },
        },
        {
          path: 'audit-logs',
          component: () => import('@/views/AuditLogsView.vue'),
          meta: { title: '审计日志' },
        },
        {
          path: 'system-config',
          component: () => import('@/views/SystemConfigView.vue'),
          meta: { title: '系统配置' },
        },
        {
          path: 'platform-ocr',
          component: () => import('@/views/PlatformOcrSettingsView.vue'),
          meta: { title: 'OCR 默认模型' },
        },
        {
          path: 'forbidden',
          component: () => import('@/views/ForbiddenView.vue'),
          meta: { title: '无权限' },
        },
        {
          path: ':pathMatch(.*)*',
          component: () => import('@/views/NotFoundView.vue'),
          meta: { title: '页面不存在' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
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
  if (to.path !== '/forbidden' && !canAccessAdminRoute(auth.platformRole, to.path)) {
    return '/forbidden'
  }
  return true
})

export default router
