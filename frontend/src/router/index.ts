import { createRouter, createWebHistory } from 'vue-router'
import { useAuthDialog } from '@/composables/useAuthDialog'
import type { AuthTab } from '@/composables/useAuthDialog'

/**
 * 认证入口：打开弹框，然后回到目标页（`redirect` 查询参数）。
 *
 * <p>没有独立的登录/注册页面 —— 弹框已覆盖全部场景（动作拦截、用户菜单、注册）。
 * /login 与 /register 仍然保留，因为它们作为 URL 有价值：可收藏可分享、老的链接不会 404，
 * 将来 GitHub OAuth 回调与找回密码邮件也需要落地地址。
 *
 * @param tab 打开弹框时显示哪个标签
 */
function authEntry(tab: AuthTab) {
  return (to: { query: Record<string, unknown> }) => {
    void useAuthDialog().open({ tab })

    // 保留 redirect 语义：只接受站内路径（排除 //evil.com 这类协议相对 URL）
    const target = to.query.redirect
    const isSafe = typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')
    return { path: isSafe ? (target as string) : '/' }
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      redirect: authEntry('login'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'Register',
      redirect: authEntry('register'),
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
