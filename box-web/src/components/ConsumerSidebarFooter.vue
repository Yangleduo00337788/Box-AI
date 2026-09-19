<template>
  <div class="consumer-footer" :class="{ 'consumer-footer--collapsed': collapsed }">
    <div class="consumer-footer__user">
      <div class="user-pill-wrap">
      <t-popup
        v-model:visible="menuVisible"
        placement="top-left"
        trigger="click"
        :overlay-inner-style="{ padding: 0, maxHeight: 'none', overflow: 'visible' }"
      >
        <button type="button" class="user-pill">
          <t-avatar
            size="24px"
            shape="circle"
            class="user-pill__avatar"
            :image="avatarUrl || undefined"
          >
            {{ avatarText }}
          </t-avatar>
          <span v-if="!collapsed" class="user-pill__meta">
            <span class="user-pill__name">{{ userName }}</span>
            <span class="user-pill__workspace">{{ currentWorkspaceName }}</span>
          </span>
        </button>

        <template #content>
          <div class="user-menu">
            <div class="user-menu__head">
              <image-picker
                :model-value="avatarUrl"
                :fallback-text="avatarText"
                hint="更换头像"
                size="40px"
                persist="avatar"
              />
              <span class="user-menu__name">{{ userName }}</span>
            </div>

            <button type="button" class="user-menu__create" @click="onCreateWorkspace">
              <t-icon name="folder-add" />
              创建工作空间
            </button>

            <div v-if="displayWorkspaces.length" class="user-menu__workspaces">
              <p class="user-menu__label">工作空间</p>
              <div
                v-for="item in displayWorkspaces"
                :key="item.id"
                class="user-menu__workspace-row"
                :class="{ 'user-menu__item--active': String(item.id) === auth.currentWorkspaceId }"
              >
                <button type="button" class="user-menu__item user-menu__item--workspace" @click="onSwitchWorkspace(item)">
                  <t-avatar
                    size="20px"
                    shape="circle"
                    class="user-menu__workspace-avatar"
                    :image="item.avatarUrl || undefined"
                  >
                    {{ item.name.slice(0, 1) }}
                  </t-avatar>
                  <span class="user-menu__workspace-name">{{ item.name }}</span>
                  <t-icon v-if="isWorkspacePinned(item.id)" name="pin" class="user-menu__pin" />
                  <t-icon
                    v-if="String(item.id) === auth.currentWorkspaceId"
                    name="check"
                    class="user-menu__check"
                  />
                </button>
                <t-dropdown
                  :options="workspaceActionOptions(item)"
                  trigger="click"
                  placement="right-top"
                  :popup-props="{ zIndex: 5700, attach: 'body' }"
                  @click="(option) => onWorkspaceAction(item, option)"
                >
                  <button type="button" class="user-menu__more" aria-label="工作空间操作" @click.stop>
                    <t-icon name="more" />
                  </button>
                </t-dropdown>
              </div>
            </div>

            <div class="user-menu__section">
              <button type="button" class="user-menu__item" @click="openDialog('overview')">
                <t-icon name="dashboard" />
                <span>概览</span>
              </button>
              <button type="button" class="user-menu__item" @click="openDialog('analytics')">
                <t-icon name="chart" />
                <span>分析</span>
              </button>
              <button type="button" class="user-menu__item" @click="go('/settings/profile')">
                <t-icon name="setting" />
                <span>设置</span>
              </button>
              <button v-if="canOpenTeam" type="button" class="user-menu__item" @click="openDialog('team')">
                <t-icon name="usergroup" />
                <span>团队</span>
              </button>
              <t-popup
                v-model:visible="inboxHover"
                trigger="hover"
                placement="right-top"
                :delay="[120, 180]"
                :z-index="5600"
                :overlay-inner-style="{ padding: 0, maxHeight: 'none', overflow: 'visible', boxShadow: '0 8px 24px rgba(0,0,0,.12)', borderRadius: '12px' }"
              >
                <button type="button" class="user-menu__item">
                  <t-icon name="mail" />
                  <span>站内信</span>
                  <span v-if="unreadCount > 0" class="user-menu__badge">{{ unreadBadgeText }}</span>
                  <t-icon name="chevron-right" class="user-menu__chevron" />
                </button>
                <template #content>
                  <notification-center :active="inboxHover" />
                </template>
              </t-popup>
            </div>

            <div class="user-menu__section">
              <button type="button" class="user-menu__item user-menu__item--hint" @click="onHelp">
                <t-icon name="help-circle" />
                <span>帮助与反馈</span>
                <t-icon name="chevron-right" class="user-menu__chevron" />
              </button>
            </div>

            <div class="user-menu__section user-menu__section--last">
              <button type="button" class="user-menu__item" @click="onLogout">
                <t-icon name="logout" />
                <span>退出登录</span>
              </button>
            </div>
          </div>
        </template>
      </t-popup>
      <span v-if="unreadCount > 0" class="user-pill__unread">{{ unreadBadgeText }}</span>
      </div>
      <div class="consumer-footer__actions">
        <consumer-sidebar-quota />
        <button
          type="button"
          class="consumer-footer__icon-btn"
          aria-label="设置"
          title="设置"
          @click="goSettings"
        >
          <t-icon name="setting" />
        </button>
      </div>
    </div>

    <create-workspace-dialog
      v-model:visible="workspaceDialogVisible"
      :workspace="editingWorkspace"
      @created="onWorkspaceCreated"
      @updated="onWorkspaceUpdated"
    />
    <help-feedback-dialog v-model:visible="helpDialogVisible" />

    <t-dialog
      v-model:visible="overviewVisible"
      attach="body"
      header="概览"
      :footer="false"
      width="960px"
      placement="center"
    >
      <dashboard-view v-if="overviewVisible" compact />
    </t-dialog>
    <t-dialog
      v-model:visible="analyticsVisible"
      attach="body"
      :footer="false"
      width="1080px"
      placement="center"
      @close="analyticsPane = 'analytics'"
    >
      <template #header>
        <div class="analytics-dialog__header">
          <t-button
            v-if="analyticsPane === 'executions'"
            variant="text"
            size="small"
            @click="analyticsPane = 'analytics'"
          >
            返回分析
          </t-button>
          <span>{{ analyticsPane === 'executions' ? '执行记录' : '分析' }}</span>
        </div>
      </template>
      <analytics-view
        v-if="analyticsVisible && analyticsPane === 'analytics'"
        compact
        @view-executions="analyticsPane = 'executions'"
      />
      <executions-view v-else-if="analyticsVisible" compact />
    </t-dialog>
    <t-dialog
      v-model:visible="teamVisible"
      attach="body"
      header="团队"
      :footer="false"
      width="960px"
      placement="center"
    >
      <team-view v-if="teamVisible && canOpenTeam" compact />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { DropdownOption } from 'tdesign-vue-next'
