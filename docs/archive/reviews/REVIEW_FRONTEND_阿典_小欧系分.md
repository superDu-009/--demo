# 前端系分评审意见 — 阿典 评 小欧 v1.0

## 文档信息

| 项目 | 内容 |
|------|------|
| 评审文档 | `DESIGN_FRONTEND_小欧_v1.0.md` |
| 评审人 | 阿典（AI/全栈专家） |
| 评审日期 | 2026-04-19 |
| 评审结论 | **有条件通过**（1 P0 + 5 P1 + 3 P2） |

---

## 评审总览

整体而言，小欧的前端系分文档结构清晰、类型定义完整、与后端 v1.2 接口对齐度高。项目结构规范、Pinia 模块化合理、Axios 拦截器设计完善。但以下四个关键领域存在需要修正的问题。

---

## 一、AI 任务轮询 — `useTaskPolling`（P0）

### 现状分析

文档中涉及**两套轮询机制**：

| 轮询场景 | 位置 | 间隔 | 最大次数 |
|---------|------|------|---------|
| 工作流执行进度 | `stores/workflow.ts` `startPolling()` | 固定 3s | 无上限（靠 executionLock=0 停止） |
| 分镜 AI 任务状态 | `composables/useTaskPolling.ts` | 固定 3s | maxPolls=200（约 10 分钟） |

后端 v1.2 设计：
- 后端对第三方 AI 服务采用**指数退避轮询**（10s → 20s → 40s → 60s 封顶）
- Phase 2 预留 Webhook 回调替代轮询

### 问题 1.1 [P0] 工作流轮询无超时保护，可能无限轮询

**位置：** `stores/workflow.ts` 第 1502-1513 行

```typescript
function startPolling(projectId: number) {
  stopPolling()
  const timer = window.setInterval(async () => {
    const res = await projectApi.getWorkflowStatus(projectId)
    status.value = res.data
    if (res.data.executionLock === 0) {
      stopPolling()
    }
  }, 3000)
  pollingTimer.value = timer as unknown as number
}
```

**风险：**
- 后端 Redis 锁最大 TTL 2h，如果极端情况下 executionLock 未能正确清零（如服务 crash 后 RecoveryRunner 未触发），前端将**永久轮询**。
- 无最大轮询次数上限，也无超时自动停止机制。

**建议：**
```typescript
const maxWorkflowPolls = 2400  // 3s * 2400 = 2h，与后端 Redis 锁 TTL 对齐
let pollCount = 0

function startPolling(projectId: number) {
  stopPolling()
  pollCount = 0
  const timer = window.setInterval(async () => {
    if (pollCount >= maxWorkflowPolls) {
      ElMessage.warning('流程执行超时，请刷新页面查看状态')
      stopPolling()
      return
    }
    try {
      const res = await projectApi.getWorkflowStatus(projectId)
      status.value = res.data
      if (res.data.executionLock === 0) {
        stopPolling()
        // 根据最终状态给出提示
        const failedStep = res.data.steps?.find(s => s.status === 3)
        if (failedStep) {
          ElMessage.error(`流程执行失败: ${failedStep.errorMsg}`)
        } else {
          ElMessage.success('流程执行完成')
        }
      }
    } catch (error) {
      console.error('Workflow polling error:', error)
    }
    pollCount++
  }, 3000)
  pollingTimer.value = timer as unknown as number
}
```

### 问题 1.2 [P1] 前端固定 3s 轮询与后端指数退避策略不匹配

**分析：**
- 后端对 Seedance 的轮询采用指数退避（10s → 20s → 40s → 60s），因为 AI 任务本身需要较长时间。
- 前端以固定 3s 轮询后端，在长任务场景（视频生成可能需要 1-5 分钟）下，将产生 **20-100 次无效请求**，每次后端都需要查库返回"处理中"状态。

**建议：前端也采用指数退避策略**

```typescript
// composables/useTaskPolling.ts 改进版
export function useTaskPolling() {
  const task = ref<AiTaskVO | null>(null)
  const isPolling = ref(false)
  let timer: number | null = null
  let pollCount = 0
  const maxPolls = 200

  // 指数退避间隔（对齐后端策略，但更积极一些）
  function getInterval(): number {
    if (pollCount === 0) return 3000   // 首次 3s，给用户即时反馈
    if (pollCount <= 3) return 5000    // 5s
    if (pollCount <= 8) return 10000   // 10s
    return 15000                        // 15s 封顶
  }

  async function startPolling(shotId: number, onStatusChange?: (task: AiTaskVO) => void) {
    stopPolling()
    isPolling.value = true
    pollCount = 0

    const poll = async () => {
      if (!isPolling.value || pollCount >= maxPolls) {
        stopPolling()
        return
      }

      try {
        const res = await aiApi.getLatestTask(shotId)
        task.value = res.data
        onStatusChange?.(res.data)

        if (res.data.status === 2 || res.data.status === 3) {
          stopPolling()
          return
        }
      } catch (error) {
        console.error('Task polling error:', error)
      }

      pollCount++
      timer = window.setTimeout(poll, getInterval())
    }

    poll()
  }

  // ...
}
```

