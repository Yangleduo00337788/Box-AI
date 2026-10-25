import type { AxiosInstance } from 'axios'
import { attachDemoHttpAdapter } from './adapter'
import { installDemoFetchPatch } from './fetchDemo'
import { resetDemoPersistence } from './world/persist'

export { DEMO_ADMIN_TOKEN } from './admin/store'
export { DEMO_CONSUMER_TOKEN } from './consumer/store'

export { installDemoFetchPatch } from './fetchDemo'

export { resetDemoPersistence } from './world/persist'

export function installBoxDemoMock(http: AxiosInstance, realm: 'consumer' | 'admin') {
  if (realm === 'consumer') {
    installDemoFetchPatch()
    if (typeof window !== 'undefined') {
      ;(window as unknown as { __BOX_DEMO_RESET__?: () => void }).__BOX_DEMO_RESET__ = () => {
        resetDemoPersistence()
        window.location.reload()
      }
    }
  }
  attachDemoHttpAdapter(http, realm)
}
