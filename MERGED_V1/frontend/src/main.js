import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router/index.js'
import { initializeClientMonitoring } from './observability/clientMonitor.js'
import { useAuth } from './stores/auth.js'
import './styles/base.css'

const app = createApp(App)
app.use(router)
app.use(ElementPlus)
initializeClientMonitoring({ app, router, auth: useAuth() })
app.mount('#app')
