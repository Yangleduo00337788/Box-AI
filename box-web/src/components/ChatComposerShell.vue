<template>
  <div class="composer-shell">
    <img :src="boxMascotGif" alt="" class="composer-shell__mascot" aria-hidden="true" />
    <slot />
  </div>
</template>

<script setup lang="ts">
import boxMascotGif from '@/assets/mascot/box-mascot.gif'
</script>

<style scoped>
.composer-shell {
  position: relative;
  width: 100%;
  overflow: visible;
}

.composer-shell__mascot {
  position: absolute;
  right: 22px;
  bottom: 100%;
  width: 88px;
  height: auto;
  display: block;
  pointer-events: none;
  user-select: none;
  z-index: 2;
}
</style>
