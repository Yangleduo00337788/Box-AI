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

      <section class="sidebar-task-panel">
        <div class="sidebar-task-panel__head">
          <span class="sidebar-task-panel__title">对话历史</span>
          <div class="sidebar-task-panel__tools">
            <t-tooltip
              :content="conversationsExpanded ? '收起列表' : '展开列表'"
              placement="top"
              theme="light"
              :show-arrow="false"
              attach="body"
            >
              <button
                type="button"
                class="sidebar-task-panel__tool"
                :aria-label="conversationsExpanded ? '收起列表' : '展开列表'"
                @click="toggleConversationsExpanded"
              >
                <t-icon :name="conversationsExpanded ? 'fullscreen-exit' : 'fullscreen'" />
              </button>
            </t-tooltip>
            <t-dropdown :options="conversationFilterOptions" trigger="click" @click="onConversationFilter">
              <t-tooltip content="筛选对话" placement="top" theme="light" :show-arrow="false" attach="body">
                <button type="button" class="sidebar-task-panel__tool" aria-label="筛选对话">
                  <t-icon name="filter" />
                </button>
              </t-tooltip>
            </t-dropdown>
          </div>
        </div>

        <t-loading v-show="conversationsExpanded" :loading="conversationsLoading" size="small">
          <nav v-if="displayConversations.length" class="sidebar-task-panel__list">
            <div
              v-for="conversation in displayConversations"
              :key="conversation.id"
              class="sidebar-task-row-wrap"
              :class="{ 'sidebar-task-row-wrap--active': isConversationRowActive(conversation.id) }"
            >
              <button
                type="button"
                class="sidebar-task-row"
                @click="openConversation(conversation)"
              >
                <t-icon name="folder" class="sidebar-task-row__icon" />
                <span class="sidebar-task-row__label">{{ conversationLabel(conversation) }}</span>
              </button>
              <div class="sidebar-task-row__actions">
                <t-tooltip
                  :content="isConversationPinned(conversation.id) ? '取消置顶' : '置顶'"
                  placement="top"
                  theme="light"
                  :show-arrow="false"
                  attach="body"
                >
                  <button
                    type="button"
                    class="sidebar-task-row__action"
                    :aria-label="isConversationPinned(conversation.id) ? '取消置顶' : '置顶'"
                    @click.stop="handleToggleConversationPin(conversation.id)"
                  >
                    <t-icon name="pin" />
                  </button>
                </t-tooltip>
                <t-tooltip content="改名" placement="top" theme="light" :show-arrow="false" attach="body">
                  <button
                    type="button"
                    class="sidebar-task-row__action"
                    aria-label="改名"
                    @click.stop="openRenameConversation(conversation)"
                  >
                    <t-icon name="edit-1" />
                  </button>
                </t-tooltip>
                <t-tooltip content="删除" placement="top" theme="light" :show-arrow="false" attach="body">
                  <button
                    type="button"
                    class="sidebar-task-row__action"
                    aria-label="删除"
                    @click.stop="handleDeleteConversation(conversation)"
                  >
                    <t-icon name="delete" />
                  </button>
                </t-tooltip>
              </div>
            </div>
          </nav>
          <p v-else class="sidebar-task-panel__empty">暂无对话</p>
        </t-loading>
      </section>

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
            <t-tooltip
              :content="pinnedExpanded ? '折叠置顶' : '展开置顶'"
              placement="top"
              theme="light"
              :show-arrow="false"
              attach="body"
            >
              <button
                type="button"
                class="sidebar-section-row__btn"
                :aria-label="pinnedExpanded ? '折叠置顶' : '展开置顶'"
                @click.stop="pinnedExpanded = !pinnedExpanded"
              >
                <t-icon :name="pinnedExpanded ? 'chevron-down' : 'chevron-right'" />
              </button>
            </t-tooltip>
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
              <t-avatar
                size="20px"
                shape="circle"
                class="sidebar-line__avatar"
                :image="item.avatarUrl || undefined"
                :style="{ background: item.color }"
              >
                {{ item.avatarText }}
              </t-avatar>
              <span class="sidebar-line__label">{{ item.label }}</span>
              <span class="sidebar-line__meta">{{ item.meta }}</span>
            </router-link>
            <div class="sidebar-row__actions">
              <t-tooltip content="取消置顶" placement="top" theme="light" :show-arrow="false" attach="body">
                <button type="button" class="sidebar-row__action" aria-label="取消置顶" @click.stop="item.onUnpin?.()">
                  <t-icon name="pin" />
                </button>
              </t-tooltip>
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
          <span class="sidebar-line__label">我的智能体</span>
        </button>
        <div class="sidebar-section-row__actions">
          <t-tooltip content="创建智能体" placement="top" theme="light" :show-arrow="false" attach="body">
            <button type="button" class="sidebar-section-row__btn" aria-label="创建智能体" @click.stop="goCreateAgent">
              <t-icon name="add" />
            </button>
          </t-tooltip>
          <t-tooltip
            :content="agentsExpanded ? '折叠智能体' : '展开智能体'"
            placement="top"
            theme="light"
            :show-arrow="false"
            attach="body"
          >
            <button
              type="button"
              class="sidebar-section-row__btn"
              :aria-label="agentsExpanded ? '折叠智能体' : '展开智能体'"
              @click.stop="toggleAgentsExpanded"
            >
              <t-icon :name="agentsExpanded ? 'chevron-down' : 'chevron-right'" />
            </button>
          </t-tooltip>
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
                :image="agent.avatarUrl || undefined"
                :style="{ background: getAvatarColor(agent.name) }"
              >
                {{ agent.name.slice(0, 1) }}
              </t-avatar>
              <span class="sidebar-line__label">{{ agent.name }}</span>
              <span class="sidebar-line__meta">{{ formatRelativeTime(agent.updatedAt) }}</span>
            </button>
            <div class="sidebar-row__actions">
              <t-tooltip
                :content="isAgentPinned(agent.id) ? '取消置顶' : '置顶'"
                placement="top"
                theme="light"
                :show-arrow="false"
                attach="body"
              >
                <button
                  type="button"
                  class="sidebar-row__action"
                  :aria-label="isAgentPinned(agent.id) ? '取消置顶' : '置顶'"
                  @click.stop="handleToggleAgentPin(agent.id)"
                >
                  <t-icon name="pin" />
                </button>
              </t-tooltip>
              <t-dropdown :options="agentMenuOptions(agent)" trigger="click" @click="onAgentMenu">
                <t-tooltip content="更多" placement="top" theme="light" :show-arrow="false" attach="body">
                  <button type="button" class="sidebar-row__action" aria-label="更多" @click.stop>
                    <t-icon name="more" />
                  </button>
                </t-tooltip>
              </t-dropdown>
            </div>
          </div>
        </div>
        <div v-else class="sidebar-line sidebar-line--placeholder">
          <span class="sidebar-line__placeholder">暂无智能体</span>
        </div>
      </t-loading>

    </section>

    <t-dialog
      v-if="renameVisible"
      v-model:visible="renameVisible"
      header="对话改名"
      width="400px"
      attach="body"
      :destroy-on-close="true"
      confirm-btn="保存"
      cancel-btn="取消"
      :confirm-loading="renameSaving"
      :confirm-on-enter="true"
      @confirm="confirmRenameConversation"
    >
      <t-input
        v-model="renameTitle"
        :maxlength="255"
        show-limit-number
        placeholder="请输入对话名称"
        :autofocus="true"
      />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { DropdownOption } from 'tdesign-vue-next'
