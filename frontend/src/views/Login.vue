<template>
  <div class="login-page">
    <section class="intro-panel">
      <p class="eyebrow">AI Drama Production Cloud</p>
      <h1 class="hud-title brand-title">漫织云</h1>
      <p class="brand-subtitle">AI漫剧生产平台</p>
      <p class="brand-copy">把小说、分镜、角色资产和视频生成收束到一个影视级控制舱，面向高频生产而不是普通后台。</p>
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
    await authStore.fetchUserInfo().catch(() => undefined)
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
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) 430px;
  gap: 56px;
  align-items: center;
  padding: 0 8vw;
  background:
    linear-gradient(90deg, rgba(3, 7, 13, 0.38), rgba(3, 7, 13, 0.08) 42%, rgba(3, 7, 13, 0.58) 72%, rgba(3, 7, 13, 0.9)),
    radial-gradient(circle at 19% 48%, rgba(92, 241, 255, 0.18), transparent 28%),
    url('/assets/images/bj.png') center / cover no-repeat,
    $bg-page;

  &::before,
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    z-index: -1;
  }

  &::before {
    background:
      linear-gradient(rgba(92, 241, 255, 0.045) 1px, transparent 1px),
      linear-gradient(90deg, rgba(92, 241, 255, 0.035) 1px, transparent 1px);
    background-size: 52px 52px;
    mask-image: linear-gradient(90deg, black, transparent 68%);
  }

  &::after {
    background:
      linear-gradient(180deg, rgba(3, 7, 13, 0.2), transparent 18%, rgba(3, 7, 13, 0.34)),
      repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.024) 0, rgba(255, 255, 255, 0.024) 1px, transparent 1px, transparent 6px);
    mix-blend-mode: screen;
    opacity: 0.55;
  }
}

.intro-panel {
  color: $text-primary;
  max-width: 680px;
  transform: translateY(-3vh);

  .eyebrow {
    color: $accent-yellow;
    font-size: 12px;
  }

  .brand-title {
    position: relative;
    margin: 12px 0 8px;
    max-width: 760px;
    font-size: clamp(72px, 9vw, 132px);
    line-height: 1.02;
    letter-spacing: 0.08em;
    color: #f6fdff;
    text-shadow:
      0 0 18px rgba(92, 241, 255, 0.38),
      0 0 54px rgba(92, 241, 255, 0.25),
      0 18px 42px rgba(0, 0, 0, 0.6);
    animation: brand-rise 0.95s cubic-bezier(0.2, 0.9, 0.22, 1) both, brand-glow 4.8s ease-in-out 1s infinite;

    &::after {
      content: '';
      position: absolute;
      left: 0.08em;
      right: 0.18em;
      bottom: 0.04em;
      height: 2px;
      background: linear-gradient(90deg, transparent, rgba(92, 241, 255, 0.86), rgba(125, 255, 178, 0.72), transparent);
      box-shadow: 0 0 18px rgba(92, 241, 255, 0.42);
      transform-origin: left;
      animation: signal-sweep 2.8s ease-in-out 0.9s infinite;
    }
  }

  .brand-subtitle {
    margin: 0;
    max-width: 560px;
    color: rgba(229, 247, 255, 0.82);
    font-family: $font-display;
    font-size: clamp(20px, 2.2vw, 32px);
    font-weight: 700;
    letter-spacing: 0.2em;
    line-height: 1.4;
    animation: brand-rise 0.95s cubic-bezier(0.2, 0.9, 0.22, 1) 0.12s both;
  }

  .brand-copy {
    max-width: 620px;
    margin: 18px 0 0;
    color: rgba(214, 232, 241, 0.76);
    font-size: 17px;
    line-height: 1.8;
    text-shadow: 0 10px 28px rgba(0, 0, 0, 0.52);
    animation: brand-rise 0.95s cubic-bezier(0.2, 0.9, 0.22, 1) 0.24s both;
  }
}

.mission-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
  animation: brand-rise 0.95s cubic-bezier(0.2, 0.9, 0.22, 1) 0.36s both;
}

@keyframes brand-rise {
  from {
    opacity: 0;
    transform: translateY(18px);
    filter: blur(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@keyframes brand-glow {
  0%, 100% {
    text-shadow:
      0 0 18px rgba(92, 241, 255, 0.34),
      0 0 54px rgba(92, 241, 255, 0.2),
      0 18px 42px rgba(0, 0, 0, 0.6);
  }
  50% {
    text-shadow:
      0 0 24px rgba(92, 241, 255, 0.52),
      0 0 72px rgba(125, 255, 178, 0.24),
      0 18px 42px rgba(0, 0, 0, 0.6);
  }
}

@keyframes signal-sweep {
  0%, 18% {
    transform: scaleX(0);
    opacity: 0;
  }
  42% {
    opacity: 1;
  }
  76%, 100% {
    transform: scaleX(1);
    opacity: 0;
  }
}

.login-card {
  justify-self: end;
  padding: 10px;
  background:
    linear-gradient(145deg, rgba(92, 241, 255, 0.09), transparent 32%),
    linear-gradient(180deg, rgba(4, 16, 27, 0.76), rgba(3, 7, 13, 0.88)) !important;
  box-shadow:
    0 0 46px rgba(92, 241, 255, 0.14),
    0 32px 90px rgba(0, 0, 0, 0.48);

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
    background-position: 42% center;
  }

  .intro-panel {
    transform: none;
  }

  .intro-panel .brand-title {
    font-size: 54px;
  }

  .login-card {
    justify-self: stretch;
  }
}
</style>
