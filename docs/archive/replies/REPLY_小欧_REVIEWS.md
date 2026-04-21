# 回复评审意见书 — 小欧 回复 老克 & 阿典

## 文档信息

| 项目 | 内容 |
|------|------|
| 原始设计文档 | `docs/design/DESIGN_FRONTEND_小欧_v1.0.md` |
| 评审文档 | `REVIEW_FRONTEND_老克_小欧系分.md` + `REVIEW_FRONTEND_阿典_小欧系分.md` |
| 回复人 | 小欧（前端架构师） |
| 回复日期 | 2026-04-19 |
| 指导原则 | 蓝烟老师特别指示：**轻量级、简单高效、不搞复杂交互** |

---

## 一、老克评审回复

### 老克 P0-1（A1）：`GET /api/project/{projectId}/shots` 跨分场查询接口未使用

**态度：【接受】**

**简化方案：**
- 在分镜工作台顶部增加一个简单的切换按钮：「当前分场」/「全部镜头」
- 切换到「全部镜头」时调用 `GET /api/project/{projectId}/shots`
- 列表区域显示所有分镜，用简单标签标注所属分场名（如 `[第2场] 分镜标题`）
- 不做筛选面板、不做分组折叠，就是平铺列表 + 分场标签

**理由：** 后端已经提供了接口，前端只需增加一个切换开关即可利用，改动极小，不需要额外的 UI 组件。

**修改计划：**
```
ShotWorkbench.vue:
  - 新增 ref currentViewMode: 'scene' | 'project'（默认 'scene'）
  - 顶部加一个 el-radio-group 切换按钮
  - watch currentViewMode → 切换时调用不同的 API
  - 列表项中简单渲染分场名称前缀
```

---

### 老克 P0-2（C1）：51001（AI 模型超时/排队中）排队状态 UI 缺失

**态度：【接受】**

**简化方案：**
- 不搞排队动画、不搞进度条，纯文字提示
- 在分镜卡片遮罩上显示：「AI模型排队中，请耐心等待…」
- 状态为 `Generating` 且 AI 任务状态为 `Submitting` 时显示排队文字，状态为 `Processing` 时显示「正在生成中…」
- 利用后端 ShotVO 中已有的 `currentAiTask` 字段判断

**理由：** 只需要根据任务状态切换一行文字提示，零动画、零复杂度，用户心理预期已满足。

**修改计划：**
```
ShotCard.vue:
  - 遮罩文案从固定"生成中"改为根据 currentAiTask.status 动态显示：
    status=0 → "AI模型排队中，请耐心等待…"
    status=1 → "正在生成中…"
  - 纯文字，无动画
```

---

### 老克 P1-1（B1）：`40301` 前端处理与后端约定不一致

**态度：【接受 — 简化处理】**

**简化方案：**
- **不新增 `/403` 页面**，违背轻量原则
- `40301` 仍用 Toast 提示：「您不是该项目创建者，无权执行此操作」
- 在 `ERROR_ACTION` 映射中将 `40301` 保持为 `'toast'`
- 在代码注释中说明：与老克确认，MVP 阶段以 Toast 提示替代无权限页面

**理由：** MVP 阶段新增一个完整页面（含路由、组件、返回逻辑）成本过高。Toast 提示已能传达核心信息，后续 Phase 2 有需要再加 403 页面。

**修改计划：**
```
constants/errors.ts:
  - 40301 保持 'toast'
  - 增加注释: /* MVP阶段用Toast替代403页面，Phase 2按需增加 */
```

---

### 老克 P1-2（A2）：`projectApi.getShots` 未使用

**态度：【接受】**

**简化方案：**
- 结合 A1 的修改，保留此 API（跨分场查询需要用到）
- 在 `api/project.ts` 中增加注释说明使用场景

**修改计划：**
```
api/project.ts:
  - getShots() 保留
  - 增加注释: /* 跨分场查询，用于分镜工作台"全部镜头"视图 */
```

---

### 老克 P1-3（5.2）：TOS 重试 Toast 重复

**态度：【接受】**

**简化方案：**
- 在 `handleBusinessError` 中，`'retry-presign'` 分支不显示 Toast，静默处理
- 只在 `useTosUpload.ts` 中显示一次：「上传链接已过期，正在重新获取…」
- 减少一处代码，用户只看到一条提示

**修改计划：**
```
api/index.ts (handleBusinessError):
  - case 'retry-presign': 不调用 ElMessage，仅 console.warn
  - 注释: "Toast 由 useTosUpload 统一展示，此处静默"
```