import type { AgentVO } from '@/api/agent'
import { extractApiError } from '@/api/apiError'
import { deleteConversation, renameConversation, type ConversationVO } from '@/api/conversation'
import { confirmResourceDelete } from '@/composables/useResourceDelete'
import { useAgentSelection } from '@/composables/useAgentSelection'
import { useCreateAgentDialog } from '@/composables/useCreateAgentDialog'
import { useConversationNav } from '@/composables/useConversationNav'
import { useSidebarPins } from '@/composables/useSidebarPins'
import { formatRelativeTime, getAvatarColor } from '@/utils/format'
import { resolveTIconName } from '@/utils/icon'
import { CONSUMER_MENU_GROUPS } from '@/constants/menu'
import { useAuthStore } from '@/stores/auth'

defineProps<{
  active: string
}>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { agents, loading: agentsLoading, selectedAgentId, refresh: refreshAgents, selectAgent, selectAgentByConversation } = useAgentSelection()
const { conversations, loading: conversationsLoading, refresh: refreshConversations } = useConversationNav()
const { openCreateAgentDialog } = useCreateAgentDialog()
const {
  pinnedAgentIds,
  isAgentPinned,
  isConversationPinned,
  toggleAgentPin,
  toggleConversationPin,
  unpinConversation,
  refresh: refreshPins,
} = useSidebarPins()

const workbenchItems = CONSUMER_MENU_GROUPS[0]?.items ?? []
const pinnedExpanded = ref(true)
const conversationsExpanded = ref(false)
const conversationFilter = ref<'all' | 'current-agent'>('all')
const agentsExpanded = ref(true)
const renameVisible = ref(false)
const renameSaving = ref(false)
const renameTitle = ref('')
const renamingConversationId = ref<number | null>(null)

type SidebarFocus =
  | { type: 'workbench'; path: string }
  | { type: 'agent'; id: number }
  | { type: 'conversation'; id: number }

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

function isWorkbenchItemActive(value: string) {
  return sidebarFocus.value.type === 'workbench' && sidebarFocus.value.path === value
}

