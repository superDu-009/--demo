# 🎬 漫织云 - AI漫剧自动化生产平台

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.4-brightgreen.svg)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.7-blue.svg)](https://element-plus.org/)

> 让AI做你的漫剧生产流水线，把文字故事一键变成可视化漫剧内容

## 📖 项目简介
漫织云是专为创作者打造的**全链路AI漫剧自动化生成平台**，彻底解决传统漫剧生产周期长、人力成本高、专业门槛高的痛点。用户仅需上传小说/文字剧本，平台就能通过AI自动完成全流程漫剧生产，将原本需要几周的制作周期压缩到几小时。

## ✨ 核心功能特性
### 🔹 全流程自动化
- 📥 小说智能解析：自动拆分章节、分场、提取核心剧情点
- 🔍 AI资产提取：自动识别角色、场景、道具等核心资产，建立可复用资产库
- 🎨 批量生成分镜：统一画风批量生成高清分镜图片，支持人工审核调整
- 🎬 漫剧自动合成：自动搭配BGM、音效、字幕，导出成片视频

### 🔹 灵活可控的创作自由度
- 🎨 自定义画风预设：支持全局画风设置，满足不同内容风格需求
- 🧩 资产复用机制：角色/场景资产可跨项目复用，保证IP形象一致性
- ✅ 人工审核节点：关键流程支持人工介入审核调整，兼顾效率和质量
- 📊 消耗看板：实时展示API调用消耗，成本可控

### 🔹 企业级技术架构
- 🔒 权限控制：基于Sa-Token的登录鉴权，多角色权限管理
- ☁️ 云端存储：对接火山引擎TOS对象存储，高可靠低成本存储方案
- ⚡ 异步任务：AI任务异步执行+指数退避轮询，高并发稳定支持
- 🔄 流程引擎：可视化拖拽编排生产流程，灵活适配不同生产需求

## 🛠️ 技术栈
### 后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.x | 后端核心框架 |
| Sa-Token | 1.37.x | 权限认证框架 |
| MySQL 8.0 | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存/分布式锁 |
| MyBatis-Plus | 3.5.x | ORM框架 |
| 火山引擎TOS SDK | 最新 | 对象存储服务 |
| Seedance API | v2.0 | 多模态AI生成服务 |

### 前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.4.x | 前端核心框架 (Composition API) |
| TypeScript | 5.3.x | 类型安全 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.7.x | UI组件库 |
| Pinia | 2.1.x | 状态管理 |
| Vue Router | 4.2.x | 路由管理 |
| Axios | 1.6.x | 网络请求 |
| SortableJS | 1.15.x | 流程编辑器拖拽 |

## 🚀 快速开始
### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.x

### 后端启动
1. 克隆项目到本地
```bash
git clone <仓库地址>
cd ai-drama-platform/backend
```
2. 修改配置文件 `src/main/resources/application.yml`
```yaml
# 数据库配置
datasource:
  url: jdbc:mysql://localhost:3306/ai_drama?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
  username: root
  password: your_password
# Redis配置
redis:
  host: localhost
  port: 6379
  password: your_password
# TOS配置
tos:
  access-key: your_access_key
  secret-key: your_secret_key
  bucket: your_bucket
# Seedance API配置
seedance:
  api-key: your_api_key
  base-url: https://api.seedance.com
```
3. 执行初始化SQL `sql/init.sql`
4. 启动后端服务
```bash
./mvnw spring-boot:run
```
后端启动成功后访问：http://localhost:8080/actuator/health 验证

### 前端启动
1. 进入前端目录
```bash
cd ../frontend
```
2. 安装依赖
```bash
npm install
```
3. 启动开发服务
```bash
npm run dev
```
前端启动成功后访问：http://localhost:5173 ，默认账号：admin / 123456

## 📂 项目结构
```
ai-drama-platform/
├── backend/                 # 后端代码
│   ├── src/main/java/
│   │   └── cn.guiyi.drama/
│   │       ├── common/      # 通用工具、异常处理、统一返回
│   │       ├── config/      # 配置类
│   │       ├── controller/  # 接口层
│   │       ├── entity/      # 实体类
│   │       ├── mapper/      # DAO层
│   │       ├── service/     # 业务逻辑层
│   │       └── AiDramaApplication.java
│   └── src/main/resources/
├── frontend/                # 前端代码
│   ├── src/
│   │   ├── api/             # API请求层
│   │   ├── components/      # 通用组件
│   │   ├── composables/     # 组合式函数
│   │   ├── stores/          # Pinia状态管理
│   │   ├── types/           # TypeScript类型定义
│   │   ├── views/           # 页面组件
│   │   └── main.ts          # 入口文件
├── docs/                    # 项目文档
│   ├── prd/                 # 产品需求文档
│   ├── design/              # 系统设计文档
│   └── review/              # 评审记录
└── sql/                     # 数据库脚本
```

## 📋 开发规范
1. 所有代码必须有清晰的中文注释，关键业务逻辑必须说明"做什么"和"为什么"
2. 前后端接口严格对齐设计文档，修改接口必须同步更新文档
3. 所有AI功能严格对齐需求文档，严禁自由发挥
4. 代码提交前必须本地测试通过，无编译错误
5. 文档修改必须在原文件上直接更新，禁止生成副本文件

## 👥 团队分工
| 成员 | 角色 | 负责范围 |
|------|------|----------|
| 蓝烟老师 | CEO/架构师 | 需求把控、技术决策 |
| 小赫 | 产品/项目经理 | 进度跟踪、需求管理、测试验收 |
| 老克 | 后端开发 | 后端架构、业务接口、流程引擎 |
| 小欧 | 前端开发 | 全栈前端、UI/交互实现 |
| 阿典 | AI/中间件 | AI模型对接、存储服务、任务调度 |

## 📌 版本信息
- 当前版本：v1.0 MVP (开发中)
- 目标上线日期：2026年4月26日
- 核心目标：跑通「登录→创建项目→上传小说→资产确认→AI生图→分镜审核」核心链路

## 📄 许可证
MIT License

---
💡 如有问题请联系项目维护人员