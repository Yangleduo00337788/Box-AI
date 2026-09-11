<template>
  <div class="tenants-page">
    <page-header title="租户管理" desc="管理平台租户（含个人/企业），支持创建、启停与成员管理。">
      <template #actions>
        <t-button theme="primary" @click="openCreate">新建租户</t-button>
      </template>
    </page-header>

    <t-table
      row-key="id"
      :data="tenants"
      :columns="columns"
      :loading="loading"
      bordered
      stripe
    />

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

    <t-dialog
      v-model:visible="planVisible"
      header="分配套餐"
      :confirm-btn="{ content: '保存', loading: assigningPlan }"
      @confirm="onAssignPlan"
    >
      <p class="plan-dialog__tenant">{{ activeTenant?.name }}</p>
      <t-select v-model="selectedPlanId" :options="planOptions" placeholder="选择套餐" />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import TenantMembersDrawer from '@/components/TenantMembersDrawer.vue'
import { fetchPlans, type PlanVO } from '@/api/plan'
import {
  assignTenantPlan,
  createTenant,
  fetchTenants,
  updateTenantStatus,
  type TenantVO,
} from '@/api/tenant'

const tenants = ref<TenantVO[]>([])
const loading = ref(false)
const createVisible = ref(false)
const creating = ref(false)
const membersVisible = ref(false)
const planVisible = ref(false)
const assigningPlan = ref(false)
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
  contactEmail: '',
})

const createRules: FormProps['rules'] = {
  name: [{ required: true, message: '请输入租户名称' }],
}

const columns: PrimaryTableCol<TenantVO>[] = [
  { colKey: 'id', title: 'ID', width: 80 },
  { colKey: 'name', title: '名称', minWidth: 160 },
  { colKey: 'slug', title: 'Slug', minWidth: 140 },
  {
    colKey: 'tenantType',
    title: '类型',
    width: 100,
    cell: (_, { row }) => (row.tenantType === 'PERSONAL' ? '个人' : '企业'),
  },
  { colKey: 'planName', title: '套餐', minWidth: 120, cell: (_, { row }) => row.planName || '-' },
  { colKey: 'contactEmail', title: '联系邮箱', minWidth: 180 },
  {
    colKey: 'status',
    title: '状态',
    width: 100,
    cell: (_, { row }) => (row.status === 1 ? '正常' : '停用'),
  },
  {
    colKey: 'createdAt',
    title: '创建时间',
    minWidth: 180,
    cell: (_, { row }) => row.createdAt?.replace('T', ' ').slice(0, 19) || '-',
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 200,
    cell: (_, { row }) =>
      h('div', { class: 'tenant-actions' }, [
        h('a', { href: 'javascript:void(0)', onClick: () => openMembers(row) }, '成员'),
        h('span', ' · '),
        h('a', { href: 'javascript:void(0)', onClick: () => openPlan(row) }, '套餐'),
        h('span', ' · '),
        h(
          'a',
          { href: 'javascript:void(0)', onClick: () => toggleStatus(row) },
          row.status === 1 ? '停用' : '启用',
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

function openPlan(row: TenantVO) {
  activeTenant.value = row
  selectedPlanId.value = row.planId || ''
  planVisible.value = true
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
  await updateTenantStatus(row.id, nextStatus)
  MessagePlugin.success(nextStatus === 1 ? '已启用' : '已停用')
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
