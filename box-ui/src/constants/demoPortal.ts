/** 演示站 C/B 端登录页互跳（合并静态站 + 本地双 dev 端口） */
export function peerDemoLoginUrl(current: 'consumer' | 'admin'): string {
  const target = current === 'consumer' ? 'admin' : 'consumer'

  if (import.meta.env.DEV) {
    return target === 'admin' ? 'http://localhost:5174/admin/login' : 'http://localhost:5173/login'
  }

  const { origin } = window.location
  return target === 'admin' ? `${origin}/admin/login` : `${origin}/login`
}
