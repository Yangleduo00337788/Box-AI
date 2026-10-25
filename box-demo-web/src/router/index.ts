import { createRouter, createWebHistory } from 'vue-router'
import DemoShell from '@/layouts/DemoShell.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: () => import('@/views/LandingView.vue'),
      meta: { title: 'Box AI 产品演示' },
    },
    {
      path: '/demo',
      component: DemoShell,
      redirect: '/demo/web/chat',
      children: [
        {
          path: 'web/chat',
          name: 'demo-chat',
          component: () => import('@/views/demo/consumer/ChatDemo.vue'),
        },
        {
          path: 'web/agent-builder',
          name: 'demo-agent',
          component: () => import('@/views/demo/consumer/AgentBuilderDemo.vue'),
        },
        {
          path: 'web/workflow',
          name: 'demo-workflow',
          component: () => import('@/views/demo/consumer/WorkflowDemo.vue'),
        },
        {
          path: 'web/knowledge',
          name: 'demo-knowledge',
          component: () => import('@/views/demo/consumer/KnowledgeDemo.vue'),
        },
        {
          path: 'web/plugin-market',
          name: 'demo-plugin-market',
          component: () => import('@/views/demo/consumer/PluginMarketDemo.vue'),
        },
        {
          path: 'web/settings',
          name: 'demo-settings',
          component: () => import('@/views/demo/consumer/SettingsDemo.vue'),
        },
        {
          path: 'web/:section',
          name: 'demo-web-fallback',
          component: () => import('@/views/demo/FeatureFallback.vue'),
          props: (route) => ({ realm: 'web', section: route.params.section }),
        },
        {
          path: 'admin/:section',
          name: 'demo-admin-fallback',
          component: () => import('@/views/demo/FeatureFallback.vue'),
          props: (route) => ({ realm: 'admin', section: route.params.section }),
        },
      ],
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.afterEach((to) => {
  const base = 'Box AI 演示'
  document.title = to.meta.title ? `${to.meta.title} · ${base}` : base
})

export default router
