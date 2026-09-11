<template>
  <t-config-provider :global-config="globalConfig">
    <router-view />
  </t-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'tdesign-vue-next/es/locale/zh_CN'

const globalConfig = {
  locale: zhCN,
}
</script>
