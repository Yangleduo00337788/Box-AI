<template>
  <div class="sidebar-workspace">
    <section class="sidebar-section sidebar-section--compact">
      <router-link
        v-for="item in workbenchItems"
        :key="item.value"
        :to="item.value"
        class="sidebar-line sidebar-line--nav"
        :class="{ 'sidebar-line--active': isWorkbenchItemActive(item.value) }"
        @click="onWorkbenchClick(item.value)"
      >
        <span class="sidebar-line__icon">
          <t-icon :name="resolveTIconName(item.icon)" />
        </span>
        <span class="sidebar-line__label">{{ item.label }}</span>
      </router-link>

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

      <div class="sidebar-task-panels">
      <section class="sidebar-task-panel">
        <button
          type="button"
          class="sidebar-task-panel__head sidebar-task-panel__head--toggle"
          :aria-expanded="projectsExpanded"
          @click="toggleProjectsExpanded"
        >
          <span class="sidebar-task-panel__title">项目</span>
          <div class="sidebar-task-panel__tools">
            <span class="sidebar-task-panel__tool sidebar-task-panel__tool--static" aria-hidden="true">
              <t-icon :name="projectsExpanded ? 'chevron-down' : 'chevron-right'" />
            </span>
            <t-tooltip content="创建项目" placement="top" theme="light" :show-arrow="false" attach="body">
              <button type="button" class="sidebar-task-panel__tool" aria-label="创建项目" @click.stop="openCreateProject">
                <t-icon name="add" />
              </button>
            </t-tooltip>
          </div>
        </button>

        <t-loading v-if="projectsExpanded" :loading="projectsLoading" size="small" class="sidebar-task-panel__body">
          <div v-if="!projects.length" class="sidebar-project-empty">
            <p class="sidebar-project-empty__desc">
              创建工作目录保存文件与对话<br />
              适合需要持续推进的复杂任务
            </p>
            <button type="button" class="sidebar-project-empty__btn" @click="openCreateProject">创建</button>
          </div>
          <nav v-else class="sidebar-task-panel__list">
            <t-popup
              v-for="project in projects"
              :key="project.id"
              trigger="hover"
              placement="right-top"
              :delay="[120, 180]"
              :z-index="5600"
              attach="body"
              :overlay-inner-style="{
                padding: 0,
                maxHeight: 'none',
                overflow: 'visible',
                boxShadow: '0 8px 24px rgba(0,0,0,.12)',
                borderRadius: '12px',
              }"
              @visible-change="(visible: boolean) => onProjectHover(project, visible)"
            >
              <div
                class="sidebar-task-row-wrap"
                :class="{ 'sidebar-task-row-wrap--active': activeProjectId === project.id }"
              >
                <button type="button" class="sidebar-task-row sidebar-task-row--project" @click="enterProject(project)">
                  <t-icon name="folder" class="sidebar-task-row__icon" />
                  <span class="sidebar-task-row__label">{{ project.name }}</span>
                </button>
              </div>
              <template #content>
                <project-conversation-panel
                  :title="project.name"
                  :loading="hoveredProjectId === project.id && projectConversationsLoading"
                  :conversations="hoveredProjectId === project.id ? displayProjectConversations : []"
                  :active-id="activeConversationId"
                  :pinned-ids="pinnedConversationIds"
                  @open="openConversation"
                  @pin="handleToggleConversationPin"
                  @rename="openRenameConversation"
                  @delete="handleDeleteConversation"
                />
              </template>
            </t-popup>
          </nav>
        </t-loading>
      </section>

      <section class="sidebar-task-panel">
        <button
          type="button"
          class="sidebar-task-panel__head sidebar-task-panel__head--toggle"
          :aria-expanded="conversationsExpanded"
          @click="toggleConversationsExpanded"
        >
          <span class="sidebar-task-panel__title">任务列表</span>
          <div class="sidebar-task-panel__tools">
            <span class="sidebar-task-panel__tool sidebar-task-panel__tool--static" aria-hidden="true">
              <t-icon :name="conversationsExpanded ? 'chevron-down' : 'chevron-right'" />
            </span>
            <t-dropdown :options="conversationFilterOptions" trigger="click" @click="onConversationFilter">
              <t-tooltip content="筛选任务" placement="top" theme="light" :show-arrow="false" attach="body">
                <button type="button" class="sidebar-task-panel__tool" aria-label="筛选任务" @click.stop>
                  <t-icon name="filter" />
                </button>
              </t-tooltip>
            </t-dropdown>
          </div>
        </button>

        <t-loading v-if="conversationsExpanded" :loading="conversationsLoading" size="small" class="sidebar-task-panel__body">
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
                <t-icon name="chat-bubble-history" class="sidebar-task-row__icon" />
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
          <p v-else class="sidebar-task-panel__empty">暂无任务</p>
        </t-loading>
      </section>

      <section v-if="pinnedConversationIds.length" class="sidebar-task-panel">
        <button
          type="button"
          class="sidebar-task-panel__head sidebar-task-panel__head--toggle"
          :aria-expanded="pinnedExpanded"
          @click="pinnedExpanded = !pinnedExpanded"
        >
          <span class="sidebar-task-panel__title">已置顶</span>
          <div class="sidebar-task-panel__tools">
            <span class="sidebar-task-panel__tool sidebar-task-panel__tool--static" aria-hidden="true">
              <t-icon :name="pinnedExpanded ? 'chevron-down' : 'chevron-right'" />
            </span>
          </div>
        </button>
        <div v-if="pinnedExpanded" class="sidebar-task-panel__body">
          <nav class="sidebar-task-panel__list">
            <div
              v-for="conversation in pinnedConversations"
              :key="conversation.id"
              class="sidebar-task-row-wrap"
              :class="{ 'sidebar-task-row-wrap--active': isConversationRowActive(conversation.id) }"
            >
              <button type="button" class="sidebar-task-row" @click="openConversation(conversation)">
                <t-icon name="chat-bubble-history" class="sidebar-task-row__icon" />
                <span class="sidebar-task-row__label">{{ conversationLabel(conversation) }}</span>
              </button>
              <div class="sidebar-task-row__actions">
                <t-tooltip content="取消置顶" placement="top" theme="light" :show-arrow="false" attach="body">
                  <button
                    type="button"
                    class="sidebar-task-row__action"
                    aria-label="取消置顶"
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
        </div>
      </section>
      </div>

    </section>

    <t-dialog
      v-if="renameVisible"
      v-model:visible="renameVisible"
      header="任务改名"
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
        placeholder="请输入任务名称"
        :autofocus="true"
      />
    </t-dialog>

    <t-dialog
      v-if="projectCreateVisible"
      v-model:visible="projectCreateVisible"
      header="创建项目"
      width="400px"
      attach="body"
      :destroy-on-close="true"
      confirm-btn="创建"
      cancel-btn="取消"
      :confirm-loading="projectSaving"
      :confirm-on-enter="true"
      @confirm="confirmCreateProject"
    >
      <t-input
        v-model="projectName"
        :maxlength="128"
        show-limit-number
        placeholder="请输入项目名称"
        :autofocus="true"
      />
    </t-dialog>

    <t-dialog
      v-if="projectRenameVisible"
      v-model:visible="projectRenameVisible"
      header="项目改名"
      width="400px"
      attach="body"
      :destroy-on-close="true"
      confirm-btn="保存"
      cancel-btn="取消"
      :confirm-loading="projectSaving"
      :confirm-on-enter="true"
      @confirm="confirmRenameProject"
    >
      <t-input
        v-model="projectName"
        :maxlength="128"
        show-limit-number
        placeholder="请输入项目名称"
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
import { createProject, deleteProject, renameProject, type ChatProjectVO } from '@/api/project'
import { confirmResourceDelete } from '@/composables/useResourceDelete'
import { useActiveProject } from '@/composables/useActiveProject'
import { useAgentSelection } from '@/composables/useAgentSelection'
import { useCreateAgentDialog } from '@/composables/useCreateAgentDialog'
import { useConversationNav } from '@/composables/useConversationNav'
import { useProjectNav } from '@/composables/useProjectNav'
import ProjectConversationPanel from '@/components/ProjectConversationPanel.vue'
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
const { conversations, projectConversations, catalog, loading: conversationsLoading, projectLoading: projectConversationsLoading, refresh: refreshConversations, refreshProjectConversations, refreshCatalog } = useConversationNav()
const { projects, loading: projectsLoading, refresh: refreshProjects } = useProjectNav()
const { activeProjectId, selectProject, clearProject, syncActiveProject } = useActiveProject()
const { openCreateAgentDialog } = useCreateAgentDialog()
const {
  pinnedAgentIds,
  pinnedConversationIds,
  isAgentPinned,
  isConversationPinned,
  toggleAgentPin,
  toggleConversationPin,
  unpinConversation,
  refresh: refreshPins,
} = useSidebarPins()

