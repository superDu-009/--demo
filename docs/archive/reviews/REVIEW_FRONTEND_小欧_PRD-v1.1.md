# AI漫剧生产平台 — 前端架构评审意见
评审人：小欧（资深前端工程师）
评审日期：2026-04-19
PRD版本：v1.0 MVP

---

## 1. AntV X6 做线性流水线编辑器是否合适？

### 结论：过重，建议换更轻量的方案

AntV X6 是一个**通用图编辑引擎**，适合画 DAG、ER 图、流程图等复杂拓扑。但你的流程是**线性流水线**，本质就是一个有序步骤列表 + 拖拽排序 + 状态展示。X6 在这里相当于用大炮打蚊子，带来以下问题：

- **包体积大**：X6 核心 + 插件轻松 200KB+，对 MVP 没必要
- **学习曲线陡峭**：节点自定义、边、锚点、snapline 等概念，纯线性流程根本用不到
- **开发成本高**：拖拽排序 + 节点配置用 X6 实现反而比用基础 UI 组件更繁琐

### 推荐替代方案（按推荐程度排序）：

**方案A（首选）：SortableJS + 手写卡片列表**
- 用 Element Plus 的 `<el-card>` 渲染步骤节点，配合 `SortableJS`（或 `vuedraggable`）实现拖拽排序
- 每个节点卡片上放：步骤图标 + 名称 + review 开关 + 启用/禁用切换
- 执行时在卡片上显示进度条/状态徽标
- 总包体积增加 < 10KB，开发时间 1-2 天

**方案B：Element Plus `<el-steps>` + 拖拽**
- `<el-steps>` 自带线性步骤条的视觉样式
- 外层套 `vuedraggable` 实现排序
- 缺点：步骤条是只读展示型，可配置性不如方案A灵活

**方案C：如果确实需要可视化连线（未来可能变DAG）**
- 用 `@vue-flow/core`（VueFlow）替代 X6
- 专门为 Vue 3 设计的流程图库，轻量且 API 友好
- 但目前 MVP 阶段不推荐，线性流程不需要

### 具体实现建议（方案A）：

```vue
<!-- WorkflowEditor.vue 核心结构 -->
<template>
  <div class="pipeline-editor">
    <VueDraggable v-model="steps" item-key="type" handle=".drag-handle">
      <template #item="{ element }">
        <el-card class="step-card" :class="{ disabled: !element.enabled }">
          <div class="drag-handle">⠿</div>
          <div class="step-icon">{{ stepIcon(element.type) }}</div>
          <div class="step-name">{{ stepName(element.type) }}</div>
          <el-switch v-model="element.review" active-text="审核" />
          <el-switch v-model="element.enabled" active-text="启用" />
          <!-- 执行时显示状态 -->
          <div v-if="isRunning" class="step-status">
            <el-tag :type="statusType(element.status)">
              {{ statusLabel(element.status) }}
            </el-tag>
          </div>
        </el-card>
      </template>
    </VueDraggable>
  </div>
</template>
```

---

## 2. 分镜工作台交互设计建议

### PRD 现状问题：
- 四级结构（项目→分集→分场→分镜），如果嵌套展示在左侧列表，层级会很深
- 大量分镜时（一集可能有 30-50 个分镜），全量渲染会卡顿
- 版本对比没有具体方案

### 建议方案：

**2.1 左侧列表设计 — 分集/分场折叠树 + 分镜平铺**

```
左侧面板（宽度可调）：
├── 第1集：重生归来
│   ├── 场景1-1：卧室 [5个分镜]  ✓✓✓✓⏳
│   ├── 场景1-2：街道 [3个分镜]  ✓⏳✗
│   └── 场景1-3：学校 [4个分镜]
├── 第2集：初遇
│   └── ...
```

- 用 `el-tree` 或手写的折叠面板，分集 → 分场 两层折叠
- 每个分场显示分镜数量和状态摘要（如 3/5 已通过）
- 点击分场后，右侧展示该分场下的所有分镜卡片

