<template>
  <div class="team-page" :class="{ 'team-page--compact': compact }">
    <page-header
      v-if="!compact"
      title="团队"
      :desc="isEnterprise ? '管理企业成员与当前工作空间协作。' : '个人版账号无需团队管理。'"
    />

    <t-empty v-if="!isEnterprise">
      <template #description>
        <p>升级企业版后可开通员工账号，并用企业标识登录。</p>
      </template>
      <t-form layout="vertical" class="upgrade-form" @submit="onUpgrade">
        <t-form-item label="企业名称">
          <t-input v-model="companyName" placeholder="填写企业或团队名称" />
        </t-form-item>
        <t-button theme="primary" type="submit" :loading="upgrading">升级为企业版</t-button>
      </t-form>
    </t-empty>

    <template v-else>
      <t-card v-if="canManage" :bordered="false" class="org-card">
        <div class="org-card__grid">
          <div class="org-field">
            <div class="org-field__label">企业标识</div>
            <div class="org-field__value">
              <span class="org-field__text">{{ orgAccess?.orgId || '-' }}</span>
              <t-button variant="text" size="small" @click="openOrgIdDialog">修改</t-button>
              <t-button
                variant="text"
                size="small"
                :disabled="!orgAccess?.orgId"
                @click="copyText(orgAccess?.orgId || '', '企业标识已复制')"
              >
                复制
              </t-button>
            </div>
          </div>
          <div class="org-field">
            <div class="org-field__label">邀请码</div>
            <div class="org-field__value">
              <span class="org-field__text">{{ orgAccess?.inviteCode || '-' }}</span>
              <t-button
                variant="text"
                size="small"
                :disabled="!orgAccess?.inviteCode"
                @click="copyText(orgAccess?.inviteCode || '', '邀请码已复制')"
              >
                复制
              </t-button>
              <t-button variant="text" size="small" :loading="rotating" @click="onRotateInvite">重置</t-button>
            </div>
          </div>
        </div>
        <div class="org-card__footer">
          <p class="org-card__hint">员工用企业标识 + 账号登录，或用邀请码加入。</p>
          <t-button variant="outline" size="small" @click="copyInvite">复制加入信息</t-button>
        </div>
      </t-card>

      <t-tabs v-model="activeTab" class="team-tabs">
        <t-tab-panel value="enterprise" :label="`企业成员 · ${members.length}`">
          <div v-if="canManage" class="team-toolbar">
            <t-button theme="primary" @click="openProvisionDialog">
              <template #icon><t-icon name="add" /></template>
              开通账号
            </t-button>
          </div>
          <t-table
            row-key="userId"
            :data="members"
            :columns="columns"
            :loading="loading"
            size="small"
            hover
            bordered
          >
            <template #loginName="{ row }">
              <span class="cell-ellipsis" :title="row.loginName">{{ row.loginName || '-' }}</span>
            </template>
            <template #email="{ row }">
              <span class="cell-ellipsis" :title="row.email">{{ row.email || '-' }}</span>
            </template>
            <template #nickname="{ row }">
              <span class="cell-ellipsis" :title="displayName(row)">{{ displayName(row) }}</span>
            </template>
            <template #roleCode="{ row }">
              <t-tag :theme="row.roleCode === 'TENANT_ADMIN' ? 'primary' : 'default'" variant="light" size="small">
                {{ row.roleCode === 'TENANT_ADMIN' ? '管理员' : '成员' }}
              </t-tag>
            </template>
            <template #status="{ row }">
              <t-tag :theme="row.status === 1 ? 'success' : 'warning'" variant="light" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </t-tag>
            </template>
            <template #actions="{ row }">
              <t-space v-if="canManage && row.userId !== auth.user?.id" size="4px">
                <t-button variant="text" size="small" @click="toggleStatus(row)">
                  {{ row.status === 1 ? '停用' : '启用' }}
                </t-button>
                <t-button variant="text" size="small" @click="openResetPassword(row)">重置密码</t-button>
              </t-space>
              <span v-else class="cell-muted">-</span>
            </template>
            <template #empty>
              <t-empty description="暂无企业成员" />
            </template>
          </t-table>
        </t-tab-panel>

        <t-tab-panel v-if="canManageWorkspace" value="workspace" :label="`当前工作空间 · ${workspaceMembers.length}`">
          <p class="section-desc">将企业成员加入当前工作空间，并分配角色。</p>
          <div class="team-toolbar">
            <t-input v-model="wsAddEmail" placeholder="同事邮箱" clearable class="toolbar-input" />
            <t-select v-model="wsAddRole" :options="wsRoleOptions" class="toolbar-select" />
            <t-button theme="primary" :loading="wsAdding" @click="onInviteWorkspace">加入工作空间</t-button>
          </div>
          <t-table
            row-key="userId"
            :data="workspaceMembers"
            :columns="wsColumns"
            :loading="wsLoading"
            size="small"
            hover
            bordered
          >
            <template #email="{ row }">
              <span class="cell-ellipsis" :title="row.email">{{ row.email || '-' }}</span>
            </template>
            <template #nickname="{ row }">
              <span class="cell-ellipsis" :title="displayName(row)">{{ displayName(row) }}</span>
            </template>
            <template #roleCode="{ row }">
              <t-select
                v-if="row.userId !== auth.user?.id"
                :value="row.roleCode"
                :options="wsRoleOptions"
                size="small"
                @change="(value) => updateWorkspaceRole(row.userId, String(value))"
              />
              <t-tag v-else variant="light" size="small" theme="primary">{{ wsRoleLabel(row.roleCode) }}</t-tag>
            </template>
            <template #actions="{ row }">
              <t-button
                v-if="row.userId !== auth.user?.id"
                variant="text"
                size="small"
                theme="danger"
                @click="removeWorkspace(row.userId)"
              >
                移除
              </t-button>
              <span v-else class="cell-muted">-</span>
            </template>
            <template #empty>
              <t-empty description="当前工作空间还没有协作成员" />
            </template>
          </t-table>

          <template v-if="pendingInvites.length">
            <h3 class="section-title">待接受邀请</h3>
            <t-table
              row-key="id"
              :data="pendingInvites"
              :columns="inviteColumns"
              size="small"
              hover
              bordered
            >
              <template #email="{ row }">
                <span class="cell-ellipsis" :title="row.email">{{ row.email }}</span>
              </template>
              <template #roleCode="{ row }">
                {{ wsRoleLabel(row.roleCode) }}
              </template>
              <template #status="{ row }">
                <t-tag variant="light" size="small">{{ row.status === 'PENDING' ? '待接受' : row.status }}</t-tag>
              </template>
              <template #actions="{ row }">
                <t-button v-if="row.status === 'PENDING'" variant="text" size="small" @click="onRevokeInvite(row.id)">
                  撤销
                </t-button>
                <span v-else class="cell-muted">-</span>
              </template>
            </t-table>
          </template>
        </t-tab-panel>
      </t-tabs>
    </template>

    <t-dialog
      v-model:visible="orgIdDialog"
      attach="body"
      header="修改企业标识"
      :confirm-btn="{ content: '保存', loading: savingOrgId }"
      @confirm="onSaveOrgId"
    >
      <p class="org-dialog-hint">修改后员工需使用新标识登录。仅支持 2-64 位小写字母、数字或短横线。</p>
      <t-input v-model="orgIdDraft" placeholder="例如 acme" />
    </t-dialog>

    <t-dialog
      v-model:visible="provisionDialog"
      attach="body"
      header="开通员工账号"
      :confirm-btn="{ content: '开通', loading: provisioning }"
      @confirm="onProvision"
    >
      <t-form label-align="top">
        <t-form-item label="登录账号" required>
          <t-input v-model="provAccount" placeholder="账号或邮箱" />
        </t-form-item>
        <t-form-item label="初始密码" required>
          <t-input v-model="provPassword" type="password" placeholder="至少 8 位" />
        </t-form-item>
        <t-form-item label="昵称">
          <t-input v-model="provNickname" placeholder="选填" />
        </t-form-item>
        <t-form-item label="角色">
          <t-select v-model="provRole" :options="roleOptions" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="resetDialog"
      attach="body"
      header="重置密码"
      :confirm-btn="{ content: '保存', loading: resetting }"
      @confirm="onResetPassword"
    >
      <p class="org-dialog-hint">为 {{ resetTarget?.loginName || resetTarget?.nickname || '该成员' }} 设置新密码。</p>
      <t-input v-model="resetPassword" type="password" placeholder="至少 8 位" />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import {
  fetchOrgAccess,
  fetchTenantMembers,
  provisionEmployee,
  resetEmployeePassword,
  rotateInviteCode,
  updateOrgId,
  updateTenantMemberStatus,
  upgradeEnterprise,
  type EnterpriseOrgAccessVO,
  type TenantMemberVO,
} from '@/api/tenant'
import {
  createInvitation,
  listInvitations,
  listWorkspaceMembers,
  removeWorkspaceMember,
  revokeInvitation,
  updateWorkspaceMemberRole,
  type WorkspaceInvitationVO,
  type WorkspaceMemberVO,
} from '@/api/workspace'
import { notifyApiError } from '@/api/apiError'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
defineProps<{ compact?: boolean }>()
const members = ref<TenantMemberVO[]>([])
const loading = ref(false)
const provisioning = ref(false)
const provisionDialog = ref(false)
const provAccount = ref('')
const provPassword = ref('')
const provNickname = ref('')
const provRole = ref('MEMBER')
const orgAccess = ref<EnterpriseOrgAccessVO | null>(null)
const rotating = ref(false)
const orgIdDialog = ref(false)
const orgIdDraft = ref('')
const savingOrgId = ref(false)
const workspaceMembers = ref<WorkspaceMemberVO[]>([])
const wsLoading = ref(false)
const wsAdding = ref(false)
const wsAddEmail = ref('')
const wsAddRole = ref('MEMBER')
const companyName = ref('')
const upgrading = ref(false)
const pendingInvites = ref<WorkspaceInvitationVO[]>([])
const activeTab = ref('enterprise')
const resetDialog = ref(false)
const resetting = ref(false)
const resetPassword = ref('')
const resetTarget = ref<TenantMemberVO | null>(null)

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

