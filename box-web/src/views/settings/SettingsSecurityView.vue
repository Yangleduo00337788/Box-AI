<template>
  <div class="settings-page">
    <h1 class="settings-page__title">账号与安全</h1>
    <div class="settings-card">
      <t-form label-align="top">
        <t-form-item label="登录邮箱">
          <t-input :value="auth.user?.email || '—'" disabled />
        </t-form-item>
        <t-form-item label="用户名">
          <t-input :value="auth.user?.username || '—'" disabled />
        </t-form-item>
        <t-form-item label="所属企业">
          <t-input :value="auth.tenant?.name || '—'" disabled />
        </t-form-item>
        <t-form-item label="当前工作空间">
          <t-input :value="auth.currentWorkspace?.name || '—'" disabled />
        </t-form-item>
      </t-form>

      <section class="password-section">
        <h2 class="password-section__title">修改密码</h2>
        <t-form label-align="top">
          <t-form-item label="当前密码">
            <t-input v-model="passwordForm.oldPassword" type="password" placeholder="输入当前密码" />
          </t-form-item>
          <t-form-item label="新密码">
            <t-input v-model="passwordForm.newPassword" type="password" placeholder="至少 6 位" />
          </t-form-item>
          <t-form-item label="确认新密码">
            <t-input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" />
          </t-form-item>
        </t-form>
      </section>

      <div class="security-actions">
        <t-button theme="primary" :loading="changingPassword" @click="submitPassword">更新密码</t-button>
        <t-button variant="outline" @click="router.push('/settings/profile')">编辑个人信息</t-button>
        <t-button theme="danger" variant="outline" @click="logout">退出登录</t-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { changePassword } from '@/api/auth'
import { extractApiError } from '@/api/apiError'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const changingPassword = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

async function submitPassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    MessagePlugin.warning('请填写完整密码信息')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    MessagePlugin.warning('新密码至少 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    MessagePlugin.warning('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    MessagePlugin.success('密码已更新')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '密码更新失败'))
  } finally {
    changingPassword.value = false
  }
}

function logout() {
  auth.logout()
  MessagePlugin.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.settings-page__title {
  margin: 0 0 24px;
  font: var(--td-font-title-large);
}

.settings-card {
  max-width: 720px;
  padding: 24px;
  border-radius: 16px;
  background: #f7f8fa;
}

.password-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--box-border);
}

.password-section__title {
  margin: 0 0 12px;
  font: var(--td-font-title-small);
}

.security-actions {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
}
</style>
