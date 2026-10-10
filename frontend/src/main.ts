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
 * <p>为什么用注入而不是让 api 层直接 import store —— 那会形成循环引用：
 * <pre>
 *   api/request.ts → api/authRefresh.ts → stores/authStore.ts
 *                  → api/auth.ts → api/request.ts        ✗ 成环
 * </pre>
 * 把回调从这里注入，依赖方向就保持单向：
 * <pre>
 *   utils/authToken.ts  ←  api 层（只读 token、只发通知）
 *                       ←  main.ts（注入处理函数）
 *                       ←  stores/authStore.ts
 * </pre>
 *
 * <p>触发时机：某次请求收到 401、且用 refresh token 也换不回新凭据
 * —— 说明会话真的结束了（refresh 过期 / 被吊销 / 因检测到重用而全量登出）。
 * 注意**启动时的静默刷新失败不会触发它**：那只是"当前是游客"，不该提示。
 */
const authStore = useAuthStore()
onSessionExpired(() => authStore.markSessionExpired())

// 注意：不在挂载前 await bootstrap（见 App.vue）——
// 那样会让首屏被一次网络请求阻塞。App.vue 用一个极简的启动占位解决闪烁问题。
app.mount('#app')
