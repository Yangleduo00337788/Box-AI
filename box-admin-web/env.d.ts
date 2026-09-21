/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

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

declare module '@box/ui/layouts/AuthLayout.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<
    {
      slogan?: string
      showMascot?: boolean
      portalLabel?: string
      portalTheme?: 'consumer' | 'enterprise' | 'personal' | 'admin'
    },
    {},
    unknown
  >
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
    {
      size?: 'lg' | 'md' | 'sm'
      align?: 'center' | 'left'
      collapsed?: boolean
      mode?: 'image' | 'text' | 'mascot'
    },
    {},
    unknown
  >
  export default component
}

declare module '@box/ui/components/PortalBadge.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<
    {
      label: string
      theme?: 'consumer' | 'enterprise' | 'personal' | 'admin'
    },
    {},
    unknown
  >
  export default component
}