function isAgentRowActive(agentId: number) {
  return sidebarFocus.value.type === 'agent' && sidebarFocus.value.id === agentId
}

function isConversationRowActive(conversationId: number) {
  return sidebarFocus.value.type === 'conversation' && sidebarFocus.value.id === conversationId
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
  return false
}

function onPinnedItemClick(item: PinnedSidebarItem) {
  if (item.key.startsWith('conversation-')) {
    const id = Number(item.key.slice('conversation-'.length))
    const conversation = conversations.value.find((row) => row.id === id)
    if (conversation) {
      openConversation(conversation)
    }
    return
  }
  if (item.key.startsWith('agent-')) {
    const id = Number(item.key.slice('agent-'.length))
    focusAgent(id)
    item.onNavigate?.()
  }
}

function toggleConversationsExpanded() {
  conversationsExpanded.value = !conversationsExpanded.value
}

function onAgentsSectionClick() {
  agentsExpanded.value = !agentsExpanded.value
}

function toggleAgentsExpanded() {
  agentsExpanded.value = !agentsExpanded.value
}

function matchWorkbenchPath(path: string): string | null {
  for (const item of workbenchItems) {
    if (path === item.value) {
      return item.value
    }
    if (item.value !== '/chat' && path.startsWith(`${item.value}/`)) {
      return item.value
    }
  }
  return null
}

function syncSidebarFocusFromRoute() {
  const path = route.path
  if (activeConversationId.value != null) {
    focusConversation(activeConversationId.value)
    return
  }
  const workbench = matchWorkbenchPath(path)
  if (workbench) {
    focusWorkbench(workbench)
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
    syncSidebarFocusFromRoute()
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
  avatarUrl?: string
  color: string
  meta: string
  to: string
  onNavigate?: () => void
  onUnpin?: () => void
}

const pinnedItems = computed<PinnedSidebarItem[]>(() => {
  const items: PinnedSidebarItem[] = []

  for (const id of pinnedAgentIds.value) {
    const agent = agents.value.find((item) => item.id === id)
    if (!agent) continue
    items.push({
      key: `agent-${id}`,
      label: agent.name,
      avatarText: agent.name.slice(0, 1),
      avatarUrl: agent.avatarUrl,
      color: getAvatarColor(agent.name),
      meta: formatRelativeTime(agent.updatedAt),
      to: '/chat',
      onNavigate: () => selectAgent(agent.id),
      onUnpin: () => handleToggleAgentPin(id),
    })
  }

  return items
})

const conversationFilterOptions: DropdownOption[] = [
  { content: '全部对话', value: 'all' },
  { content: '当前智能体', value: 'current-agent' },
]

const sortedConversations = computed(() =>
  [...conversations.value].sort((a, b) => {
    const ta = new Date(a.lastMessageAt || a.updatedAt).getTime()
    const tb = new Date(b.lastMessageAt || b.updatedAt).getTime()
    return tb - ta
  }),
)

const displayConversations = computed(() => {
  if (conversationFilter.value === 'current-agent' && selectedAgentId.value != null) {
    return sortedConversations.value.filter((item) => item.agentId === selectedAgentId.value)
  }
  return sortedConversations.value
})

function conversationLabel(conversation: ConversationVO) {
  return conversation.title || conversation.agentName || '新会话'
}

function onConversationFilter(data: { value?: string | number }) {
  const value = String(data?.value ?? '')
  if (value === 'all' || value === 'current-agent') {
    conversationFilter.value = value
  }
}

const unpinnedAgents = computed(() => {
  const pinnedSet = new Set(pinnedAgentIds.value)
  return agents.value.filter((agent) => !pinnedSet.has(agent.id))
})

function goCreateAgent() {
  openCreateAgentDialog()
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

function openRenameConversation(conversation: ConversationVO) {
  renamingConversationId.value = conversation.id
  renameTitle.value = conversationLabel(conversation)
  renameVisible.value = true
}

async function confirmRenameConversation() {
  const id = renamingConversationId.value
  const title = renameTitle.value.trim()
  if (!id) {
    renameVisible.value = false
    return
  }
  if (!title) {
    MessagePlugin.warning('请输入对话名称')
    return
  }
  renameSaving.value = true
  try {
    await renameConversation(id, title)
    await refreshConversations()
    renameVisible.value = false
    MessagePlugin.success('已改名')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '改名失败'))
  } finally {
    renameSaving.value = false
  }
}

async function handleDeleteConversation(conversation: ConversationVO) {
  const label = conversationLabel(conversation)
  await confirmResourceDelete({
    header: '删除对话',
    body: `确定删除对话「${label}」吗？消息将一并删除，此操作不可恢复。`,
    resourceLabel: '对话',
    onDelete: async () => {
      await deleteConversation(conversation.id)
      await unpinConversation(conversation.id)
    },
    onSuccess: async () => {
      await refreshConversations()
      if (activeConversationId.value === conversation.id) {
        await router.push('/chat')
      }
      MessagePlugin.success('已删除')
    },
  })
}

