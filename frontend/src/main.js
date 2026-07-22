import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import setupGuard from './router/guard'
import { useAuthStore } from '@/stores/authStore'

const app = createApp(App)
app.use(createPinia())
app.use(router)
setupGuard(router)

const authStore = useAuthStore()
authStore.initFromStorage()

app.mount('#app')
