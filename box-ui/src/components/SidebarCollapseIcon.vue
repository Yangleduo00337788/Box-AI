<template>
  <svg
    class="sidebar-collapse-icon"
    :class="{ 'sidebar-collapse-icon--collapsed': collapsed }"
    viewBox="0 0 24 24"
    width="18"
    height="18"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <rect
      x="3.5"
      y="4.5"
      width="17"
      height="15"
      rx="3"
      stroke="currentColor"
      stroke-width="1.5"
    />
    <path
      d="M9 4.5V19.5"
      stroke="currentColor"
      stroke-width="1.5"
      stroke-linecap="round"
    />
  </svg>
</template>

<script setup lang="ts">
defineProps<{
  collapsed?: boolean
}>()
</script>

<style scoped>
.sidebar-collapse-icon {
  display: block;
  transition: transform 0.2s ease;
}

.sidebar-collapse-icon--collapsed {
  transform: scaleX(-1);
}
</style>
