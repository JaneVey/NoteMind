import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/AuthPage.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/AuthPage.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/',
      component: () => import('@/views/MainLayout.vue'),
      redirect: '/chat',
      children: [
        {
          path: 'chat',
          name: 'AIChat',
          component: () => import('@/views/AIChat.vue'),
          meta: { title: 'AI 对话' },
        },
        {
          path: 'notebook',
          name: 'Notebook',
          component: () => import('@/views/Notebook.vue'),
          meta: { title: '笔记' },
        },
        {
          path: 'knowledge',
          name: 'KnowledgeBase',
          component: () => import('@/views/KnowledgeBase.vue'),
          meta: { title: '知识库' },
        },
        {
          path: 'knowledge/:id',
          name: 'KnowledgeDetail',
          component: () => import('@/views/KnowledgeDetail.vue'),
          meta: { title: '文档详情' },
        },
        {
          path: 'search',
          name: 'Search',
          component: () => import('@/views/Search.vue'),
          meta: { title: '搜索' },
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/Settings.vue'),
          meta: { title: '设置' },
        },
        {
          path: 'graph',
          name: 'Graph',
          component: () => import('@/views/Graph.vue'),
          meta: { title: '知识图谱' },
        },
        {
          // 开发阶段的技术验证页（AI 流式输出 + 思考链），验证完成后可删除
          path: 'spike/ai-stream',
          name: 'AiStreamSpike',
          component: () => import('@/views/AiStreamSpike.vue'),
          meta: { title: 'AI 流式验证' },
        },
      ],
    },
  ],
})

export default router
