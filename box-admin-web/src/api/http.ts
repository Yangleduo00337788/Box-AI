import axios from 'axios'
import { MessagePlugin } from 'tdesign-vue-next'

export interface Result<T> {
  code: number
  message: string
  data: T
}

const http = axios.create({
  baseURL: '/api/v1/admin',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('box.admin.token')
  const url = config.url || ''
  const isAuthRequest = url.startsWith('/auth/')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (token && !isAuthRequest) {
    config.headers['X-Admin-Client'] = 'box-admin-web'
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as Result<unknown>
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      MessagePlugin.error(payload.message || '请求失败')
      return Promise.reject(payload)
    }
    return response
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常'
    if (error.response?.status === 401) {
      localStorage.removeItem('box.admin.token')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    MessagePlugin.error(message)
    return Promise.reject(error)
  },
)

export default http
