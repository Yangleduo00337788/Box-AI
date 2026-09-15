<template>
  <div class="tenants-page admin-page">
    <page-header title="租户管理" desc="管理平台租户（含个人/企业），支持创建、启停与成员管理。">
      <template #actions>
        <t-button theme="primary" @click="openCreate">新建租户</t-button>
      </template>
    </page-header>

    <t-card :bordered="false" class="admin-card">
      <div class="admin-toolbar">
        <t-input
          v-model="keyword"
          clearable
          placeholder="搜索名称、标识或邮箱"
          style="width: 240px"
        />
        <t-select
          v-model="statusFilter"
          :options="statusOptions"
          placeholder="状态"
          clearable
          style="width: 120px"
        />
        <t-select
          v-model="typeFilter"
          :options="typeOptions"
          placeholder="类型"
          clearable
          style="width: 120px"
        />
      </div>
      <t-table
        row-key="id"
        :data="filteredTenants"
        :columns="columns"
        :loading="loading"
        hover
        size="medium"
      >
        <template #empty>
          <t-empty :description="tenants.length ? '没有匹配的租户' : '暂无租户'" />
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="createVisible"
      header="新建租户"
      :confirm-btn="{ content: '创建', loading: creating }"
      @confirm="onCreate"
    >
      <t-form ref="formRef" :data="createForm" :rules="createRules" label-width="88px">
        <t-form-item label="租户名称" name="name">
          <t-input v-model="createForm.name" placeholder="例如：Acme 科技" />
        </t-form-item>
        <t-form-item label="租户类型" name="tenantType">
          <t-radio-group v-model="createForm.tenantType">
            <t-radio value="ENTERPRISE">企业</t-radio>
            <t-radio value="PERSONAL">个人</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="标识 slug" name="slug">
          <t-input v-model="createForm.slug" placeholder="可选，留空自动生成" />
        </t-form-item>
        <t-form-item label="联系邮箱" name="contactEmail">
          <t-input v-model="createForm.contactEmail" placeholder="contact@company.com" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <tenant-members-drawer
      :visible="membersVisible"
      :tenant-id="activeTenant?.id ?? null"
      :tenant-name="activeTenant?.name ?? ''"
      @close="membersVisible = false"
    />
    <tenant-workspaces-drawer
      :visible="workspacesVisible"
      :tenant-id="activeTenant?.id ?? null"
      :tenant-name="activeTenant?.name ?? ''"
      @close="workspacesVisible = false"
    />

    <t-dialog
      v-model:visible="planVisible"
      header="分配套餐"
      :confirm-btn="{ content: '保存', loading: assigningPlan }"
      @confirm="onAssignPlan"
    >
      <p class="plan-dialog__tenant">{{ activeTenant?.name }}</p>
      <t-select v-model="selectedPlanId" :options="planOptions" placeholder="选择套餐" />
    </t-dialog>

    <t-dialog
      v-model:visible="quotaVisible"
      header="额度用量"
      width="640px"
      :footer="false"
    >
      <t-loading :loading="quotaLoading" size="small">
        <p class="plan-dialog__tenant">{{ activeTenant?.name }} · {{ quota?.planName || '-' }} · {{ quota?.period || '-' }}</p>
        <t-descriptions v-if="quota" :column="2" bordered>
          <t-descriptions-item label="AI 调用">{{ formatQuota(quota.usedAiCalls, quota.quotaAiCalls, quota.remainingAiCalls) }}</t-descriptions-item>
          <t-descriptions-item label="Token">{{ formatQuota(quota.usedTokens, quota.quotaTokens, quota.remainingTokens) }}</t-descriptions-item>
          <t-descriptions-item label="成员">{{ formatQuota(quota.usedMembers, quota.quotaMembers, quota.remainingMembers) }}</t-descriptions-item>
          <t-descriptions-item label="工作空间">{{ formatQuota(quota.usedWorkspaces, quota.quotaWorkspaces, quota.remainingWorkspaces) }}</t-descriptions-item>
          <t-descriptions-item label="知识库">{{ formatQuota(quota.usedKnowledgeBases ?? 0, quota.quotaKnowledgeBases ?? 0, quota.remainingKnowledgeBases ?? null) }}</t-descriptions-item>
        </t-descriptions>
        <t-empty v-else-if="!quotaLoading" description="暂无额度数据" />
      </t-loading>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { DialogPlugin, Link, MessagePlugin, Tag } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import TenantMembersDrawer from '@/components/TenantMembersDrawer.vue'