const workbenchItems = CONSUMER_MENU_GROUPS[0]?.items ?? []
const pinnedExpanded = ref(true)
const conversationsExpanded = ref(true)
const projectsExpanded = ref(true)
const hoveredProjectId = ref<number | null>(null)
const conversationFilter = ref<'all' | 'current-agent'>('all')
const agentsExpanded = ref(false)
const renameVisible = ref(false)
const renameSaving = ref(false)
const renameTitle = ref('')
const renamingConversationId = ref<number | null>(null)
const projectCreateVisible = ref(false)
const projectRenameVisible = ref(false)
const projectSaving = ref(false)
const projectName = ref('')
const renamingProjectId = ref<number | null>(null)

type SidebarFocus =
  | { type: 'workbench'; path: string }
  | { type: 'agent'; id: number }
  | { type: 'conversation'; id: number }

const sidebarFocus = ref<SidebarFocus>({ type: 'workbench', path: '/chat' })

function focusWorkbench(path: string) {
  sidebarFocus.value = { type: 'workbench', path }
}

function onWorkbenchClick(path: string) {
  focusWorkbench(path)
  if (path === '/chat') {
    clearProject()
  }
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

function toggleConversationsExpanded() {
  conversationsExpanded.value = !conversationsExpanded.value
}

function toggleProjectsExpanded() {
  projectsExpanded.value = !projectsExpanded.value
}

async function refreshTaskList() {
  await refreshConversations({ unassigned: true })
}

async function refreshProjectChats() {
  await refreshProjectConversations(activeProjectId.value)
}

async function refreshAllSidebarConversations() {
  const tasks = [refreshTaskList(), refreshCatalog(), refreshProjectChats()]
  const hoveredId = hoveredProjectId.value
  if (hoveredId != null && hoveredId !== activeProjectId.value) {
    tasks.push(refreshProjectConversations(hoveredId))
  }
  await Promise.all(tasks)
}

async function refreshProjectsAndSync() {
  try {
    await refreshProjects()
    syncActiveProject(projects.value.map((item) => item.id))
  } catch {
    clearProject()
  }
}

async function enterProject(project: ChatProjectVO) {
  selectProject(project.id)
  await refreshProjectChats()
  await router.push('/chat')
}

function onProjectHover(project: ChatProjectVO, visible: boolean) {
  if (visible) {
    hoveredProjectId.value = project.id
    void refreshProjectConversations(project.id)
    return
  }
  if (hoveredProjectId.value === project.id) {
    hoveredProjectId.value = null
  }
}

function openCreateProject() {
  projectName.value = ''
  projectCreateVisible.value = true
}

function openRenameProject(project: ChatProjectVO) {
  renamingProjectId.value = project.id
  projectName.value = project.name
  projectRenameVisible.value = true
}

async function confirmCreateProject() {
  const name = projectName.value.trim()
  if (!name) {
    MessagePlugin.warning('请输入项目名称')
    return
  }
  projectSaving.value = true
  try {
    const { data } = await createProject(name)
    await refreshProjects()
    projectCreateVisible.value = false
    if (data.data?.id) {
      selectProject(data.data.id)
      await refreshProjectChats()
      await router.push('/chat')
    }
    MessagePlugin.success('项目已创建')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '创建失败'))
  } finally {
    projectSaving.value = false
  }
}

