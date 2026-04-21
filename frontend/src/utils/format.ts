// utils/format.ts — 日期/数字格式化工具

import dayjs from 'dayjs'

// 日期格式化（默认 YYYY-MM-DD HH:mm:ss）
export function formatDate(date: string | Date, format = 'YYYY-MM-DD HH:mm:ss'): string {
  return dayjs(date).format(format)
}

// 数字格式化（添加千分位）
export function formatNumber(num: number): string {
  return num.toLocaleString('zh-CN')
}

// 费用格式化（保留 2 位小数 + ¥ 前缀）
export function formatCost(amount: number): string {
  return `¥${amount.toFixed(2)}`
}

// 秒数转分钟文本（如 1800 → "30分钟"）
export function formatSeconds(seconds: number): string {
  if (seconds < 60) return `${seconds}秒`
  return `${Math.round(seconds / 60)}分钟`
}
