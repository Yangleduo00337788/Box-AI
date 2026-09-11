import http, { type Result } from './http'

export interface AdminUserVO {
  id: number
  username: string
  email: string
  nickname: string
  avatarUrl?: string
  userType?: string
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