**2.2 大量分镜的性能优化**

- **虚拟滚动**：左侧分镜列表超过 50 项时，使用 `vue-virtual-scroller`（或 Element Plus 的虚拟列表）
- **右侧详情懒加载**：点击分镜卡片才加载详情数据，不要一次性加载全部分镜的 AI 生成内容
- **图片/视频按需加载**：缩略图用低分辨率占位，点击放大再加载原图
- **批量操作**：提供「全选当前分场」「批量通过」「批量打回」功能，减少重复点击

**2.3 版本管理交互**

建议用「时间线」方式展示版本历史：

```
当前版本: v3 (最新)
├── v2 — 2026-04-18 14:30 — 打回: 人物表情不自然
└── v1 — 2026-04-18 10:15 — 打回: 背景风格不一致

[对比 v3 vs v2] 按钮 → 左右分屏对比两张图
[恢复到此版本] 按钮 → 将选定版本设为当前
```

**2.4 分镜卡片建议布局**

每个分镜用一个卡片展示核心信息，类似 Kanban 卡片：

```
┌─────────────────────────────┐
│ #1.2.3  主角在教室发现异常   │  ← 编号 + 标题
│                             │
│  [  图片缩略图 / 视频预览  ] │  ← 16:9 比例占位
│                             │
│  👤 李明  🏫 第三中学教室    │  ← 关联资产标签
│  ─────────────────────      │
│  状态: [待审核]  版本: v3    │  ← 状态 + 版本
│  [生成] [通过] [打回] [⋮]   │  ← 操作按钮
└─────────────────────────────┘
```

---

## 3. 资产管理页面设计

### 核心设计原则：以「类型」为维度分组，卡片式展示

**页面布局建议：**

```
┌──────────────────────────────────────────────┐
│  [Tab: 角色] [Tab: 场景] [Tab: 物品] [Tab: 声音]│
├──────────────────────────────────────────────┤
│                                              │
│  [新建资产]  [AI自动提取]  [批量导入]         │
│                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ [参考图] │  │ [参考图] │  │ [参考图] │   │
│  │          │  │          │  │          │   │
│  │ 李明     │  │ 韩梅梅   │  │ 王老师   │   │
│  │ 男/17岁  │  │ 女/16岁  │  │ 男/45岁  │   │
│  │ ✅已确认 │  │ ✅已确认 │  │ ⏳草稿   │   │
│  │ [编辑]   │  │ [编辑]   │  │ [确认]   │   │
│  └──────────┘  └──────────┘  └──────────┘   │
│                                              │
└──────────────────────────────────────────────┘
```

### 关键交互建议：

1. **资产卡片正面**：参考图（大图）、名称、一句话描述、状态徽标
2. **资产卡片点击展开/弹窗编辑**：
   - 参考图上传（支持拖拽上传、从已生成图片中选择）
   - AI 描述编辑（多行文本区，标注：此文本将直接作为 prompt 的一部分）
   - 风格预设（从项目级继承 or 覆盖）
   - 资产状态切换（草稿 → 已确认 / 已废弃）
3. **参考图管理**：
   - 每个资产可以有**多张**参考图（正面/侧面/特写），不只一张
   - 标记一张为「主参考图」，用于生成时默认传入
   - PRD 中 `reference_image_url` 是单个 VARCHAR，建议改为 `reference_images JSON` 数组
4. **资产确认工作流**：
   - 从「资产提取」步骤过来的资产默认是「草稿」状态
   - 创作者必须逐条确认（或批量确认）后才能用于分镜生成
   - 在分镜工作台中，未确认的资产不可选
5. **声音资产特殊处理**：
   - 声音资产没有参考图，但有音频样本
   - 卡片上显示音频播放器（波形可视化 + 播放/暂停）

### 数据结构建议（前端）：

