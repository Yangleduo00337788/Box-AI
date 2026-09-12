<template>
  <div class="team-page">
    <page-header
      title="团队"
      :desc="isEnterprise ? '管理企业租户成员，邀请同事加入协作。' : '个人版账号无需团队管理。'"
    />

    <t-empty
      v-if="!isEnterprise"
      description="个人版账号无需团队管理，切换企业版后可邀请同事协作。"
    />

    <template v-else>
      <div class="team-toolbar">
        <t-form layout="inline" @submit="onAdd">
          <t-form-item>
            <t-input v-model="addEmail" placeholder="同事邮箱（需已注册）" clearable style="width: 280px" />
          </t-form-item>
          <t-form-item>
            <t-select v-model="addRole" :options="roleOptions" style="width: 140px" />
          </t-form-item>
          <t-form-item>
            <t-button theme="primary" type="submit" :loading="adding" :disabled="!canManage">
              邀请成员
            </t-button>
          </t-form-item>
        </t-form>
      </div>

      <t-table
        row-key="userId"
        :data="members"
        :columns="columns"
        :loading="loading"
        bordered
        stripe
      />

      <template v-if="canManageWorkspace">
        <h3 class="section-title">当前工作空间协作成员</h3>
        <p class="section-desc">将已注册的同事加入当前工作空间，分配开发者或成员角色。</p>
        <div class="team-toolbar">
          <t-form layout="inline" @submit="onInviteWorkspace">
            <t-form-item>
              <t-input v-model="wsAddEmail" placeholder="同事邮箱" clearable style="width: 280px" />
            </t-form-item>
            <t-form-item>
              <t-select v-model="wsAddRole" :options="wsRoleOptions" style="width: 140px" />
            </t-form-item>
            <t-form-item>
              <t-button theme="primary" type="submit" :loading="wsAdding">
                加入工作空间
              </t-button>
            </t-form-item>
          </t-form>
        </div>
        <t-table
          row-key="userId"
          :data="workspaceMembers"
          :columns="wsColumns"
          :loading="wsLoading"
          bordered
          stripe
        />
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { MessagePlugin, Select } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import {
  addTenantMember,
  fetchTenantMembers,
  updateTenantMemberStatus,
  type TenantMemberVO,
} from '@/api/tenant'
import {
  inviteWorkspaceMember,
  listWorkspaceMembers,
  removeWorkspaceMember,
  updateWorkspaceMemberRole,
  type WorkspaceMemberVO,
} from '@/api/workspace'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const members = ref<TenantMemberVO[]>([])
const loading = ref(false)
const adding = ref(false)
const addEmail = ref('')
const addRole = ref('MEMBER')
const workspaceMembers = ref<WorkspaceMemberVO[]>([])
const wsLoading = ref(false)
const wsAdding = ref(false)
const wsAddEmail = ref('')
const wsAddRole = ref('MEMBER')

const isEnterprise = computed(() => auth.tenant?.tenantType === 'ENTERPRISE')
const canManageWorkspace = computed(() => auth.currentWorkspace?.roleCode === 'TENANT_ADMIN')
const canManage = computed(() => {
  const me = members.value.find((item) => item.userId === auth.user?.id)
  return me?.roleCode === 'TENANT_ADMIN' && me?.status === 1
})

const roleOptions = [
  { label: '成员', value: 'MEMBER' },
  { label: '管理员', value: 'TENANT_ADMIN' },
]

const wsRoleOptions = [
  { label: '成员', value: 'MEMBER' },
  { label: '开发者', value: 'DEVELOPER' },
  { label: '管理员', value: 'TENANT_ADMIN' },
]

function wsRoleLabel(roleCode: string) {
  if (roleCode === 'TENANT_ADMIN') return '管理员'
  if (roleCode === 'DEVELOPER') return '开发者'
  return '成员'
}

