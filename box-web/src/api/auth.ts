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
  verificationCode: string
  nickname?: string
  accountType: 'PERSONAL' | 'ENTERPRISE'
  companyName?: string
  contactEmail?: string
}

export type VerificationCodePurpose = 'REGISTER' | 'RESET_PASSWORD'

export interface SendVerificationCodeResponse {
  devCode?: string | null
}

export interface ResetPasswordRequest {
  email: string
  verificationCode: string
  newPassword: string
}

export interface WorkspaceVO {
  id: number
  name: string
  slug: string
  roleCode?: string
  description?: string
  avatarUrl?: string
  status?: number
}

export interface AuthVO {
  token: string
  user: UserVO
  tenant?: TenantSummaryVO | null
  workspaces: WorkspaceVO[]
  currentWorkspaceId?: number | null
}

export interface OAuthProviderVO {
  provider: string
  displayName: string
  enabled: boolean
  authorizePath: string
}

export function fetchOAuthProviders() {
  return http.get<Result<OAuthProviderVO[]>>('/auth/oauth/providers')
}

export function login(
  account: string,
  password: string,
  accountType: 'PERSONAL' | 'ENTERPRISE',
  orgId?: string,
) {
  return http.post<Result<AuthVO>>('/auth/login', { account, password, accountType, orgId })
}

export interface EnterpriseOrgLookupVO {
  orgId: string
  name: string
}

export function lookupEnterpriseOrg(orgId: string) {
  return http.get<Result<EnterpriseOrgLookupVO>>('/auth/enterprise/org', { params: { orgId } })
}

export function joinEnterprise(payload: {
  orgId: string
  inviteCode: string
  account: string
  password: string
  nickname?: string
}) {
  return http.post<Result<AuthVO>>('/auth/enterprise/join', payload)
}

export function startOAuthLogin(
  provider: string,
  accountType: 'PERSONAL' | 'ENTERPRISE',
  redirectUri = `${window.location.origin}/chat`,
) {
  const params = new URLSearchParams({
    redirectUri,
    portal: accountType,
  })
  window.location.href = `/api/v1/auth/oauth/${provider}/authorize?${params.toString()}`
}

export function register(payload: RegisterRequest) {
  return http.post<Result<AuthVO>>('/auth/register', payload)
}

export function sendVerificationCode(email: string, purpose: VerificationCodePurpose) {
  return http.post<Result<SendVerificationCodeResponse>>('/auth/verification-code', { email, purpose })
}

export function resetPassword(payload: ResetPasswordRequest) {
  return http.post<Result<null>>('/auth/password/reset', payload)
}

export function fetchMe() {
  return http.get<Result<AuthVO>>('/auth/me')
}

export function updateProfile(payload: { nickname?: string; bio?: string; avatarUrl?: string }) {
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

export function selectCurrentWorkspace(workspaceId: number) {
  return http.put<Result<AuthVO>>('/auth/current-workspace', { workspaceId })
}

export function fetchHealth() {
  return http.get<Result<Record<string, unknown>>>('/system/health')
}

export interface UserSessionVO {
  sessionId: string
  deviceName?: string
  ipAddress?: string
  lastActiveAt?: string
  expiresAt?: string
  current: boolean
}

export function fetchSessions() {
  return http.get<Result<UserSessionVO[]>>('/auth/sessions')
}

export function revokeSession(sessionId: string) {
  return http.delete<Result<void>>(`/auth/sessions/${sessionId}`)
}
