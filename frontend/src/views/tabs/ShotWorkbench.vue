<!-- views/tabs/ShotWorkbench.vue — 分镜工作台页面占位
     系分第 4.6 节：左右分栏 + 分页模式 -->
<template>
  <div class="shot-workbench-page">
    <h2>分镜工作台</h2>
    <p class="placeholder-text">
      分集/分场导航 + 分镜卡片列表（分页模式，每页 20 条）+ 批量审核
    </p>
    <el-empty description="Sprint 3 实现分镜工作台功能" />
  </div>
</template>

<script setup lang="ts">
// TODO: Sprint 3 实现分镜工作台（左右分栏 + 分页模式）
</script>

<style scoped lang="scss">
.shot-workbench-page {
  h2 {
    color: $text-primary;
    margin-bottom: 8px;
  }
  .placeholder-text {
    color: $text-secondary;
    margin-bottom: 24px;
  }
}
</style>
