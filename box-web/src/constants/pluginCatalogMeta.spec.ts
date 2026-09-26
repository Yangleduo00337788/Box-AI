import { describe, expect, it } from 'vitest'
import {
  isComposerPluginCategory,
  isPluginInstalled,
  pluginCategoryIcon,
  pluginPillLabel,
  pluginSourceLabel,
  pluginSourceTheme,
} from './pluginCatalogMeta'

describe('pluginCatalogMeta', () => {
  it('allows composer categories case-insensitively', () => {
    expect(isComposerPluginCategory('TOOLS')).toBe(true)
    expect(isComposerPluginCategory('skills')).toBe(true)
    expect(isComposerPluginCategory('workflows')).toBe(false)
  })

  it('maps labels icons and source tags', () => {
    expect(pluginPillLabel({ title: '  Search  ' })).toBe('Search')
    expect(pluginCategoryIcon('mcp')).toBe('server')
    expect(pluginCategoryIcon('unknown')).toBe('extension')
    expect(pluginSourceLabel('USER')).toBe('工作空间')
    expect(pluginSourceTheme('USER')).toBe('success')
    expect(pluginSourceLabel('ADMIN')).toBe('官方')
    expect(isPluginInstalled({ installed: true } as Parameters<typeof isPluginInstalled>[0])).toBe(true)
  })
})
