// utils/validators.ts — 系分第 12 节：表单校验规则

import type { FormItemRule } from 'element-plus'

// 用户名必填校验
export const usernameRequired: FormItemRule = {
  required: true,
  message: '请输入用户名',
  trigger: 'blur'
}

// 密码必填 + 最少 6 字符校验
export const passwordRequired: FormItemRule = {
  required: true,
  message: '请输入密码',
  trigger: 'blur'
}

export const passwordMinLength: FormItemRule = {
  min: 6,
  message: '密码至少 6 个字符',
  trigger: 'blur'
}

// 项目名必填 + 最大 200 字符
export const projectNameRules: FormItemRule[] = [
  { required: true, message: '请输入项目名称', trigger: 'blur' },
  { max: 200, message: '项目名称不能超过 200 个字符', trigger: 'blur' }
]

// 资产名称必填
export const assetNameRules: FormItemRule[] = [
  { required: true, message: '请输入资产名称', trigger: 'blur' }
]