import type { WorkspaceVO } from '@/api/auth'
import { deleteWorkspace } from '@/api/workspace'
import { confirmResourceDelete } from '@/composables/useResourceDelete'
import { useWorkspacePins } from '@/composables/useWorkspacePins'
import ConsumerSidebarQuota from '@/components/ConsumerSidebarQuota.vue'
import AnalyticsView from '@/views/AnalyticsView.vue'
import CreateWorkspaceDialog from '@/components/CreateWorkspaceDialog.vue'
import DashboardView from '@/views/DashboardView.vue'
import ExecutionsView from '@/views/ExecutionsView.vue'
import HelpFeedbackDialog from '@/components/HelpFeedbackDialog.vue'
import ImagePicker from '@/components/ImagePicker.vue'
import NotificationCenter from '@/components/NotificationCenter.vue'
import TeamView from '@/views/TeamView.vue'
import { useNotificationUnread } from '@/composables/useNotificationUnread'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'

defineProps<{
  collapsed?: boolean
  userName: string
  userHint?: string
  avatarText: string
  avatarUrl?: string
}>()

const emit = defineEmits<{
  logout: []
}>()

const router = useRouter()
const auth = useAuthStore()
const permissionStore = usePermissionStore()
const { isWorkspacePinned, toggleWorkspacePin } = useWorkspacePins()
const isEnterprise = computed(() => auth.tenant?.tenantType === 'ENTERPRISE')
const canOpenTeam = computed(
  () => isEnterprise.value && permissionStore.can('member:manage'),
)
const currentWorkspaceName = computed(() => auth.currentWorkspace?.name || '未选择工作空间')
const displayWorkspaces = computed(() => {
  const pinned = auth.workspaces.filter((item) => isWorkspacePinned(item.id))
  const rest = auth.workspaces.filter((item) => !isWorkspacePinned(item.id))
  return [...pinned, ...rest]
})