---

## 二、阿典评审回复

### 阿典 P0-1（1.1）：工作流轮询无超时保护，可能无限轮询

**态度：【接受】**

**简化方案：**
- 增加简单计数器，`maxWorkflowPolls = 2400`（3s × 2400 = 2h，对齐 Redis 锁 TTL）
- 超时后：stopPolling() + 一条 Toast 提示「流程执行超时，请刷新页面」
- 不增加复杂的状态机，不增加详细的失败原因展示

**理由：** 加 3 行代码即可解决潜在无限轮询问题，投入产出比极高。

**修改计划：**
```typescript
stores/workflow.ts:
  let pollCount = 0
  const maxWorkflowPolls = 2400

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
        }
      } catch (e) {
        console.error('Workflow polling error:', e)
      }
      pollCount++
    }, 3000)
    pollingTimer.value = timer as unknown as number
  }
```

---

### 阿典 P1-1（1.2）：前端固定 3s 轮询与后端指数退避不匹配

**态度：【接受 — 简化指数退避】**

**简化方案：**
- 不搞复杂的退避算法，只分三档：3s → 10s → 30s
- 首次轮询 3s（即时反馈），3 次后 10s，10 次后 30s 封顶
- 统一应用到 `useTaskPolling` 和 `stores/workflow.ts` 的轮询中
- 实现为一个简单的 `getPollInterval(pollCount)` 函数，不超过 10 行

**理由：** 三档退避比复杂的多级退避简单得多，但已经能将长任务请求数减少 60%+。

**修改计划：**
```typescript
function getPollInterval(count: number): number {
  if (count < 3) return 3000
  if (count < 10) return 10000
  return 30000
}
// 将 setInterval 改为 setTimeout 递归调用即可
```

---

### 阿典 P1-2（1.3）：`useTaskPolling` 缺少完成/失败回调

**态度：【接受 — 简化回调】**

**简化方案：**
- 只增加一个 `onDone` 回调（成功和失败都触发），由调用方自行判断状态
- 不拆成 onSuccess/onError/onProgress 三个回调，减少 API 复杂度
- ShotCard 组件通过 `onDone` 触发刷新分镜数据

**理由：** 一个回调比三个回调简单得多，调用方根据 `task.status` 自行处理成功/失败分支即可。

**修改计划：**
```typescript
interface PollingOptions {
  onDone?: (task: AiTaskVO) => void
}

function startPolling(shotId: number, options?: PollingOptions) {
  // ...轮询逻辑...
  if (res.data.status === 2 || res.data.status === 3) {
    stopPolling()
    options?.onDone?.(res.data)
    return
  }
}
```

---

### 阿典 P1-3（1.4）：工作流轮询与 AI 任务轮询职责混淆

**态度：【接受 — 最简化处理】**

**简化方案：**
- 将 `stores/workflow.ts` 中的 `startPolling/stopPolling` 抽取到 `composables/useWorkflowPolling.ts`
- 删除未使用的 `composables/useWorkflowStatus.ts`（从未实现）
- store 中只保留状态数据，轮询逻辑由 composable 管理
- 不做额外的架构重构

**理由：** 抽取 composable 只需移动代码 + 调整 import，改动量小但职责清晰。删除无用文件减少维护负担。

**修改计划：**
```
新增 composables/useWorkflowPolling.ts:
  - export function useWorkflowPolling() { startPolling, stopPolling, pollingStatus }
  - 内部调用 projectApi.getWorkflowStatus

删除 composables/useWorkflowStatus.ts:
  - 未实现，直接删除

修改 stores/workflow.ts:
  - 移除 startPolling/stopPolling，改用 useWorkflowPolling
```

---

### 阿典 P1-4（2.1）：`complete` 接口失败未处理

**态度：【接受 — 最简化重试】**

**简化方案：**
- **不搞 3 次重试 + 递增延迟 + 监控日志上报**
- 只重试 1 次，失败即报错：「文件已上传，但记录保存失败，请重试」
- 不记录孤儿文件信息，不上报监控，MVP 阶段不做

**理由：** 蓝烟老师指示要简单高效。complete 失败是小概率事件，重试 1 次已能覆盖大部分网络抖动场景。

**修改计划：**
```typescript
composables/useTosUpload.ts:
  try {
    await tosApi.complete({ ... })
  } catch (e) {
    // 重试 1 次
    try {
      await tosApi.complete({ ... })
    } catch (e2) {
      ElMessage.error('文件已上传，但记录保存失败，请刷新后重试')
      throw e2
    }
  }
```

