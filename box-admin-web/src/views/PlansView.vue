<template>
  <div class="plans-page admin-page">
    <page-header title="套餐管理" desc="配置套餐方案、价格与配额上限。">
      <template #actions>
        <t-button theme="primary" @click="openCreate">新建套餐</t-button>
      </template>
    </page-header>

    <t-card :bordered="false" class="admin-card">
      <t-table row-key="id" :data="plans" :columns="columns" :loading="loading" hover>
        <template #empty>
          <t-empty description="暂无套餐" />
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="dialogVisible"
      :header="editing ? '编辑套餐' : '新建套餐'"
      width="640px"
      :confirm-btn="{ content: editing ? '保存' : '创建', loading: saving }"
      @confirm="onSave"
    >
      <t-form ref="formRef" :data="form" :rules="rules" label-width="120px">
        <t-form-item v-if="!editing" label="编码" name="code">
          <t-input v-model="form.code" placeholder="例如 enterprise_pro" />
        </t-form-item>
        <t-form-item label="名称" name="name">
          <t-input v-model="form.name" />
        </t-form-item>
        <t-form-item label="描述" name="description">
          <t-textarea v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" />
        </t-form-item>
        <t-form-item label="月费（元）" name="priceMonthly">
          <t-input-number v-model="form.priceMonthly" :min="0" :decimal-places="2" theme="column" />
        </t-form-item>
        <t-form-item label="AI 调用/月" name="quotaAiCalls">
          <t-input-number v-model="form.quotaAiCalls" :min="0" theme="column" />
        </t-form-item>
        <t-form-item label="Token/月" name="quotaTokens">
          <t-input-number v-model="form.quotaTokens" :min="0" theme="column" />
        </t-form-item>
        <t-form-item label="成员上限" name="quotaMembers">
          <t-input-number v-model="form.quotaMembers" :min="0" theme="column" />
        </t-form-item>
        <t-form-item label="工作空间上限" name="quotaWorkspaces">
          <t-input-number v-model="form.quotaWorkspaces" :min="0" theme="column" />
        </t-form-item>
        <t-form-item label="知识库上限" name="quotaKnowledgeBases">
          <t-input-number v-model="form.quotaKnowledgeBases" :min="0" theme="column" />
        </t-form-item>
        <t-form-item v-if="editing" label="状态" name="status">
          <t-radio-group v-model="form.status">
            <t-radio :value="1">启用</t-radio>
            <t-radio :value="0">停用</t-radio>
          </t-radio-group>
        </t-form-item>
        <p class="form-hint">配额填 0 表示不限</p>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { DialogPlugin, Link, MessagePlugin, Tag } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import { createPlan, deletePlan, fetchPlans, updatePlan, type PlanVO } from '@/api/plan'

const plans = ref<PlanVO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstanceFunctions>()

const form = reactive({
  code: '',
  name: '',
  description: '',
  priceMonthly: 0,
  quotaAiCalls: 0,
  quotaTokens: 0,
  quotaMembers: 0,
  quotaWorkspaces: 0,
  quotaKnowledgeBases: 0,
  status: 1,
})

const rules: FormProps['rules'] = {
  code: [{ required: true, message: '请输入编码' }],
  name: [{ required: true, message: '请输入名称' }],
}

const columns: PrimaryTableCol<PlanVO>[] = [
  { colKey: 'name', title: '名称', minWidth: 140 },
  { colKey: 'code', title: '编码', width: 160 },
  {
    colKey: 'priceMonthly',
    title: '月费',
    width: 100,
    cell: (_, { row }) => `¥${row.priceMonthly}`,
  },
  { colKey: 'quotaAiCalls', title: 'AI 调用', width: 100, cell: (_, { row }) => formatLimit(row.quotaAiCalls) },
  {
    colKey: 'quotaTokens',
    title: 'Token',
    width: 120,
    cell: (_, { row }) => formatLimit(row.quotaTokens),
  },
  { colKey: 'quotaMembers', title: '成员', width: 80, cell: (_, { row }) => formatLimit(row.quotaMembers) },
  { colKey: 'quotaWorkspaces', title: '空间', width: 80, cell: (_, { row }) => formatLimit(row.quotaWorkspaces) },
  { colKey: 'quotaKnowledgeBases', title: '知识库', width: 90, cell: (_, { row }) => formatLimit(row.quotaKnowledgeBases) },
  {
    colKey: 'status',
    title: '状态',
    width: 80,
    cell: (_, { row }) =>
      h(Tag, { theme: row.status === 1 ? 'success' : 'warning', variant: 'light' }, () =>
        row.status === 1 ? '启用' : '停用',
      ),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 140,
    fixed: 'right',
    cell: (_, { row }) =>
      h('div', { class: 'admin-ops' }, [
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openEdit(row) }, () => '编辑'),
        h(Link, { theme: 'danger', hover: 'color', onClick: () => confirmDelete(row) }, () => '删除'),
      ]),
  },
]

function formatLimit(value: number) {
  return value > 0 ? value.toLocaleString() : '不限'
}

async function loadPlans() {
  loading.value = true
  try {
    const { data } = await fetchPlans()
    plans.value = data.data
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.code = ''
  form.name = ''
  form.description = ''
  form.priceMonthly = 0
  form.quotaAiCalls = 100
  form.quotaTokens = 50000
  form.quotaMembers = 1
  form.quotaWorkspaces = 1
  form.quotaKnowledgeBases = 1
  form.status = 1
}

function openCreate() {
  editing.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: PlanVO) {
  editing.value = true
  editingId.value = row.id
  form.name = row.name
  form.description = row.description || ''
  form.priceMonthly = Number(row.priceMonthly)
  form.quotaAiCalls = row.quotaAiCalls
  form.quotaTokens = row.quotaTokens
  form.quotaMembers = row.quotaMembers
  form.quotaWorkspaces = row.quotaWorkspaces
  form.quotaKnowledgeBases = row.quotaKnowledgeBases ?? 0
  form.status = row.status
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate()
  if (valid !== true) return false
  saving.value = true
  try {
    if (editing.value && editingId.value) {
      await updatePlan(editingId.value, {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        priceMonthly: form.priceMonthly,
        quotaAiCalls: form.quotaAiCalls,
        quotaTokens: form.quotaTokens,
        quotaMembers: form.quotaMembers,
        quotaWorkspaces: form.quotaWorkspaces,
        quotaKnowledgeBases: form.quotaKnowledgeBases,
        status: form.status,
      })
      MessagePlugin.success('套餐已更新')
    } else {
      await createPlan({
        code: form.code.trim(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        priceMonthly: form.priceMonthly,
        quotaAiCalls: form.quotaAiCalls,
        quotaTokens: form.quotaTokens,
        quotaMembers: form.quotaMembers,
        quotaWorkspaces: form.quotaWorkspaces,
        quotaKnowledgeBases: form.quotaKnowledgeBases,
      })
      MessagePlugin.success('套餐已创建')
    }
    dialogVisible.value = false
    await loadPlans()
  } finally {
    saving.value = false
  }
  return true
}

function confirmDelete(row: PlanVO) {
  const dialog = DialogPlugin.confirm({
    header: '删除套餐',
    body: `确定删除「${row.name}」？仍有租户使用时将无法删除。`,
    theme: 'warning',
    onConfirm: async () => {
      await deletePlan(row.id)
      MessagePlugin.success('套餐已删除')
      dialog.destroy()
      await loadPlans()
    },
  })
}

onMounted(loadPlans)
</script>

<style scoped>
.form-hint {
  margin: 0;
  font-size: 12px;
  color: var(--box-muted);
}
</style>
