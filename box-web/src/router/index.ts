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
      path: '/register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true, title: '注册' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        { path: '', redirect: '/chat' },
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
        { path: 'agents', component: () => import('@/views/AgentsView.vue'), meta: { title: '智能体' } },
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
        { path: 'team', component: () => import('@/views/TeamView.vue'), meta: { title: '团队' } },
        { path: 'market', component: () => import('@/views/MarketView.vue'), meta: { title: '市场' } },
        { path: 'analytics', component: () => import('@/views/AnalyticsView.vue'), meta: { title: '分析' } },
        {
          path: 'settings',
          component: () => import('@/layouts/SettingsLayout.vue'),
          children: [
            { path: '', redirect: '/settings/profile' },
            { path: 'profile', component: () => import('@/views/settings/SettingsProfileView.vue'), meta: { title: '个人信息' } },
            { path: 'appearance', component: () => import('@/views/settings/SettingsAppearanceView.vue'), meta: { title: '外观设置' } },
            { path: 'general', component: () => import('@/views/settings/SettingsGeneralView.vue'), meta: { title: '通用设置' } },
            { path: 'security', component: () => import('@/views/settings/SettingsSecurityView.vue'), meta: { title: '账号与安全' } },
            { path: 'quota', component: () => import('@/views/settings/SettingsQuotaView.vue'), meta: { title: '额度管理' } },
            { path: 'capacity', component: () => import('@/views/settings/SettingsCapacityView.vue'), meta: { title: '容量管理' } },
            { path: 'about', component: () => import('@/views/settings/SettingsAboutView.vue'), meta: { title: '关于盒子' } },
            { path: 'legal', component: () => import('@/views/settings/SettingsLegalView.vue'), meta: { title: '隐私与协议' } },
          ],
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
