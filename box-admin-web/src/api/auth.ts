import http, { type Result } from './http'

export interface AdminUserVO {
  id: number
  username?: string
  email?: string
  nickname?: string
  avatarUrl?: string
  userType?: string
  platformAdminRole?: string
}

export interface AdminAuthVO {
  token: string
  user: AdminUserVO
}

export function login(account: string, password: string) {
  return http.post<Result<AdminAuthVO>>('/auth/login', { account, password })
}

export function fetchMe() {
  return http.get<Result<AdminAuthVO>>('/auth/me')
}

export type VerificationCodePurpose = 'RESET_PASSWORD'

export interface SendVerificationCodeResponse {
  devCode?: string | null
}

export interface ResetPasswordRequest {
  email: string
  verificationCode: string
  newPassword: string
}

export function sendAdminVerificationCode(email: string, purpose: VerificationCodePurpose = 'RESET_PASSWORD') {
  return http.post<Result<SendVerificationCodeResponse>>('/auth/verification-code', { email, purpose })
}

export function resetAdminPassword(payload: ResetPasswordRequest) {
  return http.post<Result<null>>('/auth/password/reset', payload)
}
