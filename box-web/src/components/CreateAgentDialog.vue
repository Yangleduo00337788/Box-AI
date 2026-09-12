<template>
  <t-dialog
    v-model:visible="visible"
    attach="body"
    placement="center"
    header="创建智能体"
    :confirm-btn="{ content: '创建', loading: creating }"
    @confirm="submit"
  >
    <t-form :data="form" :rules="rules" label-width="80px">
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
import { useCreateAgentDialog } from '@/composables/useCreateAgentDialog'

const emit = defineEmits<{
  created: []
}>()

const router = useRouter()
const { visible } = useCreateAgentDialog()
const creating = ref(false)
const platformModels = ref<PlatformModelVO[]>([])

const form = reactive({
  name: '',
  description: '',
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

watch(visible, async (open) => {
  if (!open) return
  form.name = ''
  form.description = ''
  if (!platformModels.value.length) {
    const { data } = await listPlatformModels()
    platformModels.value = data.data || []
  }
  form.platformModelId = platformModelOptions.value[0]?.value
})

async function submit() {
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