function looksGeneratedName(value: string) {
  const nick = value.trim()
  if (!nick) return true
  if (/^E2E-/i.test(nick)) return true
  if (/^[A-Z0-9-]{16,}$/.test(nick) && /\d{6,}/.test(nick)) return true
  return false
}

function displayName(row: { nickname?: string; email?: string; loginName?: string }) {
  const nick = (row.nickname || '').trim()
  if (nick && !looksGeneratedName(nick)) return nick
  return row.loginName || row.email || '-'
}

const columns: PrimaryTableCol<TenantMemberVO>[] = [
  { colKey: 'loginName', title: '登录账号', ellipsis: true, minWidth: 160 },
  { colKey: 'email', title: '邮箱', ellipsis: true, minWidth: 180 },
  { colKey: 'nickname', title: '昵称', ellipsis: true, minWidth: 120 },
  { colKey: 'roleCode', title: '角色', width: 100 },
  { colKey: 'status', title: '状态', width: 88 },
  { colKey: 'actions', title: '操作', width: 168 },
]

const wsColumns: PrimaryTableCol<WorkspaceMemberVO>[] = [
  { colKey: 'email', title: '邮箱', ellipsis: true, minWidth: 180 },
  { colKey: 'nickname', title: '昵称', ellipsis: true, minWidth: 120 },
  { colKey: 'roleCode', title: '角色', width: 160 },
  { colKey: 'actions', title: '操作', width: 88 },
]

