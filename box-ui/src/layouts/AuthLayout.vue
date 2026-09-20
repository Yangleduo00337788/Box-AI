<template>
  <div class="auth-layout">
    <div class="auth-bg" aria-hidden="true">
      <div class="auth-bg__blob auth-bg__blob--1" />
      <div class="auth-bg__blob auth-bg__blob--2" />
      <div class="auth-bg__blob auth-bg__blob--3" />
    </div>

    <div class="auth-shell">
      <portal-badge v-if="portalLabel" :label="portalLabel" :theme="portalTheme" />
      <img v-if="showMascot" :src="logoMascot" alt="" class="auth-mascot" />
      <p v-if="slogan" class="auth-slogan">{{ slogan }}</p>

      <div class="auth-card">
        <slot />
      </div>

      <p class="auth-footer">Copyright © Box. All Rights Reserved.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import PortalBadge from '../components/PortalBadge.vue'
import logoMascot from '../assets/logo-mascot.png'

withDefaults(
  defineProps<{
    slogan?: string
    showMascot?: boolean
    portalLabel?: string
    portalTheme?: 'consumer' | 'enterprise' | 'personal' | 'admin'
  }>(),
  {
    slogan: '把模型、知识、工具、工作流装进一个 Box',
    showMascot: true,
    portalTheme: 'consumer',
  },
)
</script>

<style scoped>
.auth-layout {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  overflow: hidden;
  background: var(--box-bg);
}

.auth-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.auth-bg__blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
}

.auth-bg__blob--1 {
  top: -10%;
  left: -5%;
  width: 420px;
  height: 420px;
  background: var(--td-gray-color-3);
}

.auth-bg__blob--2 {
  right: -8%;
  bottom: -10%;
  width: 360px;
  height: 360px;
  background: var(--td-gray-color-2);
}

.auth-bg__blob--3 {
  top: 35%;
  right: 20%;
  width: 240px;
  height: 240px;
  background: var(--td-brand-color-2);
}

.auth-shell {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 520px;
}

.auth-slogan {
  margin: 0 0 20px;
  text-align: center;
  font-family: var(--box-brand-font);
  font-size: 14px;
  font-weight: 900;
  line-height: 1.65;
  letter-spacing: 0.03em;
  color: var(--box-ink);
  opacity: 0.72;
  -webkit-font-smoothing: antialiased;
}

.auth-mascot {
  display: block;
  width: min(160px, 50vw);
  height: auto;
  margin: 0 auto 12px;
  background: transparent;
  object-fit: contain;
  -webkit-user-drag: none;
  user-select: none;
}

.auth-card {
  width: 100%;
  padding: 36px 40px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
  box-shadow: var(--box-shadow-soft);
}

.auth-footer {
  margin: 24px 0 0;
  text-align: center;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}
</style>
