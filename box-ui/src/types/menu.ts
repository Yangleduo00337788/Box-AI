export interface MenuItem {
  value: string
  label: string
  icon: string
  desc?: string
}

export interface MenuGroup {
  title: string
  items: MenuItem[]
}
