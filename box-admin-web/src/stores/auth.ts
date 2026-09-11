import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchMe, login as loginApi, type AdminAuthVO, type AdminUserVO } from '@/api/auth'

const TOKEN_KEY = 'box.admin.token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<AdminUserVO | null>(null)

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
    if (data.data.user.userType !== 'PLATFORM_ADMIN') {
      logout()
      throw new Error('无平台管理权限')
    }
    user.value = data.data.user
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, user, login, hydrate, logout }
})
