<template>
  <el-container class="app-layout">
    <el-aside class="sidebar" width="220px">
      <div class="sidebar-logo scanline" @click="router.push({ name: 'ProjectList' })">
        <div class="logo-mark">
          <el-icon :size="30"><VideoPlay /></el-icon>
        </div>
        <div>
          <span>LanYan OS</span>
          <small>AI DRAMA COMMAND</small>
        </div>
      </div>

      <div class="sidebar-menu">
        <div
          v-for="item in menuList"
          :key="item.name"
          class="menu-item"
          :class="{ active: route.name === item.name, disabled: item.requiresProject && !route.params.id }"
          @click="handleMenuClick(item)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </div>
      </div>
    </el-aside>

    <el-container class="main-container">
      <el-header class="app-header">
        <div>
          <p class="hud-kicker page-kicker">Production Matrix</p>
          <h1 class="page-title hud-title text-neon">{{ pageTitle }}</h1>
          <p class="page-subtitle">{{ pageSubtitle }}</p>
        </div>

        <el-dropdown trigger="click">
          <div class="user-entry">
            <el-avatar :size="38" :src="authStore.userInfo?.avatar || undefined">
              {{ authStore.displayName.charAt(0) }}
            </el-avatar>
            <div>
              <strong>{{ authStore.displayName }}</strong>
              <span>Operator Online</span>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="profileDialogVisible = true">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="profileDialogVisible" title="个人中心" width="520px" destroy-on-close>
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="84px">
        <el-form-item label="头像">
          <div class="avatar-field">
            <el-avatar :size="64" :src="profileForm.avatar || undefined">
              {{ (profileForm.username || authStore.displayName).charAt(0) }}
            </el-avatar>
            <TosUpload
              v-model="profileForm.avatar"
              :project-id="0"
              file-type="other"
              button-text="上传头像"
              tip-text="支持 png/jpg/webp"
              accept=".png,.jpg,.jpeg,.webp"
              :allowed-types="['image/png', 'image/jpeg', 'image/webp']"
              :max-file-size="5 * 1024 * 1024"
              :show-preview="false"
            />
          </div>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="profileForm.username" maxlength="32" />
        </el-form-item>
        <el-divider>修改密码</el-divider>
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="profileForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="profileForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button class="btn-gradient" :loading="profileSaving" @click="submitProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="loginDialogVisible"
      title="登录已过期"
      width="420px"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" show-password @keyup.enter="submitExpiredLogin" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="btn-gradient" :loading="loginSubmitting" @click="submitExpiredLogin">重新登录</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Collection, Film, Picture, SwitchButton, VideoPlay } from '@element-plus/icons-vue'
import TosUpload from '@/components/Common/TosUpload.vue'
import { AUTH_EXPIRED_EVENT } from '@/utils/auth-events'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/user'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const profileDialogVisible = ref(false)
const loginDialogVisible = ref(false)
const profileSaving = ref(false)
const loginSubmitting = ref(false)
const profileFormRef = ref<FormInstance>()
const loginFormRef = ref<FormInstance>()

const menuList = [
  { name: 'ProjectList', title: '项目管理', icon: Collection, requiresProject: false },
  { name: 'ScriptPreview', title: '剧本预览', icon: Picture, requiresProject: true },
  { name: 'ShotWorkbench', title: '分镜工作台', icon: Film, requiresProject: true },
  { name: 'AssetLibrary', title: '资产库', icon: Picture, requiresProject: true }
]

const pageTitle = computed(() => {
  const item = menuList.find(entry => entry.name === route.name)
  return item?.title || 'AI漫剧生产平台'
})

const pageSubtitle = computed(() => {
  if (route.name === 'ProjectList') return '上传小说、设置全局参数，并进入项目详情。'
  return '严格按 PRD 保留三段式主流程：剧本预览、分镜工作台、资产库。'
})

const profileForm = reactive({
  username: '',
  avatar: '',
  oldPassword: '',
  newPassword: ''
})

const loginForm = reactive({
  username: '',
  password: ''
})

const profileRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  newPassword: [{ min: 6, message: '新密码至少 6 位', trigger: 'blur' }]
}

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleMenuClick = (item: { name: string; requiresProject: boolean }) => {
  if (item.requiresProject && !route.params.id) {
    ElMessage.warning('请先选择项目')
    return
  }
  if (item.name === 'ProjectList') {
    router.push({ name: 'ProjectList' })
    return
  }
  router.push({ name: item.name, params: { id: route.params.id } })
}

const fillProfileForm = () => {
  profileForm.username = authStore.userInfo?.username || ''
  profileForm.avatar = authStore.userInfo?.avatar || ''
  profileForm.oldPassword = ''
  profileForm.newPassword = ''
}

