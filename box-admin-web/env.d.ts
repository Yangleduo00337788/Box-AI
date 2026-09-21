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
