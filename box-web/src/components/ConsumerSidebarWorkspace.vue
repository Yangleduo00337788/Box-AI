<template>
  <div class="sidebar-workspace">
    <section class="sidebar-section sidebar-section--compact">
      <router-link
        v-for="item in workbenchItems"
        :key="item.value"
        :to="item.value"
        class="sidebar-line sidebar-line--nav"
        :class="{ 'sidebar-line--active': isWorkbenchItemActive(item.value) }"
        @click="focusWorkbench(item.value)"
      >
        <span class="sidebar-line__icon">
          <t-icon :name="resolveTIconName(item.icon)" />
        </span>
        <span class="sidebar-line__label">{{ item.label }}</span>
      </router-link>

      <template v-if="pinnedItems.length">
        <div class="sidebar-section-row">
          <button
            type="button"
            class="sidebar-line sidebar-line--nav sidebar-line--section"
            @click="pinnedExpanded = !pinnedExpanded"
          >
            <span class="sidebar-line__icon">
              <t-icon name="pin" />
            </span>
            <span class="sidebar-line__label">置顶</span>
          </button>
          <div class="sidebar-section-row__actions">
            <button
              type="button"
              class="sidebar-section-row__btn"
              :aria-label="pinnedExpanded ? '折叠置顶' : '展开置顶'"
              @click.stop="pinnedExpanded = !pinnedExpanded"
            >
              <t-icon :name="pinnedExpanded ? 'chevron-down' : 'chevron-right'" />
            </button>
          </div>
        </div>

        <div v-show="pinnedExpanded" class="sidebar-group">
          <div
            v-for="item in pinnedItems"
            :key="item.key"
            class="sidebar-row-wrap"
            :class="{ 'sidebar-row-wrap--active': isPinnedItemActive(item) }"
          >
            <router-link
              :to="item.to"
              class="sidebar-line sidebar-line--item"
              @click="onPinnedItemClick(item)"
            >
              <t-avatar size="20px" shape="circle" class="sidebar-line__avatar" :style="{ background: item.color }">
                {{ item.avatarText }}
              </t-avatar>
              <span class="sidebar-line__label">{{ item.label }}</span>
              <span class="sidebar-line__meta">{{ item.meta }}</span>
            </router-link>
            <div class="sidebar-row__actions">
              <button type="button" class="sidebar-row__action" aria-label="取消置顶" @click.stop="item.onUnpin?.()">
                <t-icon name="pin" />
              </button>
            </div>
          </div>
        </div>
      </template>

      <div class="sidebar-section-row">
        <button
          type="button"
          class="sidebar-line sidebar-line--nav sidebar-line--section"
          @click="onAgentsSectionClick"
        >
          <span class="sidebar-line__icon">
            <t-icon name="robot" />
          </span>
          <span class="sidebar-line__label">智能体</span>
        </button>
        <div class="sidebar-section-row__actions">
          <t-tooltip content="创建智能体" placement="top" theme="light" :show-arrow="false">
            <button type="button" class="sidebar-section-row__btn" aria-label="创建智能体" @click.stop="goCreateAgent">
              <t-icon name="add" />
            </button>
          </t-tooltip>
          <button
            type="button"
            class="sidebar-section-row__btn"
            :aria-label="agentsExpanded ? '折叠智能体' : '展开智能体'"
            @click.stop="toggleAgentsExpanded"
          >
            <t-icon :name="agentsExpanded ? 'chevron-down' : 'chevron-right'" />
          </button>
        </div>
      </div>

      <t-loading v-show="agentsExpanded" :loading="agentsLoading" size="small" class="sidebar-group">
        <div v-if="unpinnedAgents.length" class="sidebar-group__list">
          <div
            v-for="agent in unpinnedAgents"
            :key="agent.id"
            class="sidebar-row-wrap"
            :class="{ 'sidebar-row-wrap--active': isAgentRowActive(agent.id) }"
          >
            <button type="button" class="sidebar-line sidebar-line--item" @click="openAgent(agent)">
              <t-avatar
                size="20px"
                shape="circle"
                class="sidebar-line__avatar"
                :style="{ background: getAvatarColor(agent.name) }"
              >
                {{ agent.name.slice(0, 1) }}
              </t-avatar>
              <span class="sidebar-line__label">{{ agent.name }}</span>
              <span class="sidebar-line__meta">{{ formatRelativeTime(agent.updatedAt) }}</span>
            </button>
            <div class="sidebar-row__actions">
              <button
                type="button"
                class="sidebar-row__action"
                :aria-label="isAgentPinned(agent.id) ? '取消置顶' : '置顶'"
                @click.stop="handleToggleAgentPin(agent.id)"
              >
                <t-icon name="pin" />
              </button>
              <t-dropdown :options="agentMenuOptions(agent)" trigger="click" @click="onAgentMenu">
                <button type="button" class="sidebar-row__action" aria-label="更多" @click.stop>
                  <t-icon name="more" />
                </button>
              </t-dropdown>
            </div>
          </div>
        </div>
        <div v-else class="sidebar-line sidebar-line--placeholder">
          <span class="sidebar-line__placeholder">暂无智能体</span>
        </div>
      </t-loading>

      <div class="sidebar-section-row">
        <button
          type="button"
          class="sidebar-line sidebar-line--nav sidebar-line--section"
          @click="onProjectsSectionClick"
        >
          <span class="sidebar-line__icon">
            <t-icon name="folder" />
          </span>
          <span class="sidebar-line__label">项目</span>
        </button>
        <div class="sidebar-section-row__actions">
          <t-tooltip content="创建项目" placement="top" theme="light" :show-arrow="false">
            <button type="button" class="sidebar-section-row__btn" aria-label="创建项目" @click.stop="onCreateProject">
              <t-icon name="add" />
            </button>
          </t-tooltip>
          <button
            type="button"
            class="sidebar-section-row__btn"
            :aria-label="projectsExpanded ? '折叠项目' : '展开项目'"
            @click.stop="toggleProjectsExpanded"
          >
            <t-icon :name="projectsExpanded ? 'chevron-down' : 'chevron-right'" />
          </button>
        </div>
      </div>

      <div v-show="projectsExpanded" class="sidebar-group">
        <div v-if="!auth.workspaces.length" class="sidebar-line sidebar-line--placeholder">
          <span class="sidebar-line__placeholder">暂无项目</span>
        </div>
        <div v-else-if="unpinnedWorkspaces.length" class="sidebar-group__list">
          <div
            v-for="workspace in unpinnedWorkspaces"
            :key="workspace.id"
            class="sidebar-row-wrap"
            :class="{ 'sidebar-row-wrap--active': isWorkspaceRowActive(workspace.id) }"
          >
            <button type="button" class="sidebar-line sidebar-line--item" @click="switchWorkspace(workspace.id)">
              <span class="sidebar-line__icon">
                <t-icon name="folder" />
              </span>
              <span class="sidebar-line__label">{{ workspace.name }}</span>
              <span v-if="isWorkspaceActive(workspace.id)" class="sidebar-line__meta">当前</span>
            </button>
            <div class="sidebar-row__actions">
              <button
                type="button"
                class="sidebar-row__action"
                :aria-label="isWorkspacePinned(workspace.id) ? '取消置顶' : '置顶'"
                @click.stop="handleToggleWorkspacePin(workspace.id)"
              >
                <t-icon name="pin" />
              </button>
              <t-dropdown :options="workspaceMenuOptions(workspace)" trigger="click" @click="onWorkspaceMenu">
                <button type="button" class="sidebar-row__action" aria-label="更多" @click.stop>
                  <t-icon name="more" />
                </button>
              </t-dropdown>
            </div>
          </div>
        </div>
      </div>
    </section>

    <create-workspace-dialog v-model:visible="workspaceDialogVisible" @created="onWorkspaceCreated" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { DropdownOption } from 'tdesign-vue-next'
