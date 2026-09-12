import axios from 'axios'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  CONFLICT_CODE,
  extractApiError,
  isAuthApiPath,
  isDependencyConflict,
  isTenantDisabled,
  isUnauthorized,
  QUOTA_EXCEEDED_CODE,
  rememberTraceId,
  resolveErrorMessage,
  TENANT_DISABLED_CODE,
  TOO_MANY_REQUESTS_CODE,
  UNAUTHORIZED_CODE,
} from './apiError'

export interface Result<T> {
  code: number
  message: string
  data: T
}

export { CONFLICT_CODE, QUOTA_EXCEEDED_CODE, TENANT_DISABLED_CODE, TOO_MANY_REQUESTS_CODE, UNAUTHORIZED_CODE }

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('box.token')
  const workspaceId = localStorage.getItem('box.workspaceId')
  const url = config.url || ''
  const isAuthRequest = url.startsWith('/auth/')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (token && workspaceId && !isAuthRequest) {
    config.headers['X-Workspace-Id'] = workspaceId
  }
  return config
})

function forceLogout() {
  localStorage.removeItem('box.token')
  localStorage.removeItem('box.workspaceId')
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login'
  }
}

function handleBusinessError(payload: Result<unknown>) {
  const message = resolveErrorMessage(payload)
  if (isTenantDisabled(payload)) {
    MessagePlugin.error(message || '租户已停用')
    forceLogout()
    return
  }
  if (isUnauthorized(payload)) {
    MessagePlugin.error(message || '登录已过期，请重新登录')
    forceLogout()
    return
  }
  MessagePlugin.error(message)
}

http.interceptors.response.use(
  (response) => {
    rememberTraceId(response.headers['x-request-id'])
    const payload = response.data as Result<unknown>
    const url = response.config.url || ''
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      if (!isAuthApiPath(url)) {
        handleBusinessError(payload)
      }
      return Promise.reject(payload)
    }
    return response
  },
  (error) => {
    rememberTraceId(error.response?.headers?.['x-request-id'])
    const url = error.config?.url || ''
    const payload = error.response?.data as Result<unknown> | undefined
    const message = extractApiError(error, '网络异常')
    if (isTenantDisabled(payload) || isUnauthorized(payload) || error.response?.status === 401) {
      if (!isAuthApiPath(url)) {
        handleBusinessError({
          code: payload?.code ?? (error.response?.status === 401 ? UNAUTHORIZED_CODE : 0),
          message: resolveErrorMessage(payload, message),
          data: null,
        })
      }
    } else if (!isAuthApiPath(url) && !isDependencyConflict(payload) && error.response?.status !== CONFLICT_CODE) {
      MessagePlugin.error(message)
    }
    return Promise.reject(error)
  },
)

export default http
