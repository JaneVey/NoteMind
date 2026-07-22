import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
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
      ],
    },
  ],
})

export default router
