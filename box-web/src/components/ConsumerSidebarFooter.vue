<template>
  <div class="consumer-footer" :class="{ 'consumer-footer--collapsed': collapsed }">
    <div class="consumer-footer__user">
      <t-popup
        v-model:visible="menuVisible"
        placement="top-left"
        trigger="click"
        :overlay-inner-style="{ padding: 0 }"
        destroy-on-close
      >
        <button type="button" class="user-pill">
          <t-avatar size="28px" shape="circle" class="user-pill__avatar">{{ avatarText }}</t-avatar>
          <span v-if="!collapsed" class="user-pill__name">{{ userName }}</span>
        </button>

        <template #content>
          <div class="user-menu">
            <div class="user-menu__head">
              <t-avatar size="40px" shape="circle">{{ avatarText }}</t-avatar>
              <span class="user-menu__name">{{ userName }}</span>
            </div>

            <button type="button" class="user-menu__create" @click="onCreateWorkspace">
              <t-icon name="user-add" />
              创建工作空间
            </button>

            <div class="user-menu__section">
              <button type="button" class="user-menu__item" @click="go('/dashboard')">
                <t-icon name="dashboard" />
                <span>概览</span>
              </button>
              <button type="button" class="user-menu__item" @click="go('/analytics')">
                <t-icon name="chart" />
                <span>分析</span>
              </button>
              <button type="button" class="user-menu__item" @click="go('/settings/profile')">
                <t-icon name="setting" />
                <span>设置</span>
              </button>
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

      <div v-if="!collapsed" class="consumer-footer__actions">
        <t-tooltip content="设置" placement="top" theme="light" :show-arrow="false">
          <button type="button" class="footer-icon-btn" @click="go('/settings/profile')">
            <t-icon name="setting" />
          </button>
        </t-tooltip>
      </div>
    </div>

    <create-workspace-dialog v-model:visible="workspaceDialogVisible" />
    <help-feedback-dialog v-model:visible="helpDialogVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import CreateWorkspaceDialog from '@/components/CreateWorkspaceDialog.vue'
import HelpFeedbackDialog from '@/components/HelpFeedbackDialog.vue'

defineProps<{
  collapsed?: boolean
  userName: string
  userHint?: string
  avatarText: string
}>()

const emit = defineEmits<{
  logout: []
}>()

const router = useRouter()

const menuVisible = ref(false)
const workspaceDialogVisible = ref(false)
const helpDialogVisible = ref(false)

function go(path: string) {
  menuVisible.value = false
  router.push(path)
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

.consumer-footer__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.footer-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #646a73;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.footer-icon-btn:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #1f2329;
}

.user-menu {
  width: 260px;
  padding: 12px 0 8px;
  border-radius: 12px;
  background: #fff;
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
  margin-left: auto;
  font-size: 14px;
  color: #b0b4bc;
}
</style>
