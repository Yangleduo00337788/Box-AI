/** 与 tdesign-icons-vue-next 0.4.x 内置 CDN 一致，首屏前加载避免 `<t-icon name>` 空白 */
const TDESIGN_ICON_SPRITE_URL = 'https://tdesign.gtimg.com/icon/0.4.5/fonts/index.js'
const TDESIGN_ICON_SPRITE_CLASS = 't-svg-js-stylesheet--unique-class'

let iconSpritePromise: Promise<void> | null = null

export function loadTDesignIconSprite(): Promise<void> {
  if (typeof document === 'undefined') {
    return Promise.resolve()
  }
  if (iconSpritePromise) {
    return iconSpritePromise
  }

  iconSpritePromise = new Promise((resolve, reject) => {
    const selector = `script.${TDESIGN_ICON_SPRITE_CLASS}[src="${TDESIGN_ICON_SPRITE_URL}"]`
    const existing = document.querySelector(selector) as HTMLScriptElement | null
    if (existing) {
      if (existing.dataset.loaded === 'true') {
        resolve()
        return
      }
      existing.addEventListener('load', () => resolve(), { once: true })
      existing.addEventListener('error', () => reject(new Error('TDesign icon sprite failed to load')), {
        once: true,
      })
      return
    }

    const script = document.createElement('script')
    script.className = TDESIGN_ICON_SPRITE_CLASS
    script.src = TDESIGN_ICON_SPRITE_URL
    script.onload = () => {
      script.dataset.loaded = 'true'
      resolve()
    }
    script.onerror = () => reject(new Error('TDesign icon sprite failed to load'))
    document.body.appendChild(script)
  })

  return iconSpritePromise
}