async function confirmRenameProject() {
  const id = renamingProjectId.value
  const name = projectName.value.trim()
  if (!id) {
    projectRenameVisible.value = false
    return
  }
  if (!name) {
    MessagePlugin.warning('请输入项目名称')
    return
  }
  projectSaving.value = true
  try {
    await renameProject(id, name)
    await refreshProjects()
    projectRenameVisible.value = false
    MessagePlugin.success('已改名')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '改名失败'))
  } finally {
    projectSaving.value = false
  }
}

async function handleDeleteProject(project: ChatProjectVO) {
  await confirmResourceDelete({
    header: '删除项目',
    body: `确定删除项目「${project.name}」吗？项目内对话将回到任务列表，此操作不可恢复。`,
    resourceLabel: '项目',
    onDelete: async () => {
      await deleteProject(project.id)
    },
    onSuccess: async () => {
      if (activeProjectId.value === project.id) {
        clearProject()
      }
      await Promise.all([refreshProjects(), refreshTaskList(), refreshProjectChats()])
      MessagePlugin.success('已删除')
    },
  })
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

const pinnedConversations = computed(() => {
  const lookup = new Map<number, ConversationVO>()
  for (const item of [...catalog.value, ...conversations.value, ...projectConversations.value]) {
    lookup.set(item.id, item)
  }
  return pinnedConversationIds.value
    .map((id) => lookup.get(id))
    .filter((item): item is ConversationVO => item != null)
})

const displayProjectConversations = computed(() => {
  const pinnedSet = new Set(pinnedConversationIds.value)
  return projectConversations.value.filter((item) => !pinnedSet.has(item.id))
})

const conversationFilterOptions: DropdownOption[] = [
  { content: '全部任务', value: 'all' },
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
  const pinnedSet = new Set(pinnedConversationIds.value)
  let list = sortedConversations.value.filter((item) => !pinnedSet.has(item.id))
  if (conversationFilter.value === 'current-agent' && selectedAgentId.value != null) {
    list = list.filter((item) => item.agentId === selectedAgentId.value)
  }
  return list
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
  const wasPinned = isConversationPinned(conversationId)
  try {
    await toggleConversationPin(conversationId)
    if (!wasPinned) {
      pinnedExpanded.value = true
    }
    await refreshAllSidebarConversations()
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
    MessagePlugin.warning('请输入任务名称')
    return
  }
  renameSaving.value = true
  try {
    await renameConversation(id, title)
    await refreshAllSidebarConversations()
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
    header: '删除任务',
    body: `确定删除任务「${label}」吗？消息将一并删除，此操作不可恢复。`,
    resourceLabel: '任务',
    onDelete: async () => {
      await deleteConversation(conversation.id)
      await unpinConversation(conversation.id)
    },
    onSuccess: async () => {
      await Promise.all([refreshAllSidebarConversations(), refreshProjects()])
      if (activeConversationId.value === conversation.id) {
        await router.push('/chat')
      }
      MessagePlugin.success('已删除')
    },
  })
}

