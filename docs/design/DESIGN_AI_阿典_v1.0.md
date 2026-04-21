# AI漫剧生产平台 — AI 服务模块详细设计文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 产品名 | AI漫剧生产平台 |
| 模块 | AI 服务模块 (ai-service) |
| 版本 | **v1.2** (代码注释规范+评审修订) |
| 基于 | 后端系分 DESIGN_BACKEND_老克_v1.2 + 前端系分 DESIGN_FRONTEND_小欧_v1.1 |
| 作者 | 阿典（AI 工程师） |
| 评审人 | 蓝烟老师、老克、小欧 |
| 创建日期 | 2026-04-19 |
| 状态 | 待评审 |

**v1.1 变更说明：** 根据老克评审意见修复 5 项问题（Sa-Token 排除、Webhook 异步化、签名校验、错误码修正）。详见 `REVIEW_AI_老克_阿典系分.md`。

---

## 0. 开发规范

### 0.1 代码注释规范 (P0)

**开发时，所有关键行必须有清晰明确的中文注释。** 具体要求：
- **AI 服务调用**：Seedance API 调用需注释端点用途、请求参数和响应处理逻辑。
- **Webhook 处理**：回调签名校验、幂等检查、状态更新需注释安全机制。
- **异步任务**：线程池任务、轮询调度需注释并发场景和超时策略。
- **提示词工程**：Prompt 模板需注释每个占位符的业务含义和注入时机。
- **错误处理**：catch 块需注释异常场景和降级/重试策略。

> 示例：
```java
// Seedance 视频生成任务提交：调用火山引擎 Seedance 2.0 API
// 请求体包含图片 URL + Prompt + 参数配置，返回异步 task_id
SeedanceTaskResult result = seedanceClient.submitVideoTask(prompt, imageUrl);
```

---

## 目录

