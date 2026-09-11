<template>
  <div
    class="brand-wordmark"
    :class="[
      `brand-wordmark--${size}`,
      `brand-wordmark--${align}`,
      { 'brand-wordmark--collapsed': collapsed },
    ]"
  >
    <img
      v-if="collapsed"
      :src="logoMascot"
      alt="盒子"
      class="brand-wordmark__mascot"
    />
    <img
      v-else
      :src="logoWordmark"
      alt="盒子 Box"
      class="brand-wordmark__img"
    />
  </div>
</template>

<script setup lang="ts">
import logoWordmark from '@/assets/logo.png'
import logoMascot from '@/assets/logo-mascot.png'

withDefaults(
  defineProps<{
    size?: 'lg' | 'md' | 'sm'
    align?: 'center' | 'left'
    collapsed?: boolean
  }>(),
  {
    size: 'lg',
    align: 'center',
    collapsed: false,
  },
)
</script>

<style scoped>
.brand-wordmark {
  display: inline-flex;
  align-items: center;
  user-select: none;
}

.brand-wordmark--center {
  justify-content: center;
}

.brand-wordmark--left {
  justify-content: flex-start;
}

.brand-wordmark__img,
.brand-wordmark__mascot {
  display: block;
  width: auto;
  object-fit: contain;
  background: transparent;
}

/* lg — 登录页 */
.brand-wordmark--lg .brand-wordmark__img {
  height: 64px;
}

/* md */
.brand-wordmark--md .brand-wordmark__img {
  height: 44px;
}

/* sm — 侧栏 */
.brand-wordmark--sm .brand-wordmark__img {
  height: 44px;
}

.brand-wordmark--sm .brand-wordmark__mascot,
.brand-wordmark--collapsed .brand-wordmark__mascot {
  width: 40px;
  height: 40px;
}
</style>
