export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export function ok<T>(data: T, message = 'success'): ApiResult<T> {
  return { code: 0, message, data }
}

export function fail(message: string, code = 1): ApiResult<null> {
  return { code, message, data: null }
}