import type { AgentVO } from '@/api/agent'
import { useAgentSelection } from '@/composables/useAgentSelection'
import { useConversationNav } from '@/composables/useConversationNav'
import { useSidebarPins } from '@/composables/useSidebarPins'
import { useWorkspacePins } from '@/composables/useWorkspacePins'
import CreateWorkspaceDialog from '@/components/CreateWorkspaceDialog.vue'
import { formatRelativeTime, getAvatarColor } from '@/utils/format'
import { resolveTIconName } from '@/utils/icon'
import { useAuthStore } from '@/stores/auth'
import { CONSUMER_MENU_GROUPS } from '@/constants/menu'
import type { WorkspaceVO } from '@/api/auth'

defineProps<{
  active: string
}>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const workspaceDialogVisible = ref(false)
const { agents, loading: agentsLoading, selectedAgentId, refresh: refreshAgents, reloadSelection, selectAgent } = useAgentSelection()
const { conversations, refresh: refreshConversations } = useConversationNav()
const { pinnedAgentIds, pinnedConversationIds, isAgentPinned, toggleAgentPin, toggleConversationPin, refresh: refreshPins } = useSidebarPins()
const { pinnedWorkspaceIds, isWorkspacePinned, toggleWorkspacePin, refresh: refreshWorkspacePins } = useWorkspacePins()