function openConversation(conversation: ConversationVO) {
  focusConversation(conversation.id)
  selectAgentByConversation(conversation.agentId)
  router.push(`/chat/${conversation.id}`)
}

function openAgent(agent: AgentVO) {
  focusAgent(agent.id)
  selectAgent(agent.id)
  router.push('/chat')
}

function agentMenuOptions(agent: AgentVO): DropdownOption[] {
  return [{ content: '编辑配置', value: `builder:${agent.id}` }]
}

function onAgentMenu(data: { value?: string | number }) {
  const value = String(data?.value ?? '')
  if (value.startsWith('builder:')) {
    const id = Number(value.split(':')[1])
    if (Number.isFinite(id)) {
      router.push(`/agents/${id}/builder`)
    }
  }
}

onMounted(async () => {
  await Promise.all([refreshAgents(), refreshConversations(), refreshPins()])
  syncSidebarFocusFromRoute()
})

watch(
  () => auth.currentWorkspaceId,
  async (workspaceId, previousId) => {
    if (!workspaceId || workspaceId === previousId) {
      return
    }
    await Promise.all([refreshAgents(), refreshConversations(), refreshPins()])
    syncSidebarFocusFromRoute()
  },
)
</script>

<style scoped>
.sidebar-workspace {
  --sidebar-row-pad-x: 8px;
  --sidebar-icon-size: 20px;
  --sidebar-col-gap: 6px;
  --sidebar-icon-col: var(--sidebar-icon-size);
  --sidebar-row-bg: var(--box-hover);
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

.sidebar-section-row__actions :deep(.t-popup__reference),
.sidebar-row__actions :deep(.t-popup__reference),
.sidebar-task-panel__tools :deep(.t-popup__reference),
.sidebar-task-row__actions :deep(.t-popup__reference) {
  display: inline-flex;
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
  background: var(--box-hover);
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
  color: var(--box-muted);
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
  background: var(--box-hover);
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

.sidebar-task-panel {
  margin-top: 10px;
  padding-top: 2px;
}

.sidebar-task-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 28px;
  padding: 0 var(--sidebar-row-pad-x) 6px;
}

.sidebar-task-panel__title {
  font-size: 12px;
  line-height: 20px;
  color: var(--box-muted);
}

.sidebar-task-panel__tools {
  display: inline-flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
}

.sidebar-task-panel__tool {
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

.sidebar-task-panel__tool :deep(.t-icon) {
  font-size: 14px;
}

.sidebar-task-panel__tool:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}

.sidebar-task-panel__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-task-panel__empty {
  margin: 0;
  padding: 8px var(--sidebar-row-pad-x) 4px;
  padding-left: calc(var(--sidebar-row-pad-x) + var(--sidebar-icon-col) + var(--sidebar-col-gap));
  font-size: 12px;
  line-height: 20px;
  color: var(--box-muted);
}

.sidebar-task-row-wrap {
  position: relative;
  padding: 0 var(--sidebar-row-pad-x);
  border-radius: 8px;
}

.sidebar-task-row-wrap:hover,
.sidebar-task-row-wrap--active {
  background: var(--sidebar-row-bg);
}

.sidebar-task-row {
  display: flex;
  align-items: center;
  gap: var(--sidebar-col-gap);
  width: 100%;
  min-height: 36px;
  padding: 0;
  padding-right: 84px;
  border: none;
  background: transparent;
  color: var(--sidebar-text);
  text-align: left;
  cursor: pointer;
}

.sidebar-task-row__icon {
  flex-shrink: 0;
  width: var(--sidebar-icon-size);
  font-size: 16px;
  color: var(--box-muted);
}

.sidebar-task-row__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  line-height: 20px;
}

.sidebar-task-row__actions {
  position: absolute;
  right: var(--sidebar-row-pad-x);
  top: 50%;
  display: inline-flex;
  align-items: center;
  opacity: 0;
  transform: translateY(-50%);
  transition: opacity 0.15s;
  pointer-events: none;
}

.sidebar-task-row-wrap:hover .sidebar-task-row__actions {
  opacity: 1;
  pointer-events: auto;
}

.sidebar-task-row__action {
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

.sidebar-task-row__action :deep(.t-icon) {
  font-size: 14px;
}

.sidebar-task-row__action:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}

.sidebar-task-panel :deep(.t-loading) {
  display: block;
  width: 100%;
}

.sidebar-task-panel :deep(.t-loading__parent) {
  display: flex;
  flex-direction: column;
  width: 100%;
}
</style>
