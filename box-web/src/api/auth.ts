import http, { type Result } from './http'

export interface UserVO {
  id: number
  username: string
  email: string
  nickname: string
  avatarUrl?: string
  bio?: string
  userType?: string
}

export interface UserPreferenceVO {
  theme: 'light' | 'dark' | 'system'
  sendWithEnter: boolean
}

export interface TenantSummaryVO {
  id: number
  name: string
  slug: string
  tenantType?: 'PERSONAL' | 'ENTERPRISE'
}

export interface RegisterRequest {
  email: string
  password: string
  nickname?: string
  accountType: 'PERSONAL' | 'ENTERPRISE'
  companyName?: string
  contactEmail?: string
}

export interface WorkspaceVO {
  id: number
  name: string
  slug: string
  roleCode?: string
  description?: string
}

export interface AuthVO {
  token: string
  user: UserVO
  tenant?: TenantSummaryVO | null
  workspaces: WorkspaceVO[]
}

export function login(account: string, password: string, accountType: 'PERSONAL' | 'ENTERPRISE') {
  return http.post<Result<AuthVO>>('/auth/login', { account, password, accountType })
}

export function register(payload: RegisterRequest) {
  return http.post<Result<AuthVO>>('/auth/register', payload)
}

export function fetchMe() {
  return http.get<Result<AuthVO>>('/auth/me')
}

export function updateProfile(payload: { nickname?: string; bio?: string }) {
  return http.put<Result<AuthVO>>('/auth/profile', payload)
}

export function changePassword(payload: { oldPassword: string; newPassword: string }) {
  return http.put<Result<null>>('/auth/password', payload)
}

export function fetchPreferences() {
  return http.get<Result<UserPreferenceVO>>('/auth/preferences')
}

export function updatePreferences(payload: Partial<UserPreferenceVO>) {
  return http.put<Result<UserPreferenceVO>>('/auth/preferences', payload)
}

export function fetchWorkspaces() {
  return http.get<Result<WorkspaceVO[]>>('/workspaces')
}

export function fetchHealth() {
  return http.get<Result<Record<string, unknown>>>('/system/health')
}
