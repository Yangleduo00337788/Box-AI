import type { Result } from './http'

export const TENANT_DISABLED_CODE = 18002
export const QUOTA_EXCEEDED_CODE = 18003
export const UNAUTHORIZED_CODE = 401

export function isQuotaExceeded(payload?: Result<unknown> | null) {
  return payload?.code === QUOTA_EXCEEDED_CODE
}

export function isTenantDisabled(payload?: Result<unknown> | null) {
  return payload?.code === TENANT_DISABLED_CODE
}

export function isUnauthorized(payload?: Result<unknown> | null) {
  return payload?.code === UNAUTHORIZED_CODE
}

export function resolveErrorMessage(payload?: Result<unknown> | null, fallback = '请求失败') {
  return payload?.message || fallback
}

export function isAuthApiPath(url?: string) {
  if (!url) return false
  return url.startsWith('/auth/login') || url.startsWith('/auth/register')
}

/** 从 axios 拒绝对象或业务 Result 中提取可读错误文案 */
export function extractApiError(error: unknown, fallback = '请求失败') {
  if (!error || typeof error !== 'object') {
    return fallback
  }
  const payload = error as Result<unknown>
  if (typeof payload.code === 'number' && payload.code !== 0 && payload.message) {
    return payload.message
  }
  const axiosError = error as {
    response?: { data?: Result<unknown> }
    message?: string
  }
  if (axiosError.response?.data?.message) {
    return axiosError.response.data.message
  }
  const raw = axiosError.message || ''
  if (raw.startsWith('Request failed with status code')) {
    return fallback
  }
  return raw || fallback
}

export async function readJsonResult<T>(response: Response): Promise<Result<T>> {
  return (await response.json()) as Result<T>
}

function parseJsonResult(text: string): Result<unknown> | null {
  const trimmed = text.trim()
  if (!trimmed.startsWith('{')) {
    return null
  }
  try {
    return JSON.parse(trimmed) as Result<unknown>
  } catch {
    return null
  }
}

/** 流式接口在业务失败时返回 JSON；成功时为 SSE，不要预读 body */
export async function assertStreamResponseOk(response: Response): Promise<void> {
  const contentType = response.headers.get('content-type') || ''
  if (response.ok && contentType.includes('text/event-stream')) {
    return
  }

  const text = await response.clone().text()
  const payload = parseJsonResult(text)
  if (payload && payload.code !== undefined && payload.code !== 0) {
    throw new Error(resolveErrorMessage(payload, '对话请求失败'))
  }
  if (!response.ok) {
    throw new Error(resolveErrorMessage(payload, text || '对话请求失败'))
  }
}
