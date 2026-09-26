import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchCurrentPermissions } from '@/api/workspace'
import { usePermissionStore } from './permission'

vi.mock('@/api/workspace', () => ({
  fetchCurrentPermissions: vi.fn(),
}))

describe('permission store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(fetchCurrentPermissions).mockReset()
  })

  it('can and canAny read loaded codes', async () => {
    vi.mocked(fetchCurrentPermissions).mockResolvedValue({
      data: { code: 0, message: 'ok', data: ['agent:create', 'workflow:execute'] },
    } as Awaited<ReturnType<typeof fetchCurrentPermissions>>)

    const store = usePermissionStore()
    await store.load()

    expect(store.can('agent:create')).toBe(true)
    expect(store.can('member:manage')).toBe(false)
    expect(store.canAny(['member:manage', 'workflow:execute'])).toBe(true)
    expect(store.loaded).toBe(true)
  })

  it('load skips a second request unless forced', async () => {
    vi.mocked(fetchCurrentPermissions).mockResolvedValue({
      data: { code: 0, message: 'ok', data: ['agent:create'] },
    } as Awaited<ReturnType<typeof fetchCurrentPermissions>>)

    const store = usePermissionStore()
    await store.load()
    await store.load()
    expect(fetchCurrentPermissions).toHaveBeenCalledTimes(1)

    await store.load(true)
    expect(fetchCurrentPermissions).toHaveBeenCalledTimes(2)
  })

  it('reset clears codes', async () => {
    vi.mocked(fetchCurrentPermissions).mockResolvedValue({
      data: { code: 0, message: 'ok', data: ['agent:create'] },
    } as Awaited<ReturnType<typeof fetchCurrentPermissions>>)
    const store = usePermissionStore()
    await store.load()
    store.reset()
    expect(store.codes).toEqual([])
    expect(store.loaded).toBe(false)
    expect(store.can('agent:create')).toBe(false)
  })
})
