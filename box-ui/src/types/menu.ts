export interface MenuItem {
  value: string
  label: string
  icon: string
  desc?: string
  roles?: string[]
}

export interface MenuGroup {
  title: string
  /** 多级菜单父级 key；不填则仅作分组标题 */
  value?: string
  icon?: string
  items: MenuItem[]
}
