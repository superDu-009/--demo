# AI漫剧平台后端代码评审结果
**评审人**: 阿典 (Codex)
**评审日期**: 2026-04-21
**评审范围**: /Users/mac/Desktop/workspace/agent/ai-drama-platform/backend/ 全部后端Java代码
**评审类型**: 代码质量+Bug+安全专项检查（无需参考系分文档）

---
## 【严重问题（必须改）】
### 1. TOS接口未鉴权
- 文件路径：`/module/storage/controller/TosController.java`
- 问题描述：`/api/tos/presign`和`/api/tos/complete`两个接口未添加任何鉴权校验，恶意用户可无限调用生成预签名URL刷存储流量，或伪造上传记录，存在严重安全隐患
- 改进建议：添加`@SaCheckLogin`注解校验登录态，同时可根据业务场景添加权限校验

### 2. TOS秘钥暴露风险
- 文件路径：`/config/TosConfig.java`
- 问题描述：暴露了`secretKey`的getter方法，敏感的TOS秘钥可被其他任意注入该配置类的代码获取，存在秘钥泄露风险
- 改进建议：删除`getSecretKey()`方法，仅在内部初始化S3客户端时使用该配置

### 3. 内容模块接口未鉴权+越权漏洞
- 文件路径：`/module/content/controller/ContentController.java`
- 问题描述：所有接口未添加鉴权校验，且未校验用户是否拥有对应项目/资源的操作权限，存在越权访问、恶意修改/删除他人项目内容的严重风险
- 改进建议：添加`@SaCheckLogin`注解做登录态校验，同时在Service层校验当前登录用户是否为对应项目的成员/所有者，禁止越权操作

### 4. 全量查询导致慢查询/OOM风险
- 文件路径：`/module/content/controller/ContentController.java`
- 问题描述：`listEpisodes`、`listScenes`接口未做分页处理，当项目下分集/分场数量过多时，会触发全量查询，导致慢查询、接口响应超时甚至服务OOM
- 改进建议：添加分页参数，使用MyBatis Plus分页插件进行分页查询

---
## 【一般问题（建议改）】
### 1. 参数未校验导致脏数据风险
- 文件路径：`/module/content/controller/ContentController.java`
- 问题描述：`bindAssetToShot`接口的`assetType`参数未做枚举值校验，用户可传入任意非法值，导致脏数据写入
- 改进建议：将`assetType`改为枚举类型参数，或在接收参数后校验是否为允许的类型值

### 2. 登出接口未加鉴权注解
- 文件路径：`/module/user/controller/UserController.java`
- 问题描述：`/api/user/logout`接口未添加`@SaCheckLogin`注解，虽然内部Service可能会校验，但不符合代码规范，容易产生未登录调用的异常
- 改进建议：添加`@SaCheckLogin`注解

---
## 【优化建议】
### 1. TOS配置缺失校验
- 文件路径：`/config/TosConfig.java`
- 问题描述：初始化S3Client和S3Presigner时未校验`accessKey`、`secretKey`、`endpoint`、`bucket`等配置是否为空，配置缺失时会直接启动失败，错误提示不友好
- 改进建议：添加配置校验，若必填配置为空则抛出明确的配置错误提示，便于排查问题

### 2. 登录接口无暴力破解防护
- 文件路径：`/module/user/controller/UserController.java`
- 问题描述：`/api/user/login`接口未添加限流和验证码校验，存在被暴力破解密码的风险
- 改进建议：添加登录接口限流，多次登录失败后要求输入验证码

---
## 评审说明
本次未发现SQL注入风险（所有Mapper使用MyBatis Plus BaseMapper，无`${}`语法的自定义SQL），未发现敏感信息硬编码问题（所有配置均通过`@Value`注入）。
