<template>
  <t-dialog
    v-model:visible="visible"
    attach="body"
    placement="center"
    header="创建智能体"
    :confirm-btn="{ content: confirmLabel, loading: creating }"
    width="560px"
    @confirm="submit"
  >
    <t-radio-group v-model="createMode" variant="default-filled" style="margin-bottom: 16px">
      <t-radio-button value="blank">空白创建</t-radio-button>
      <t-radio-button value="template">从模板创建</t-radio-button>
    </t-radio-group>

    <t-form v-if="createMode === 'blank'" :data="form" :rules="rules" label-width="80px">
      <t-form-item label="Logo">
        <image-picker v-model="form.avatarUrl" :fallback-text="(form.name || '智').slice(0, 1)" hint="上传 Logo" />
      </t-form-item>
      <t-form-item label="名称" name="name">
        <t-input v-model="form.name" placeholder="给你的智能体起个名字" maxlength="128" />
      </t-form-item>
      <t-form-item label="描述" name="description">
        <t-textarea
          v-model="form.description"
          placeholder="简要描述智能体的用途"
          :autosize="{ minRows: 2, maxRows: 4 }"
          maxlength="500"
        />
      </t-form-item>
      <t-form-item label="平台模型" name="platformModelId">
        <t-select
          v-model="form.platformModelId"
          :options="platformModelOptions"
          placeholder="请选择平台模型"
        />
      </t-form-item>
    </t-form>

    <div v-else class="template-picker">
      <t-loading :loading="loadingTemplates" size="small">
        <div v-if="templates.length" class="template-list">
          <button
            v-for="item in templates"
            :key="item.id"
            type="button"
            class="template-item"
            :class="{ 'template-item--active': selectedTemplateId === item.id }"
            @click="selectedTemplateId = item.id"
          >
            <strong>{{ item.name }}</strong>
            <span>{{ item.description || '暂无描述' }}</span>
            <small>{{ item.platformModelName || '平台模型' }}</small>
          </button>
        </div>
        <t-empty v-else description="暂无上架模板，请先在智能体市场添加" />
      </t-loading>
    </div>
  </t-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormProps } from 'tdesign-vue-next'
import { createAgent } from '@/api/agent'
import { extractApiError } from '@/api/apiError'
import { listPlatformModels, type PlatformModelVO } from '@/api/platform'
import { enableMarketTemplate, listMarketTemplates, type AgentTemplateVO } from '@/api/market'
import { useCreateAgentDialog } from '@/composables/useCreateAgentDialog'
import ImagePicker from '@/components/ImagePicker.vue'

const emit = defineEmits<{
  created: []
}>()

const router = useRouter()
const { visible } = useCreateAgentDialog()
const creating = ref(false)
const createMode = ref<'blank' | 'template'>('blank')
const platformModels = ref<PlatformModelVO[]>([])
const templates = ref<AgentTemplateVO[]>([])
const loadingTemplates = ref(false)
const selectedTemplateId = ref<number | undefined>()

const form = reactive({
  name: '',
  description: '',
  avatarUrl: '',
  platformModelId: undefined as number | undefined,
})

const rules: FormProps['rules'] = {
  name: [{ required: true, message: '请输入名称' }],
  platformModelId: [{ required: true, message: '请选择平台模型' }],
}

const platformModelOptions = computed(() =>
  platformModels.value
    .filter((item) => item.status === 1)
    .map((item) => ({
      label: item.providerName ? `${item.modelName}（${item.providerName}）` : item.modelName,
      value: item.id,
    })),
)

const confirmLabel = computed(() => (createMode.value === 'template' ? '启用模板' : '创建'))

watch(visible, async (open) => {
  if (!open) return
  createMode.value = 'blank'
  selectedTemplateId.value = undefined
  form.name = ''
  form.description = ''
  form.avatarUrl = ''
  if (!platformModels.value.length) {
    const { data } = await listPlatformModels()
    platformModels.value = data.data || []
  }
  form.platformModelId = platformModelOptions.value[0]?.value
})

watch(createMode, async (mode) => {
  if (mode !== 'template' || templates.value.length) {
    return
  }
  loadingTemplates.value = true
  try {
    const { data } = await listMarketTemplates()
    templates.value = data.data || []
    selectedTemplateId.value = templates.value[0]?.id
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '加载模板失败'))
  } finally {
    loadingTemplates.value = false
  }
})

async function submit() {
  if (createMode.value === 'template') {
    if (!selectedTemplateId.value) {
      MessagePlugin.warning('请选择一个模板')
      return
    }
    creating.value = true
    try {
      const { data } = await enableMarketTemplate(selectedTemplateId.value)
      visible.value = false
      MessagePlugin.success('模板已启用')
      emit('created')
      if (data.data?.id) {
        router.push(`/agents/${data.data.id}/builder`)
      }
    } catch (error) {
      MessagePlugin.error(extractApiError(error, '启用失败'))
    } finally {
      creating.value = false
    }
    return
  }

  const name = form.name.trim()
  if (!name) {
    MessagePlugin.warning('请输入名称')
    return
  }
  if (!form.platformModelId) {
    MessagePlugin.warning('请选择平台模型')
    return
  }
  creating.value = true
  try {
    const { data } = await createAgent({
      name,
      description: form.description.trim() || undefined,
      avatarUrl: form.avatarUrl.trim() || undefined,
      platformModelId: form.platformModelId,
    })
    visible.value = false
    MessagePlugin.success('智能体创建成功')
    emit('created')
    if (data.data?.id) {
      router.push(`/agents/${data.data.id}/builder`)
    }
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '创建失败'))
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.template-picker {
  min-height: 220px;
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.template-item {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--td-component-border);
  border-radius: 10px;
  background: var(--td-bg-color-container);
  text-align: left;
  cursor: pointer;
}

.template-item strong {
  font-size: 14px;
}

.template-item span {
  color: var(--td-text-color-secondary);
  font-size: 13px;
}

.template-item small {
  color: var(--td-text-color-placeholder);
  font-size: 12px;
}

.template-item--active {
  border-color: var(--td-brand-color);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--td-brand-color) 35%, transparent);
}
</style>
