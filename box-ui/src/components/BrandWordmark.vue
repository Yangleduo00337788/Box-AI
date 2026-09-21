<template>
  <div
    class="brand-wordmark"
    :class="[
      `brand-wordmark--${props.size}`,
      `brand-wordmark--${props.align}`,
      `brand-wordmark--mode-${props.mode}`,
      { 'brand-wordmark--collapsed': props.collapsed },
    ]"
  >
    <img
      v-if="props.collapsed || props.mode === 'mascot'"
      :src="props.mode === 'mascot' ? logoSidebar : logoMascot"
      alt="盒子"
      class="brand-wordmark__mascot"
    />
    <span v-else-if="props.mode === 'text'" class="brand-wordmark__text">盒子</span>
    <img
      v-else
      :src="logoWordmark"
      alt="盒子"
      class="brand-wordmark__img"
    />
  </div>
</template>

<script setup lang="ts">
import logoWordmark from '../assets/logo.png'
import logoMascot from '../assets/logo-mascot.png'
import logoSidebar from '../assets/logo-sidebar.png'

const props = withDefaults(
  defineProps<{
    size?: 'lg' | 'md' | 'sm'
    align?: 'center' | 'left'
    collapsed?: boolean
    mode?: 'image' | 'text' | 'mascot'
  }>(),
  {
    size: 'lg',
    align: 'center',
    collapsed: false,
    mode: 'image',
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

.brand-wordmark--lg .brand-wordmark__img {
  height: 64px;
}

.brand-wordmark--md .brand-wordmark__img {
  height: 44px;
}

.brand-wordmark--sm .brand-wordmark__img {
  height: 44px;
}

.brand-wordmark--sm .brand-wordmark__mascot,
.brand-wordmark--collapsed .brand-wordmark__mascot {
  width: 40px;
  height: 40px;
}

.brand-wordmark--mode-mascot .brand-wordmark__mascot {
  width: 32px;
  height: 32px;
}

.brand-wordmark__text {
  font-family: var(--box-brand-font);
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.04em;
  color: var(--box-ink);
}

.brand-wordmark--sm .brand-wordmark__text {
  font-size: 24px;
}
</style>
