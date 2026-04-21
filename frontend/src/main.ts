// main.ts — 应用入口文件
// 挂载 Vue 应用，引入 Router、Pinia、Element Plus（含中文语言包）、全局样式

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn' // Element Plus 中文语言包
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css' // Element Plus 暗色主题 CSS 变量

import App from './App.vue'
import router from './router'
import './styles/index.scss' // 全局样式（变量 → 重置 → 暗色主题）

const app = createApp(App)

// 注册 Pinia 状态管理
app.use(createPinia())
// 注册 Vue Router 路由
app.use(router)
// 注册 Element Plus 组件库（使用中文语言包）
app.use(ElementPlus, { locale: zhCn })

// 全局异常捕获（系分第 12 节）
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue error:', err, info)
  // TODO: 接入 Sentry 或类似错误追踪
}

// 挂载到 #app 节点
app.mount('#app')
