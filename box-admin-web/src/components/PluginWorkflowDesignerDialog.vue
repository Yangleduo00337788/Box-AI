<template>
  <plugin-workflow-designer-dialog v-bind="$attrs" />
</template>

<script setup lang="ts">
import PluginWorkflowDesignerDialog from '@box/ui/components/PluginWorkflowDesignerDialog.vue'

defineOptions({ inheritAttrs: false })
</script>
