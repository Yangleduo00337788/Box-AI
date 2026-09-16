import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchMe, login as loginApi, type AdminAuthVO, type AdminUserVO } from '@/api/auth'
import { firstAccessibleAdminRoute, normalizePlatformRole, PLATFORM_ADMIN_ROLES } from '@/constants/rbac'

const TOKEN_KEY = 'box.admin.token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<AdminUserVO | null>(null)
  const platformRole = computed(() => normalizePlatformRole(user.value?.platformAdminRole))
  const isSuperAdmin = computed(() => platformRole.value === PLATFORM_ADMIN_ROLES.SUPER_ADMIN)
  const homePath = computed(() => firstAccessibleAdminRoute(platformRole.value))

  function persist(payload: AdminAuthVO) {
    if (payload.user.userType !== 'PLATFORM_ADMIN') {
      throw new Error('无平台管理权限')
    }
    token.value = payload.token
    user.value = payload.user
    localStorage.setItem(TOKEN_KEY, payload.token)
  }

  async function login(account: string, password: string) {
    const { data } = await loginApi(account, password)
    persist(data.data)
  }

  async function hydrate() {
    if (!token.value) {
      return
    }
    const { data } = await fetchMe()
    persist(data.data)
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, user, platformRole, isSuperAdmin, homePath, login, hydrate, logout }
})
