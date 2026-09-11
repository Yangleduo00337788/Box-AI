import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        { path: '', redirect: '/tenants' },
        {
          path: 'tenants',
          component: () => import('@/views/TenantsView.vue'),
          meta: { title: '租户管理' },
        },
        {
          path: 'plans',
          component: () => import('@/views/PlansView.vue'),
          meta: { title: '套餐管理' },
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
          path: 'system-config',
          component: () => import('@/views/SystemConfigView.vue'),
          meta: { title: '系统配置' },
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
  return true
})

export default router
