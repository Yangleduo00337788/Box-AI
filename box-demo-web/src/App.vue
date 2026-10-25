<script setup lang="ts">
import zhCn from 'tdesign-vue-next/es/locale/zh_CN'
</script>

<template>
  <t-config-provider :global-config="{ classPrefix: 't' }" :locale="zhCn">
    <router-view />
  </t-config-provider>
</template>