const menuVisible = ref(false)
const workspaceDialogVisible = ref(false)
const editingWorkspace = ref<WorkspaceVO | null>(null)
const helpDialogVisible = ref(false)
const overviewVisible = ref(false)
const analyticsVisible = ref(false)
const teamVisible = ref(false)
const inboxHover = ref(false)
const { unreadCount, refreshUnread } = useNotificationUnread()
const analyticsPane = ref<'analytics' | 'executions'>('analytics')

const unreadBadgeText = computed(() => (unreadCount.value > 99 ? '99+' : String(unreadCount.value)))

watch(menuVisible, (open) => {
  if (open) {
    void refreshUnread()
  }
})

watch(
  () => auth.currentWorkspaceId,
  (workspaceId, previousId) => {
    if (!workspaceId || workspaceId === previousId) {
      return
    }
    void refreshUnread()
  },
)

function go(path: string) {
  menuVisible.value = false
  router.push(path)
}

function goSettings() {
  go('/settings/profile')
}

function openDialog(name: 'overview' | 'analytics' | 'team') {
  if (name === 'team' && !canOpenTeam.value) {
    return
  }
  menuVisible.value = false
  overviewVisible.value = name === 'overview'
  analyticsVisible.value = name === 'analytics'
  teamVisible.value = name === 'team'
  if (name === 'analytics') {
    analyticsPane.value = 'analytics'
  }
}

function onLogout() {
  menuVisible.value = false
  emit('logout')
}

function onHelp() {
  menuVisible.value = false
  helpDialogVisible.value = true
}

function onCreateWorkspace() {
  menuVisible.value = false
  editingWorkspace.value = null
  workspaceDialogVisible.value = true
}

function onWorkspaceCreated() {
  closeWorkspaceOverlays()
}

function onWorkspaceUpdated() {
  closeWorkspaceOverlays()
}

function onSwitchWorkspace(item: WorkspaceVO) {
  closeWorkspaceOverlays()
  if (String(item.id) === auth.currentWorkspaceId) {
    return
  }
  auth.setWorkspace(item.id)
  MessagePlugin.success(`已切换到「${item.name}」`)
}

function canManageWorkspace(item: WorkspaceVO) {
  return item.roleCode === 'TENANT_ADMIN'
}

function workspaceActionOptions(item: WorkspaceVO): DropdownOption[] {
  const options: DropdownOption[] = [
    { content: isWorkspacePinned(item.id) ? '取消置顶' : '置顶', value: 'pin' },
  ]
  if (canManageWorkspace(item)) {
    options.push({ content: '编辑', value: 'edit' })
    options.push({ content: '删除', value: 'delete', theme: 'error' })
  }
  return options
}

function onWorkspaceAction(item: WorkspaceVO, option: DropdownOption) {
  const value = String(option.value ?? '')
  if (value === 'pin') {
    toggleWorkspacePin(item.id)
    return
  }
  if (value === 'edit') {
    menuVisible.value = false
    editingWorkspace.value = item
    workspaceDialogVisible.value = true
    return
  }
  if (value === 'delete') {
    menuVisible.value = false
    void removeWorkspace(item)
  }
}

async function removeWorkspace(item: WorkspaceVO) {
  await confirmResourceDelete({
    header: '删除工作空间',
    body: `确定删除工作空间「${item.name}」？空间内的智能体、知识库等资源将一并不可访问。`,
    resourceLabel: '工作空间',
    onDelete: async () => {
      await deleteWorkspace(item.id)
    },
    onSuccess: async () => {
      const wasCurrent = String(item.id) === auth.currentWorkspaceId
      await auth.refreshWorkspaces()
      if (wasCurrent && auth.workspaces.length > 0) {
        auth.setWorkspace(auth.workspaces[0].id)
      }
      MessagePlugin.success('工作空间已删除')
    },
  })
}

function closeWorkspaceOverlays() {
  menuVisible.value = false
  overviewVisible.value = false
  analyticsVisible.value = false
  teamVisible.value = false
}

onMounted(() => {
  void refreshUnread()
})
</script>

<style scoped>
.consumer-footer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.consumer-footer--collapsed {
  align-items: center;
}

