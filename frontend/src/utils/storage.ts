// utils/storage.ts — localStorage 封装（系分第 2 节：工具函数）

// 带 TTL 的 localStorage 存储封装
export const storage = {
  // 设置值（可选过期时间，单位毫秒）
  set(key: string, value: any, ttl?: number) {
    const data = {
      value,
      // 如果设置了 ttl，存储过期时间戳
      expire: ttl ? Date.now() + ttl : null
    }
    localStorage.setItem(key, JSON.stringify(data))
  },

  // 获取值（自动检查过期）
  get(key: string): any {
    const raw = localStorage.getItem(key)
    if (!raw) return null

    try {
      const data = JSON.parse(raw)
      // 检查是否过期
      if (data.expire && Date.now() > data.expire) {
        localStorage.removeItem(key)
        return null
      }
      return data.value
    } catch {
      return null
    }
  },

  // 删除值
  remove(key: string) {
    localStorage.removeItem(key)
  },

  // 清空所有
  clear() {
    localStorage.clear()
  }
}