const workbenchItems = CONSUMER_MENU_GROUPS[0]?.items ?? []
const pinnedExpanded = ref(true)
const agentsExpanded = ref(true)
const projectsExpanded = ref(false)

type SidebarFocus =
  | { type: 'workbench'; path: string }
  | { type: 'agent'; id: number }
  | { type: 'conversation'; id: number }
  | { type: 'workspace'; id: number }

const sidebarFocus = ref<SidebarFocus>({ type: 'workbench', path: '/chat' })

function focusWorkbench(path: string) {
  sidebarFocus.value = { type: 'workbench', path }
}

function focusAgent(agentId: number) {
  sidebarFocus.value = { type: 'agent', id: agentId }
}

function focusConversation(conversationId: number) {
  sidebarFocus.value = { type: 'conversation', id: conversationId }
}

function focusWorkspace(workspaceId: number) {
  sidebarFocus.value = { type: 'workspace', id: workspaceId }
}

function isWorkbenchItemActive(value: string) {
  return sidebarFocus.value.type === 'workbench' && sidebarFocus.value.path === value
}

function isAgentRowActive(agentId: number) {
  return sidebarFocus.value.type === 'agent' && sidebarFocus.value.id === agentId
}

function isConversationRowActive(conversationId: number) {
  return sidebarFocus.value.type === 'conversation' && sidebarFocus.value.id === conversationId
}

function isWorkspaceRowActive(workspaceId: number) {
  return sidebarFocus.value.type === 'workspace' && sidebarFocus.value.id === workspaceId
}

function isPinnedItemActive(item: PinnedSidebarItem) {
  if (item.key.startsWith('conversation-')) {
    const id = Number(item.key.slice('conversation-'.length))
    return isConversationRowActive(id)
  }
  if (item.key.startsWith('agent-')) {
    const id = Number(item.key.slice('agent-'.length))
    return isAgentRowActive(id)
  }
  if (item.key.startsWith('workspace-')) {
    const id = Number(item.key.slice('workspace-'.length))
    return isWorkspaceRowActive(id)
  }
  return false
}

function onPinnedItemClick(item: PinnedSidebarItem) {
  if (item.key.startsWith('conversation-')) {
    const id = Number(item.key.slice('conversation-'.length))
    focusConversation(id)
    return
  }
  if (item.key.startsWith('agent-')) {
    const id = Number(item.key.slice('agent-'.length))
    focusAgent(id)
    item.onNavigate?.()
    return
  }
  if (item.key.startsWith('workspace-')) {
    const id = Number(item.key.slice('workspace-'.length))
    focusWorkspace(id)
    item.onNavigate?.()
  }
}

