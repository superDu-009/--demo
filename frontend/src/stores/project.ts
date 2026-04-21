// stores/project.ts — 系分第 7 节：项目状态 Store（Sprint 1 骨架，Sprint 2 补充完整实现）

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { projectApi } from '@/api/project'
import type { ProjectVO, PageResult } from '@/types'

export const useProjectStore = defineStore('project', () => {
  const projectList = ref<PageResult<ProjectVO>>({
    total: 0, page: 1, size: 10, hasNext: false, list: []
  })
  const currentProject = ref<ProjectVO | null>(null)
  const loading = ref(false)

  async function fetchProjectList(page = 1, size = 10) {
    loading.value = true
    try {
      const res = await projectApi.list({ page, size })
      projectList.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchProjectDetail(id: number) {
    loading.value = true
    try {
      const res = await projectApi.getDetail(id)
      currentProject.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function createProject(data: { name: string; description?: string }) {
    await projectApi.create(data)
    await fetchProjectList()
  }

  async function deleteProject(id: number) {
    await projectApi.delete(id)
    await fetchProjectList()
  }

  return {
    projectList, currentProject, loading,
    fetchProjectList, fetchProjectDetail, createProject, deleteProject
  }
})