function openConversation(conversation: ConversationVO) {
  focusConversation(conversation.id)
  if (conversation.projectId != null) {
    selectProject(conversation.projectId)
  } else {
    clearProject()
  }
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
  await Promise.all([
    refreshAgents(),
    refreshProjectsAndSync(),
    refreshTaskList(),
    refreshProjectChats(),
    refreshCatalog(),
    refreshPins(),
  ])
  syncSidebarFocusFromRoute()
})

watch(
  () => auth.currentWorkspaceId,
  async (workspaceId, previousId) => {
    if (!workspaceId || workspaceId === previousId) {
      return
    }
    await Promise.all([
      refreshAgents(),
      refreshProjectsAndSync(),
      refreshTaskList(),
      refreshProjectChats(),
      refreshCatalog(),
      refreshPins(),
    ])
    syncSidebarFocusFromRoute()
  },
)

watch(activeProjectId, () => {
  void refreshProjectChats()
})
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
.sidebar-task-panel__tools :deep(.t-dropdown),
.sidebar-task-row__actions :deep(.t-popup__reference) {
  display: inline-flex;
  align-items: center;
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

.sidebar-task-panels {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 2px;
}

.sidebar-task-panel {
  margin-top: 0;
  padding-top: 0;
}

.sidebar-task-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 36px;
  padding: 0 var(--sidebar-row-pad-x);
  box-sizing: border-box;
}

