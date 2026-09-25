<template>
  <t-form label-width="140px">
    <t-form-item v-if="showYamlFallback" label="回退部署配置">
      <t-switch v-model="model.useYamlFallback" />
      <p class="field-hint">开启后，未填写的项使用 application.yml 中的 box.minio。</p>
    </t-form-item>
    <t-form-item label="Endpoint">
      <t-input v-model="model.endpoint" :placeholder="endpointPlaceholder" />
      <p class="field-hint">不要包含桶路径；桶名请填在下方 Bucket（误填 /桶名 会在保存时自动纠正）。</p>
    </t-form-item>
    <t-form-item label="Access Key">
      <t-input v-model="model.accessKey" placeholder="Access Key ID" />
    </t-form-item>
    <t-form-item label="Secret Key">
      <t-input v-model="model.secretKey" type="password" :placeholder="secretPlaceholder" />
    </t-form-item>
    <t-form-item label="Bucket">
      <t-input v-model="model.bucket" placeholder="box" />
    </t-form-item>
    <t-form-item label="状态">
      <t-space>
        <t-tag :theme="status?.configured ? 'success' : 'default'" variant="light">
          {{ status?.configured ? '已配置' : '未配置' }}
        </t-tag>
        <t-tag :theme="status?.reachable ? 'success' : 'warning'" variant="light">
          {{ status?.reachable ? '可连接' : '不可达' }}
        </t-tag>
        <t-button size="small" variant="text" @click="emit('test')">测试连接</t-button>
      </t-space>
    </t-form-item>
  </t-form>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PlatformObjectStorageBackendVO } from '@/api/platform'

const model = defineModel<{
  endpoint: string
  accessKey: string
  secretKey: string
  bucket: string
  useYamlFallback?: boolean
}>({ required: true })

const props = defineProps<{
  status?: PlatformObjectStorageBackendVO
  showYamlFallback?: boolean
  endpointPlaceholder?: string
}>()

const emit = defineEmits<{ test: [] }>()

const secretPlaceholder = computed(() =>
  props.status?.hasSecretKey ? '留空表示不修改已保存的密钥' : '请输入 Secret Key',
)
</script>

<style scoped>
.field-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--td-text-color-placeholder);
}
</style>
