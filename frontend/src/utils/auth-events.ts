// utils/auth-events.ts — 登录过期弹窗事件

export const AUTH_EXPIRED_EVENT = 'auth:expired'

export const emitAuthExpired = (payload?: { redirect?: string }) => {
  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT, { detail: payload || {} }))
}