.sidebar-task-panel__head--toggle {
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--sidebar-text);
  text-align: left;
  cursor: pointer;
  transition: background 0.2s;
}

.sidebar-task-panel__head--toggle:hover {
  background: var(--sidebar-row-bg);
  color: var(--box-ink);
}

.sidebar-task-panel__title {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  overflow: hidden;
  padding-left: 0;
  font-size: 13px;
  line-height: 20px;
  color: var(--sidebar-text);
  font-weight: inherit;
}

.sidebar-task-panel__tools {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex-shrink: 0;
  height: 24px;
}

.sidebar-task-panel__tool {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  margin: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--box-muted);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  vertical-align: middle;
}

.sidebar-task-panel__tool :deep(.t-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  line-height: 1;
}

.sidebar-task-panel__tool:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}

.sidebar-task-panel__tool--static {
  cursor: default;
  pointer-events: none;
}

.sidebar-task-panel__tool--static:hover {
  background: transparent;
  color: var(--box-muted);
}

.sidebar-task-panel__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-task-panel__list :deep(.t-popup__reference) {
  display: block;
  width: 100%;
}

.sidebar-task-row--project {
  padding-right: 0;
}

.sidebar-project-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin: 4px var(--sidebar-row-pad-x) 8px;
  padding: 20px 16px 16px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.03);
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.04);
  text-align: center;
}

.sidebar-project-empty__desc {
  margin: 0;
  font-size: 12px;
  line-height: 20px;
  color: var(--box-muted);
}

.sidebar-project-empty__btn {
  min-width: 72px;
  height: 32px;
  padding: 0 20px;
  border: none;
  border-radius: 999px;
  background: var(--box-surface);
  color: var(--box-ink);
  font-size: 13px;
  line-height: 32px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: background 0.2s, box-shadow 0.2s;
}

.sidebar-project-empty__btn:hover {
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.sidebar-task-panel__empty {
  margin: 0;
  padding: 8px var(--sidebar-row-pad-x) 4px;
  padding-left: calc(var(--sidebar-row-pad-x) + var(--sidebar-icon-col) + var(--sidebar-col-gap));
  font-size: 12px;
  line-height: 20px;
  color: var(--box-muted);
}

.sidebar-task-panel__empty--nested {
  padding-left: calc(var(--sidebar-row-pad-x) + var(--sidebar-icon-col) + var(--sidebar-col-gap));
}

.sidebar-task-row__meta {
  margin-left: auto;
  padding-right: 4px;
  font-size: 11px;
  line-height: 20px;
  color: var(--box-muted);
  flex-shrink: 0;
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

.sidebar-task-panel__body :deep(.t-loading),
.sidebar-task-panel__body :deep(.t-loading__parent) {
  display: flex;
  flex-direction: column;
  width: 100%;
}
</style>
