/** TDesign 图标统一使用描边版 */
export function resolveTIconName(name: string) {
  if (!name) return name
  return name.endsWith('-filled') ? name.replace(/-filled$/, '') : name
}
