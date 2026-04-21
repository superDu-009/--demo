<!-- views/Login.vue — 登录页
     系分第 4.1 节：居中卡片暗色背景，用户名/密码表单 -->
<template>
  <div class="login-page">
    <!-- 登录卡片 -->
    <el-card class="login-card">
      <h2 class="login-title">AI漫剧生产平台</h2>
      <p class="login-subtitle">登录以继续</p>

      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <!-- 用户名输入 -->
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
          />
        </el-form-item>

        <!-- 密码输入 -->
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/user'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

// 表单数据
const form = reactive({
  username: '',
  password: ''
})

// 表单校验规则（系分 4.1：用户名必填，密码必填且最少6字符）
const rules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码最少 6 个字符', trigger: 'blur' }
  ]
})

// 处理登录（系分 4.1 关键逻辑）
const handleLogin = async () => {
  // 表单校验
  const valid = await formRef.value!.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // 调用登录接口
    const res = await userApi.login(form)
    // 存储 Token 到 Pinia auth store（同时持久化到 localStorage）
    authStore.setToken(res.data.token)
    // 存储用户信息
    authStore.setUserInfo({
      id: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname
    })
    ElMessage.success('登录成功')
    // 登录后跳转：优先使用 redirect 参数，否则跳转项目列表
    const redirect = (route.query.redirect as string) || '/projects'
    router.push(redirect)
  } catch {
    // 拦截器已处理错误提示，此处无需额外处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: $bg-page;
}

.login-card {
  width: 400px;
  background-color: $bg-card;
  border-color: $border-color;
}

.login-title {
  text-align: center;
  font-size: 24px;
  color: $primary-color;
  margin-bottom: 8px;
}

.login-subtitle {
  text-align: center;
  color: $text-secondary;
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  min-width: $btn-min-width;
}

// Element Plus 卡片样式适配
:deep(.el-card__body) {
  padding: 32px;
}

:deep(.el-form-item__label) {
  color: $text-primary;
}
</style>
