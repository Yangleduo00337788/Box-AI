<template>
  <div class="platform-ocr-page admin-page">
    <page-header
      title="平台 OCR 默认模型"
      desc="知识库上传图片或扫描版 PDF 时，由平台统一调用此模型识别文字。C 端用户无需配置，费用由平台承担。"
    >
      <template #actions>
        <t-button theme="primary" :loading="saving" @click="onSave">保存</t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <t-card :bordered="false" class="admin-card">
        <t-form label-width="140px">
          <t-form-item label="默认 OCR 模型">
            <t-select
              v-model="selectedModelId"
              clearable
              filterable
              placeholder="留空则自动选择 OCR / 视觉模型"
              :options="modelOptions"
              style="max-width: 520px"
            />
            <p class="field-hint">
              仅显示平台模型池中已识别的 OCR 或视觉多模态模型；请先在「平台模型池」上架模型并绑定密钥。
            </p>
          </t-form-item>

          <t-form-item label="当前生效">
            <t-space direction="vertical" size="small">
              <t-tag :theme="settings?.runnable ? 'success' : 'warning'" variant="light">
                {{ resolutionModeLabel }}
              </t-tag>
              <span v-if="settings?.modelName" class="effective-model">
                {{ settings.modelName }}
                <template v-if="settings.modelCode">（{{ settings.modelCode }}）</template>
                <template v-if="settings.providerName"> · {{ settings.providerName }}</template>
              </span>
              <span class="field-hint">{{ settings?.resolutionHint || '加载中…' }}</span>
            </t-space>
          </t-form-item>
        </t-form>
      </t-card>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import {
  fetchPlatformModels,
  fetchPlatformOcrDefault,
  updatePlatformOcrDefault,
  type PlatformModelVO,
  type PlatformOcrDefaultVO,
} from '@/api/platform'
import { isOcrOrVisionModel } from '@/utils/modelCapability'

const loading = ref(false)
const saving = ref(false)
const models = ref<PlatformModelVO[]>([])
const settings = ref<PlatformOcrDefaultVO | null>(null)
const selectedModelId = ref<number | undefined>()

const modelOptions = computed(() =>
  models.value
    .filter((model) => model.status === 1 && isOcrOrVisionModel(model.modelCode, model.modelName, model.description))
    .map((model) => ({
      label: `${model.modelName}（${model.providerName}）`,
      value: model.id,
    })),
)

const resolutionModeLabel = computed(() => {
  const mode = settings.value?.resolutionMode || 'NONE'
  const labels: Record<string, string> = {
    CONFIGURED: '管理端已配置',
    YAML: '部署配置',
    AUTO_OCR: '自动 OCR',
    AUTO_VISION: '自动视觉',
    NONE: '未就绪',
  }
  return labels[mode] || mode
})

async function load() {
  loading.value = true
  try {
    const [modelsRes, settingsRes] = await Promise.all([fetchPlatformModels(), fetchPlatformOcrDefault()])
    models.value = modelsRes.data.data || []
    settings.value = settingsRes.data.data || null
    const configuredId = settings.value?.configuredPlatformModelId
    selectedModelId.value = configuredId && configuredId > 0 ? configuredId : undefined
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const { data } = await updatePlatformOcrDefault(selectedModelId.value ?? null)
    settings.value = data.data || null
    MessagePlugin.success('已保存平台 OCR 默认模型')
    await load()
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

.effective-model {
  font-size: 14px;
  color: var(--td-text-color-primary);
}
</style>
