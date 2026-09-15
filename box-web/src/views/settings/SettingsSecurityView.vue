<template>
  <div class="settings-page">
    <h1 class="settings-page__title">账号与安全</h1>
    <p class="settings-page__desc">管理登录邮箱与密码。切换工作空间请使用左下角账号菜单。</p>

    <section class="settings-card">
      <h2 class="settings-card__heading">登录信息</h2>
      <t-descriptions :column="1">
        <t-descriptions-item label="登录邮箱">{{ auth.user?.email || '—' }}</t-descriptions-item>
        <t-descriptions-item label="用户名">{{ auth.user?.username || '—' }}</t-descriptions-item>
        <t-descriptions-item label="所属企业">{{ auth.tenant?.name || '—' }}</t-descriptions-item>
      </t-descriptions>
    </section>

    <section class="settings-card">
      <h2 class="settings-card__heading">修改密码</h2>
      <t-form label-align="top" class="password-form">
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
      <t-button theme="primary" :loading="changingPassword" @click="submitPassword">更新密码</t-button>
    </section>

    <section class="settings-card settings-card--danger">
      <h2 class="settings-card__heading">退出登录</h2>
      <p class="settings-card__hint">退出后需要重新登录才能访问此工作台。</p>
      <t-button theme="danger" variant="outline" @click="confirmLogout">退出登录</t-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'
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

function confirmLogout() {
  const dialog = DialogPlugin.confirm({
    header: '退出登录',
    body: '确定退出当前账号吗？',
    confirmBtn: '退出',
    theme: 'danger',
    onConfirm: () => {
      auth.logout()
      dialog.hide()
      MessagePlugin.success('已退出登录')
      void router.push('/login')
    },
  })
}
</script>

<style scoped>
.settings-card__heading {
  margin: 0 0 16px;
  font: var(--td-font-title-small);
}

.settings-card__hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--box-muted);
}

.password-form {
  max-width: 420px;
}

.settings-card--danger {
  border-color: var(--td-error-color-3);
}
</style>
