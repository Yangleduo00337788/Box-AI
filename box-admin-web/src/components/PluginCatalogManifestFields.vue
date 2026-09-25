<template>
  <plugin-manifest-fields
    v-model:manifest="manifest"
    :category="category"
    :upload-asset="uploadCatalogAsset"
  />
</template>

<script setup lang="ts">
import PluginManifestFields from '@box/ui/components/PluginManifestFields.vue'
import { uploadPluginCatalogAsset } from '@/api/plugin'
import type { PluginManifestDraft } from '@box/ui/plugin/pluginCatalogManifest'

defineProps<{
  category: string
}>()

const manifest = defineModel<PluginManifestDraft>('manifest', { required: true })

async function uploadCatalogAsset(file: File) {
  const { data } = await uploadPluginCatalogAsset(file)
  return data.data
}
</script>
