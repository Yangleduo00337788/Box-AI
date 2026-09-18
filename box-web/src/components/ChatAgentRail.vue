<template>
  <t-popup
    v-model:visible="panelOpen"
    trigger="click"
    placement="top-left"
    :show-arrow="false"
    overlay-class-name="chat-agent-switch-overlay"
    :overlay-inner-style="{ padding: 0, borderRadius: '12px' }"
    destroy-on-close
    @visible-change="onPanelVisible"
  >
    <button
      type="button"
      class="chat-agent-rail__pill"
      :class="{ 'chat-agent-rail__pill--open': panelOpen }"
      aria-haspopup="listbox"
      :aria-expanded="panelOpen"
      aria-label="切换智能体或项目"
    >
      <t-icon v-if="pillShowProjectIcon" name="layers" class="chat-agent-rail__pill-icon" />
      <t-avatar
        v-else-if="selectedAgent"
        size="16px"
        shape="circle"
        class="chat-agent-rail__pill-avatar"
        :image="selectedAgent.avatarUrl || undefined"
        :style="agentAvatarStyle(selectedAgent)"
      >
        {{ selectedAgent.name.slice(0, 1) }}
      </t-avatar>
      <t-icon v-else name="layers" class="chat-agent-rail__pill-icon chat-agent-rail__pill-icon--muted" />
      <span class="chat-agent-rail__pill-badge">{{ pillLabel }}</span>
      <t-icon name="chevron-down" class="chat-agent-rail__pill-arrow" />
    </button>

    <template #content>
      <div class="chat-switch-panel">
        <section class="chat-switch-panel__section">
          <p class="chat-switch-panel__title">Agent</p>
          <button type="button" class="chat-switch-panel__create" @click="onCreateAgent">
            <span class="chat-switch-panel__create-icon">+</span>
            <span>新建 Agent</span>
          </button>
          <button
            v-for="item in agents"
            :key="item.id"
            type="button"
            class="chat-switch-panel__item"
            @click="onPickAgent(item.id)"
          >
            <t-avatar size="24px" shape="circle" class="chat-switch-panel__avatar" :image="item.avatarUrl || undefined" :style="agentAvatarStyle(item)">
              {{ item.name.slice(0, 1) }}
            </t-avatar>
            <span class="chat-switch-panel__label">{{ item.name }}</span>
            <t-icon v-if="item.id === selectedAgentId" name="check" class="chat-switch-panel__check" />
          </button>
        </section>

        <div class="chat-switch-panel__divider" />

        <section class="chat-switch-panel__section">
          <p class="chat-switch-panel__title">项目</p>
          <button type="button" class="chat-switch-panel__create" @click="openProjectDialog">
            <span class="chat-switch-panel__create-icon">+</span>
            <span>新建项目</span>
          </button>
          <button
            v-for="project in projects"
            :key="project.id"
            type="button"
            class="chat-switch-panel__item"
            @click="onPickProject(project.id)"
          >
            <span class="chat-switch-panel__project-icon" aria-hidden="true">
              <t-icon name="layers" size="16px" />
            </span>
            <span class="chat-switch-panel__label">{{ project.name }}</span>
            <t-icon v-if="project.id === activeProjectId" name="check" class="chat-switch-panel__check" />
          </button>
        </section>
      </div>
    </template>
  </t-popup>

  <t-dialog
    v-model:visible="projectDialogVisible"
    header="新建项目"
    :confirm-btn="{ content: '创建', loading: projectSaving }"
    @confirm="confirmCreateProject"
  >
    <t-input v-model="projectName" placeholder="项目名称" maxlength="64" @enter="confirmCreateProject" />
  </t-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { AgentVO } from '@/api/agent'
import { createProject } from '@/api/project'
import { extractApiError } from '@/api/apiError'
import { useActiveProject } from '@/composables/useActiveProject'
import { useAgentSelection } from '@/composables/useAgentSelection'
import { useCreateAgentDialog } from '@/composables/useCreateAgentDialog'
import { useProjectNav } from '@/composables/useProjectNav'
import { getAvatarColor } from '@/utils/format'

const panelOpen = ref(false)
const projectDialogVisible = ref(false)
const projectName = ref('')
const projectSaving = ref(false)

