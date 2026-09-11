<template>
  <component :is="tag" class="app-logo" :class="{ 'app-logo--collapsed': collapsed }">
    <img
      v-if="collapsed || variant === 'icon'"
      :src="logoMascot"
      alt="Box"
      class="app-logo__icon"
    />
    <img v-else :src="logoWordmark" alt="Box" class="app-logo__wordmark" />
  </component>
</template>

<script setup lang="ts">
import logoWordmark from '@/assets/logo.png'
import logoMascot from '@/assets/logo-mascot.png'

withDefaults(
  defineProps<{
    collapsed?: boolean
    variant?: 'auto' | 'wordmark' | 'icon'
    tag?: string
  }>(),
  {
    collapsed: false,
    variant: 'auto',
    tag: 'div',
  },
)
</script>

<style scoped>
.app-logo {
  display: flex;
  align-items: center;
  min-width: 0;
}

.app-logo__wordmark,
.app-logo__icon {
  display: block;
  background: transparent;
  object-fit: contain;
}

.app-logo__wordmark {
  height: 32px;
  width: auto;
}

.app-logo__icon {
  width: 36px;
  height: 36px;
}

.app-logo--collapsed {
  justify-content: center;
}
</style>