---

### 阿典 P1-5（2.2）：`extractKeyFromUrl` 实现脆弱

**态度：【接受】**

**简化方案：**
- 增加 `decodeURIComponent` 处理即可
- 不要求后端改接口返回 key（减少跨端协调成本）
- 如果后端后续改了 URL 格式再调整

**修改计划：**
```typescript
function extractKeyFromUrl(url: string): string {
  const urlObj = new URL(url)
  return decodeURIComponent(urlObj.pathname.substring(1))
}
```

---

### 阿典 P1-6（3.1）：工作流进度条缺乏细化展示

**态度：【接受 — 文字面板方案】**

**简化方案：**
- **不搞炫酷的可视化面板**
- 用 2-3 行简单文字 + 一个进度条：
  ```
  流程执行中
  ████████░░░░░░░  45%
  当前: image_gen — 正在生成第45/100个分镜
  预计剩余: 约30分钟
  ```
- 所有数据直接用后端 `WorkflowStatusVO` 中已有字段，不做额外计算
- `estimatedRemainingSeconds` 简单格式化为「X分钟」，不精确到秒

**理由：** 纯文字展示 + Element Plus 自带 el-progress 组件，开发成本极低但用户体验提升显著。

**修改计划：**
```
WorkflowEditor.vue 执行状态面板:
  - el-progress 展示 overallProgress
  - 2行 el-text 展示 currentDetail + 预计剩余时间
  - 预计剩余时间: formatSecondsToMinutes(estimatedRemainingSeconds)
```

---

### 阿典 P1-7（3.2）：分镜卡片"生成中"状态缺少进度感知

**态度：【接受 — 最简化处理】**

**简化方案：**
- 结合 C1 的修改（排队文字提示），同时显示任务类型
- 遮罩文字改为：「🖼️ 图片生成中…」或「🎬 视频生成中…」
- 不做"已等待X分钟"的计时器（需要额外状态管理）
- 利用 `currentAiTask.taskType` 字段区分任务类型

**理由：** 一行文字区分任务类型，用户能知道是图片还是视频在生成即可，不需要精确计时。

**修改计划：**
```
ShotCard.vue 遮罩文案:
  taskType === 'image_gen' → "🖼️ 图片生成中…"
  taskType === 'video_gen' → "🎬 视频生成中…"
  currentAiTask.status === 0 → 附加 "（排队中）"
```

---

### 阿典 P1-8（4.1）：`RecycleScroller` 固定 item-size 不适配动态高度

**态度：【接受 — 固定高度方案】**

**简化方案：**
- **不引入 DynamicScroller**（增加依赖 + 复杂度）
- 用 CSS 强制所有 ShotCard 固定高度 280px
- 超出内容用 `overflow: hidden` + `text-overflow: ellipsis` 截断
- 用户想看详情可点击卡片展开（MVP 先不做展开，保持简单）

**理由：** 固定高度方案零额外依赖，CSS 几行搞定，是最轻量的选择。

**修改计划：**
```
ShotCard.vue:
  .shot-card {
    height: 280px;
    overflow: hidden;
  }
  .shot-card .error-msg {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
```

---

### 阿典 P1-9（4.2）：虚拟滚动与分页控件共存逻辑不清

**态度：【接受 — 选择分页，去掉虚拟滚动】**

**简化方案：**
- **去掉 `vue-virtual-scroller`，改用普通列表 + 分页**
- MVP 阶段一个项目不太可能有上千分镜，每页 20 条足够
- 如果后续数据量真的增大，再考虑虚拟滚动
- 减少一个第三方依赖

**理由：** 分页方案最简单成熟（Element Plus el-pagination 开箱即用），MVP 阶段不需要为极端场景引入虚拟滚动。蓝烟老师指示"轻量级"，去掉一个依赖库就是最直接的体现。

**修改计划：**
```
ShotWorkbench.vue:
  - 删除 RecycleScroller / DynamicScroller
  - 改用 v-for 遍历当前页数据
  - 底部加 el-pagination（page-size=20）
  - 删除 vue-virtual-scroller 依赖

package.json:
  - 移除 vue-virtual-scroller
```

---

### 阿典 P1-10（4.3）：大量图片/视频的内存管理未考虑

**态度：【接受 — 最简化处理】**

**简化方案：**
- 由于已决定去掉虚拟滚动改用分页，每页最多 20 条，内存问题大幅缓解
- `el-image` 自带 lazy 加载，默认开启即可
- `video` 标签加 `preload="none"`，一行属性搞定
- 不做可见性检测、不做手动内存释放

