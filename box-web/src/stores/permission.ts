import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchCurrentPermissions } from '@/api/workspace'
import type { PermissionCode } from '@/constants/permissions'

export const usePermissionStore = defineStore('permission', () => {
  const codes = ref<string[]>([])
  const loaded = ref(false)
  const loading = ref(false)

  async function load(force = false) {
    if (loading.value) {
      return
    }
    if (loaded.value && !force) {
      return
    }
    loading.value = true
    try {
      const { data } = await fetchCurrentPermissions()
      codes.value = data.data || []
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  function reset() {
    codes.value = []
    loaded.value = false
  }

  function can(code: PermissionCode | string) {
    return codes.value.includes(code)
  }

  function canAny(items: Array<PermissionCode | string>) {
    return items.some((code) => can(code))
  }

  return {
    codes,
    loaded,
    loading,
    load,
    reset,
    can,
    canAny,
  }
})
