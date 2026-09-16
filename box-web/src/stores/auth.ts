import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { usePermissionStore } from '@/stores/permission'
import {
  fetchMe,
  login as loginApi,
  register as registerApi,
  selectCurrentWorkspace,
  updateProfile as updateProfileApi,
  type AuthVO,
  type RegisterRequest,
  type TenantSummaryVO,
  type UserVO,
  type WorkspaceVO,
} from '@/api/auth'
import { uploadAvatar as uploadAvatarApi } from '@/api/asset'
import { fetchWorkspaces } from '@/api/workspace'

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
    applyWorkspaceSelection(payload.currentWorkspaceId, false)
  }

  function applyLocalWorkspace(id: number) {
    currentWorkspaceId.value = String(id)
    localStorage.setItem(WORKSPACE_KEY, String(id))
  }

  function applyWorkspaceSelection(preferredId: number | null | undefined, persistRemote: boolean) {
    const ids = new Set(workspaces.value.map((item) => String(item.id)))
    // 受邀加入的其他租户工作空间可能暂未出现在 workspaces 列表，保留本地选择。
    if (currentWorkspaceId.value && !ids.has(currentWorkspaceId.value)) {
      if (persistRemote) {
        void persistCurrentWorkspace(Number(currentWorkspaceId.value))
      }
      reloadPermissions()
      return
    }
    const preferred = preferredId != null ? String(preferredId) : ''
    if (preferred && ids.has(preferred)) {
      applyLocalWorkspace(Number(preferred))
      return
    }
    if (currentWorkspaceId.value && ids.has(currentWorkspaceId.value)) {
      if (persistRemote) {
        void persistCurrentWorkspace(Number(currentWorkspaceId.value))
      }
      return
    }
    if (workspaces.value.length > 0) {
      const nextId = workspaces.value[0].id
      applyLocalWorkspace(nextId)
      if (persistRemote) {
        void persistCurrentWorkspace(nextId)
      }
      reloadPermissions()
      return
    }
    currentWorkspaceId.value = ''
    localStorage.removeItem(WORKSPACE_KEY)
  }

  function reloadPermissions() {
    const permissionStore = usePermissionStore()
    permissionStore.reset()
    void permissionStore.load(true)
  }

  async function persistCurrentWorkspace(id: number) {
    try {
      await selectCurrentWorkspace(id)
    } catch {
      /* keep local selection even if sync fails */
    }
  }

  function setWorkspace(id: number) {
    if (String(id) === currentWorkspaceId.value) {
      return
    }
    applyLocalWorkspace(id)
    reloadPermissions()
    void persistCurrentWorkspace(id)
  }

  async function refreshWorkspaces() {
    if (!token.value) {
      return
    }
    const { data } = await fetchWorkspaces()
    workspaces.value = data.data || []
    applyWorkspaceSelection(Number(currentWorkspaceId.value) || undefined, true)
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
    applyWorkspaceSelection(data.data.currentWorkspaceId, true)
  }

  async function updateProfile(payload: { nickname?: string; bio?: string; avatarUrl?: string }) {
    const { data } = await updateProfileApi(payload)
    persist(data.data)
  }

  async function uploadAvatar(file: File) {
    const { data } = await uploadAvatarApi(file)
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
    usePermissionStore().reset()
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
    refreshWorkspaces,
    updateProfile,
    uploadAvatar,
    logout,
    setWorkspace,
  }
})
