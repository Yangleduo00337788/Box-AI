<template>
  <t-dialog
    v-model:visible="visible"
    attach="body"
    placement="center"
    :header="isEdit ? '编辑工作空间' : '创建工作空间'"
    :confirm-btn="{ content: isEdit ? '保存' : '创建', loading: saving, disabled: !isEdit && atQuota }"
    @confirm="submit"
  >
    <t-alert
      v-if="!isEdit && atQuota"
      theme="warning"
      message="当前套餐工作空间数量已达上限，升级套餐后再创建。"
      style="margin-bottom: 16px"
    />
    <t-form :data="form" label-width="72px">
      <t-form-item label="图标">
        <image-picker
          v-model="form.avatarUrl"
          :fallback-text="(form.name || '空').slice(0, 1)"
          hint="更换图标"
          size="48px"
          persist="asset"
        />
      </t-form-item>
      <t-form-item label="名称" name="name">
        <t-input v-model="form.name" placeholder="输入工作空间名称" :disabled="!isEdit && atQuota" />
      </t-form-item>
      <t-form-item label="描述" name="description">
        <t-textarea
          v-model="form.description"
          placeholder="可选"
          :autosize="{ minRows: 2, maxRows: 4 }"
          :disabled="!isEdit && atQuota"
        />
      </t-form-item>
    </t-form>
  </t-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { extractApiError } from '@/api/apiError'
import { fetchQuota, type QuotaSnapshotVO } from '@/api/quota'
import { createWorkspace, updateWorkspace } from '@/api/workspace'
import type { WorkspaceVO } from '@/api/auth'
import ImagePicker from '@/components/ImagePicker.vue'
import { useAuthStore } from '@/stores/auth'

const visible = defineModel<boolean>('visible', { default: false })
const props = defineProps<{
  workspace?: WorkspaceVO | null
}>()

const emit = defineEmits<{
  created: []
  updated: []
}>()

const auth = useAuthStore()
const saving = ref(false)
const quota = ref<QuotaSnapshotVO | null>(null)
const form = reactive({ name: '', description: '', avatarUrl: '' })
const isEdit = computed(() => !!props.workspace)
const atQuota = computed(() => {
  if (!quota.value) return false
  return quota.value.quotaWorkspaces > 0 && (quota.value.remainingWorkspaces ?? 0) <= 0
})

watch(visible, (open) => {
  if (!open) return
  form.name = props.workspace?.name || ''
  form.description = props.workspace?.description || ''
  form.avatarUrl = props.workspace?.avatarUrl || ''
  if (!isEdit.value) {
    void loadQuota()
  }
})

async function loadQuota() {
  try {
    const { data } = await fetchQuota()
    quota.value = data.data
  } catch {
    quota.value = null
  }
}

async function submit() {
  if (!isEdit.value && atQuota.value) {
    MessagePlugin.warning('工作空间数量已达套餐上限')
    return
  }
  const name = form.name.trim()
  if (!name) {
    MessagePlugin.warning('请输入工作空间名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name,
      description: form.description.trim() || undefined,
      avatarUrl: form.avatarUrl || undefined,
    }
    if (isEdit.value && props.workspace) {
      await updateWorkspace(props.workspace.id, payload)
      await auth.refreshWorkspaces()
      MessagePlugin.success('工作空间已更新')
      emit('updated')
    } else {
      const { data } = await createWorkspace(payload)
      await auth.refreshWorkspaces()
      const createdId = data.data?.id
      if (createdId) {
        auth.setWorkspace(createdId)
      }
      MessagePlugin.success('工作空间已创建')
      emit('created')
    }
    visible.value = false
  } catch (error) {
    MessagePlugin.error(extractApiError(error, isEdit.value ? '保存失败' : '创建失败'))
  } finally {
    saving.value = false
  }
}
</script>
