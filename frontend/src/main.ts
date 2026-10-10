import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import setupRouter from './router/guard'
import { useAuthStore } from '@/stores/authStore'
import { onSessionExpired } from '@/utils/authToken'

const app = createApp(App)

app.use(createPinia())
app.use(router)
setupRouter(router)

/**
 * 依赖注入：把「会话失效」的处理交给 store。
 *
 * 不让 api 层直接 import store，那会成环：
 * api/request.ts → api/authRefresh.ts → stores/authStore.ts → api/auth.ts → api/request.ts。
 * 回调从入口注入后，依赖方向保持单向：utils/authToken.ts ← api 层（只读 token、只发通知）
 * ← main.ts（注入处理函数）← stores/authStore.ts。
 *
 * 触发条件是「收到 401 且 refresh 也换不回凭据」，即会话真的结束（refresh 过期 / 被吊销 /
 * 因检测到重用而全量登出）。**启动时的静默刷新失败不触发它** —— 那只是当前是游客，不该提示。
 */
const authStore = useAuthStore()
onSessionExpired(() => authStore.markSessionExpired())

// 不在挂载前 await bootstrap（见 App.vue），否则首屏被一次网络请求阻塞
app.mount('#app')