import TenantWorkspacesDrawer from '@/components/TenantWorkspacesDrawer.vue'
import { fetchPlans, type PlanVO } from '@/api/plan'
import {
  assignTenantPlan,
  createTenant,
  fetchTenantQuota,
  fetchTenants,
  updateTenantStatus,
  type QuotaSnapshotVO,
  type TenantVO,
} from '@/api/tenant'
import { formatDateTime } from '@/utils/datetime'

const tenants = ref<TenantVO[]>([])
const keyword = ref('')
const statusFilter = ref<number | ''>('')
const typeFilter = ref<'' | 'PERSONAL' | 'ENTERPRISE'>('')
const statusOptions = [
  { label: '正常', value: 1 },
  { label: '停用', value: 0 },
]
const typeOptions = [
  { label: '企业', value: 'ENTERPRISE' },
  { label: '个人', value: 'PERSONAL' },
]

const filteredTenants = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return tenants.value.filter((item) => {
    if (statusFilter.value !== '' && item.status !== statusFilter.value) return false
    if (typeFilter.value && item.tenantType !== typeFilter.value) return false
    if (!q) return true
    return [item.name, item.slug, item.contactEmail, item.planName]
      .filter(Boolean)
      .some((field) => String(field).toLowerCase().includes(q))
  })
})
const loading = ref(false)
const createVisible = ref(false)
const creating = ref(false)
const membersVisible = ref(false)
const workspacesVisible = ref(false)
const planVisible = ref(false)
const assigningPlan = ref(false)
const quotaVisible = ref(false)
const quotaLoading = ref(false)
const quota = ref<QuotaSnapshotVO | null>(null)
const selectedPlanId = ref<number | ''>('')
const planList = ref<PlanVO[]>([])
const activeTenant = ref<TenantVO | null>(null)
const formRef = ref<FormInstanceFunctions>()

const planOptions = computed(() =>
  planList.value.map((item) => ({ label: `${item.name}（¥${item.priceMonthly}/月）`, value: item.id })),
)

const createForm = reactive({
  name: '',
  slug: '',
  tenantType: 'ENTERPRISE' as 'PERSONAL' | 'ENTERPRISE',
  contactEmail: '',
})

const createRules: FormProps['rules'] = {
  name: [{ required: true, message: '请输入租户名称' }],
  tenantType: [{ required: true, message: '请选择租户类型' }],
}

const columns: PrimaryTableCol<TenantVO>[] = [
  { colKey: 'id', title: 'ID', width: 80 },
  { colKey: 'name', title: '名称', minWidth: 160 },
  { colKey: 'slug', title: 'Slug', minWidth: 140 },
  {
    colKey: 'tenantType',
    title: '类型',
    width: 100,
    cell: (_, { row }) =>
      h(Tag, { theme: row.tenantType === 'PERSONAL' ? 'default' : 'primary', variant: 'light' }, () =>
        row.tenantType === 'PERSONAL' ? '个人' : '企业',
      ),
  },
  { colKey: 'planName', title: '套餐', minWidth: 120, cell: (_, { row }) => row.planName || '-' },
  { colKey: 'contactEmail', title: '联系邮箱', minWidth: 180 },
  {
    colKey: 'status',
    title: '状态',
    width: 100,
    cell: (_, { row }) =>
      h(Tag, { theme: row.status === 1 ? 'success' : 'warning', variant: 'light' }, () =>
        row.status === 1 ? '正常' : '停用',
      ),
  },
  {
    colKey: 'createdAt',
    title: '创建时间',
    minWidth: 180,
    cell: (_, { row }) => formatDateTime(row.createdAt),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 320,
    fixed: 'right',
    cell: (_, { row }) =>
      h('div', { class: 'admin-ops' }, [
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openMembers(row) }, () => '成员'),
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openWorkspaces(row) }, () => '空间'),
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openPlan(row) }, () => '套餐'),
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openQuota(row) }, () => '额度'),
        h(
          Link,
          { theme: row.status === 1 ? 'warning' : 'success', hover: 'color', onClick: () => toggleStatus(row) },
          () => (row.status === 1 ? '停用' : '启用'),
        ),
      ]),
  },
]