const inviteColumns: PrimaryTableCol<WorkspaceInvitationVO>[] = [
  { colKey: 'email', title: '邮箱', ellipsis: true, minWidth: 180 },
  { colKey: 'roleCode', title: '角色', width: 100 },
  { colKey: 'status', title: '状态', width: 100 },
  { colKey: 'actions', title: '操作', width: 88 },
]

async function copyText(text: string, ok = '已复制') {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    MessagePlugin.success(ok)
  } catch {
    MessagePlugin.info(text)
  }
}

async function loadOrgAccess() {
  if (!isEnterprise.value || !canManage.value) return
  try {
    const { data } = await fetchOrgAccess()
    orgAccess.value = data.data
  } catch {
    orgAccess.value = null
  }
}

async function loadMembers() {
  if (!isEnterprise.value) return
  loading.value = true
  try {
    const { data } = await fetchTenantMembers()
    members.value = data.data
    await loadOrgAccess()
  } finally {
    loading.value = false
  }
}

function openProvisionDialog() {
  provAccount.value = ''
  provPassword.value = ''
  provNickname.value = ''
  provRole.value = 'MEMBER'
  provisionDialog.value = true
}

async function onProvision() {
  if (!provAccount.value.trim() || !provPassword.value) {
    MessagePlugin.warning('请填写账号与密码')
    return false
  }
  if (provPassword.value.length < 8) {
    MessagePlugin.warning('密码至少 8 位')
    return false
  }
  provisioning.value = true
  try {
    await provisionEmployee({
      account: provAccount.value.trim(),
      password: provPassword.value,
      nickname: provNickname.value.trim() || undefined,
      roleCode: provRole.value,
    })
    MessagePlugin.success('员工账号已开通')
    provisionDialog.value = false
    await loadMembers()
  } catch (error) {
    notifyApiError(error, '开通失败')
    return false
  } finally {
    provisioning.value = false
  }
}

