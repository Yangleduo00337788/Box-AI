<template>
  <t-dialog
    v-model:visible="visible"
    attach="body"
    placement="center"
    header="创建工作空间"
    :confirm-btn="{ content: '创建', loading: creating }"
    @confirm="submit"
  >
    <t-form :data="form" label-width="72px">
      <t-form-item label="名称" name="name">
        <t-input v-model="form.name" placeholder="输入工作空间名称" />
      </t-form-item>
      <t-form-item label="描述" name="description">
        <t-textarea v-model="form.description" placeholder="可选" :autosize="{ minRows: 2, maxRows: 4 }" />
      </t-form-item>
    </t-form>
  </t-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { extractApiError } from '@/api/apiError'
import { createWorkspace } from '@/api/workspace'
import { useAuthStore } from '@/stores/auth'

const visible = defineModel<boolean>('visible', { default: false })

const emit = defineEmits<{
  created: []
}>()

const auth = useAuthStore()
const creating = ref(false)
const form = reactive({ name: '', description: '' })

watch(visible, (open) => {
  if (open) {
    form.name = ''
    form.description = ''
  }
})

async function submit() {
  const name = form.name.trim()
  if (!name) {
    MessagePlugin.warning('请输入工作空间名称')
    return
  }
  creating.value = true
  try {
    await createWorkspace({ name, description: form.description.trim() || undefined })
    await auth.hydrate()
    visible.value = false
    MessagePlugin.success('工作空间已创建')
    emit('created')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '创建失败'))
  } finally {
    creating.value = false
  }
}
</script>