async function loadTenants() {
  loading.value = true
  try {
    const { data } = await fetchTenants()
    tenants.value = data.data
  } finally {
    loading.value = false
  }
}

function openMembers(row: TenantVO) {
  activeTenant.value = row
  membersVisible.value = true
}

function openWorkspaces(row: TenantVO) {
  activeTenant.value = row
  workspacesVisible.value = true
}

function openPlan(row: TenantVO) {
  activeTenant.value = row
  selectedPlanId.value = row.planId || ''
  planVisible.value = true
}

function formatQuota(used: number, limit: number, remaining: number | null) {
  if (!limit) {
    return `${Number(used || 0).toLocaleString()} / 不限`
  }
  const left = remaining == null ? Math.max(limit - used, 0) : remaining
  return `${Number(used || 0).toLocaleString()} / ${Number(limit).toLocaleString()}（剩余 ${Number(left).toLocaleString()}）`
}

async function openQuota(row: TenantVO) {
  activeTenant.value = row
  quota.value = null
  quotaVisible.value = true
  quotaLoading.value = true
  try {
    const { data } = await fetchTenantQuota(row.id)
    quota.value = data.data
  } finally {
    quotaLoading.value = false
  }
}

async function onAssignPlan() {
  if (!activeTenant.value || !selectedPlanId.value) {
    MessagePlugin.warning('请选择套餐')
    return false
  }
  assigningPlan.value = true
  try {
    await assignTenantPlan(activeTenant.value.id, Number(selectedPlanId.value))
    MessagePlugin.success('套餐已更新')
    planVisible.value = false
    await loadTenants()
  } finally {
    assigningPlan.value = false
  }
  return true
}

function openCreate() {
  createForm.name = ''
  createForm.slug = ''
  createForm.tenantType = 'ENTERPRISE'
  createForm.contactEmail = ''
  createVisible.value = true
}

async function onCreate() {
  const valid = await formRef.value?.validate()
  if (valid !== true) {
    return false
  }
  creating.value = true
  try {
    await createTenant({
      name: createForm.name.trim(),
      slug: createForm.slug.trim() || undefined,
      tenantType: createForm.tenantType,
      contactEmail: createForm.contactEmail.trim() || undefined,
    })
    MessagePlugin.success('租户已创建')
    createVisible.value = false
    await loadTenants()
  } finally {
    creating.value = false
  }
  return true
}

async function toggleStatus(row: TenantVO) {
  const nextStatus = row.status === 1 ? 0 : 1
  if (nextStatus === 0) {
    const dialog = DialogPlugin.confirm({
      header: '停用租户',
      body: `确定停用「${row.name}」？该租户下用户将无法继续使用。`,
      theme: 'warning',
      onConfirm: async () => {
        await updateTenantStatus(row.id, 0)
        MessagePlugin.success('已停用')
        dialog.destroy()
        await loadTenants()
      },
    })
    return
  }
  await updateTenantStatus(row.id, 1)
  MessagePlugin.success('已启用')
  await loadTenants()
}

onMounted(async () => {
  const { data } = await fetchPlans()
  planList.value = data.data
  await loadTenants()
})
</script>

<style scoped>
.plan-dialog__tenant {
  margin: 0 0 12px;
  color: var(--box-muted);
}
</style>