const wsColumns: PrimaryTableCol<WorkspaceMemberVO>[] = [
  { colKey: 'email', title: '邮箱', minWidth: 180 },
  { colKey: 'nickname', title: '昵称', width: 120 },
  {
    colKey: 'roleCode',
    title: '角色',
    width: 160,
    cell: (_, { row }) => {
      if (!canManageWorkspace.value || row.userId === auth.user?.id) {
        return wsRoleLabel(row.roleCode)
      }
      return h(Select, {
        value: row.roleCode,
        options: wsRoleOptions,
        size: 'small',
        onChange: (value: unknown) => updateWorkspaceRole(row.userId, String(value)),
      })
    },
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 90,
    cell: (_, { row }) => {
      if (!canManageWorkspace.value || row.userId === auth.user?.id) {
        return '-'
      }
      return h(
        'a',
        { href: 'javascript:void(0)', onClick: () => removeWorkspace(row.userId) },
        '移除',
      )
    },
  },
]

const columns: PrimaryTableCol<TenantMemberVO>[] = [
  { colKey: 'email', title: '邮箱', minWidth: 180 },
  { colKey: 'nickname', title: '昵称', width: 120 },
  {
    colKey: 'roleCode',
    title: '角色',
    width: 100,
    cell: (_, { row }) => (row.roleCode === 'TENANT_ADMIN' ? '管理员' : '成员'),
  },
  {
    colKey: 'status',
    title: '状态',
    width: 90,
    cell: (_, { row }) => (row.status === 1 ? '正常' : '停用'),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 90,
    cell: (_, { row }) => {
      if (!canManage.value || row.userId === auth.user?.id) {
        return '-'
      }
      return h(
        'a',
        {
          href: 'javascript:void(0)',
          onClick: () => toggleStatus(row),
        },
        row.status === 1 ? '停用' : '启用',
      )
    },
  },
]

async function loadMembers() {
  if (!isEnterprise.value) return
  loading.value = true
  try {
    const { data } = await fetchTenantMembers()
    members.value = data.data
  } finally {
    loading.value = false
  }
}

async function onAdd() {
  if (!addEmail.value.trim()) {
    MessagePlugin.warning('请输入成员邮箱')
    return
  }
  adding.value = true
  try {
    await addTenantMember({
      email: addEmail.value.trim(),
      roleCode: addRole.value,
    })
    MessagePlugin.success('成员已添加')
    addEmail.value = ''
    await loadMembers()
  } finally {
    adding.value = false
  }
}

async function toggleStatus(row: TenantMemberVO) {
  const nextStatus = row.status === 1 ? 0 : 1
  await updateTenantMemberStatus(row.userId, nextStatus)
  MessagePlugin.success(nextStatus === 1 ? '已启用' : '已停用')
  await loadMembers()
}

async function loadWorkspaceMembers() {
  if (!isEnterprise.value || !canManageWorkspace.value) return
  wsLoading.value = true
  try {
    const { data } = await listWorkspaceMembers()
    workspaceMembers.value = data.data
  } finally {
    wsLoading.value = false
  }
}

async function onInviteWorkspace() {
  if (!wsAddEmail.value.trim()) {
    MessagePlugin.warning('请输入同事邮箱')
    return
  }
  wsAdding.value = true
  try {
    await inviteWorkspaceMember({
      email: wsAddEmail.value.trim(),
      roleCode: wsAddRole.value,
    })
    MessagePlugin.success('已加入工作空间')
    wsAddEmail.value = ''
    await loadWorkspaceMembers()
  } finally {
    wsAdding.value = false
  }
}

async function removeWorkspace(userId: number) {
  await removeWorkspaceMember(userId)
  MessagePlugin.success('已移除工作空间成员')
  await loadWorkspaceMembers()
}

async function updateWorkspaceRole(userId: number, roleCode: string) {
  try {
    await updateWorkspaceMemberRole(userId, { roleCode })
    MessagePlugin.success('角色已更新')
    await loadWorkspaceMembers()
  } catch (error) {
    MessagePlugin.error('更新角色失败')
    await loadWorkspaceMembers()
  }
}

onMounted(() => {
  loadMembers()
  if (canManageWorkspace.value) {
    loadWorkspaceMembers()
  }
})
</script>

<style scoped>
.team-toolbar {
  margin-bottom: 16px;
}

.section-title {
  margin: 32px 0 8px;
  font-size: 16px;
  font-weight: 600;
}

.section-desc {
  margin: 0 0 12px;
  color: var(--td-text-color-secondary);
  font-size: 13px;
}

.team-page :deep(.t-empty) {
  padding: 48px 0;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}
</style>