```typescript
interface Asset {
  id: number
  projectId: number
  assetType: 'character' | 'scene' | 'prop' | 'voice'
  name: string
  description: string           // AI prompt 描述
  referenceImages: ReferenceImage[]  // 改为数组
  mainImageIndex: number        // 主参考图索引
  audioSampleUrl?: string       // 声音资产专用
  stylePreset?: Record<string, any>
  status: 'draft' | 'confirmed' | 'deprecated'
  createdAt: string
}

interface ReferenceImage {
  url: string
  label: string  // 如 "正面"、"侧面"、"特写"
}
```

---

## 4. Pinia 状态管理模块划分

### 推荐划分（5个核心模块）：

```
src/stores/
├── auth.ts          # 用户认证（token, userInfo, login/logout）
├── project.ts       # 项目列表、当前项目、workflow配置
├── asset.ts         # 资产列表（按类型过滤）、CRUD操作
├── content.ts       # 分集/分场/分镜层级数据
└── ai.ts            # AI任务状态、进度轮询、成本统计
```

### 各模块职责：

**auth.ts**
```typescript
// 职责：登录状态管理
interface AuthState {
  token: string | null
  userInfo: { id: number; username: string; nickname: string } | null
}
// Actions: login(), logout(), checkAuth(), refreshToken()
```

**project.ts**
```typescript
// 职责：项目列表 + 当前项目上下文
interface ProjectState {
  projects: ProjectSummary[]
  currentProject: ProjectDetail | null   // 包含 workflowConfig, stylePreset
  loading: boolean
}
// Actions: fetchProjects(), selectProject(id), updateWorkflow(), startWorkflow()
// ⚠️ 注意：currentProject 是全局上下文，多个页面共享
```

**asset.ts**
```typescript
// 职责：当前项目的资产库
interface AssetState {
  assets: Asset[]
  filterType: AssetType | 'all'
  loading: boolean
}
// Actions: fetchAssets(projectId), createAsset(), updateAsset(),
//          confirmAsset(id), batchConfirm(ids)
// 计算属性：confirmedAssets, characters, scenes, props, voices
```

**content.ts**
```typescript
// 职责：分集→分场→分镜 四级数据
interface ContentState {
  episodes: Episode[]
  activeEpisodeId: number | null
  scenes: Record<number, Scene[]>           // episodeId -> scenes
  shots: Record<number, Shot[]>             // sceneId -> shots
  activeShotId: number | null
}
// Actions: fetchEpisodes(), fetchScenes(episodeId), fetchShots(sceneId),
//          updateShot(), reviewShot(id, action, comment)
// 计算属性：shotsByScene(), shotStats()  // 按状态统计
```

**ai.ts**
```typescript
// 职责：AI任务追踪 + 成本
interface AIState {
  runningTasks: Record<string, TaskProgress>   // taskId -> progress
  costReport: CostReport | null
}
// Actions: triggerImageGen(), triggerVideoGen(), pollTask(taskId),
//          fetchCostReport()
```

### 重要注意事项：
1. **避免状态冗余**：`currentProject` 只存一份，不要在多个 store 中重复存储 projectId
2. **路由同步**：当 URL 中 projectId 变化时，用路由守卫自动切换 `currentProject`
3. **乐观更新**：分镜审核操作（通过/打回）建议乐观更新 UI，失败再回滚
4. **不要滥用 Pinia**：分镜工作台的分页/筛选等局部 UI 状态用 `ref()` 即可，不必放入 Pinia

---

## 5. AI生成任务进度展示优化方案

### PRD 方案的问题：
「用户手动刷新」体验很差——用户在等待时不知道是卡住了、失败了、还是正常处理中。Seedance 视频生成可能需要 30-120 秒，这段时间用户会焦虑。

### 推荐方案（按体验从低到高）：

**方案A：前端定时轮询（最低成本，推荐 MVP 使用）**

