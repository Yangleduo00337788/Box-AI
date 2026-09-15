import { createApp } from 'vue'
import { createPinia } from 'pinia'
import TDesign from 'tdesign-vue-next'
import TDesignChat from '@tdesign-vue-next/chat'
import App from './App.vue'
import router from './router'
import 'tdesign-vue-next/es/style/index.css'
import '@tdesign-vue-next/chat/es/style/index.css'
import '@box/ui/styles/theme.css'
import '@box/ui/styles/fonts.css'
import '@box/ui/styles/index.css'
import '@/styles/theme.css'
import '@/styles/index.css'
import { initAppPreferences } from '@/composables/useAppPreferences'

initAppPreferences()

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(TDesign)
app.use(TDesignChat)
app.mount('#app')
