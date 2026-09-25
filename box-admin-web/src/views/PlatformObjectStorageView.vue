<template>
  <div class="platform-storage-page admin-page">
    <page-header
      title="对象存储"
      desc="平台文件（知识库原文、运营图片等）写入对象存储。支持 MinIO 与 Cloudflare R2（S3 兼容），保存后立即生效，无需重启。连接测试针对已保存的配置，修改密钥后请先保存。"
    >
      <template #actions>
        <t-button variant="outline" :loading="testing" @click="onTestActive">测试当前后端</t-button>
        <t-button theme="primary" :loading="saving" @click="onSave">保存并生效</t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <t-card :bordered="false" class="admin-card">
        <t-form label-width="140px">
          <t-form-item label="当前写入后端">
            <t-radio-group v-model="activeBackend">
              <t-radio value="MINIO">MinIO</t-radio>
              <t-radio value="R2">Cloudflare R2</t-radio>
            </t-radio-group>
            <p class="field-hint">
              生效桶：<strong>{{ settings?.effectiveBucket || '—' }}</strong>。
              {{ settings?.resolutionHint }}
            </p>
          </t-form-item>
        </t-form>
      </t-card>

      <t-card :bordered="false" title="MinIO" class="admin-card">
        <backend-form
          v-model="minioForm"
          :status="settings?.minio"
          show-yaml-fallback
          endpoint-placeholder="http://127.0.0.1:9000"
          @test="() => onTest('MINIO')"
        />
      </t-card>

      <t-card :bordered="false" title="Cloudflare R2" class="admin-card">
        <backend-form
          v-model="r2Form"
          :status="settings?.r2"
          endpoint-placeholder="https://&lt;account_id&gt;.r2.cloudflarestorage.com"
          @test="() => onTest('R2')"
        />
      </t-card>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import BackendForm from '@/components/PlatformObjectStorageBackendForm.vue'
import {
  fetchPlatformObjectStorage,
  testPlatformObjectStorage,
  updatePlatformObjectStorage,
  type PlatformObjectStorageSettingsVO,
} from '@/api/platform'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const settings = ref<PlatformObjectStorageSettingsVO | null>(null)
const activeBackend = ref('MINIO')

const minioForm = reactive({
  endpoint: '',
  accessKey: '',
  secretKey: '',
  bucket: '',
  useYamlFallback: true,
})

const r2Form = reactive({
  endpoint: '',
  accessKey: '',
  secretKey: '',
  bucket: '',
})

function applyFormFromSettings(data: PlatformObjectStorageSettingsVO) {
  activeBackend.value = data.activeBackend || 'MINIO'
  minioForm.endpoint = data.minio?.endpoint || ''
  minioForm.accessKey = data.minio?.accessKey || ''
  minioForm.bucket = data.minio?.bucket || ''
  minioForm.useYamlFallback = data.minio?.useYamlFallback ?? true
  minioForm.secretKey = ''
  r2Form.endpoint = data.r2?.endpoint || ''
  r2Form.accessKey = data.r2?.accessKey || ''
  r2Form.bucket = data.r2?.bucket || ''
  r2Form.secretKey = ''
}

async function load() {
  loading.value = true
  try {
    const { data } = await fetchPlatformObjectStorage()
    settings.value = data.data || null
    if (settings.value) {
      applyFormFromSettings(settings.value)
    }
  } finally {
    loading.value = false
  }
}

async function onTest(backend: string) {
  testing.value = true
  try {
    const { data } = await testPlatformObjectStorage(backend)
    if (data.data?.ok) {
      MessagePlugin.success(`${backend} 连接成功`)
    } else {
      MessagePlugin.warning(`${backend} 连接失败，请检查 Endpoint 与密钥`)
    }
    await load()
  } finally {
    testing.value = false
  }
}

async function onTestActive() {
  await onTest(activeBackend.value)
}

async function onSave() {
  saving.value = true
  try {
    const payload = {
      activeBackend: activeBackend.value,
      minio: {
        endpoint: minioForm.endpoint,
        accessKey: minioForm.accessKey,
        bucket: minioForm.bucket,
        useYamlFallback: minioForm.useYamlFallback,
        ...(minioForm.secretKey ? { secretKey: minioForm.secretKey } : {}),
      },
      r2: {
        endpoint: r2Form.endpoint,
        accessKey: r2Form.accessKey,
        bucket: r2Form.bucket,
        ...(r2Form.secretKey ? { secretKey: r2Form.secretKey } : {}),
      },
    }
    const { data } = await updatePlatformObjectStorage(payload)
    settings.value = data.data || null
    MessagePlugin.success('对象存储配置已保存并生效')
    if (settings.value) {
      applyFormFromSettings(settings.value)
    }
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.field-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--td-text-color-placeholder);
}

.admin-card {
  margin-bottom: 16px;
}
</style>
