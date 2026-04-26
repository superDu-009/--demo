<template>
  <div class="login-page">
    <section class="intro-panel">
      <p class="eyebrow">LanYan Studio / Neural Drama Foundry</p>
      <h1 class="hud-title">AI漫剧生产平台</h1>
      <p>把小说、分镜、角色资产和视频生成收束到一个影视级控制舱，面向高频生产而不是普通后台。</p>
      <div class="mission-grid">
        <span class="data-chip">Script Parse</span>
        <span class="data-chip">Shot Engine</span>
        <span class="data-chip">Asset Matrix</span>
      </div>
    </section>

    <el-card class="login-card card-glass border-neon hud-panel hud-corner scanline">
      <p class="panel-code">ACCESS NODE 01</p>
      <h2 class="hud-title">登录控制台</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button class="btn-gradient login-btn" :loading="loading" @click="handleLogin">进入生产矩阵</el-button>
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
  grid-template-columns: minmax(0, 1.28fr) 430px;
  gap: 56px;
  align-items: center;
  padding: 0 8vw;
  background:
    linear-gradient(115deg, rgba(92, 241, 255, 0.12), transparent 34%),
    radial-gradient(circle at 20% 20%, rgba(92, 241, 255, 0.22), transparent 28%),
    radial-gradient(circle at 80% 80%, rgba(255, 204, 102, 0.18), transparent 20%),
    $bg-page;
}

.intro-panel {
  color: $text-primary;

  .eyebrow {
    color: $accent-yellow;
    font-size: 12px;
  }

  h1 {
    margin: 14px 0;
    max-width: 760px;
    font-size: clamp(46px, 6vw, 84px);
    line-height: 0.95;
    text-shadow: 0 0 38px rgba(92, 241, 255, 0.28);
  }

  p {
    max-width: 560px;
    color: $text-secondary;
    font-size: 18px;
    line-height: 1.7;
  }
}

.mission-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
}

.login-card {
  padding: 10px;

  h2 {
    margin: 0 0 18px;
    color: $text-primary;
    font-size: 30px;
  }
}

.panel-code {
  margin: 0 0 8px;
  color: $accent-green;
  font-family: $font-display;
  font-size: 12px;
  letter-spacing: 0.22em;
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