function onAgentsSectionClick() {
  agentsExpanded.value = !agentsExpanded.value
}

function toggleAgentsExpanded() {
  agentsExpanded.value = !agentsExpanded.value
}

function onProjectsSectionClick() {
  projectsExpanded.value = !projectsExpanded.value
}

function toggleProjectsExpanded() {
  projectsExpanded.value = !projectsExpanded.value
}

function initSidebarFocusFromRoute() {
  const path = route.path
  if (path.startsWith('/plugin-market')) {
    focusWorkbench('/plugin-market')
    return
  }
  if (path.startsWith('/models')) {
    focusWorkbench('/models')
    return
  }
  if (activeConversationId.value != null) {
    focusConversation(activeConversationId.value)
    return
  }
  if ((path === '/chat' || path.startsWith('/chat/')) && selectedAgentId.value != null) {
    focusAgent(selectedAgentId.value)
    return
  }
  if (path === '/chat' || path.startsWith('/chat/')) {
    focusWorkbench('/chat')
  }
}

watch(
  () => route.fullPath,
  () => {
    const path = route.path
    if (path.startsWith('/plugin-market')) {
      focusWorkbench('/plugin-market')
      return
    }
    if (path.startsWith('/models')) {
      focusWorkbench('/models')
      return
    }
    if (activeConversationId.value != null) {
      focusConversation(activeConversationId.value)
    }
  },
)

const activeConversationId = computed(() => {
  const id = route.params.id
  if (!id || Array.isArray(id)) return null
  const num = Number(id)
  return Number.isFinite(num) ? num : null
})

interface PinnedSidebarItem {
  key: string
  label: string
  avatarText: string
  color: string
  meta: string
  to: string
  onNavigate?: () => void
  onUnpin?: () => void
}

const pinnedItems = computed<PinnedSidebarItem[]>(() => {
  const items: PinnedSidebarItem[] = []

  for (const id of pinnedConversationIds.value) {
    const conversation = conversations.value.find((item) => item.id === id)
    if (!conversation) continue
    items.push({
      key: `conversation-${id}`,
      label: conversation.title || conversation.agentName || '新会话',
      avatarText: (conversation.agentName || '会').slice(0, 1),
      color: getAvatarColor(conversation.agentName || '会话'),
      meta: formatRelativeTime(conversation.lastMessageAt || conversation.updatedAt),
      to: `/chat/${conversation.id}`,
      onUnpin: () => handleToggleConversationPin(id),
    })
  }

  for (const id of pinnedAgentIds.value) {
    const agent = agents.value.find((item) => item.id === id)
    if (!agent) continue
    items.push({
      key: `agent-${id}`,
      label: agent.name,
      avatarText: agent.name.slice(0, 1),
      color: getAvatarColor(agent.name),
      meta: formatRelativeTime(agent.updatedAt),
      to: '/chat',
      onNavigate: () => selectAgent(agent.id),
      onUnpin: () => handleToggleAgentPin(id),
    })
  }

  for (const id of pinnedWorkspaceIds.value) {
    const workspace = auth.workspaces.find((item) => item.id === id)
    if (!workspace) continue
    items.push({
      key: `workspace-${id}`,
      label: workspace.name,
      avatarText: workspace.name.slice(0, 1),
      color: getAvatarColor(workspace.name),
      meta: isWorkspaceActive(id) ? '当前' : '项目',
      to: route.path,
      onNavigate: () => switchWorkspace(id),
      onUnpin: () => handleToggleWorkspacePin(id),
    })
  }

  return items
})

const unpinnedAgents = computed(() => {
  const pinnedSet = new Set(pinnedAgentIds.value)
  return agents.value.filter((agent) => !pinnedSet.has(agent.id))
})

const unpinnedWorkspaces = computed(() => {
  const pinnedSet = new Set(pinnedWorkspaceIds.value)
  return auth.workspaces.filter((workspace) => !pinnedSet.has(workspace.id))
})

function goCreateAgent() {
  router.push('/agents')
}

