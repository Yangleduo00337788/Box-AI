import { createApp } from 'vue'
import { createPinia } from 'pinia'
import TDesign from 'tdesign-vue-next'
import App from './App.vue'
import router from './router'
import 'tdesign-vue-next/es/style/index.css'
import 'tdesign-icons-vue-next/esm/style/index.css'
import '@box/ui/styles/theme.css'
import '@box/ui/styles/fonts.css'
import '@box/ui/styles/index.css'
import './styles/admin.css'
import { loadTDesignIconSprite } from '@box/ui/setup/tdesignIconSprite'

async function bootstrap() {
  try {
    await loadTDesignIconSprite()
  } catch {
    console.warn('[box-admin-web] TDesign icon sprite failed to load')
  }

  const app = createApp(App)
  app.use(createPinia())
  app.use(router)
  app.use(TDesign)
  app.mount('#app')
}

void bootstrap()
