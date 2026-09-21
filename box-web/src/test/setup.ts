import { vi } from 'vitest'

vi.mock('tdesign-vue-next', () => ({
  DialogPlugin: {
    alert: vi.fn(),
    confirm: vi.fn(),
  },
  MessagePlugin: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))