const { agents, selectedAgent, selectedAgentId, selectAgent } = useAgentSelection()
const { projects, refresh: refreshProjects } = useProjectNav()
const { activeProjectId, selectProject } = useActiveProject()
const { openCreateAgentDialog } = useCreateAgentDialog()

const activeProject = computed(
  () => projects.value.find((item) => item.id === activeProjectId.value) ?? null,
)

const pillShowProjectIcon = computed(() => activeProject.value != null)

const pillLabel = computed(() => {
  if (activeProject.value) return activeProject.value.name
  if (selectedAgent.value) return String(selectedAgent.value.id)
  return '–'
})

function agentAvatarStyle(agent: AgentVO) {
  if (agent.avatarUrl) return undefined
  return { background: getAvatarColor(agent.name) }
}

function onPanelVisible(visible: boolean) {
  if (visible) {
    void refreshProjects()
  }
}

onMounted(() => {
  void refreshProjects()
})

function onPickAgent(id: number) {
  void selectAgent(id)
  panelOpen.value = false
}

function onPickProject(id: number) {
  selectProject(id)
  panelOpen.value = false
}

function onCreateAgent() {
  panelOpen.value = false
  openCreateAgentDialog()
}

function openProjectDialog() {
  panelOpen.value = false
  projectName.value = ''
  projectDialogVisible.value = true
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
    projectDialogVisible.value = false
    if (data.data?.id) {
      selectProject(data.data.id)
    }
    MessagePlugin.success('项目已创建')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '创建失败'))
  } finally {
    projectSaving.value = false
  }
}
</script>

<style scoped>
.chat-agent-rail__pill {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px 2px 4px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--box-ink);
  font-size: 13px;
  line-height: 20px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.chat-agent-rail__pill--open {
  background: rgba(0, 0, 0, 0.06);
}

.chat-agent-rail__pill:hover:not(.chat-agent-rail__pill--open) {
  background: rgba(0, 0, 0, 0.03);
}

.chat-agent-rail__pill-icon {
  flex-shrink: 0;
  font-size: 16px;
  color: var(--box-ink);
}

.chat-agent-rail__pill-icon--muted {
  color: var(--box-muted);
}

.chat-agent-rail__pill-avatar {
  flex-shrink: 0;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
}

.chat-agent-rail__pill-badge {
  min-width: 0;
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  line-height: 20px;
  font-variant-numeric: tabular-nums;
}

.chat-agent-rail__pill-arrow {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--box-muted);
}

.chat-switch-panel {
  width: 220px;
  max-height: min(360px, 56vh);
  overflow: auto;
  padding: 6px 0 8px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.chat-switch-panel::-webkit-scrollbar {
  display: none;
}

.chat-switch-panel__section {
  padding: 0 6px;
}

.chat-switch-panel__section + .chat-switch-panel__section {
  padding-top: 2px;
}

.chat-switch-panel__title {
  margin: 0;
  padding: 4px 10px 6px;
  font-size: 12px;
  line-height: 20px;
  font-weight: 400;
  color: var(--box-muted);
}

.chat-switch-panel__create {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  margin: 0;
  padding: 6px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--box-ink);
  font-size: 14px;
  line-height: 22px;
  text-align: left;
  cursor: pointer;
}

.chat-switch-panel__create-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  flex-shrink: 0;
  color: var(--box-muted);
  font-size: 16px;
  font-weight: 300;
  line-height: 1;
}

.chat-switch-panel__create:hover {
  background: rgba(0, 0, 0, 0.04);
}

.chat-switch-panel__item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 6px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--box-ink);
  font-size: 14px;
  line-height: 22px;
  text-align: left;
  cursor: pointer;
}

.chat-switch-panel__item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.chat-switch-panel__avatar {
  flex-shrink: 0;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.chat-switch-panel__project-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  color: var(--box-muted);
}

.chat-switch-panel__label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-switch-panel__check {
  flex-shrink: 0;
  font-size: 18px;
  color: var(--box-ink);
}

.chat-switch-panel__divider {
  height: 1px;
  margin: 6px 10px;
  background: var(--box-border);
}
</style>

<style>
.chat-agent-switch-overlay .t-popup__content {
  border: 1px solid var(--box-border);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}
</style>
