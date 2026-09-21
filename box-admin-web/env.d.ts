/// <reference types="vite/client" />

declare module '*.png' {
  const src: string
  export default src
}

declare module '*.webp' {
  const src: string
  export default src
}

declare module '*.gif' {
  const src: string
  export default src
}

import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    title?: string
    catalogCategory?: 'tools' | 'mcp'
  }
}

declare module '@box/ui/layouts/AuthLayout.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{ slogan?: string; showMascot?: boolean }, {}, unknown>
  export default component
}

declare module '@box/ui/layouts/AppShellLayout.vue' {
  import type { DefineComponent } from 'vue'
  import type { MenuGroup } from '@box/ui/types/menu'
  const component: DefineComponent<
    {
      menuGroups: MenuGroup[]
      active: string
      userName: string
      userHint: string
    },
    {},
    unknown,
    {},
    {},
    {},
    {
      'footer-extra': (props: { collapsed: boolean }) => unknown
    },
    { logout: [] }
  >
  export default component
}

declare module '@box/ui/components/PageHeader.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{ title: string; desc?: string }, {}, unknown>
  export default component
}

declare module '@box/ui/components/BrandWordmark.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<
    { size?: 'lg' | 'md' | 'sm'; align?: 'center' | 'left'; collapsed?: boolean },
    {},
    unknown
  >
  export default component
}
