import { describe, expect, it, vi, beforeEach } from 'vitest'
import { DialogPlugin } from 'tdesign-vue-next'
import { confirmResourceDelete, showDependencyConflictDialog } from './useResourceDelete'
import { CONFLICT_CODE } from '@/api/apiError'

describe('useResourceDelete', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('showDependencyConflictDialog lists dependency labels', () => {
    showDependencyConflictDialog(
      [
        {
          resourceType: 'agent',
          resourceId: 1,
          resourceName: 'Demo',
          relation: 'published',
        },
      ],
      '知识库',
    )
    expect(DialogPlugin.alert).toHaveBeenCalledWith(
      expect.objectContaining({
        header: '无法删除知识库',
        body: expect.stringContaining('智能体「Demo」已发布'),
      }),
    )
  })

  it('confirmResourceDelete runs onDelete when user confirms', async () => {
    const onDelete = vi.fn().mockResolvedValue(undefined)
    const hide = vi.fn()
    vi.mocked(DialogPlugin.confirm).mockImplementation((options) => {
      void options.onConfirm?.()
      return { hide } as unknown as ReturnType<typeof DialogPlugin.confirm>
    })

    await confirmResourceDelete({
      header: '删除',
      body: '确认？',
      resourceLabel: '工具',
      onDelete,
    })

    expect(onDelete).toHaveBeenCalled()
    expect(hide).toHaveBeenCalled()
  })

  it('confirmResourceDelete shows dependency dialog on 409', async () => {
    const hide = vi.fn()
    vi.mocked(DialogPlugin.confirm).mockImplementation((options) => {
      void options.onConfirm?.()
      return { hide } as unknown as ReturnType<typeof DialogPlugin.confirm>
    })
    const conflict = {
      response: {
        status: CONFLICT_CODE,
        data: {
          code: CONFLICT_CODE,
          message: '冲突',
          data: {
            dependencies: [
              {
                resourceType: 'workflow',
                resourceId: 2,
                resourceName: 'Flow',
                relation: 'binding',
              },
            ],
          },
        },
      },
    }

    await confirmResourceDelete({
      header: '删除',
      body: '确认？',
      resourceLabel: '工具',
      onDelete: () => Promise.reject(conflict),
    })

    expect(DialogPlugin.alert).toHaveBeenCalledWith(
      expect.objectContaining({ header: '无法删除工具' }),
    )
  })
})
