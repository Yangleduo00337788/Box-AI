<template>
  <p class="box-chat-thinking-status" aria-live="polite">
    <span class="box-chat-thinking-status__text">{{ label }}</span>
  </p>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
  }>(),
  {
    label: '正在思考中',
  },
)
</script>
