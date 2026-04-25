<template>
  <div class="login-page">
    <section class="intro-panel">
      <p class="eyebrow">LanYan Studio</p>
      <h1>AI漫剧生产平台</h1>
      <p>按最新 PRD 只保留三段主流程：剧本预览、分镜工作台、资产库。</p>
    </section>

    <el-card class="login-card card-glass border-neon">
      <h2>登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button class="btn-gradient login-btn" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await userApi.login(form)
    authStore.setToken(res.data.token)
    authStore.setUserInfo({
      id: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      status: 1,
      avatar: ''
    })
    ElMessage.success(`欢迎你，${res.data.nickname || res.data.username}~`)
    const redirect = (route.query.redirect as string) || '/projects'
    router.push(redirect.startsWith('/') ? redirect : '/projects')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.2fr 420px;
  gap: 48px;
  align-items: center;
  padding: 0 8vw;
  background:
    radial-gradient(circle at 20% 20%, rgba(100, 108, 255, 0.26), transparent 28%),
    radial-gradient(circle at 80% 80%, rgba(16, 185, 129, 0.18), transparent 20%),
    $bg-page;
}

.intro-panel {
  color: $text-primary;

  .eyebrow {
    color: $accent-green;
    text-transform: uppercase;
    letter-spacing: 0.18em;
    font-size: 12px;
  }

  h1 {
    margin: 14px 0;
    font-size: 56px;
    line-height: 1.08;
  }

  p {
    max-width: 560px;
    color: $text-secondary;
    font-size: 18px;
    line-height: 1.7;
  }
}

.login-card {
  padding: 8px;

  h2 {
    margin-top: 0;
    color: $text-primary;
  }
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 48px 20px;
  }

  .intro-panel h1 {
    font-size: 38px;
  }
}
</style>
