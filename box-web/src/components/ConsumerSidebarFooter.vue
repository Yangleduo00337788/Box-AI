<template>
  <div class="consumer-footer" :class="{ 'consumer-footer--collapsed': collapsed }">
    <div class="consumer-footer__user">
      <t-popup
        v-model:visible="menuVisible"
        placement="top-left"
        trigger="click"
        :overlay-inner-style="{ padding: 0, maxHeight: 'none', overflow: 'visible' }"
      >
        <button type="button" class="user-pill">
          <t-avatar
            size="28px"
            shape="circle"
            class="user-pill__avatar"
            :image="avatarUrl || undefined"
          >
            {{ avatarText }}
          </t-avatar>
          <span v-if="!collapsed" class="user-pill__name">{{ userName }}</span>
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
              <t-icon name="user-add" />
              创建工作空间
            </button>

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
              <button v-if="isEnterprise" type="button" class="user-menu__item" @click="openDialog('team')">
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
                  <t-badge
                    v-if="unreadCount"
                    :count="unreadCount"
                    :max-count="99"
                    class="user-menu__badge"
                  />
                  <t-icon name="chevron-right" class="user-menu__chevron" />
                </button>
                <template #content>
                  <notification-center :active="inboxHover" @unread-change="unreadCount = $event" />
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
    </div>

    <create-workspace-dialog v-model:visible="workspaceDialogVisible" />
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
      <team-view v-if="teamVisible" compact />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AnalyticsView from '@/views/AnalyticsView.vue'
import CreateWorkspaceDialog from '@/components/CreateWorkspaceDialog.vue'
import DashboardView from '@/views/DashboardView.vue'
import ExecutionsView from '@/views/ExecutionsView.vue'
import HelpFeedbackDialog from '@/components/HelpFeedbackDialog.vue'
import ImagePicker from '@/components/ImagePicker.vue'
import NotificationCenter from '@/components/NotificationCenter.vue'
import TeamView from '@/views/TeamView.vue'
import { getUnreadNotificationCount } from '@/api/notification'
import { useAuthStore } from '@/stores/auth'

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
const isEnterprise = computed(() => auth.tenant?.tenantType === 'ENTERPRISE')

const menuVisible = ref(false)
const workspaceDialogVisible = ref(false)
const helpDialogVisible = ref(false)
const overviewVisible = ref(false)
const analyticsVisible = ref(false)
const teamVisible = ref(false)
const inboxHover = ref(false)
const unreadCount = ref(0)
const analyticsPane = ref<'analytics' | 'executions'>('analytics')

function go(path: string) {
  menuVisible.value = false
  router.push(path)
}

function openDialog(name: 'overview' | 'analytics' | 'team') {
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
  workspaceDialogVisible.value = true
}

async function refreshUnread() {
  const { data } = await getUnreadNotificationCount()
  unreadCount.value = data.data?.count || 0
}

let unreadTimer: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  refreshUnread()
  unreadTimer = setInterval(refreshUnread, 60_000)
})

onUnmounted(() => {
  if (unreadTimer) clearInterval(unreadTimer)
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

.consumer-footer__user {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  padding: 4px 8px 4px 4px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition: background 0.2s;
}

.user-pill:hover {
  background: rgba(0, 0, 0, 0.04);
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

.user-pill__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--box-ink);
}

.user-menu {
  width: 260px;
  padding: 12px 0 8px;
  border-radius: 12px;
  background: #fff;
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
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  background: #fff;
  color: #1f2329;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.user-menu__create:hover {
  border-color: #d0d3d6;
  background: #fafbfc;
}

.user-menu__section {
  padding: 4px 8px;
  border-top: 1px solid #f0f1f2;
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
  color: #1f2329;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s;
}

.user-menu__item:hover {
  background: #f5f6f7;
}

.user-menu__item--hint {
  color: #646a73;
}

.user-menu__chevron {
  font-size: 14px;
  color: #b0b4bc;
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