async function handleToggleAgentPin(agentId: number) {
  const wasPinned = isAgentPinned(agentId)
  try {
    await toggleAgentPin(agentId)
    if (!wasPinned) {
      pinnedExpanded.value = true
    }
  } catch {
    MessagePlugin.error('置顶操作失败')
  }
}

async function handleToggleConversationPin(conversationId: number) {
  try {
    await toggleConversationPin(conversationId)
  } catch {
    MessagePlugin.error('置顶操作失败')
  }
}

function handleToggleWorkspacePin(workspaceId: number) {
  const wasPinned = isWorkspacePinned(workspaceId)
  toggleWorkspacePin(workspaceId)
  if (!wasPinned) {
    pinnedExpanded.value = true
  }
}

function openAgent(agent: AgentVO) {
  focusAgent(agent.id)
  selectAgent(agent.id)
  router.push('/chat')
}

function isWorkspaceActive(workspaceId: number) {
  return auth.currentWorkspaceId === String(workspaceId)
}

async function switchWorkspace(workspaceId: number) {
  focusWorkspace(workspaceId)
  if (isWorkspaceActive(workspaceId)) return
  auth.setWorkspace(workspaceId)
  await reloadSelection()
  await Promise.all([refreshAgents(), refreshConversations(), refreshPins()])
  MessagePlugin.success('已切换工作空间')
}

function onCreateProject() {
  workspaceDialogVisible.value = true
}

async function onWorkspaceCreated() {
  refreshWorkspacePins()
  await Promise.all([refreshAgents(), refreshConversations()])
}

function workspaceMenuOptions(workspace: WorkspaceVO): DropdownOption[] {
  const options: DropdownOption[] = [
    { content: '切换到此项目', value: `switch:${workspace.id}` },
  ]
  if (!isWorkspacePinned(workspace.id)) {
    options.push({ content: '置顶', value: `pin:${workspace.id}` })
  }
  return options
}

function onWorkspaceMenu(data: { value?: string | number }) {
  const value = String(data?.value ?? '')
  if (value.startsWith('switch:')) {
    const id = Number(value.split(':')[1])
    if (Number.isFinite(id)) {
      switchWorkspace(id)
    }
    return
  }
  if (value.startsWith('pin:')) {
    const id = Number(value.split(':')[1])
    if (Number.isFinite(id)) {
      handleToggleWorkspacePin(id)
    }
  }
}

function agentMenuOptions(agent: AgentVO): DropdownOption[] {
  return [
    { content: '编辑配置', value: `builder:${agent.id}` },
    { content: '管理全部', value: 'manage' },
  ]
}

function onAgentMenu(data: { value?: string | number }) {
  const value = String(data?.value ?? '')
  if (value === 'manage') {
    router.push('/agents')
    return
  }
  if (value.startsWith('builder:')) {
    const id = Number(value.split(':')[1])
    if (Number.isFinite(id)) {
      router.push(`/agents/${id}/builder`)
    }
  }
}

onMounted(async () => {
  refreshWorkspacePins()
  await Promise.all([refreshAgents(), refreshConversations(), refreshPins()])
  initSidebarFocusFromRoute()
})
</script>

<style scoped>
.sidebar-workspace {
  --sidebar-row-pad-x: 8px;
  --sidebar-icon-size: 20px;
  --sidebar-col-gap: 6px;
  --sidebar-icon-col: var(--sidebar-icon-size);
  --sidebar-row-bg: rgba(0, 0, 0, 0.04);
  --sidebar-text: var(--box-ink);

  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0;
  box-sizing: border-box;
}

.sidebar-section--compact {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-line {
  box-sizing: border-box;
  display: grid;
  grid-template-columns: var(--sidebar-icon-col) minmax(0, 1fr) auto;
  column-gap: var(--sidebar-col-gap);
  align-items: center;
  width: 100%;
  min-height: 36px;
  padding: 0 var(--sidebar-row-pad-x);
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--box-ink);
  text-decoration: none;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s;
}