```typescript
// composables/useTaskPolling.ts
export function useTaskPolling(taskId: string, interval = 3000) {
  const status = ref<TaskStatus>('pending')
  const progress = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  function start() {
    timer = setInterval(async () => {
      const res = await fetchTaskStatus(taskId)
      status.value = res.status
      progress.value = res.progress
      if (['success', 'failed'].includes(res.status)) stop()
    }, interval)
  }

  function stop() { if (timer) { clearInterval(timer); timer = null } }

  onUnmounted(stop)
  return { status, progress, start, stop }
}
```

- 每 3 秒轮询一次，分镜卡片上显示进度条
- 完成后自动刷新列表，无需用户手动操作
- 后端需要提供 `GET /api/ai/task/{taskId}` 接口

**方案B：SSE 服务器推送（体验更好，增加少量后端成本）**

```typescript
// composables/useSSE.ts
export function useSSE(taskId: string) {
  const status = ref('pending')
  const events = ref<string[]>([])

  function connect() {
    const source = new EventSource(`/api/sse/task/${taskId}`)
    source.onmessage = (e) => {
      const data = JSON.parse(e.data)
      status.value = data.status
      events.value.push(data.message)  // 如 "正在生成首帧..."
      if (['success', 'failed'].includes(data.status)) source.close()
    }
  }
  return { status, events, connect }
}
```

- 用户可以实时看到进度变化，甚至看到中间状态描述
- Spring Boot 支持 SSE（`SseEmitter`），实现成本不高

**方案C（不推荐）：WebSocket**
- 对于 MVP 来说过重，轮询或 SSE 完全够用

### 前端 UI 展示建议：

```
分镜卡片上：
┌────────────────────────────┐
│ #1.2.3  主角在教室发现异常  │
│                            │
│ [████████░░░░░░] 60%       │  ← 进度条
│ ⏳ 正在生成视频... 约40秒   │  ← 状态文案 + 预估时间
│                            │
│ [取消任务]                   │
└────────────────────────────┘
```

- 生成中状态显示**进度条 + 状态文案 + 预估剩余时间**
- 提供「取消任务」按钮（避免浪费 API 费用）
- 任务完成后自动将卡片切换到「待审核」状态，并播放轻微的提示音或通知

---

## 6. 前端项目结构建议

### PRD 中的结构过于扁平，建议以下结构：