**收益：** 对于 2 分钟的视频生成任务，请求数从 ~40 次降至 ~15 次，减少 60%+ 的无效后端查询。

### 问题 1.3 [P1] `useTaskPolling` 缺少完成/失败回调机制

**现状：** `startPolling(shotId)` 只接受 shotId 参数，任务完成或失败后没有任何回调通知调用方。

**影响：** ShotCard 组件无法在任务完成后自动刷新分镜数据、展示结果图片或弹出成功通知。

**建议：** 增加 `onSuccess` 和 `onError` 回调参数，或使用 Vue 的 `emit` 模式：

```typescript
interface PollingCallbacks {
  onSuccess?: (task: AiTaskVO) => void
  onError?: (task: AiTaskVO) => void
  onProgress?: (task: AiTaskVO) => void  // 可选：每次轮询都触发
}

function startPolling(shotId: number, callbacks?: PollingCallbacks)
```

### 问题 1.4 [P1] 工作流轮询与 AI 任务轮询职责混淆

文档中出现了**三个轮询相关实体**但职责边界不清：

| 实体 | 设计位置 | 实际用途 |
|------|---------|---------|
| `stores/workflow.ts` `startPolling` | Pinia store | 工作流整体进度轮询 |
| `useTaskPolling` | composable | 单个分镜 AI 任务轮询 |
| `useWorkflowStatus` | composables/ 目录中声明 | **未实现！** |

**建议：**
- 明确 `useWorkflowStatus` 是否需要，如果不需要则从目录结构中移除。
- 工作流轮询可以抽取为 `useWorkflowPolling` composable，从 store 中剥离纯 UI 逻辑。

### 问题 1.5 [P2] `useTaskPolling` 中 `onUnmounted` 清理不完整

**现状：** 仅调用 `stopPolling()`，但如果组件卸载时任务仍在后端处理中，用户切回页面时将看不到任务完成。

**建议：** 考虑在路由 `beforeRouteLeave` 或 `onBeforeUnmount` 中增加提示："AI 任务仍在后台执行中，完成后将自动更新"。

---

## 二、TOS 上传闭环 — `useTosUpload`（P1）

### 现状分析

上传流程设计为四步：
1. 前端校验文件类型和大小
2. `POST /api/tos/presign` 获取预签名 URL
3. XHR PUT 直传 TOS
4. `POST /api/tos/complete` 通知后端

### 问题 2.1 [P1] `complete` 接口失败未处理

**位置：** `composables/useTosUpload.ts` 第 1772-1777 行

```typescript
// 3. 直传 TOS
await uploadToTos(presignResult.uploadUrl, file)

// 4. 通知后端
await tosApi.complete({
  key: extractKeyFromUrl(presignResult.uploadUrl),
  projectId: options.projectId,
  fileType: options.fileType,
  metadata: { originalName: file.name, size: file.size }
})
```

**风险：**
- 如果 TOS 上传成功但 `complete` 接口调用失败（网络超时、后端异常），文件已存在于 TOS 但**数据库中无记录**，成为孤儿文件。
- 当前重试逻辑仅处理 `40005 预签名过期`（发生在 presign 阶段），对 complete 阶段失败没有重试。

**建议：**
```typescript
// complete 失败时增加重试
let completeRetries = 0
const maxCompleteRetries = 3

while (completeRetries < maxCompleteRetries) {
  try {
    await tosApi.complete({ /* ... */ })
    break  // 成功则退出
  } catch (error: any) {
    completeRetries++
    if (completeRetries >= maxCompleteRetries) {
      // 最终失败：TOS 上有文件但数据库无记录
      ElMessage.error('上传成功但通知后端失败，请联系管理员')
      // 可选：上报监控日志，包含 key 和 projectId 供后续修复
      throw new Error('TOS complete notification failed after retries')
    }
    await new Promise(r => setTimeout(r, 1000 * completeRetries))
  }
}
```

### 问题 2.2 [P1] `extractKeyFromUrl` 实现脆弱

**位置：** 第 1820-1824 行

```typescript
function extractKeyFromUrl(url: string): string {
  const urlObj = new URL(url)
  return urlObj.pathname.substring(1)
}
```

**风险：**
- 预签名 URL 的 pathname 可能包含编码字符（如空格被编码为 `%20`），直接 substring 可能得到非预期结果。
- 如果 TOS 返回的 URL 格式变更（如包含额外的前缀路径），此方法将失效。