.sidebar-line--nav {
  grid-template-columns: var(--sidebar-icon-col) minmax(0, 1fr);
  color: var(--sidebar-text);
}

.sidebar-line--section {
  padding-right: 56px;
}

.sidebar-line--nav:hover,
.sidebar-line--nav.sidebar-line--active {
  background: var(--sidebar-row-bg);
  color: var(--box-ink);
}

.sidebar-section-row {
  position: relative;
  border-radius: 8px;
}

.sidebar-section-row:hover {
  background: var(--sidebar-row-bg);
}

.sidebar-section-row--active {
  background: var(--sidebar-row-bg);
}

.sidebar-section-row--active:hover .sidebar-line--section {
  background: transparent;
}

.sidebar-section-row:hover .sidebar-line--section {
  background: transparent;
}

.sidebar-section-row__actions {
  position: absolute;
  right: var(--sidebar-row-pad-x);
  top: 50%;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transform: translateY(-50%);
  transition: opacity 0.15s;
  pointer-events: none;
}

.sidebar-section-row:hover .sidebar-section-row__actions {
  opacity: 1;
  pointer-events: auto;
}

.sidebar-section-row__btn {
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

.sidebar-section-row__btn :deep(.t-icon) {
  font-size: 16px;
}

.sidebar-section-row__btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: var(--box-ink);
}

.sidebar-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-group__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-row-wrap {
  position: relative;
  padding: 0 var(--sidebar-row-pad-x);
  border-radius: 8px;
  box-sizing: border-box;
}

.sidebar-row-wrap:hover,
.sidebar-row-wrap--active {
  background: var(--sidebar-row-bg);
}

.sidebar-line--item {
  background: transparent;
}

.sidebar-line--item.sidebar-line--active {
  background: var(--sidebar-row-bg);
}

.sidebar-line--placeholder {
  grid-template-columns: minmax(0, 1fr);
  min-height: 32px;
  cursor: default;
  color: #646a73;
}

.sidebar-line__placeholder {
  padding-left: calc(var(--sidebar-row-pad-x) + var(--sidebar-icon-col) + var(--sidebar-col-gap));
  font-size: 12px;
  line-height: 20px;
}

.sidebar-line__icon {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  width: var(--sidebar-icon-size);
  height: var(--sidebar-icon-size);
  flex-shrink: 0;
}

.sidebar-line__icon :deep(.t-icon) {
  font-size: 16px;
  line-height: 1;
}

.sidebar-line__avatar {
  width: var(--sidebar-icon-size);
  height: var(--sidebar-icon-size);
  flex-shrink: 0;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.sidebar-line__avatar :deep(.t-avatar) {
  margin: 0;
}

.sidebar-line__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  line-height: 20px;
  color: var(--sidebar-text);
}

.sidebar-line__meta {
  grid-column: 3;
  font-size: 12px;
  line-height: 20px;
  color: var(--box-muted);
  transition: opacity 0.15s;
}

.sidebar-row-wrap:hover .sidebar-line__meta {
  opacity: 0;
}

.sidebar-row-wrap:hover .sidebar-row__actions {
  opacity: 1;
}

.sidebar-row__actions {
  position: absolute;
  right: var(--sidebar-row-pad-x);
  top: 50%;
  display: inline-flex;
  align-items: center;
  gap: 0;
  opacity: 0;
  transform: translateY(-50%);
  transition: opacity 0.15s;
  pointer-events: none;
}

.sidebar-row-wrap:hover .sidebar-row__actions {
  pointer-events: auto;
}

.sidebar-row__action {
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
}

.sidebar-row__action :deep(.t-icon) {
  font-size: 16px;
}

.sidebar-row__action:hover {
  background: rgba(0, 0, 0, 0.06);
  color: var(--box-ink);
}

.sidebar-row-wrap .sidebar-line--item {
  padding-left: 0;
  padding-right: 48px;
}

.sidebar-group :deep(.t-loading) {
  display: block;
  width: 100%;
}

.sidebar-group :deep(.t-loading__parent) {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
}
</style>
