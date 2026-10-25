import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { dispatchAdminMock } from './admin/dispatch'
import { dispatchConsumerMock } from './consumer/dispatch'

function resolveRequestUrl(config: InternalAxiosRequestConfig): string {
  let url = config.url || ''
  const base = config.baseURL || ''
  if (url.startsWith('http')) {
    return url
  }
  if (base) {
    const b = base.endsWith('/') ? base.slice(0, -1) : base
    url = url.startsWith('/') ? `${b}${url}` : `${b}/${url}`
  }
  return url
}

function parseBody(config: InternalAxiosRequestConfig): unknown {
  const data = config.data
  if (typeof data === 'string' && data) {
    try {
      return JSON.parse(data)
    } catch {
      return data
    }
  }
  return data
}

export function attachDemoHttpAdapter(http: AxiosInstance, realm: 'consumer' | 'admin') {
  const dispatch = realm === 'consumer' ? dispatchConsumerMock : dispatchAdminMock

  http.defaults.adapter = async (config) => {
    const url = resolveRequestUrl(config)
    const method = config.method || 'get'
    const payload = dispatch(method, url, parseBody(config))
    const response: AxiosResponse = {
      data: payload,
      status: 200,
      statusText: 'OK',
      headers: { 'x-request-id': 'box-demo-mock' },
      config,
    }
    return response
  }
}