async function copyInvite() {
  if (!orgAccess.value) return
  const text = `企业标识：${orgAccess.value.orgId}\n邀请码：${orgAccess.value.inviteCode}\n员工登录：${window.location.origin}/login?portal=enterprise&mode=employee\n邀请加入：${window.location.origin}/login?portal=enterprise&mode=join`
  await copyText(text, '已复制加入信息')
}

function openOrgIdDialog() {
  orgIdDraft.value = orgAccess.value?.orgId || ''
  orgIdDialog.value = true
}

async function onSaveOrgId() {
  const next = orgIdDraft.value.trim().toLowerCase()
  if (next.length < 2) {
    MessagePlugin.warning('企业标识至少 2 位')
    return false
  }
  savingOrgId.value = true
  try {
    const { data } = await updateOrgId(next)
    orgAccess.value = data.data
    orgIdDialog.value = false
    MessagePlugin.success('企业标识已更新')
  } catch (error) {
    notifyApiError(error, '修改失败')
    return false
  } finally {
    savingOrgId.value = false
  }
}

async function onRotateInvite() {
  rotating.value = true
  try {
    const { data } = await rotateInviteCode()
    orgAccess.value = data.data
    MessagePlugin.success('邀请码已重置')
  } catch (error) {
    notifyApiError(error, '重置失败')
  } finally {
    rotating.value = false
  }
}

function openResetPassword(row: TenantMemberVO) {
  resetTarget.value = row
  resetPassword.value = ''
  resetDialog.value = true
}

async function onResetPassword() {
  if (!resetTarget.value) return false
  if (!resetPassword.value || resetPassword.value.length < 8) {
    MessagePlugin.warning('密码至少 8 位')
    return false
  }
  resetting.value = true
  try {
    await resetEmployeePassword(resetTarget.value.userId, resetPassword.value)
    MessagePlugin.success('密码已重置')
    resetDialog.value = false
  } catch (error) {
    notifyApiError(error, '重置失败')
    return false
  } finally {
    resetting.value = false
  }
}

async function toggleStatus(row: TenantMemberVO) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await updateTenantMemberStatus(row.userId, nextStatus)
    MessagePlugin.success(nextStatus === 1 ? '已启用' : '已停用')
    await loadMembers()
  } catch (error) {
    notifyApiError(error, '操作失败')
  }
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

async function onUpgrade() {
  if (!companyName.value.trim()) {
    MessagePlugin.warning('请填写企业名称')
    return
  }
  upgrading.value = true
  try {
    await upgradeEnterprise(companyName.value.trim())
    MessagePlugin.success('已升级为企业版')
    await auth.hydrate()
    companyName.value = ''
    await loadMembers()
  } finally {
    upgrading.value = false
  }
}