- [1. 模块架构总览](#1-模块架构总览)
- [2. Seedance 2.0 集成](#2-seedance-20-集成)
  - [2.1 HTTP 客户端封装](#21-http-客户端封装)
  - [2.2 图片生成接口](#22-图片生成接口)
  - [2.3 视频生成接口](#23-视频生成接口)
  - [2.4 任务查询接口](#24-任务查询接口)
  - [2.5 结果下载](#25-结果下载)
  - [2.6 Webhook 回调签名校验](#26-webhook-回调签名校验)
  - [2.7 Webhook 回调接口](#27-webhook-回调接口)
- [3. 提示词工程 (Prompt Engineering)](#3-提示词工程-prompt-engineering)
  - [3.1 分镜 Prompt 生成 (shot_gen)](#31-分镜-prompt-生成-shot_gen)
  - [3.2 角色一致性控制策略](#32-角色一致性控制策略)
  - [3.3 风格预设注入](#33-风格预设注入)
  - [3.4 Prompt 安全与长度控制](#34-prompt-安全与长度控制)
- [4. 资产与一致性保障](#4-资产与一致性保障)
  - [4.1 参考图提取路径](#41-参考图提取路径)
  - [4.2 Reference Image 拼接策略](#42-reference-image-拼接策略)
  - [4.3 一致性降级策略](#43-一致性降级策略)
- [5. 视频合并 (FFmpeg)](#5-视频合并-ffmpeg)
  - [5.1 FFmpeg 命令设计](#51-ffmpeg-命令设计)
  - [5.2 转场效果](#52-转场效果)
  - [5.3 背景音乐叠加](#53-背景音乐叠加)
  - [5.4 黑场处理与片头片尾](#54-黑场处理与片头片尾)
  - [5.5 ExportNodeExecutor 执行流程](#55-exportnodeexecutor-执行流程)
- [6. AI 任务队列与轮询](#6-ai-任务队列与轮询)
  - [6.1 整体架构: Webhook + 慢轮询双通道](#61-整体架构-webhook--慢轮询双通道)
  - [6.2 防重机制](#62-防重机制)
  - [6.3 防漏机制](#63-防漏机制)
  - [6.4 指数退避轮询实现](#64-指数退避轮询实现)
  - [6.5 任务状态机](#65-任务状态机)
- [7. API 消耗记录](#7-api-消耗记录)
  - [7.1 Token 消耗计算](#71-token-消耗计算)
  - [7.2 api_call_log 写入时机](#72-api_call_log-写入时机)
  - [7.3 费用计算模型](#73-费用计算模型)
  - [7.4 聚合查询接口](#74-聚合查询接口)
- [8. 熔断与限流](#8-熔断与限流)
- [9. 配置项清单](#9-配置项清单)
- [10. 开发任务拆分](#10-开发任务拆分)

---

## 1. 模块架构总览

AI 服务模块位于后端系统的核心位置, 负责与 Seedance 2.0 API 交互, 管理 AI 任务生命周期, 保障生成内容的一致性。

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vue3)                             │
│  ShotWorkbench ──── useTaskPolling(三档退避)                  │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP + SSE(Phase2)
┌─────────────────────────▼───────────────────────────────────┐
│                   Backend (Spring Boot)                      │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ WorkflowEngine│  │  ContentMgmt │  │   AssetMgmt      │   │
│  │ (流程状态机)   │  │  (分镜CRUD)   │  │   (参考图管理)    │   │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘   │
│         │                 │                    │              │
│         ▼                 ▼                    ▼              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │              AI Service Module (本模块)                │    │
│  │                                                      │    │
│  │  ┌────────────┐  ┌─────────────┐  ┌───────────────┐  │    │
│  │  │Seedance    │  │ PromptEngine │  │ AssetRef      │  │    │
│  │  │Client      │  │ (提示词工程) │  │ Resolver      │  │    │
│  │  │            │  │             │  │ (参考图解析)   │  │    │
│  │  └─────┬──────┘  └──────┬──────┘  └──────┬────────┘  │    │
│  │        │                 │                │            │    │
│  │  ┌─────▼─────────────────▼────────────────▼────────┐  │    │
│  │  │             AiTaskService                        │  │    │
│  │  │  (任务提交 / 状态管理 / 轮询 / 熔断)              │  │    │
│  │  └────────────────────┬────────────────────────────┘  │    │
│  │                       │                                │    │
│  │  ┌────────────────────▼────────────────────────────┐  │    │
│  │  │           WebhookController                      │  │    │
│  │  │   (火山引擎回调 + HMAC-SHA256签名校验)            │  │    │
│  │  └─────────────────────────────────────────────────┘  │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │ ExportService │  │ ApiCallLog   │                         │
│  │ (FFmpeg合并)  │  │ (消耗记录)    │                         │
│  └──────────────┘  └──────────────┘                         │
└──────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  外部系统: Seedance 2.0 API / 火山 TOS / FFmpeg              │
└──────────────────────────────────────────────────────────────┘
```

**包结构定位（基于后端系分附录 B）：**

```
com.lanyan.aidrama/
├── module/aitask/
│   ├── controller/
│   │   ├── AiTaskController.java          # 查询接口
│   │   └── WebhookController.java         # Webhook 回调入口 (本模块新增)
│   ├── service/
│   │   ├── AiTaskService.java             # 任务管理核心服务
│   │   ├── ExportService.java             # FFmpeg 视频合并服务 (本模块新增)
│   │   ├── PromptEngine.java              # 提示词生成引擎 (本模块新增)
│   │   └── AssetRefResolver.java          # 参考图解析服务 (本模块新增)
│   ├── client/
│   │   ├── SeedanceClient.java            # Seedance API HTTP 封装
│   │   ├── WebhookSignatureVerifier.java  # Webhook 签名校验器 (本模块新增)
│   │   └── dto/                           # 请求/响应 DTO
│   │       ├── ImageGenParams.java
│   │       ├── VideoGenParams.java
│   │       ├── TaskResult.java
│   │       ├── WebhookPayload.java
│   │       └── PromptTemplate.java
│   └── aspect/
│       └── ApiCallLogAspect.java          # API 消耗记录切面
├── module/workflow/executor/
│   └── ExportNodeExecutor.java            # 导出步骤执行器
├── scheduler/
│   └── AiTaskScheduler.java               # 定时轮询调度器
└── aspect/
    ├── ApiCallLogAspect.java              # 统一切面
    └── CircuitBreakerAspect.java          # 熔断切面
```

---

## 2. Seedance 2.0 集成

### 2.1 HTTP 客户端封装

**技术方案：** 使用 Spring Boot `RestTemplate` (或 `WebClient`) 进行 HTTP 调用。考虑到 Seedance 2.0 是异步任务型 API（提交 → 轮询/Webhook 回调获取结果），客户端需支持长连接超时。

```java
@Configuration
public class SeedanceHttpClientConfig {

    @Bean("seedanceRestTemplate")
    public RestTemplate seedanceRestTemplate(
            @Value("${seedance.connect-timeout:10000}") int connectTimeout,
            @Value("${seedance.read-timeout:30000}") int readTimeout) {

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setConnectionRequestTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        return new RestTemplate(factory);
    }
}
```

**配置项：**

```yaml
seedance:
  api-url: https://visual.volcengineapi.com          # 火山引擎 API 地址
  api-key: ${SEEDANCE_API_KEY}
  api-secret: ${SEEDANCE_API_SECRET}                 # 新增：用于 Webhook 签名校验
  webhook-url: ${SEEDANCE_WEBHOOK_URL:https://your-domain.com/api/webhook/seedance}
  connect-timeout: 10000
  read-timeout: 30000
  max-retry: 3                                       # 查询重试次数
  rate-limit-qps: 5                                  # 提交限流 QPS
```

**SeedanceClient 核心实现：**

```java
@Component
public class SeedanceClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    // 所有请求统一携带鉴权 Header
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }

    // 防日志泄露
    @Override
    public String toString() {
        return "SeedanceClient{apiUrl='" + apiUrl + "', apiKey='***'}";
    }
}
```

### 2.2 图片生成接口

**Seedance 2.0 图片生成 API（火山引擎版）：**

```
POST https://visual.volcengineapi.com/?Action=CVProcess&Version=2022-08-31
Content-Type: application/json
Authorization: Bearer {api_key}

{
    "req_key": "high_aes_general_v20_L",
    "prompt": "A young woman with long black hair, wearing a red qipao...",
    "width": 1024,
    "height": 1024,
    "seed": -1,
    "image_urls": ["https://tos-reference-url..."],       // 参考图 URL 数组
    "scale": 0.5                                          // 参考图影响权重
}
```

**Java 封装：**

```java
public String submitImage(ImageGenParams params) {
    String url = apiUrl + "/?Action=CVProcess&Version=2022-08-31";
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(params.toSeedanceRequest(), buildHeaders());

    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
    Map body = response.getBody();

    // 解析火山引擎返回格式
    String taskId = extractTaskId(body);
    if (taskId == null) {
        throw new SeedanceApiException("提交图片生成任务失败: " + extractErrorMessage(body));
    }
    return taskId;
}
```

**ImageGenParams DTO：**

```java
@Data
@Builder
public class ImageGenParams {
    private String reqKey;           // 模型标识, 默认 "high_aes_general_v20_L"
    private String prompt;           // 英文提示词
    private int width;               // 默认 1024
    private int height;              // 默认 1024
    private int seed;                // -1 = 随机
    private List<String> imageUrls;  // 参考图 URL 数组 (Asset 主图)
    private double scale;            // 参考图影响权重, 默认 0.5
}
```

### 2.3 视频生成接口

**Seedance 2.0 视频生成 API（火山引擎版）：**

```
POST https://visual.volcengineapi.com/?Action=CVProcess&Version=2022-08-31
Content-Type: application/json

{
    "req_key": "seedance_1_0_pro_i2v",
    "prompt": "The woman slowly turns her head and looks at the camera...",
    "first_frame_image_url": "https://generated-image-url...",
    "reference_image_urls": ["https://asset-ref-url..."],  // 额外参考图
    "duration": 5,                                         // 视频时长(秒)
    "resolution": "1024x1024",
    "seed": -1,
    "watermark": false
}
```

**Java 封装：**

```java
public String submitVideo(VideoGenParams params) {
    String url = apiUrl + "/?Action=CVProcess&Version=2022-08-31";
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(params.toSeedanceRequest(), buildHeaders());

    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
    String taskId = extractTaskId(response.getBody());
    if (taskId == null) {
        throw new SeedanceApiException("提交视频生成任务失败");
    }
    return taskId;
}
```

**VideoGenParams DTO：**

```java
@Data
@Builder
public class VideoGenParams {
    private String reqKey;               // 默认 "seedance_1_0_pro_i2v"
    private String prompt;               // 英文提示词
    private String firstFrameImageUrl;   // 首帧图 URL (由 image_gen 生成)
    private List<String> referenceImageUrls; // 额外参考图
    private int duration;                // 默认 5 秒
    private String resolution;           // 默认 "1024x1024"
    private int seed;                    // -1 = 随机
    private boolean watermark;           // 默认 false
}
```

### 2.4 任务查询接口

```java
@Retryable(
    value = {SeedanceApiException.class, TimeoutException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 5000, multiplier = 2)
)
public TaskResult queryTask(String providerTaskId) {
    String url = apiUrl + "/?Action=CVGetResult&Version=2022-08-31&task_id=" + providerTaskId;
    HttpEntity<Void> request = new HttpEntity<>(buildHeaders());

    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
    return parseTaskResult(response.getBody());
}
```

**TaskResult DTO：**

```java
@Data
public class TaskResult {
    private String taskId;
    private String status;       // "submitted" | "running" | "done" | "failed"
    private String resultUrl;    // 结果文件 URL (done 时有效)
    private String errorMsg;     // failed 时有效
    private String progress;     // "0%" ~ "100%" (running 时有效)
    private long costTokens;     // Token 消耗量 (done 时有效)
}
```

### 2.5 结果下载

AI 任务完成后, Seedance 返回的结果 URL 通常是临时链接, 需下载到服务器后上传至火山 TOS。

```java
@Component
public class ResultDownloader {

    private final RestTemplate restTemplate;
    private final TosService tosService;

    /**
     * 从 Seedance 结果 URL 下载并上传至 TOS
     * @param resultUrl Seedance 返回的临时结果 URL
     * @param targetKey TOS 目标路径
     * @return TOS 公开访问 URL
     */
    public String downloadAndUploadToTos(String resultUrl, String targetKey) {
        // 1. 下载字节流
        ResponseEntity<byte[]> response = restTemplate.exchange(resultUrl, HttpMethod.GET, null, byte[].class);
        byte[] data = response.getBody();

        // 2. 上传到 TOS
        return tosService.uploadFromBytes(data, targetKey);
    }
}
```

### 2.6 Webhook 回调签名校验

**火山引擎 Webhook 签名算法：**

火山引擎使用 HMAC-SHA256 签名算法。回调请求的 Header 中包含签名信息:

```
X-Content-Sha256: <请求体SHA256>
Authorization: HMAC-SHA256 Credential=<AccessKeyId>, SignedHeaders=<header-list>, Signature=<signature>
```

**签名校验实现：**

```java
@Component
public class WebhookSignatureVerifier {

    @Value("${seedance.api-secret}")
    private String apiSecret;

    /**
     * 校验火山引擎 Webhook 回调签名
     *
     * @param authorizationHeader Authorization Header 值
     * @param requestBody         原始请求体字节
     * @param timestampHeader     可选: 时间戳 (用于防重放)
     * @return 是否校验通过
     */
    public boolean verify(String authorizationHeader, byte[] requestBody, String timestampHeader) {
        try {
            // 1. 解析 Authorization Header (P1 评审修复: 使用正则替代硬分割)
            // 格式: HMAC-SHA256 Credential=AKxxx, SignedHeaders=host;x-content-sha256, Signature=xxx
            // 使用正则匹配，避免 split(", ") 在空格变化时越界崩溃
            java.util.regex.Pattern authPattern = java.util.regex.Pattern.compile(
                "HMAC-SHA256\\s+Credential=(\\S+),\\s*SignedHeaders=([^,]+),\\s*Signature=(\\S+)");
            java.util.regex.Matcher matcher = authPattern.matcher(authorizationHeader);
            if (!matcher.find()) {
                log.error("Invalid Authorization header format: {}", authorizationHeader);
                return false;
            }

            String credential = matcher.group(1);  // AKxxx
            String signedHeaders = matcher.group(2); // host;x-content-sha256
            String providedSignature = matcher.group(3); // xxx

            // 1.5 防重放校验 (P1 评审修复: 启用 5 分钟时间窗口)
            if (!isTimestampValid(timestampHeader)) {
                log.warn("Webhook replay attack detected or invalid timestamp");
                return false;
            }

            // 2. 构建待签名字符串
            // 火山引擎签名格式: StringToSign = HTTPMethod + "\n" + CanonicalURI + "\n" + CanonicalQueryString + "\n" + HashedPayload
            String httpMethod = "POST";
            String canonicalUri = "/";
            String canonicalQueryString = ""; // Webhook 回调通常无 query 参数

            // 3. 计算请求体的 SHA256
            String hashedPayload = sha256Hex(requestBody);

            // 4. 构建 StringToSign
            String stringToSign = httpMethod + "\n" + canonicalUri + "\n" + canonicalQueryString + "\n" + hashedPayload;

            // 5. 计算 HMAC-SHA256 签名
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signatureBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(signatureBytes);

            // 6. 比对签名
            return MessageDigest.isEqual(
                    providedSignature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    private String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        return HexFormat.of().formatHex(hash);
    }
}
```

> **签名算法待验证 (P1 评审备注)：** 当前 StringToSign 构建方式为 `HTTPMethod + "\n" + CanonicalURI + "\n" + CanonicalQueryString + "\n" + HashedPayload`，此为简化实现。火山引擎实际签名可能还需要 Credential 中的日期、区域、服务信息。**需在对接火山引擎时验证实际签名格式**，必要时调整 StringToSign 构建逻辑。

**防重放攻击（已启用）：**

```java
// 校验时间戳窗口 (5分钟内有效)
public boolean isTimestampValid(String timestampHeader) {
    if (timestampHeader == null || timestampHeader.isEmpty()) return true;
    try {
        long ts = Long.parseLong(timestampHeader);
        long now = System.currentTimeMillis() / 1000;
        return Math.abs(now - ts) <= 300; // 5分钟窗口
    } catch (NumberFormatException e) {
        return false;
    }
}
```

### 2.7 Webhook 回调接口

**鉴权排除说明 (P0 评审修复)：**
Webhook 回调接口 `/api/webhook/**` 为第三方火山引擎调用，不需要 Sa-Token 鉴权。
需在后端系分 `SaTokenConfig` (DESIGN_BACKEND_老克_v1.2 行 294-297) 的全局拦截规则中增加排除路径：

```java
SaRouter.match("/**")
    .notMatch("/api/user/login")
    .notMatch("/api/tos/presign")
    .notMatch("/error")
    .notMatch("/api/webhook/**")   // 第三方回调接口不需要鉴权 ← 新增
    .check(r -> StpUtil.checkLogin());
```

> **注意：** 此修改属于后端系分 DESIGN_BACKEND_老克_v1.2 的协同变更，本模块开发前需确认后端已完成此项配置。

**白名单备注：** 生产环境需在网关或应用层配置 Webhook 来源 IP 白名单，仅允许火山引擎官方回调出口访问 `/api/webhook/seedance`，白名单校验先于签名校验执行。

```java
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final AiTaskService aiTaskService;

    @PostMapping("/seedance")
    public Result<Void> handleSeedanceWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String rawBody,
            HttpServletRequest request) {

        byte[] bodyBytes = rawBody.getBytes(StandardCharsets.UTF_8);

        // 1. 签名校验 (P1 评审修复: 增加时间戳防重放)
        String timestamp = request.getHeader("X-Date"); // 火山引擎时间戳 Header
        if (!signatureVerifier.verify(authorization, bodyBytes, timestamp)) {
            log.warn("Webhook signature verification failed from {}", request.getRemoteAddr());
            return Result.fail(51003, "签名校验失败");
        }

        // 2. 解析回调载荷
        WebhookPayload payload = parsePayload(rawBody);

        // 3. 异步处理回调 (P1 评审修复: 避免阻塞 Tomcat 工作线程)
        aiTaskService.handleWebhookCallback(payload);

        return Result.ok(null);
    }
}
```

**WebhookPayload DTO：**

```java
@Data
public class WebhookPayload {
    private String taskId;       // provider_task_id
    private String status;       // "done" / "failed"
    private String resultUrl;    // done 时的结果 URL
    private String errorMsg;     // failed 时的错误信息
    private long costTokens;     // Token 消耗
    private long callbackTime;   // 回调时间戳
}
```

---

## 3. 提示词工程 (Prompt Engineering)

### 3.1 分镜 Prompt 生成 (shot_gen)

**核心目标：** 将中文小说文本转换为符合 Seedance 2.0 要求的英文分镜提示词, 同时保证角色外观一致性。

**ShotGenNodeExecutor 流程：**

```
1. 读取 scene.content (中文分场描述)
2. 读取关联的 asset 信息 (角色/场景/物品的 reference_images[0])
3. 调用 PromptEngine 生成 prompt_en
4. 创建 shot 记录, 保存 prompt + prompt_en
```

**PromptEngine 核心实现：**

```java
@Component
public class PromptEngine {

    private final RestTemplate llmRestTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 生成分镜英文提示词
     *
     * @param sceneContent  分场中文描述
     * @param characters    本分场涉及的角色资产列表
     * @param scenes        本分场涉及的场景资产列表
     * @param stylePreset   全局风格预设
     * @return PromptTemplate (含 prompt, prompt_en, shot 列表)
     */
    public PromptTemplate generateShotPrompts(
            String sceneContent,
            List<AssetVO> characters,
            List<AssetVO> scenes,
            StylePreset stylePreset) {
        // 构建系统 Prompt + 用户 Prompt
        String systemPrompt = buildSystemPrompt(stylePreset);
        String userPrompt = buildUserPrompt(sceneContent, characters, scenes);

        // 调用强模型 (如 GPT-4o / DeepSeek) 生成
        String llmResponse = callLLM(systemPrompt, userPrompt);

        // 解析 JSON 返回
        return objectMapper.readValue(llmResponse, PromptTemplate.class);
    }
}
```

**系统 Prompt 模板：**

```
You are a professional storyboard artist and prompt engineer for AI video generation.
Your task is to convert a Chinese scene description into a sequence of shot-level prompts
suitable for Seedance 2.0 video generation model.

OUTPUT REQUIREMENTS:
1. Return JSON only, no additional text.
2. Each shot must have:
   - shot_number: sequential number (1, 2, 3...)
   - prompt: Chinese description of the shot
   - prompt_en: English translation optimized for AI video generation
   - camera_angle: camera perspective (close-up/medium-shot/wide-shot/over-the-shoulder/etc.)
   - characters: list of character names appearing in this shot
   - duration_estimate: estimated duration in seconds (3-8)

PROMPT_EN WRITING RULES:
1. Use present tense, active voice.
2. Describe ONLY what is visually visible.
3. Include character appearance details when characters first appear:
   "A young woman with long black hair, wearing a traditional red qipao, ..."
4. For recurring characters, use concise reference:
   "The woman in the red qipao walks toward the door..."
5. Specify camera movement: "The camera slowly zooms in on...", "A wide shot reveals..."
6. Specify lighting and atmosphere: "warm sunset lighting", "dim candlelit room"
7. Avoid abstract concepts, internal thoughts, or dialogue without visual context.
8. Keep each prompt_en between 80-200 words.
9. Use concrete, specific visual descriptors.

STYLE: {artStyle} {colorTone}
```

**用户 Prompt 模板：**

```
SCENE DESCRIPTION:
{scene_content}

CHARACTER ASSETS IN THIS SCENE:
{
  "characters": [
    {"name": "林婉儿", "description": "18岁少女, 黑色长发, 红色旗袍, 手持折扇"},
    {"name": "张将军", "description": "40岁中年男子, 短胡须, 身穿银色铠甲"}
  ],
  "scenes": [
    {"name": "王府花园", "description": "中式园林, 假山, 竹林, 石板路"}
  ]
}

Convert the scene description into 3-8 sequential shots.
Each shot should represent a distinct visual moment that advances the story.
```

**LLM 返回格式 (PromptTemplate)：**

```java
@Data
public class PromptTemplate {
    private List<ShotPrompt> shots;

    @Data
    public static class ShotPrompt {
        private int shotNumber;
        private String prompt;       // 中文描述
        private String promptEn;     // 英文 Seedance 提示词
        private String cameraAngle;  // 镜头角度
        private List<String> characters; // 出现的角色名
        private int durationEstimate;    // 预估时长(秒)
    }
}
```

**LLM 调用配置：**

```yaml
prompt-engine:
  llm-api-url: ${LLM_API_URL:https://api.deepseek.com/v1}
  llm-api-key: ${LLM_API_KEY}
  llm-model: deepseek-chat                  # 或 gpt-4o
  max-tokens: 4096
  temperature: 0.7
  timeout: 30000
```

### 3.2 角色一致性控制策略

**问题：** Seedance 2.0 在不同分镜中生成同一角色时, 外观可能不一致。

**解决方案：Reference Image 锚定 + 描述一致性。**

```
┌──────────────────────────────────────────────┐
│           角色一致性控制策略                    │
│                                              │
│  策略1: 参考图锚定                             │
│  ─────────────────                           │
│  每个角色 Asset 有 reference_images 数组       │
│  → 取第1张作为"主参考图"(primary)              │
│  → 传给 Seedance 的 reference_images 参数      │
│  → scale=0.5 控制参考图影响权重                │
│                                              │
│  策略2: Prompt 描述一致性                      │
│  ──────────────────                          │
│  LLM 生成 prompt_en 时, 首次出现角色使用完整     │
│  外观描述, 后续出现使用固定短语引用              │
│  → 首次: "A young woman with long black hair, │
│          wearing a red qipao..."              │
│  → 后续: "The woman in the red qipao..."      │
│                                              │
│  策略3: 首帧图复用 (video_gen)                │
│  ─────────────────                            │
│  video_gen 的 first_frame_image 使用           │
│  该 shot 的 generated_image_url               │
│  确保视频从正确的角色外观开始                   │
│                                              │
│  策略4: Seed 固定 (同场景批量)                 │
│  ─────────────                               │
│  同一分场内连续生成的 shot 使用相同 seed        │
│  减少风格波动                                 │
└──────────────────────────────────────────────┘
```

**Reference Image 在 Seedance 调用中的具体位置：**

```java
// ImageGenParams 构建示例
public ImageGenParams buildImageGenParams(Shot shot, List<AssetVO> assets) {
    // 提取所有关联资产的主参考图
    List<String> refImageUrls = assets.stream()
            .filter(a -> a.getReferenceImages() != null && !a.getReferenceImages().isEmpty())
            .map(a -> a.getReferenceImages().get(0))  // 取第一张主图
            .toList();

    return ImageGenParams.builder()
            .prompt(shot.getPromptEn())
            .imageUrls(refImageUrls)
            .scale(0.5)          // 参考图影响权重
            .width(1024)
            .height(1024)
            .seed(-1)            // 随机 seed
            .build();
}
```

### 3.3 风格预设注入

风格预设在两个层级注入：

1. **系统 Prompt 层级（LLM 生成 prompt_en 时）：**

```
STYLE: {artStyle} {colorTone}
Example: STYLE: anime style, warm golden hour tones
```

2. **Seedance 调用层级：**

```java
// 将风格预设追加到 prompt_en 末尾
String enhancedPrompt = shot.getPromptEn() +
    " Art style: " + stylePreset.getArtStyle() +
    ". Color tone: " + stylePreset.getColorTone() + ".";
```

### 3.4 Prompt 安全与长度控制

| 约束项 | 规则 | 处理方式 |
|--------|------|---------|
| 最大长度 | prompt_en ≤ 500 字符 | 截断并添加 "..." |
| 敏感词过滤 | 暴力/色情/政治敏感 | 调用内容安全 API 检测 |
| 空 Prompt | prompt_en 为空或全空白 | 标记 shot 状态为 4(打回), 记录 error_msg |
| 语言检测 | prompt_en 必须为英文 | LLM 输出后检测, 非英文则重试生成 |

---

## 4. 资产与一致性保障

### 4.1 参考图提取路径

```java
@Component
public class AssetRefResolver {

    private final ShotAssetRefMapper refMapper;
    private final AssetMapper assetMapper;

    /**
     * 为指定分镜解析参考图列表
     *
     * 流程:
     * 1. 通过 shot_asset_ref 表查询该分镜绑定的所有资产
     * 2. 按 asset_type 分组 (character 优先)
     * 3. 取每个资产的 reference_images[0] 作为主图
     * 4. 返回 URL 列表, 按优先级排序: character > scene > prop
     *
     * @param shotId 分镜ID
     * @return 参考图 URL 列表 (最多 4 张, 超出则截断)
     */
    public List<String> resolveRefImages(Long shotId) {
        // 1. 查询关联
        List<ShotAssetRef> refs = refMapper.selectByShotId(shotId);

        // 2. 按类型优先级排序: character > scene > prop
        List<ShotAssetRef> sorted = refs.stream()
                .sorted(Comparator.comparingInt(r -> switch (r.getAssetType()) {
                    case "character" -> 1;
                    case "scene"     -> 2;
                    case "prop"      -> 3;
                    default          -> 4;
                }))
                .toList();

        // 3. 提取主参考图
        List<String> refUrls = new ArrayList<>();
        for (ShotAssetRef ref : sorted) {
            Asset asset = assetMapper.selectById(ref.getAssetId());
            if (asset != null && asset.getReferenceImages() != null
                    && !asset.getReferenceImages().isEmpty()) {
                refUrls.add(asset.getReferenceImages().get(0));
            }
            if (refUrls.size() >= 4) break;  // 最多 4 张参考图
        }
        return refUrls;
    }

    /**
     * 批量为多个分镜预加载参考图 (减少 N+1 查询)
     */
    public Map<Long, List<String>> batchResolveRefImages(List<Long> shotIds) {
        // 批量查询 shot_asset_ref + asset, 内存中组装
        // 返回: shotId -> refImageUrls
    }
}
```

### 4.2 Reference Image 拼接策略

**不同生成阶段的参考图使用策略：**

| 生成阶段 | 使用的参考图 | 用途 |
|----------|-------------|------|
| **image_gen** | 所有关联资产的主图 | Seedance 的 `image_urls` 参数, 控制生成结果的外观 |
| **video_gen** | 该 shot 的 `generated_image_url` (首帧) + 角色资产主图 | `first_frame_image_url` + `reference_image_urls` |
| **shot_gen** (LLM) | 资产的 description 文本 | 注入 LLM Prompt, 让 LLM 知道角色外观描述 |

**VideoGen 参数构建：**

```java
public VideoGenParams buildVideoGenParams(Shot shot, List<AssetVO> assets) {
    // 首帧图: 必须是该 shot 之前生成的图片
    String firstFrameUrl = shot.getGeneratedImageUrl();
    if (firstFrameUrl == null) {
        throw new BusinessException(51002, "分镜首帧图不存在, 无法生成视频");
    }

    // 额外参考图: 角色资产的主图 (排除首帧图已覆盖的内容)
    List<String> extraRefs = assets.stream()
            .filter(a -> "character".equals(a.getAssetType()))
            .filter(a -> a.getReferenceImages() != null && !a.getReferenceImages().isEmpty())
            .map(a -> a.getReferenceImages().get(0))
            .filter(url -> !url.equals(firstFrameUrl))
            .limit(3)  // 额外最多 3 张
            .toList();

    return VideoGenParams.builder()
            .prompt(shot.getPromptEn())
            .firstFrameImageUrl(firstFrameUrl)
            .referenceImageUrls(extraRefs)
            .duration(5)
            .resolution("1024x1024")
            .build();
}
```

### 4.3 一致性降级策略

当参考图不足或生成质量不佳时, 采用降级策略：

```
┌───────────────────────────────────────────────┐
│  一致性降级策略                                 │
│                                               │
│  Level 1 (正常):                              │
│  有角色参考图 + 场景参考图                      │
│  → 全量传入 Seedance                           │
│                                               │
│  Level 2 (部分降级):                           │
│  只有角色参考图, 无场景参考图                    │
│  → 仅传角色参考图, prompt_en 中加强场景描述     │
│                                               │
│  Level 3 (严重降级):                           │
│  无任何参考图                                  │
│  → 纯 prompt_en 驱动, 记录 warning 日志         │
│  → 在 shot.review_comment 中标注"缺少参考图"   │
│                                               │
│  Level 4 (生成失败):                           │
│  Seedance 返回结果质量不达标                    │
│  → 记录 error, shot 标记为"待审核(打回)"        │
│  → generation_attempts++                      │
│  → 最多重试 3 次, 超出则标记为失败             │
└───────────────────────────────────────────────┘
```

---

## 5. 视频合并 (FFmpeg)

### 5.1 FFmpeg 命令设计

ExportNodeExecutor 负责将所有已通过审核的分镜视频拼接为最终成品。

**基础拼接命令：**

```bash
# 1. 生成文件列表 (按 sort_order 排序)
cat > filelist.txt << EOF
file 'shot_1.mp4'
file 'shot_2.mp4'
file 'shot_3.mp4'
EOF

# 2. 简单拼接 (无转场)
ffmpeg -y -f concat -safe 0 -i filelist.txt \
  -c copy \
  output_merged.mp4
```

**带转场的拼接命令 (xfade filter)：**

```bash
# 带交叉淡入淡出转场 (1秒)
ffmpeg -y \
  -i shot_1.mp4 \
  -i shot_2.mp4 \
  -i shot_3.mp4 \
  -filter_complex "
    [0:v][1:v]xfade=transition=fade:duration=1:offset=4[x1];
    [x1][2:v]xfade=transition=fade:duration=1:offset=8[outv];
    [0:a][1:a]acrossfade=d=1[a1];
    [a1][2:a]acrossfade=d=1[outa]
  " \
  -map "[outv]" -map "[outa]" \
  -c:v libx264 -preset medium -crf 23 \
  -c:a aac -b:a 128k \
  output_with_transitions.mp4
```

### 5.2 转场效果

**支持的转场类型：**

| 转场类型 | FFmpeg filter | 适用场景 |
|---------|--------------|---------|
| 淡入淡出 | `fade` | 通用场景切换 |
| 平滑过渡 | `smoothleft` / `smoothright` | 角色移动方向一致时 |
| 擦除 | `wipeleft` / `wiperight` | 时空转换 |
| 溶解 | `dissolve` | 回忆/梦境场景 |
| 无转场 | `concat` | 快速剪辑 / 动作场景 |

**转场配置（通过 workflow_config 传入）：**

```java
@Data
public class ExportConfig {
    private String transitionType = "fade";       // 转场类型
    private double transitionDuration = 1.0;      // 转场时长(秒)
    private boolean addBgm = true;                // 是否添加背景音乐
    private String bgmTosPath;                    // 背景音乐 TOS 路径
    private double bgmVolume = 0.3;               // 背景音乐音量 (0.0-1.0)
    private boolean addBlackLeader = true;        // 是否加片头黑场
    private int blackLeaderDuration = 2;          // 片头黑场时长(秒)
    private boolean addBlackTrailer = true;       // 是否加片尾黑场
    private int blackTrailerDuration = 3;         // 片尾黑场时长(秒)
    private String outputResolution = "1920x1080"; // 输出分辨率
    private String outputFormat = "mp4";           // 输出格式
}
```

### 5.3 背景音乐叠加

```bash
# 背景音乐叠加命令
ffmpeg -y \
  -i merged_video.mp4 \
  -i bgm.mp3 \
  -filter_complex "
    [1:a]volume=0.3[bgm];
    [0:a][bgm]amix=inputs=2:duration=first:dropout_transition=2[outa]
  " \
  -map 0:v -map "[outa]" \
  -c:v libx264 -preset medium -crf 23 \
  -c:a aac -b:a 128k \
  output_with_bgm.mp4
```

**关键参数说明：**
- `volume=0.3`：BGM 音量降低到 30%, 避免盖过原视频声音
- `amix`：混音, `duration=first` 以第一个音频流(原视频)长度为准
- `dropout_transition=2`：淡出过渡 2 秒

### 5.4 黑场处理与片头片尾

```bash
# 添加片头黑场 (2秒, 带淡入)
ffmpeg -y \
  -f lavfi -i color=c=black:s=1920x1080:r=30:d=2 \
  -i merged.mp4 \
  -filter_complex "
    [0:v]fade=t=in:st=0:d=1[leader];
    [leader][1:v]concat=n=2:v=1:a=0[outv];
    [1:a]adelay=2000|2000[a]
  " \
  -map "[outv]" -map "[a]" \
  -c:v libx264 -preset medium -crf 23 \
  -c:a aac -b:a 128k \
  output_with_leader.mp4
```

### 5.5 ExportNodeExecutor 执行流程

```java
@Component
public class ExportNodeExecutor implements NodeExecutor {

    private final ShotMapper shotMapper;
    private final ProjectMapper projectMapper;
    private final TosService tosService;
    private final FfmpegExecutor ffmpegExecutor;

    @Override
    public String getStepType() {
        return "export";
    }

    @Override
    public NodeResult execute(Long projectId, Long episodeId, StepConfig config) {
        ExportConfig exportConfig = parseConfig(config);

        // 1. 查询该分集下所有 status=3(已通过) 的 shot
        List<Shot> approvedShots = shotMapper.selectApprovedShots(episodeId);
        if (approvedShots.isEmpty()) {
            throw new BusinessException(51002, "没有已通过的分镜, 无法导出");
        }

        // 2. 按 sort_order 排序
        approvedShots.sort(Comparator.comparingInt(Shot::getSortOrder));

        // 3. 下载所有视频到本地临时目录
        String tempDir = createTempDir(projectId, episodeId);
        List<String> localPaths = new ArrayList<>();
        for (Shot shot : approvedShots) {
            if (shot.getGeneratedVideoUrl() == null) continue;
            String localPath = downloadToTemp(shot.getGeneratedVideoUrl(), tempDir,
                    "shot_" + shot.getSortOrder() + ".mp4");
            localPaths.add(localPath);
        }

        // 4. 构建 FFmpeg 命令
        String outputTempPath = tempDir + "/final_output.mp4";
        String ffmpegCmd = buildFfmpegCommand(localPaths, exportConfig, outputTempPath);

        // 5. 执行 FFmpeg
        ffmpegExecutor.execute(ffmpegCmd);

        // 6. 上传结果到 TOS
        String timestamp = String.valueOf(System.currentTimeMillis());
        String tosKey = String.format("projects/%d/output/final_%d_%s.mp4",
                projectId, projectId, timestamp);
        String publicUrl = tosService.uploadFromPath(outputTempPath, tosKey);

        // 7. 清理临时文件
        cleanupTempDir(tempDir);

        // 8. 更新项目状态
        projectMapper.update(projectId, Project.builder()
                .status(2)  // 已完成
                .build());

        return NodeResult.builder()
                .success(true)
                .outputData(Map.of("finalVideoUrl", publicUrl))
                .build();
    }
}
```

**FFmpeg 执行封装：**

```java
@Component
@Slf4j
public class FfmpegExecutor {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffmpeg.timeout:600}")  // 默认 10 分钟超时
    private int timeoutSeconds;

    public void execute(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, command.split(" "));
            pb.redirectErrorStream(true);  // 合并 stdout 和 stderr

            Process process = pb.start();

            // 读取日志输出
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[FFmpeg] {}", line);
            }

            // 等待完成, 带超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(51001, "FFmpeg 执行超时(" + timeoutSeconds + "s)");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new BusinessException(51001, "FFmpeg 执行失败, exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new BusinessException(51001, "FFmpeg 执行异常: " + e.getMessage());
        }
    }
}
```

---

## 6. AI 任务队列与轮询

### 6.1 整体架构: Webhook + 慢轮询双通道

```
┌─────────────────────────────────────────────────────────────┐
│                    AI 任务状态同步双通道                      │
│                                                             │
│  通道1: Webhook 回调 (主通道)                                │
│  ─────────────────────────────────                          │
│  Seedance 任务完成 → 主动回调 → WebhookController            │
│  优点: 实时性高, 无延迟                                      │
│  缺点: 依赖网络可达性, 可能丢包                              │
│                                                             │
│  通道2: 慢轮询 (兜底通道)                                    │
│  ────────────────                                          │
│  @Scheduled 定时任务 → 查 ai_task 表 → 主动查询 Seedance     │
│  优点: 不依赖回调可达性                                      │
│  缺点: 有延迟, 增加 API 调用量                               │
│                                                             │
│  两条通道互不冲突, 状态以数据库为准                           │
│  先到达的结果写入 DB, 后到达的同幂等忽略                       │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 防重机制

**Webhook 回调防重：**

```java
    @Async("aiTaskExecutor")  // P1 评审修复: 指定线程池, 避免阻塞 Tomcat 工作线程
    public void handleWebhookCallback(WebhookPayload payload) {
    String lockKey = "webhook:duplicate:" + payload.getTaskId();

    // 使用 Redis 分布式锁防并发重复处理
    RLock lock = redissonClient.getLock(lockKey);
    try {
        // 尝试获取锁, 不等待
        if (!lock.tryLock(0, 10, TimeUnit.SECONDS)) {
            log.warn("Webhook callback already being processed for taskId={}", payload.getTaskId());
            return;
        }

        // 幂等检查: 查询 ai_task 当前状态
        AiTask existingTask = aiTaskMapper.selectByProviderTaskId(payload.getTaskId());
        if (existingTask == null) {
            log.error("AiTask not found for providerTaskId={}", payload.getTaskId());
            return;
        }

        // 如果任务已经是终态 (成功/失败), 忽略重复回调
        if (existingTask.getStatus() >= 2) {
            log.info("AiTask already in terminal state, ignoring duplicate webhook. " +
                     "taskId={}, currentStatus={}", payload.getTaskId(), existingTask.getStatus());
            return;
        }

        // 更新任务状态
        updateTaskFromWebhook(existingTask, payload);

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

**轮询防重：**

```java
@Scheduled(fixedDelay = 5000, scheduler = "schedulerExecutor")
public void pollAiTaskResults() {
    // 只查询 next_poll_time 已到期的任务
    List<AiTask> dueTasks = aiTaskMapper.selectDueTasks(LocalDateTime.now(), 50);

    for (AiTask task : dueTasks) {
        String lockKey = "poll:lock:" + task.getId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 获取锁, 不等待 — 防止同一任务被多个调度器实例同时轮询
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                continue;
            }

            // 双重检查: 任务是否已被 Webhook 更新为终态
            AiTask fresh = aiTaskMapper.selectById(task.getId());
            if (fresh.getStatus() >= 2) {
                continue;  // 已被 Webhook 处理, 跳过轮询
            }

            // 执行轮询
            pollSingleTask(fresh);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 6.3 防漏机制

**漏检场景与对策：**

| 漏检场景 | 原因 | 对策 |
|---------|------|------|
| Webhook 回调丢失 | 网络中断/回调地址不可达 | 慢轮询兜底, 指数退避 |
| 任务提交成功但 provider_task_id 未记录 | 数据库写入失败 | 提交前本地预生成 correlation_id |
| 轮询调度器宕机 | 服务重启 | RecoveryRunner 恢复机制, 基于 sub_step 断点续跑 |
| 任务超过 poll_count 上限 | 超时未返回 | poll_count > 30 标记失败 + 告警 |

**RecoveryRunner 恢复逻辑（基于后端系分 5.4 增强）：**

```java
@Component
public class RecoveryRunner {

    /**
     * 服务启动时扫描未完成的 AI 任务
     */
    @PostConstruct
    public void recoverPendingTasks() {
        // 1. 查询 status=0(提交中) 或 status=1(处理中) 且 next_poll_time 过期的任务
        List<AiTask> pendingTasks = aiTaskMapper.selectPendingTasks();

        for (AiTask task : pendingTasks) {
            // 2. 通过 provider_task_id 向 Seedance 查询最新状态
            try {
                TaskResult result = seedanceClient.queryTask(task.getProviderTaskId());

                switch (result.getStatus()) {
                    case "done":
                        // 任务在宕机期间已完成, 直接走下载+上传流程
                        handleCompletedTask(task, result);
                        break;
                    case "running":
                    case "submitted":
                        // 任务仍在处理中, 重置 next_poll_time 继续轮询
                        resetPollTime(task);
                        break;
                    case "failed":
                        // 任务已失败, 更新状态
                        markTaskFailed(task, result.getErrorMsg());
                        break;
                }
            } catch (Exception e) {
                log.error("Failed to recover task id={}", task.getId(), e);
                // 查询失败, 标记为需人工检查
                markTaskNeedsReview(task);
            }
        }
    }
}
```

### 6.4 指数退避轮询实现

**后端轮询策略（与前端三档退避对齐）：**

```java
public class PollBackoffStrategy {

    // 退避间隔表: poll_count → 下次轮询延迟(秒)
    private static final int[] BACKOFF_SECONDS = {
        // 前 3 次: 每 3s 查一次 (即时反馈)
        3, 3, 3,
        // 第 4-10 次: 每 10s 查一次
        10, 10, 10, 10, 10, 10, 10,
        // 第 11-30 次: 每 30s 查一次 (慢轮询)
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30
    };

    public static int getNextPollDelay(int pollCount) {
        if (pollCount < 0 || pollCount >= BACKOFF_SECONDS.length) {
            return 30;  // 兜底 30s
        }
        return BACKOFF_SECONDS[pollCount];
    }

    public static int getMaxPollCount() {
        return BACKOFF_SECONDS.length;  // 30 次
    }
}
```

**轮询单任务处理：**

```java
private void pollSingleTask(AiTask task) {
    try {
        TaskResult result = seedanceClient.queryTask(task.getProviderTaskId());

        // 更新轮询统计
        task.setLastPollTime(LocalDateTime.now());
        task.setPollCount(task.getPollCount() + 1);

        switch (result.getStatus()) {
            case "done":
                // 任务成功
                task.setStatus(2);
                task.setResultUrl(result.getResultUrl());
                task.setNextPollTime(null);  // 不再轮询

                // 异步下载并上传 TOS
                downloadAndUploadAsync(task, result.getResultUrl());

                // 写入 API 消耗记录
                recordApiCall(task, result);
                break;

            case "failed":
                task.setStatus(3);
                task.setErrorMsg(result.getErrorMsg());
                task.setNextPollTime(null);
                break;

            case "running":
            case "submitted":
                // 仍在处理, 设置下次轮询时间
                int delay = PollBackoffStrategy.getNextPollDelay(task.getPollCount());
                task.setNextPollTime(LocalDateTime.now().plusSeconds(delay));

                // 检查是否超时
                if (task.getPollCount() > PollBackoffStrategy.getMaxPollCount()) {
                    task.setStatus(3);
                    task.setErrorMsg("AI任务轮询超时(" + task.getPollCount() + "次)");
                    task.setNextPollTime(null);
                    log.warn("Task poll timeout: id={}", task.getId());
                }
                break;
        }

        aiTaskMapper.updateById(task);

        // 同步更新 Redis 缓存
        updateRedisCache(task);

    } catch (SeedanceApiException e) {
        // API 调用失败, 延迟重试
        int delay = PollBackoffStrategy.getNextPollDelay(task.getPollCount());
        task.setNextPollTime(LocalDateTime.now().plusSeconds(delay));
        aiTaskMapper.updateById(task);
    }
}
```

### 6.5 任务状态机

```
                         submitImageGenTask()
                              │
                              ▼
                    ┌─────────────────┐
                    │  0-提交中         │
                    │  (Submitting)    │
                    └────────┬────────┘
                             │
                             │ @Async 提交到 Seedance
                             │ 获取 provider_task_id
                             ▼
                    ┌─────────────────┐
              ┌─────│  1-处理中         │─────┐
              │     │  (Processing)    │     │
              │     └────────┬────────┘     │
              │              │              │
              │   Webhook 回调│ 或           │ 轮询
              │   或 轮询     │              │ 发现
              │   成功        ▼              │ 失败
              │     ┌─────────────────┐     │
              │     │  2-成功          │     │
              │     │  (Success)       │     │
              │     └─────────────────┘     │
              │              │              │
              │              ▼              │
              │     下载 → 上传 TOS          │
              │     更新 shot 状态           │
              │                             │
              │              ┌──────────────┘
              │              ▼
              │     ┌─────────────────┐
              └─────│  3-失败          │
                    │  (Failed)        │
                    └─────────────────┘
                            │
                            │ 更新 shot.generation_attempts++
                            │ 若 attempts < 3 可重试
                            ▼
                    ┌─────────────────┐
                    │  回到 0-提交中    │ (重试)
                    └─────────────────┘
```

**状态流转规则：**

| 当前状态 | 触发事件 | 下一状态 | 操作 |
|---------|---------|---------|------|
| 0-提交中 | 成功提交到 Seedance | 1-处理中 | 记录 provider_task_id, 设置 next_poll_time |
| 0-提交中 | 提交失败 | 3-失败 | 记录 error_msg |
| 1-处理中 | Webhook/轮询返回 done | 2-成功 | 下载结果, 上传 TOS, 更新 shot |
| 1-处理中 | Webhook/轮询返回 failed | 3-失败 | 记录 error_msg |
| 1-处理中 | poll_count > 30 | 3-失败 | 超时标记 |
| 2-成功 | — | 终态 | 不再变化 |
| 3-失败 | generation_attempts < 3 | 0-提交中 | 可重试 |
| 3-失败 | generation_attempts >= 3 | 终态 | 不再变化 |

---

## 7. API 消耗记录

### 7.1 Token 消耗计算

Seedance 2.0 API 的 Token 消耗模型：

| API 类型 | Token 计算方式 | 说明 |
|---------|---------------|------|
| **图片生成** | 按分辨率计费 | 1024x1024 = 1 unit, 不同分辨率有不同单价 |
| **视频生成** | 按时长+分辨率计费 | 5s/1024x1024 = 1 unit |
| **LLM Prompt 生成** | 按输入/输出 Token 数 | input_tokens + output_tokens |

**Token 消耗获取方式：**

1. **Seedance 回调/查询结果中包含 `cost_tokens` 字段：**
   ```java
   // TaskResult 中直接读取
   long costTokens = result.getCostTokens();
   ```

2. **LLM 调用通过响应 Header 获取：**
   ```java
   // OpenAI 兼容格式
   long promptTokens = response.getHeaders().getFirst("X-Prompt-Tokens");
   long completionTokens = response.getHeaders().getFirst("X-Completion-Tokens");
   long totalTokens = promptTokens + completionTokens;
   ```

### 7.2 api_call_log 写入时机

**统一写入时机（通过 AOP 切面 + 手动写入结合）：**

```java
@Aspect
@Component
public class ApiCallLogAspect {

    private final ApiCallLogMapper logMapper;
    private final AiTaskService aiTaskService;

    /**
     * Seedance 任务完成时记录 (由 AiTaskService 调用)
     */
    public void recordSeedanceCall(ApiCallLog log) {
        logMapper.insert(log);
    }

    /**
     * LLM 调用完成后记录 (由 PromptEngine 调用)
     */
    @AfterReturning(
        pointcut = "execution(* com.lanyan.aidrama.module.aitask.service.PromptEngine.callLLM(..))",
        returning = "result"
    )
    public void recordLlmCall(JoinPoint joinPoint, Object result) {
        Object[] args = joinPoint.getArgs();
        String sceneContent = (String) args[0];

        // 估算 Token 数 (中文字符 ≈ 1.5 tokens, 英文单词 ≈ 1.3 tokens)
        int estimatedTokens = estimateTokenCount(sceneContent) + estimateTokenCount(result.toString());

        ApiCallLog log = ApiCallLog.builder()
                .userId(getCurrentUserId())
                .projectId(extractProjectId(joinPoint))
                .apiProvider("llm")
                .apiEndpoint("chat.completions")
                .requestParams(maskSensitiveData(sceneContent))
                .tokenUsage(estimatedTokens)
                .cost(calculateLlmCost(estimatedTokens))
                .status(1)
                .build();

        logMapper.insert(log);
    }

    /**
     * 记录失败的 API 调用
     */
    @AfterThrowing(
        pointcut = "execution(* com.lanyan.aidrama.module.aitask.client.SeedanceClient.*(..))",
        throwing = "ex"
    )
    public void recordFailedCall(JoinPoint joinPoint, Exception ex) {
        ApiCallLog log = ApiCallLog.builder()
                .userId(getCurrentUserId())
                .projectId(extractProjectId(joinPoint))
                .apiProvider("seedance")
                .apiEndpoint(joinPoint.getSignature().getName())
                .requestParams("{}")
                .tokenUsage(0)
                .cost(BigDecimal.ZERO)
                .status(0)  // 失败
                .build();

        logMapper.insert(log);
    }
}
```

**AiTaskService 中任务完成时的精确记录：**

```java
private void recordApiCall(AiTask task, TaskResult result) {
    // 从项目关联获取 user_id
    Project project = projectMapper.selectById(task.getProjectId());

    ApiCallLog log = ApiCallLog.builder()
            .userId(project.getUserId())
            .projectId(task.getProjectId())
            .apiProvider("seedance")
            .apiEndpoint(task.getTaskType().equals("image_gen")
                    ? "CVProcess:image" : "CVProcess:video")
            .requestParams(buildRequestSummary(task))
            .tokenUsage(result.getCostTokens())
            .cost(calculateSeedanceCost(task.getTaskType(), result.getCostTokens()))
            .status(1)  // 成功
            .build();

    apiCallLogMapper.insert(log);
}
```

### 7.3 费用计算模型

```java
@Component
public class CostCalculator {

    // Seedance 价格 (示例, 实际以火山引擎定价为准)
    private static final BigDecimal IMAGE_GEN_PRICE = new BigDecimal("0.04");   // 每张 0.04 元
    private static final BigDecimal VIDEO_GEN_PRICE = new BigDecimal("0.20");   // 每 5s 视频 0.20 元

    // LLM 价格 (DeepSeek 示例)
    private static final BigDecimal LLM_INPUT_PRICE_PER_K = new BigDecimal("0.001");   // 每千 Token 0.001 元
    private static final BigDecimal LLM_OUTPUT_PRICE_PER_K = new BigDecimal("0.004");  // 每千 Token 0.004 元

    public BigDecimal calculateSeedanceCost(String taskType, long costTokens) {
        return taskType.equals("image_gen") ? IMAGE_GEN_PRICE : VIDEO_GEN_PRICE;
    }

    public BigDecimal calculateLlmCost(int inputTokens, int outputTokens) {
        BigDecimal inputCost = new BigDecimal(inputTokens)
                .divide(BigDecimal.valueOf(1000))
                .multiply(LLM_INPUT_PRICE_PER_K);
        BigDecimal outputCost = new BigDecimal(outputTokens)
                .divide(BigDecimal.valueOf(1000))
                .multiply(LLM_OUTPUT_PRICE_PER_K);
        return inputCost.add(outputCost);
    }
}
```

### 7.4 聚合查询接口

**前端 API 消耗看板需要的接口：**

```
GET /api/ai/cost-report
查询参数:
  - startDate: string (可选, ISO-8601, 默认30天前)
  - endDate: string (可选, ISO-8601, 默认今天)
  - projectId: long (可选, 按项目过滤)

响应体 (匹配前端 ApiCostReport 类型):
{
  "code": 0,
  "data": {
    "totalCalls": 1250,
    "totalCost": 186.4500,
    "avgCostPerCall": 0.1492,
    "failRate": 0.032,
    "dailyStats": [
      { "date": "2026-04-18", "cost": 12.34, "calls": 85 }
    ],
    "providerStats": [
      { "provider": "seedance", "cost": 150.00, "calls": 750 },
      { "provider": "llm", "cost": 36.45, "calls": 500 }
    ]
  }
}
```

**SQL 实现：**

```xml
<!-- AiCallLogMapper.xml -->
<select id="selectCostReport" resultType="ApiCostReportVO">
    SELECT
        COUNT(*) as totalCalls,
        COALESCE(SUM(cost), 0) as totalCost,
        COALESCE(ROUND(SUM(cost) / NULLIF(COUNT(*), 0), 4), 0) as avgCostPerCall,
        ROUND(COUNT(CASE WHEN status = 0 THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0), 2) as failRate
    FROM api_call_log
    WHERE user_id = #{userId}
      <if test="startDate != null">
        AND create_time &gt;= #{startDate}
      </if>
      <if test="endDate != null">
        AND create_time &lt;= #{endDate}
      </if>
      <if test="projectId != null">
        AND project_id = #{projectId}
      </if>
</select>

<select id="selectDailyStats" resultType="DailyStatVO">
    SELECT
        DATE(create_time) as date,
        COUNT(*) as calls,
        SUM(cost) as cost
    FROM api_call_log
    WHERE user_id = #{userId}
      AND create_time &gt;= #{startDate}
      AND create_time &lt;= #{endDate}
    GROUP BY DATE(create_time)
    ORDER BY date ASC
</select>

<select id="selectProviderStats" resultType="ProviderStatVO">
    SELECT
        api_provider as provider,
        COUNT(*) as calls,
        SUM(cost) as cost
    FROM api_call_log
    WHERE user_id = #{userId}
      AND create_time &gt;= #{startDate}
      AND create_time &lt;= #{endDate}
    GROUP BY api_provider
    ORDER BY cost DESC
</select>
```

---

## 8. 熔断与限流

**熔断器实现（基于后端系分 4.6.4）：**

```java
@Component
public class CircuitBreaker {

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile State state = State.CLOSED;
    private volatile long lastFailureTime = 0;

    private static final int FAILURE_THRESHOLD = 5;
    private static final long HALF_OPEN_DELAY_MS = 30_000;

    public enum State { CLOSED, OPEN, HALF_OPEN }

    public boolean allowRequest() {
        State currentState = getState();
        switch (currentState) {
            case CLOSED:
                return true;
            case OPEN:
                if (System.currentTimeMillis() - lastFailureTime > HALF_OPEN_DELAY_MS) {
                    state = State.HALF_OPEN;
                    return true;  // 半开状态, 允许一个试探请求
                }
                return false;
            case HALF_OPEN:
                return false;  // 半开状态下只允许一个请求
            default:
                return false;
        }
    }

    public void recordSuccess() {
        failureCount.set(0);
        state = State.CLOSED;
    }

    public void recordFailure() {
        int count = failureCount.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();
        if (count >= FAILURE_THRESHOLD) {
            state = State.OPEN;
            log.warn("Seedance API circuit breaker OPEN after {} consecutive failures", count);
        }
    }

    private State getState() {
        if (state == State.OPEN &&
            System.currentTimeMillis() - lastFailureTime > HALF_OPEN_DELAY_MS) {
            return State.HALF_OPEN;
        }
        return state;
    }
}
```

**限流（Guava RateLimiter）：**

```java
@Configuration
public class SeedanceRateLimitConfig {

    @Bean
    public RateLimiter seedanceRateLimiter(
            @Value("${seedance.rate-limit-qps:5}") int qps) {
        return RateLimiter.create(qps);
    }
}
```

**在 SeedanceClient 中使用：**

```java
public String submitImage(ImageGenParams params) {
    // 限流检查
    if (!rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
        throw new BusinessException(42900, "AI调用频率过高, 请稍后重试");
    }

    // 熔断检查
    if (!circuitBreaker.allowRequest()) {
        throw new BusinessException(51000, "AI服务暂时不可用, 请稍后重试");
    }

    // ... 执行 HTTP 调用 ...
}
```

---

## 9. 配置项清单

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `seedance.api-url` | `https://visual.volcengineapi.com` | 火山引擎 API 地址 |
| `seedance.api-key` | `${SEEDANCE_API_KEY}` | API Key (环境变量) |
| `seedance.api-secret` | `${SEEDANCE_API_SECRET}` | API Secret (签名校验) |
| `seedance.webhook-url` | `${SEEDANCE_WEBHOOK_URL}` | Webhook 回调地址 |
| `seedance.connect-timeout` | `10000` | 连接超时(ms) |
| `seedance.read-timeout` | `30000` | 读取超时(ms) |
| `seedance.max-retry` | `3` | 查询重试次数 |
| `seedance.rate-limit-qps` | `5` | 提交限流 QPS |
| `prompt-engine.llm-api-url` | `https://api.deepseek.com/v1` | LLM API 地址 |
| `prompt-engine.llm-api-key` | `${LLM_API_KEY}` | LLM API Key |
| `prompt-engine.llm-model` | `deepseek-chat` | LLM 模型名称 |
| `prompt-engine.max-tokens` | `4096` | LLM 最大 Token |
| `prompt-engine.temperature` | `0.7` | LLM 温度参数 |
| `ffmpeg.path` | `ffmpeg` | FFmpeg 可执行文件路径 |
| `ffmpeg.timeout` | `600` | FFmpeg 超时(秒) |

---

## 10. 开发任务拆分

### Sprint 3 中本模块相关任务（基于后端系分 Sprint 3）

| # | 任务 | 交付物 | 优先级 |
|---|------|--------|--------|
| 3.6 | AiTaskService 异步处理 | submit + 指数退避 poll + @Async | P0 |
| 3.7 | SeedanceClient 封装 + 重试 | HTTP 调用 + @Retryable + 熔断器 | P0 |
| 3.8 | AI 任务指数退避轮询 | @Scheduled + next_poll_time 策略 | P0 |
| 3.9 | PromptEngine 提示词生成 | 强模型翻译+生成 prompt_en | P0 |
| 3.10 | FFmpeg 导出 | ExportService (合并视频) | P0 |
| — | WebhookController + 签名校验 | 回调入口 + HMAC-SHA256 验证 | P0 |
| — | AssetRefResolver 参考图解析 | shot_asset_ref → reference URLs | P0 |
| — | ApiCallLogAspect + 费用计算 | 消耗记录切面 + 聚合查询 | P1 |
| — | CircuitBreaker 熔断器 | 手动熔断实现 | P1 |
| — | RecoveryRunner 恢复增强 | 宕机后 AI 任务恢复 | P1 |

---

## 附录 A: 评审意见处理记录

> 评审人: 老克 | 评审日期: 2026-04-19 | 评审结论: ⚠️ 有条件通过

| # | 评审意见 | 级别 | 处理结果 | 说明 |
|---|---------|------|---------|------|
| 1 | Sa-Token 全局拦截未排除 `/api/webhook/**`，导致 Webhook 回调 401 | P0 | ✅ 已修复 | 在 2.7 节增加 SaTokenConfig 协同变更说明，需在 DESIGN_BACKEND_老克_v1.2 中增加 `.notMatch("/api/webhook/**")` |
| 2 | Webhook 回调同步处理阻塞 Tomcat 线程 | P1 | ✅ 已修复 | `handleWebhookCallback` 方法已增加 `@Async("aiTaskExecutor")` 注解，使用独立线程池异步处理 |
| 3a | Authorization Header 解析使用 `split(", ")` 硬分割，格式变化会越界 | P1 | ✅ 已修复 | 改用正则表达式 `Pattern.compile("HMAC-SHA256\\s+Credential=(\\S+),\\s*SignedHeaders=([^,]+),\\s*Signature=(\\S+)")` 解析，容错空格变化 |
| 3b | 签名算法可能不完整，StringToSign 可能缺少日期/区域/服务信息 | P1 | ⚠️ 已标注待验证 | 在 2.6 节增加签名算法待验证备注，需在对接火山引擎时验证实际签名格式 |
| 3c | 防重放功能未启用（timestampHeader 传了 null） | P1 | ✅ 已修复 | WebhookController 现在从 `X-Date` Header 提取时间戳并传入 verifier，启用 5 分钟窗口校验 |
| 4 | 签名校验失败返回 40100（未登录），语义不当 | P1 | ✅ 已修复 | 错误码从 51002 改为 51003（Webhook 签名无效），避免前端误判为未登录 |

---

*文档版本: v1.1 (评审修订版) | 作者: 阿典 | 日期: 2026-04-19*
*评审状态: 老克评审修订完成，待二次确认*