**建议：**
- 让后端在 presign 响应中直接返回 `key` 字段，而不是从 URL 中解析。
- 或者至少增加 `decodeURIComponent` 处理：

```typescript
function extractKeyFromUrl(url: string): string {
  const urlObj = new URL(url)
  return decodeURIComponent(urlObj.pathname.substring(1))
}
```

### 问题 2.3 [P2] 缺少并发上传控制

**现状：** 多文件上传时（如资产参考图多张上传），`upload()` 函数没有并发限制，可能同时发起多个 presign + upload + complete 请求。

**建议：** 增加并发控制（如最多 3 个并发上传），或在 ImageUploader 组件层实现队列。

### 问题 2.4 [P2] 大文件上传未提示分片上传

后端 v1.2 已预留 Phase 2 分片上传接口，但前端文档未标注何时切换。建议在文档中注明：

> "当文件 > 10MB 时，Phase 2 将自动切换为分片上传模式（`presign-multipart` → `presign-part` × N → `complete-multipart`）。"

---

## 三、用户体验 — 长时间 AI 任务进度展示（P1）

### 问题 3.1 [P1] 工作流进度条缺乏细化展示

**后端 v1.2 已返回丰富的进度字段：**

```json
{
  "overallProgress": 45,
  "totalShots": 100,
  "processedShots": 45,
  "estimatedRemainingSeconds": 1800,
  "currentEpisodeTitle": "第三集",
  "steps": [
    { "stepType": "image_gen", "progress": 60, "currentDetail": "正在生成第45/100个分镜" }
  ]
}
```

**前端问题：**
- `stores/workflow.ts` 仅存储 `status.value = res.data`，但**没有将这些细化字段映射到 UI**。
- 进度条设计仅提到 "整体进度 overallProgress (0-100)" 和 "步骤级 el-progress"，但未描述如何展示 `currentDetail`、`estimatedRemainingSeconds`、`currentEpisodeTitle` 等关键信息。
- 对于可能持续数十分钟的完整流程执行，用户只能看到一个百分比数字，缺乏"正在做什么"的上下文。

**建议：** 在 WorkflowEditor 的执行状态面板中增加：
```
┌──────────────────────────────────────────┐
│ 🔵 流程执行中                            │
│                                          │
│ ████████████░░░░░░░░░░░  45%            │
│                                          │
│ 📺 正在处理: 第三集                       │
│ 🖼️  image_gen: 正在生成第45/100个分镜     │
│ ⏱️  预计剩余: 30 分钟                     │
└──────────────────────────────────────────┘
```

### 问题 3.2 [P1] 分镜卡片"生成中"状态缺少进度感知

**现状：** ShotCard 展示"生成中遮罩"，但没有显示 AI 任务的进度信息。

**建议：**
- 利用 `currentAiTask` 字段（后端 v1.2 已内嵌在 ShotVO 中），在遮罩上显示任务类型和进度。
- 对于视频生成等长任务，显示"已等待 X 分钟，预计还需 Y 分钟"。
- 使用 `pollCount`（如果后端返回的话）或基于时间推算，给用户提供心理预期。

### 问题 3.3 [P2] 轮询期间页面切换未保存状态

**风险：** 用户在分镜工作台触发了 AI 生成，然后切换到资产库 Tab。此时 `useTaskPolling` 的 `onUnmounted` 会停止轮询，用户回来时不知道任务是否完成。

**建议：**
- 考虑将轮询状态提升到 store 层级，或使用 `BroadcastChannel` / `localStorage` 事件实现 Tab 间同步。
- 至少在页面切换时弹出提示："有 AI 任务正在后台执行"。

---

## 四、分镜工作台性能 — 虚拟滚动策略（P1）

### 问题 4.1 [P1] `RecycleScroller` 固定 `item-size="280"` 不适配动态高度

**位置：** 第 606-618 行

```vue
<RecycleScroller
  :items="filteredShots"
  :item-size="280"
  key-field="id"
  v-slot="{ item }"
>
  <ShotCard :shot="item" />
</RecycleScroller>
```

**风险：**
- ShotCard 高度可能因内容而异：有图片 vs 无图片、有绑定资产 vs 无、错误信息多行时高度不同。
- 固定 `item-size` 会导致滚动位置计算错误，出现跳动、重叠或空白。

**建议方案（二选一）：**
1. **使用 `DynamicScroller`** 代替 `RecycleScroller`，支持动态高度：
```vue
<DynamicScroller
  :items="filteredShots"
  :min-item-size="280"
  key-field="id"
>
  <template v-slot="{ item, index, active }">
    <DynamicScrollerItem :item="item" :active="active" :size-dependencies="[item.status, item.generatedImageUrl]">
      <ShotCard :shot="item" />
    </DynamicScrollerItem>
  </template>
</DynamicScroller>
```