**理由：** 分页 + lazy 加载 + preload="none" 三管齐下，MVP 阶段完全够用。

**修改计划：**
```
ShotCard.vue:
  <el-image :src="shot.generatedImageUrl" lazy fit="cover" />
  <video v-if="shot.generatedVideoUrl" preload="none" ... />
```

---

### 阿典 P1-11（5.1）：`useWorkflowStatus` composable 声明但未实现

**态度：【接受】**

**简化方案：**
- 与 1.4 联动处理：直接删除 `composables/useWorkflowStatus.ts`
- 轮询功能抽取到 `useWorkflowPolling.ts` 中

**理由：** 从未实现的空文件，直接删除零成本。

---

## 三、修改计划汇总

### 必须修改（P0）

| # | 来源 | 问题 | 修改方案 | 改动量 |
|---|------|------|---------|--------|
| 1 | 老克 A1 | 跨分场查询未使用 | 增加「当前分场/全部镜头」切换按钮 | 小 |
| 2 | 老克 C1 | 51001 排队状态 UI 缺失 | 卡片遮罩增加排队文字提示 | 小 |
| 3 | 阿典 1.1 | 工作流轮询无超时 | 增加 maxWorkflowPolls = 2400 计数器 | 极小 |

### 建议修改（P1）

| # | 来源 | 问题 | 修改方案 | 改动量 |
|---|------|------|---------|--------|
| 4 | 老克 B1 | 40301 处理不一致 | 保持 Toast，加注释说明 MVP 方案 | 极小 |
| 5 | 老克 A2 | getShots 未使用 | 保留 + 加注释 | 极小 |
| 6 | 老克 5.2 | TOS 重试 Toast 重复 | 拦截器中静默处理 retry-presign | 小 |
| 7 | 阿典 1.2 | 轮询无指数退避 | 三档退避：3s → 10s → 30s | 小 |
| 8 | 阿典 1.3 | 轮询缺少回调 | 增加 onDone 单回调 | 小 |
| 9 | 阿典 1.4 | 轮询职责混淆 | 抽取 useWorkflowPolling，删除 useWorkflowStatus | 中 |
| 10 | 阿典 2.1 | complete 失败无重试 | 重试 1 次，失败即报错 | 小 |
| 11 | 阿典 2.2 | extractKeyFromUrl 脆弱 | 增加 decodeURIComponent | 极小 |
| 12 | 阿典 3.1 | 工作流进度无细化展示 | 文字面板 + el-progress | 小 |
| 13 | 阿典 3.2 | 分镜卡片无任务类型展示 | 遮罩文字区分图片/视频生成 | 小 |
| 14 | 阿典 4.1 | RecycleScroller 固定高度问题 | CSS 强制固定高度 280px | 小 |
| 15 | 阿典 4.2 | 虚拟滚动与分页共存 | **去掉虚拟滚动，改用分页** | 中 |
| 16 | 阿典 4.3 | 媒体内存管理 | el-image lazy + video preload="none" | 极小 |
| 17 | 阿典 5.1 | useWorkflowStatus 未实现 | 删除文件 | 极小 |

---

## 四、轻量化原则说明

在以上所有修改中，我始终遵循蓝烟老师的「轻量级、简单高效」原则，具体体现在：

1. **不增加第三方依赖**：去掉 `vue-virtual-scroller`，减少一个依赖
2. **不搞复杂动画**：排队状态用纯文字，不用动画/进度条
3. **不搞复杂重试**：complete 失败只重试 1 次，不做递增延迟
4. **不搞复杂回调**：轮询只加一个 `onDone`，不拆三个回调
5. **不搞复杂退避**：指数退避只分三档，不多级细化
6. **不搞复杂页面**：40301 用 Toast 替代新增页面
7. **不搞复杂内存管理**：分页 + lazy + preload="none" 三行搞定

---

## 五、与评审人的确认事项

以下事项需要与评审人进一步确认：

| # | 事项 | 需确认人 | 说明 |
|---|------|---------|------|
| 1 | 40301 MVP 阶段用 Toast 替代无权限页面 | 老克 | 请确认此方案是否可接受 |
| 2 | `estimatedRemainingSeconds` 仅做简单分钟格式化，不做精确倒计时 | 阿典 | 请确认是否满足 MVP 需求 |
| 3 | 分镜工作台去掉虚拟滚动、改用分页 | 阿典 | 请确认 MVP 阶段是否可接受 |

---

*回复人：小欧 | 日期：2026-04-19*