const submitProfile = async () => {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return
  profileSaving.value = true
  try {
    await authStore.updateProfile({
      username: profileForm.username.trim(),
      avatar: profileForm.avatar || undefined
    })
    if (profileForm.oldPassword && profileForm.newPassword) {
      await authStore.updatePassword({
        oldPassword: profileForm.oldPassword,
        newPassword: profileForm.newPassword
      })
    }
    ElMessage.success('个人信息已更新')
    profileDialogVisible.value = false
  } finally {
    profileSaving.value = false
  }
}

const handleLogout = async () => {
  await authStore.logout()
  router.push({ name: 'Login' })
}

const submitExpiredLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return
  loginSubmitting.value = true
  try {
    const res = await userApi.login(loginForm)
    authStore.setToken(res.data.token)
    authStore.setUserInfo({
      id: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      status: 1,
      avatar: authStore.userInfo?.avatar || ''
    })
    loginDialogVisible.value = false
    loginForm.password = ''
    ElMessage.success(`欢迎你，${res.data.nickname || res.data.username}~`)
  } finally {
    loginSubmitting.value = false
  }
}

const handleAuthExpired = () => {
  loginDialogVisible.value = true
  loginForm.username = authStore.userInfo?.username || ''
  loginForm.password = ''
}

onMounted(async () => {
  if (!authStore.userInfo && authStore.token) {
    await authStore.fetchUserInfo().catch(() => undefined)
  }
  fillProfileForm()
  window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
})

onBeforeUnmount(() => {
  window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
})
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(92, 241, 255, 0.18), transparent 28%),
    radial-gradient(circle at bottom right, rgba(255, 204, 102, 0.12), transparent 26%),
    $bg-page;
}

.sidebar {
  position: relative;
  background:
    linear-gradient(180deg, rgba(8, 31, 45, 0.96), rgba(3, 7, 13, 0.94)),
    $bg-page;
  border-right: 1px solid rgba(92, 241, 255, 0.18);
  padding: 20px 16px;

  &::after {
    content: '';
    position: absolute;
    top: 20px;
    right: -1px;
    width: 1px;
    height: 160px;
    background: linear-gradient(transparent, $border-glow-color, transparent);
    box-shadow: 0 0 18px rgba(92, 241, 255, 0.7);
  }
}

.sidebar-logo {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 74px;
  padding: 12px;
  border: 1px solid rgba(92, 241, 255, 0.22);
  border-radius: 18px;
  background: rgba(92, 241, 255, 0.06);
  color: $text-primary;
  font-family: $font-display;
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 28px;
  cursor: pointer;

  small {
    display: block;
    margin-top: 2px;
    color: $text-tertiary;
    font-size: 10px;
    letter-spacing: 0.2em;
  }
}

.logo-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  color: #031018;
  background: $primary-gradient;
  border-radius: 14px;
  box-shadow: 0 0 26px rgba(92, 241, 255, 0.35);
}

.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: 14px;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  &.active {
    color: $text-primary;
    background: linear-gradient(90deg, rgba(92, 241, 255, 0.18), rgba(125, 255, 178, 0.06));
    border-color: rgba(92, 241, 255, 0.35);
    box-shadow: 0 12px 32px rgba(92, 241, 255, 0.12);
  }

  &:hover:not(.disabled) {
    border-color: rgba(92, 241, 255, 0.26);
    transform: translateX(3px);
  }

  &.disabled {
    opacity: 0.45;
  }
}

.main-container {
  min-width: 0;
}

.app-header {
  height: 86px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 $page-padding;
  border-bottom: 1px solid rgba(92, 241, 255, 0.14);
  background: rgba(3, 7, 13, 0.68);
  backdrop-filter: blur(18px);
}

.page-kicker {
  margin: 0 0 4px;
  color: $accent-yellow;
  font-size: 11px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  line-height: 1;
}

.page-subtitle {
  margin: 6px 0 0;
  color: $text-secondary;
  font-size: 13px;
}

.app-main {
  padding: $page-padding;
  position: relative;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  color: $text-primary;

  strong,
  span {
    display: block;
  }

  span {
    margin-top: 4px;
    font-size: 12px;
    color: $text-secondary;
  }
}

.avatar-field {
  display: flex;
  align-items: center;
  gap: 16px;
}

@media (max-width: 900px) {
  .sidebar {
    width: 92px !important;
  }

  .sidebar-logo span,
  .sidebar-logo small,
  .menu-item span {
    display: none;
  }

  .sidebar-logo {
    justify-content: center;
    padding: 10px;
  }
}
</style>