2. **统一卡片高度**：CSS 强制所有 ShotCard 固定高度（如 280px），超出内容省略。

### 问题 4.2 [P1] 虚拟滚动与分页控件共存逻辑不清

**现状：** 文档第 548-550 行同时提到：
- "列表区域：`vue-virtual-scroller` 虚拟滚动（防卡顿）"
- "底部：分页控件"

**问题：** 虚拟滚动和分页是两种互斥的列表加载策略。如果用了虚拟滚动加载全部数据，分页控件就没有意义；如果用了分页，虚拟滚动的价值就大打折扣（每页 20 条不需要虚拟滚动）。

**建议：** 明确选择一种策略：
- **推荐方案**：去掉分页，使用虚拟滚动 + 虚拟列表懒加载（滚动到底部自动加载下一页）。这符合"分镜可能上千条"的场景。
- 如果选择分页，则每页数据量较小（20-50 条），无需虚拟滚动。

### 问题 4.3 [P1] 大量图片/视频的内存管理未考虑

**风险：**
- `vue-virtual-scroller` 的 `RecycleScroller` 会复用 DOM 节点，但 `<img>` 和 `<video>` 标签的**浏览器内存缓存不会被自动释放**。
- 当用户快速滚动上千个包含图片/视频的卡片时，浏览器可能积累大量 media 缓存，导致页面卡顿甚至 OOM。

**建议：**
1. 使用 `el-image` 的 `lazy` 模式（文档已提到但未在 ShotCard 中明确使用）
2. 对 `video` 标签增加 `preload="none"`，仅在实际可见时加载
3. 在 `RecycleScroller` 的 `v-slots` 中增加可见性检测：

```vue
<RecycleScroller
  :items="filteredShots"
  :item-size="280"
  key-field="id"
  v-slot="{ item, active }"
>
  <ShotCard :shot="item" :lazy-media="active" />
</RecycleScroller>
```

`active` 属性表示当前 item 是否在可视区域内，`lazy-media=true` 时才渲染 `<img>` / `<video>`。

### 问题 4.4 [P2] 左侧分集/分场树与右侧列表的联动性能

**现状：** 点击左侧树节点会"过滤右侧分镜列表"。如果每次都重新请求 API + 重新渲染虚拟列表，频繁切换节点可能造成性能抖动。

**建议：** 增加缓存机制，已加载过的分场数据缓存在 store 中，切换时先展示缓存再静默刷新。

---

## 五、其他发现

### 5.1 [P1] `useWorkflowStatus` composable 声明但未实现

项目结构 `composables/` 目录中声明了 `useWorkflowStatus.ts`，但系分文档的 "8. Composables 设计" 章节只详细描述了 `useTaskPolling` 和 `useTosUpload`，未描述 `useWorkflowStatus`。

**建议：** 要么补充设计文档，要么从目录中移除。

### 5.2 [P2] 缺少全局 Loading 状态管理

文档第 12 节"错误处理策略"中没有提到全局 Loading 状态（如路由切换 loading、API 请求全局 loading 遮罩）。对于长时间 AI 任务，用户需要明确的"系统正在工作"的视觉反馈。

### 5.3 [P2] 路由守卫中 `isLoggedIn` 的初始状态问题

```typescript
if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
  return next({ name: 'Login', query: { redirect: to.fullPath } })
}
```

`isLoggedIn` 基于 `token` 是否存在判断，但 token 存储在 localStorage 中。如果用户手动修改了 localStorage 中的 token 为无效值，`isLoggedIn` 仍为 true，会尝试访问受保护页面然后被 401 拦截。

**建议：** 在路由守卫中增加一次 token 有效性校验（调用 `GET /api/user/info`），或使用 silent refresh 机制。

---

## 评审结论

| 优先级 | 数量 | 状态 |
|--------|------|------|
| P0（阻塞） | 1 | 必须修复后才能开发 |
| P1（重要） | 5 | 建议本 Sprint 修复 |
| P2（优化） | 3 | 可后续迭代处理 |

**总结：** 小欧的系分文档整体质量不错，与后端 v1.2 接口对齐良好。主要问题集中在：
1. **轮询策略需要优化**——工作流轮询缺少超时保护（P0），建议引入指数退避减少无效请求（P1）。
2. **TOS 上传闭环需要增强**——complete 阶段失败无重试（P1），key 解析方式脆弱（P1）。
3. **长任务 UX 需要细化**——后端已返回丰富进度字段，前端需充分利用（P1）。
4. **虚拟滚动策略需明确**——固定高度不适配动态卡片，虚拟滚动与分页策略需二选一（P1）。

建议小欧针对 P0 和 P1 问题进行修订后再次评审。

---

*评审人: 阿典 | 日期: 2026-04-19*
