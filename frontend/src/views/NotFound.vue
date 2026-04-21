<!-- views/NotFound.vue — 404 页面 -->
<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <h1 class="error-code">404</h1>
      <p class="error-message">页面不存在</p>
      <el-button type="primary" @click="router.push({ name: 'ProjectList' })">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: $bg-page;
}

.not-found-content {
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: $primary-color;
  margin-bottom: 16px;
}

.error-message {
  font-size: 20px;
  color: $text-secondary;
  margin-bottom: 32px;
}
</style>
