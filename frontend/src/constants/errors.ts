// constants/errors.ts — 系分第 9 节：错误码处理映射

// 错误处理动作类型
export type ErrorAction =
  | 'toast'           // Toast 提示
  | 'form'            // 表单高亮（调用方处理）
  | 'redirect-login'  // 跳转登录
  | 'retry-presign'   // 自动重试预签名
  | 'disabled-btn'    // 禁用按钮
  | 'global-error'    // 全局错误页

// 错误码 → 处理动作映射（系分 9 节完整映射表）
export const ERROR_ACTION: Record<number, ErrorAction> = {
  // 参数校验
  40001: 'form',    // 用户名或密码错误
  40002: 'form',    // 字段校验失败
  40003: 'toast',   // 文件类型不支持
  40004: 'toast',   // 文件大小超限
  40005: 'retry-presign', // 预签名 URL 过期

  // 认证鉴权
  40100: 'redirect-login', // 未登录
  40101: 'redirect-login', // Token 过期

  // 权限（v1.1：40301 保持 toast，MVP 阶段用 Toast 替代无权限页面）
  40300: 'toast',   // 无操作权限
  40301: 'toast',   // 非项目创建者

  // 资源
  40400: 'toast',   // 资源不存在

  // 业务冲突
  40900: 'toast',   // 项目正在执行中
  40901: 'toast',   // 资产被引用不可删除
  40902: 'disabled-btn', // 分镜状态不支持操作
  40903: 'toast',   // 数据已被修改

  // 限流
  42900: 'toast',   // 请求频率过高

  // 系统异常
  50000: 'global-error', // 服务器内部错误

  // AI/存储
  51000: 'toast',   // AI 调用失败
  51001: 'toast',   // AI 模型超时
  51002: 'toast',   // AI 生成内容为空
  51100: 'toast',   // TOS 上传失败
  51101: 'toast'    // TOS 空间不足
}
