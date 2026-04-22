<!-- views/Login.vue — 登录页
     系分第 4.1 节：居中卡片暗色背景，用户名/密码表单 -->
<template>
  <div class="login-page">
    <!-- 左侧背景图区域 -->
    <div class="login-bg">
      <div class="bg-overlay"></div>
      <div class="bg-text">
        <h1 class="text-neon">AI漫剧生产平台</h1>
        <p>让AI成为你的创作助手，一键生成高质量漫画分镜</p>
      </div>
    </div>

    <!-- 右侧登录卡片区域 -->
    <div class="login-content">
      <el-card class="login-card card-glass border-neon">
        <h2 class="login-title text-neon">欢迎回来</h2>
        <p class="login-subtitle">登录以继续创作</p>

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
              class="login-btn btn-gradient"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
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
      // 安全校验：仅允许站内相对路径跳转，防止开放重定向漏洞
      const isInternalPath = /^\/[^/\\]/g.test(redirect) && !/^(http|\/\/)/i.test(redirect)
      router.push(isInternalPath ? redirect : '/projects')
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
  height: 100vh;
  overflow: hidden;
}

// 左侧背景区域
.login-bg {
  flex: 1;
  position: relative;
  background: url('/assets/images/login-bg.png') center/cover no-repeat;
  
  // 渐变遮罩，过渡到右侧深色
  .bg-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(to right, rgba(10, 10, 18, 0.1), rgba(10, 10, 18, 0.9));
  }

  // 背景文字
  .bg-text {
    position: absolute;
    left: 80px;
    top: 50%;
    transform: translateY(-50%);
    color: $text-primary;
    max-width: 400px;

    h1 {
      font-size: 48px;
      font-weight: 700;
      margin-bottom: 16px;
      text-shadow: 0 0 20px rgba(100, 108, 255, 0.6);
      animation: float 6s ease-in-out infinite;
    }

    p {
      font-size: 18px;
      color: $text-secondary;
      line-height: 1.6;
      opacity: 0.9;
    }
  }
}

// 右侧登录区域
.login-content {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 100%;
  animation: slideUp 0.8s ease-out;
}

.login-title {
  text-align: center;
  font-size: 28px;
  margin-bottom: 8px;
  font-weight: 600;
}

.login-subtitle {
  text-align: center;
  color: $text-secondary;
  margin-bottom: 32px;
  font-size: 14px;
}

.login-btn {
  width: 100%;
  min-width: $btn-min-width;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}

// 动画
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// Element Plus 卡片样式适配
:deep(.el-card__body) {
  padding: 40px 32px;
}

:deep(.el-form-item__label) {
  color: $text-primary;
  font-weight: 500;
  margin-bottom: 8px;
}

:deep(.el-input__wrapper) {
  height: 44px;
}
</style>
