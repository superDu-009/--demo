<!-- views/tabs/WorkflowEditor.vue — 流程编辑器页面占位
     系分第 4.5 节：SortableJS 拖拽排序 + 步骤卡片 -->
<template>
  <div class="workflow-editor-page">
    <h2>流程编辑器</h2>
    <p class="placeholder-text">
      可拖拽步骤卡片列表（导入剧本 → 资产提取 → 分镜生成 → 图片生成 → 视频生成 → 导出）
    </p>
    <el-empty description="Sprint 3 实现流程编辑器功能" />
  </div>
</template>

<script setup lang="ts">
// TODO: Sprint 3 实现流程编辑器（SortableJS 拖拽）
</script>

<style scoped lang="scss">
.workflow-editor-page {
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
