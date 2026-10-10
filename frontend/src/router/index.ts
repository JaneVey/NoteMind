import { createRouter, createWebHistory } from 'vue-router'
import { useAuthDialog } from '@/composables/useAuthDialog'
import type { AuthTab } from '@/composables/useAuthDialog'

/**
 * 认证入口：打开弹框，然后回到首页。
 *
 * <h3>为什么删掉了独立的登录/注册页面</h3>
 *
 * 游客模式下，需要登录的**动作**会自己弹出认证框（见 `composables/useAuthGuard.ts`），
 * 用户菜单里的"登录 / 注册"也走同一个弹框。也就是说**弹框已覆盖全部场景**，
 * 再维护一套独立页面就是重复的界面面 —— 而重复的界面面必然走偏。
 *
 * <h3>那为什么还保留 /login 与 /register 路由</h3>
 *
 * 因为它们作为 **URL** 仍然有价值：
 * <ul>
 *   <li>可收藏、可分享（老的链接不会 404）</li>
 *   <li>将来 GitHub OAuth 的回调、以及找回密码的邮件链接需要一个落地地址</li>
 * </ul>
 * 所以让它们"打开弹框 + 回到首页"——**URL 依然有效，但只有一套认证界面**。
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
