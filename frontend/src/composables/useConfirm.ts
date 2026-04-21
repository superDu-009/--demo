// composables/useConfirm.ts — 系分第 2 节：确认操作封装

import { ElMessageBox } from 'element-plus'

// 确认弹窗封装，统一调用方式
export function useConfirm() {
  async function confirm(message: string, title = '确认操作'): Promise<boolean> {
    try {
      await ElMessageBox.confirm(message, title, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      return true
    } catch {
      return false
    }
  }

  return { confirm }
}