async function loadPendingInvites() {
  if (!canManageWorkspace.value) return
  try {
    const { data } = await listInvitations()
    pendingInvites.value = data.data || []
  } catch {
    pendingInvites.value = []
  }
}

async function onInviteWorkspace() {
  if (!wsAddEmail.value.trim()) {
    MessagePlugin.warning('请输入同事邮箱')
    return
  }
  wsAdding.value = true
  try {
    const { data } = await createInvitation({
      email: wsAddEmail.value.trim(),
      roleCode: wsAddRole.value,
    })
    const invite = data.data
    if (invite?.status === 'PENDING' && invite.token) {
      const link = `${window.location.origin}/invite/${invite.token}`
      MessagePlugin.success(`邀请已发送，链接：${link}`)
    } else {
      MessagePlugin.success('成员已加入工作空间')
    }
    wsAddEmail.value = ''
    await loadWorkspaceMembers()
    await loadPendingInvites()
  } catch (error) {
    notifyApiError(error, '邀请失败')
  } finally {
    wsAdding.value = false
  }
}

async function onRevokeInvite(id: number) {
  try {
    await revokeInvitation(id)
    MessagePlugin.success('已撤销邀请')
    await loadPendingInvites()
  } catch (error) {
    notifyApiError(error, '撤销失败')
  }
}

async function removeWorkspace(userId: number) {
  try {
    await removeWorkspaceMember(userId)
    MessagePlugin.success('已移除工作空间成员')
    await loadWorkspaceMembers()
  } catch (error) {
    notifyApiError(error, '移除失败')
  }
}

async function updateWorkspaceRole(userId: number, roleCode: string) {
  try {
    await updateWorkspaceMemberRole(userId, { roleCode })
    MessagePlugin.success('角色已更新')
    await loadWorkspaceMembers()
  } catch (error) {
    notifyApiError(error, '更新角色失败')
    await loadWorkspaceMembers()
  }
}

onMounted(() => {
  loadMembers()
  if (canManageWorkspace.value) {
    loadWorkspaceMembers()
    loadPendingInvites()
  }
})

watch(
  () => auth.currentWorkspaceId,
  () => {
    if (canManageWorkspace.value) {
      loadWorkspaceMembers()
      loadPendingInvites()
    } else {
      workspaceMembers.value = []
      pendingInvites.value = []
      if (activeTab.value === 'workspace') {
        activeTab.value = 'enterprise'
      }
    }
  },
)

watch(canManage, (value) => {
  if (value) {
    void loadOrgAccess()
  }
})
</script>

<style scoped>
.team-page--compact {
  max-height: min(72vh, 680px);
  overflow: auto;
  padding-right: 4px;
}

.org-card {
  margin-bottom: 16px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
}

.org-card__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
}

.org-field__label {
  font-size: 12px;
  color: var(--td-text-color-secondary);
  margin-bottom: 4px;
}

.org-field__value {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.org-field__text {
  font-size: 16px;
  font-weight: 600;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.org-card__hint {
  margin: 0;
  font-size: 13px;
  color: var(--td-text-color-secondary);
}

.org-dialog-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--td-text-color-secondary);
  line-height: 1.5;
}

.team-tabs {
  margin-top: 4px;
}

.team-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.toolbar-input {
  width: 260px;
}

.toolbar-select {
  width: 140px;
}

.section-title {
  margin: 20px 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.section-desc {
  margin: 0 0 12px;
  color: var(--td-text-color-secondary);
  font-size: 13px;
}

.cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.cell-muted {
  color: var(--td-text-color-placeholder);
}

.upgrade-form {
  max-width: 360px;
  margin: 0 auto;
}

.team-page :deep(.t-empty) {
  padding: 48px 0;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}

.team-page :deep(.t-table .t-empty) {
  border: 0;
  background: transparent;
  padding: 32px 0;
}

@media (max-width: 720px) {
  .org-card__grid {
    grid-template-columns: 1fr;
  }

  .org-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-input,
  .toolbar-select {
    width: 100%;
  }
}
</style>