```
frontend/
├── index.html
├── vite.config.ts
├── package.json
├── tsconfig.json
├── .env.development
├── .env.production
└── src/
    ├── main.ts
    ├── App.vue
    ├── router/
    │   ├── index.ts              # 路由定义 + 导航守卫
    │   └── routes.ts             # 路由配置表
    ├── stores/
    │   ├── auth.ts
    │   ├── project.ts
    │   ├── asset.ts
    │   ├── content.ts
    │   └── ai.ts
    ├── views/                    # 页面级组件
    │   ├── Login.vue
    │   ├── ProjectList.vue
    │   ├── ProjectDetail.vue     # 项目概览（可作为布局容器）
    │   ├── AssetLibrary.vue
    │   ├── WorkflowEditor.vue
    │   ├── ShotWorkbench.vue
    │   └── ApiCost.vue
    ├── components/               # 全局复用组件
    │   ├── layout/
    │   │   ├── AppHeader.vue
    │   │   ├── AppSidebar.vue
    │   │   └── AppLayout.vue
    │   ├── common/
    │   │   ├── StatusTag.vue          # 状态徽标（统一颜色映射）
    │   │   ├── ConfirmDialog.vue       # 确认弹窗
    │   │   ├── ImageUploader.vue       # 图片上传（带预览+裁剪）
    │   │   ├── RichTextEditor.vue      # 小说内容/提示词编辑
    │   │   └── PaginationBar.vue
    │   └── business/                   # 业务组件
    │       ├── asset/
    │       │   ├── AssetCard.vue       # 资产卡片
    │       │   ├── AssetEditor.vue     # 资产编辑面板
    │       │   └── ReferenceImageGallery.vue
    │       ├── workflow/
    │       │   ├── PipelineStep.vue    # 流程步骤卡片
    │       │   └── PipelineStatus.vue  # 执行状态展示
    │       ├── shot/
    │       │   ├── ShotCard.vue        # 分镜卡片
    │       │   ├── ShotDetail.vue      # 分镜详情面板
    │       │   ├── ShotList.vue        # 分镜列表（带虚拟滚动）
    │       │   ├── ShotVersionTimeline.vue
    │       │   └── ImageCompare.vue    # 版本对比（左右分屏）
    │       └── project/
    │           └── NovelUploader.vue   # 小说上传组件
    ├── composables/              # 组合式函数
    │   ├── useAuth.ts
    │   ├── useTaskPolling.ts     # AI任务轮询
    │   ├── useImageUpload.ts     # 上传到TOS
    │   ├── useShotFilter.ts      # 分镜筛选/分页
    │   └── useConfirmAction.ts   # 二次确认封装
    ├── api/                      # API 层
    │   ├── request.ts            # Axios 实例 + 拦截器
    │   ├── user.ts
    │   ├── project.ts
    │   ├── asset.ts
    │   ├── content.ts
    │   ├── workflow.ts
    │   ├── ai.ts
    │   └── storage.ts
    ├── types/                    # TypeScript 类型定义
    │   ├── api.ts                # API 请求/响应类型
    │   ├── project.ts
    │   ├── asset.ts
    │   ├── content.ts
    │   ├── workflow.ts
    │   └── common.ts
    ├── utils/
    │   ├── format.ts             # 时间/金额格式化
    │   ├── image.ts              # 图片处理工具
    │   └── validate.ts           # 表单校验
    ├── assets/
    │   ├── styles/
    │   │   ├── variables.scss    # 全局样式变量
    │   │   ├── mixins.scss
    │   │   └── global.scss       # 全局重置 + Element Plus 覆盖
    │   └── images/
    └── constants/
        ├── asset.ts              # 资产类型枚举 + 映射
        ├── workflow.ts           # 步骤类型枚举
        └── status.ts             # 状态码 → 标签/颜色映射
```

### 关键设计决策说明：

1. **`components/business/` vs `components/common/`**：业务组件与特定功能域耦合，不复用；通用组件全站复用
2. **`composables/` 独立目录**：Vue 3 的组合式函数是核心抽象层，独立出来便于测试和复用
3. **`api/` 和 `types/` 分离**：类型定义不混在 api 文件中，便于全局引用
4. **`constants/`**：状态映射、枚举值集中管理，避免硬编码散落各处
5. **样式用 SCSS**：Element Plus 默认用 SCSS，变量覆盖更方便

---

## 7. 最可能出问题的前端环节

### 🚨 Top 5 风险环节（按严重程度排序）：

**风险1：分镜工作台的图片/视频加载和渲染（高概率）**
- **问题**：一集 30-50 个分镜，每个分镜有参考图、生成图、视频缩略图。如果不做懒加载和虚拟滚动，页面会严重卡顿
- **视频预览**：视频自动播放会产生大量网络请求和性能问题
- **应对**：
  - 左侧列表必须用虚拟滚动（`vue-virtual-scroller`）
  - 图片用 `loading="lazy"` + 缩略图占位
  - 视频默认不加载 poster 以外的内容，hover 或点击才预加载
  - 使用 IntersectionObserver 实现视口内图片的渐进加载

**风险2：AI 任务状态同步（高概率）**
- **问题**：用户触发生成后切到其他页面，回来时状态不一致；多标签页操作同一项目时状态冲突
- **应对**：
  - 使用 `visibilitychange` 事件，页面恢复时自动刷新
  - 关键操作前校验最新状态（乐观更新 + 后端确认）
  - 考虑用 BroadcastChannel API 实现同浏览器多标签页同步

