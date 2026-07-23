import './assets/main.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import axios from 'axios'

import App from './App.vue'
import router from './router'

// 配置axios默认设置
axios.defaults.withCredentials = true  // 支持跨域发送凭证
axios.defaults.timeout = 10000         // 设置超时时间
axios.interceptors.request.use((config) => {
  const url = config.url || ''
  if (url.includes('/api/v1/admin')) {
    const token = sessionStorage.getItem('adminToken')
    if (token) {
      config.headers = config.headers || {}
      config.headers['X-Admin-Token'] = token
    }
  } else if (url.includes('/api/v1/employee') || url.includes('/api/v1/ai')) {
    const token = sessionStorage.getItem('employeeToken')
    if (token) {
      config.headers = config.headers || {}
      config.headers['X-Emp-Token'] = token
    }
  }
  return config
})

const app = createApp(App)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 全局注册axios
app.config.globalProperties.$axios = axios

app.mount('#app')
