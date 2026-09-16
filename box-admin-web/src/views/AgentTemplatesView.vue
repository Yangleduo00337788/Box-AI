<template>
  <div class="templates-page admin-page">
    <page-header title="智能体市场" desc="审核通过后再上架，C 端市场只展示已通过且已上架的模板。">
      <template #actions>
        <t-button theme="primary" @click="openCreate">新建模板</t-button>
      </template>
    </page-header>

    <t-card :bordered="false" class="admin-card">
      <t-table row-key="id" :data="templates" :columns="columns" :loading="loading" hover>
        <template #empty>
          <t-empty description="暂无模板" />
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="dialogVisible"
      :header="editing ? '编辑模板' : '新建模板'"
      width="720px"
      :confirm-btn="{ content: editing ? '保存' : '创建', loading: saving }"
      @confirm="onSave"
    >
      <t-form ref="formRef" :data="form" :rules="rules" label-width="110px">
        <t-form-item v-if="!editing" label="编码" name="templateCode">
          <t-input v-model="form.templateCode" placeholder="如 writing-assistant" />
        </t-form-item>
        <t-form-item label="名称" name="name">
          <t-input v-model="form.name" />
        </t-form-item>
        <t-form-item label="分类" name="category">
          <t-input v-model="form.category" placeholder="如 创作、开发" />
        </t-form-item>
        <t-form-item label="描述" name="description">
          <t-textarea v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" />
        </t-form-item>
        <t-form-item label="平台模型" name="platformModelId">
          <t-select v-model="form.platformModelId" :options="platformModelOptions" placeholder="请选择" />
        </t-form-item>
        <t-form-item label="System Prompt" name="systemPrompt">
          <t-textarea v-model="form.systemPrompt" :autosize="{ minRows: 4, maxRows: 10 }" />
        </t-form-item>
        <t-form-item label="排序" name="sortOrder">
          <t-input-number v-model="form.sortOrder" :min="0" theme="column" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { DialogPlugin, Link, MessagePlugin, Tag } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import {
  createAgentTemplate,
  deleteAgentTemplate,
  fetchAgentTemplates,
  updateAgentTemplate,
  updateAgentTemplateReview,
  updateAgentTemplateStatus,
  type AgentTemplateVO,
} from '@/api/agentTemplate'
import { fetchPlatformModels, type PlatformModelVO } from '@/api/platform'

const templates = ref<AgentTemplateVO[]>([])
const platformModels = ref<PlatformModelVO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstanceFunctions>()

const form = reactive({
  templateCode: '',
  name: '',
  category: '',
  description: '',
  platformModelId: undefined as number | undefined,
  systemPrompt: '',
  sortOrder: 0,
})