.consumer-footer--collapsed .user-pill-wrap {
  flex: 0;
  width: 24px;
}

.consumer-footer__user {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.consumer-footer--collapsed .consumer-footer__user {
  flex-direction: column;
  gap: 8px;
}

.consumer-footer__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 0;
}

.consumer-footer :deep(.consumer-footer__icon-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--box-muted);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.consumer-footer :deep(.consumer-footer__icon-btn:hover) {
  background: var(--box-hover);
  color: var(--box-ink);
}

.consumer-footer :deep(.consumer-footer__icon-btn--warn) {
  color: var(--td-warning-color);
}

.consumer-footer :deep(.consumer-footer__icon-btn--danger) {
  color: var(--td-error-color);
}

.consumer-footer :deep(.consumer-footer__icon-btn .t-icon) {
  font-size: 16px;
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
  padding: 2px 4px 2px 2px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition: background 0.2s;
}

.user-pill:hover {
  background: var(--box-hover);
}

.user-pill-wrap {
  position: relative;
  flex: 1;
  min-width: 0;
}

.user-pill__unread {
  position: absolute;
  top: 0;
  left: 16px;
  z-index: 2;
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  border-radius: 999px;
  background: var(--td-error-color);
  color: #fff;
  font-size: 9px;
  font-weight: 600;
  line-height: 14px;
  text-align: center;
  box-sizing: border-box;
  pointer-events: none;
}

.user-pill__avatar {
  flex-shrink: 0;
  background: var(--box-ink);
  color: #fff;
  font-size: 12px;
}

.user-pill__avatar :deep(.t-avatar) {
  border-radius: 50%;
}

.user-pill__meta {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
}

.user-pill__name,
.user-pill__workspace {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.user-pill__name {
  font-size: 14px;
  font-weight: 500;
  color: var(--box-ink);
}

.user-pill__workspace {
  font-size: 12px;
  color: var(--box-muted);
}

.user-menu {
  width: 260px;
  padding: 12px 0 8px;
  border-radius: 12px;
  background: var(--box-surface);
  overflow: visible;
}

.user-menu__head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 16px 12px;
}

.user-menu__name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.user-menu__create {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: calc(100% - 32px);
  margin: 0 16px 8px;
  padding: 8px 12px;
  border: 1px solid var(--box-border);
  border-radius: 10px;
  background: var(--box-surface);
  color: var(--box-ink);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.user-menu__create:hover {
  border-color: var(--box-border);
  background: var(--box-hover);
}

.user-menu__workspaces {
  max-height: 220px;
  overflow: auto;
  padding: 4px 8px 8px;
}

.user-menu__label {
  margin: 0 8px 4px;
  font-size: 12px;
  color: var(--box-muted);
}

.user-menu__workspace-row {
  display: flex;
  align-items: center;
  gap: 2px;
}

.user-menu__item--workspace {
  flex: 1;
  min-width: 0;
}

.user-menu__workspace-avatar {
  flex-shrink: 0;
  background: var(--box-ink);
  color: #fff;
  font-size: 11px;
}

.user-menu__workspace-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-menu__more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--box-muted);
  cursor: pointer;
}

.user-menu__more:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}

.user-menu__pin {
  font-size: 12px;
  color: var(--box-muted);
}

.user-menu__check {
  margin-left: auto;
  color: var(--td-brand-color);
}

.user-menu__item--active {
  background: var(--box-active);
}

.user-menu__section {
  padding: 4px 8px;
  border-top: 1px solid var(--box-border);
}

.user-menu__section--last {
  padding-bottom: 0;
}

.user-menu__item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--box-ink);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s;
}

.user-menu__item:hover {
  background: var(--box-hover);
}

.user-menu__item--hint {
  color: var(--box-muted);
}

.user-menu__chevron {
  font-size: 14px;
  color: var(--box-muted);
}

.user-menu :deep(.t-popup) {
  display: block;
  width: 100%;
}

.user-menu__item .user-menu__chevron:last-child {
  margin-left: auto;
}

.user-menu__badge {
  margin-left: auto;
  min-width: 16px;
  height: 16px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--td-error-color);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  box-sizing: border-box;
}

.user-menu__badge + .user-menu__chevron {
  margin-left: 0;
}

.analytics-dialog__header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}
</style>