**风险3：四级导航层级过深（中概率）**
- **问题**：项目→分集→分场→分镜，用户在深层级时容易迷失，返回操作繁琐
- **应对**：
  - 顶部面包屑导航：`项目名 > 第1集 > 场景1-2 > 分镜#3`
  - 面包屑的每级可点击跳转
  - 左侧面板支持快速切换分集/分场

**风险4：大文件上传（小说文本 + 图片/视频）（中概率）**
- **问题**：小说文本可能几万字到几十万字，直接 POST 可能超时；图片/视频上传到 TOS 如果走后端中转，大文件会撑爆后端内存
- **应对**：
  - 小说上传：前端先分段/分块，后端分批处理
  - 图片/视频上传：**前端直传 TOS**（使用 TOS 预签名 URL），不经过后端
  - 上传进度条 + 断点续传

**风险5：状态颜色/徽标不一致（低概率但影响体验）**
- **问题**：待处理/生成中/待审核/已通过/已打回，如果在多个页面（分镜列表、流程状态、资产状态）中颜色/图标不统一，用户会困惑
- **应对**：
  - 在 `constants/status.ts` 中统一定义映射表
  - 封装 `<StatusTag>` 组件，全局使用
  ```typescript
  // constants/status.ts
  export const SHOT_STATUS_MAP = {
    0: { label: '待处理', type: 'info', icon: 'Clock' },
    1: { label: '生成中', type: 'warning', icon: 'Loading' },
    2: { label: '待审核', type: 'primary', icon: 'View' },
    3: { label: '已通过', type: 'success', icon: 'Check' },
    4: { label: '已打回', type: 'danger', icon: 'Close' },
    5: { label: '已完成', type: '', icon: 'Finished' },
  } as const
  ```

---

## 8. PRD 中遗漏的前端相关事项

1. **路由权限控制**：未登录用户访问任意页面应重定向到登录页；PRD 提到 Sa-Token 但没提前端路由守卫
2. **全局错误处理**：API 请求失败、网络断连、TOS 上传失败时的错误提示和重试机制
3. **响应式适配**：虽然是 Chrome/Edge 桌面端，但创作者可能用不同分辨率（1366px ~ 4K），布局需要适配
4. **快捷键支持**：分镜审核高频操作（通过/打回）应支持快捷键（如 `Ctrl+Enter` 通过，`Ctrl+Backspace` 打回）
5. **操作日志/撤销**：误操作打回后能否撤销？建议至少关键操作有 undo 提示
6. **国际化**：目前不需要，但如果有海外用户需要考虑
7. **数据导入导出**：项目配置、资产数据是否支持导出备份？

---

## 总结

| 评审项 | 结论 | 优先级 |
|--------|------|--------|
| X6 流程编辑器 | 替换为 SortableJS + 卡片列表 | 🔴 高 |
| 分镜工作台性能 | 虚拟滚动 + 懒加载 + 按需加载 | 🔴 高 |
| AI任务进度 | 前端轮询或 SSE，自动刷新替代手动 | 🔴 高 |
| 资产管理 | 卡片式 + Tab 分组 + 多参考图 | 🟡 中 |
| 前端结构 | 按业务域分层，API/types/composables 分离 | 🟡 中 |
| 文件上传 | 前端直传 TOS（预签名URL） | 🟡 中 |
| 状态管理 | 5个模块，避免冗余，路由同步 | 🟢 低 |
| 样式统一 | 常量映射表 + 封装组件 | 🟢 低 |

**MVP 开发顺序建议：**
1. 先搭基础框架（路由、布局、API层、Pinia基础模块）
2. 项目管理 + 资产 CRUD
3. 分镜工作台（核心页面，迭代次数最多）
4. 流程编辑器（轻量实现）
5. AI 集成 + 任务轮询
6. 最后打磨体验（快捷键、虚拟滚动、对比功能等）