const rules: FormProps['rules'] = {
  templateCode: [{ required: true, message: '请输入编码' }],
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

const statusLabel: Record<string, string> = {
  DRAFT: '草稿',
  LISTED: '已上架',
  ARCHIVED: '已下架',
}

const reviewLabel: Record<string, string> = {
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
}

const columns: PrimaryTableCol<AgentTemplateVO>[] = [
  { colKey: 'name', title: '名称', minWidth: 140 },
  { colKey: 'templateCode', title: '编码', width: 160 },
  { colKey: 'category', title: '分类', width: 100 },
  { colKey: 'platformModelName', title: '平台模型', width: 140 },
  { colKey: 'installCount', title: '启用次数', width: 100 },
  {
    colKey: 'status',
    title: '上架',
    width: 90,
    cell: (_, { row }) => {
      const theme = row.status === 'LISTED' ? 'success' : row.status === 'DRAFT' ? 'default' : 'warning'
      return h(Tag, { theme, variant: 'light' }, () => statusLabel[row.status] || row.status)
    },
  },
  {
    colKey: 'reviewStatus',
    title: '审核',
    width: 100,
    cell: (_, { row }) => {
      const status = row.reviewStatus || 'PENDING_REVIEW'
      const theme = status === 'APPROVED' ? 'success' : status === 'REJECTED' ? 'danger' : 'warning'
      return h(Tag, { theme, variant: 'light' }, () => reviewLabel[status] || status)
    },
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 280,
    fixed: 'right',
    cell: (_, { row }) =>
      h('div', { class: 'admin-ops' }, [
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openEdit(row) }, () => '编辑'),
        row.reviewStatus !== 'APPROVED'
          ? h(Link, { theme: 'success', hover: 'color', onClick: () => reviewTemplate(row, 'APPROVED') }, () => '通过')
          : null,
        row.reviewStatus !== 'REJECTED'
          ? h(Link, { theme: 'danger', hover: 'color', onClick: () => reviewTemplate(row, 'REJECTED') }, () => '拒绝')
          : null,
        row.reviewStatus === 'APPROVED' && row.status !== 'LISTED'
          ? h(Link, { theme: 'primary', hover: 'color', onClick: () => listTemplate(row) }, () => '上架')
          : null,
        row.status === 'LISTED'
          ? h(Link, { theme: 'warning', hover: 'color', onClick: () => archiveTemplate(row) }, () => '下架')
          : null,
        h(Link, { theme: 'danger', hover: 'color', onClick: () => confirmDelete(row) }, () => '删除'),
      ]),
  },
]

async function loadData() {
  loading.value = true
  try {
    const [templateRes, modelRes] = await Promise.all([fetchAgentTemplates(), fetchPlatformModels()])
    templates.value = templateRes.data.data || []
    platformModels.value = modelRes.data.data || []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.templateCode = ''
  form.name = ''
  form.category = ''
  form.description = ''
  form.platformModelId = platformModelOptions.value[0]?.value
  form.systemPrompt = ''
  form.sortOrder = 0
}

function openCreate() {
  editing.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: AgentTemplateVO) {
  editing.value = true
  editingId.value = row.id
  form.name = row.name
  form.category = row.category || ''
  form.description = row.description || ''
  form.platformModelId = row.platformModelId
  form.systemPrompt = row.systemPrompt || ''
  form.sortOrder = row.sortOrder ?? 0
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate()
  if (valid !== true || !form.platformModelId) return false
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      category: form.category.trim() || undefined,
      description: form.description.trim() || undefined,
      platformModelId: form.platformModelId,
      systemPrompt: form.systemPrompt.trim() || undefined,
      sortOrder: form.sortOrder,
    }
    if (editing.value && editingId.value) {
      await updateAgentTemplate(editingId.value, payload)
      MessagePlugin.success('模板已更新')
    } else {
      await createAgentTemplate({
        templateCode: form.templateCode.trim(),
        ...payload,
      })
      MessagePlugin.success('模板已创建，通过审核后才能上架')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
  return true
}

async function reviewTemplate(row: AgentTemplateVO, reviewStatus: 'APPROVED' | 'REJECTED') {
  await updateAgentTemplateReview(row.id, reviewStatus)
  MessagePlugin.success(reviewStatus === 'APPROVED' ? '已通过审核' : '已拒绝')
  await loadData()
}

async function listTemplate(row: AgentTemplateVO) {
  await updateAgentTemplateStatus(row.id, 'LISTED')
  MessagePlugin.success('模板已上架')
  await loadData()
}

async function archiveTemplate(row: AgentTemplateVO) {
  await updateAgentTemplateStatus(row.id, 'ARCHIVED')
  MessagePlugin.success('模板已下架')
  await loadData()
}

function confirmDelete(row: AgentTemplateVO) {
  const dialog = DialogPlugin.confirm({
    header: '删除模板',
    body: `确定删除「${row.name}」？已启用该模板的智能体不会被删除。`,
    theme: 'warning',
    onConfirm: async () => {
      await deleteAgentTemplate(row.id)
      MessagePlugin.success('模板已删除')
      dialog.destroy()
      await loadData()
    },
  })
}

onMounted(loadData)
</script>
