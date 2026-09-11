import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  fetchMe,
  login as loginApi,
  register as registerApi,
  updateProfile as updateProfileApi,
  type AuthVO,
  type RegisterRequest,
  type TenantSummaryVO,
  type UserVO,
  type WorkspaceVO,
} from '@/api/auth'

const TOKEN_KEY = 'box.token'
const WORKSPACE_KEY = 'box.workspaceId'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<UserVO | null>(null)
  const tenant = ref<TenantSummaryVO | null>(null)
  const workspaces = ref<WorkspaceVO[]>([])
  const currentWorkspaceId = ref(localStorage.getItem(WORKSPACE_KEY) || '')

  const currentWorkspace = computed(() =>
    workspaces.value.find((item) => String(item.id) === currentWorkspaceId.value),
  )

  function persist(payload: AuthVO) {
    if (payload.user.userType === 'PLATFORM_ADMIN') {
      throw new Error('请使用平台管理端登录')
    }
    token.value = payload.token
    user.value = payload.user
    tenant.value = payload.tenant || null
    workspaces.value = payload.workspaces || []
    localStorage.setItem(TOKEN_KEY, payload.token)
    if (!currentWorkspaceId.value && workspaces.value.length > 0) {
      setWorkspace(workspaces.value[0].id)
    }
  }

  function setWorkspace(id: number) {
    currentWorkspaceId.value = String(id)
    localStorage.setItem(WORKSPACE_KEY, String(id))
  }

  async function login(account: string, password: string, accountType: 'PERSONAL' | 'ENTERPRISE') {
    const { data } = await loginApi(account, password, accountType)
    persist(data.data)
  }

  async function register(payload: RegisterRequest) {
    const { data } = await registerApi(payload)
    persist(data.data)
  }

  async function hydrate() {
    if (!token.value) {
      return
    }
    const { data } = await fetchMe()
    if (data.data.user.userType === 'PLATFORM_ADMIN') {
      logout()
      throw new Error('请使用平台管理端登录')
    }
    user.value = data.data.user
    tenant.value = data.data.tenant || null
    workspaces.value = data.data.workspaces || []
    if (!currentWorkspaceId.value && workspaces.value.length > 0) {
      setWorkspace(workspaces.value[0].id)
    }
  }

  async function updateProfile(payload: { nickname?: string; bio?: string }) {
    const { data } = await updateProfileApi(payload)
    persist(data.data)
  }

  function logout() {
    token.value = ''
    user.value = null
    tenant.value = null
    workspaces.value = []
    currentWorkspaceId.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(WORKSPACE_KEY)
  }

  return {
    token,
    user,
    tenant,
    workspaces,
    currentWorkspaceId,
    currentWorkspace,
    login,
    register,
    hydrate,
    updateProfile,
    logout,
    setWorkspace,
  }
})
