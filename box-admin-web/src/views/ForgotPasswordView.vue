<template>
  <auth-layout slogan="平台运营与租户治理" :show-mascot="false">
    <span class="form-badge">管理端</span>
    <h2 class="form-title">重置密码</h2>
    <p class="form-desc">
      已有账号？
      <router-link to="/login">返回登录</router-link>
    </p>

    <t-form :data="formData" :rules="rules" label-width="0" @submit="onSubmit">
      <t-form-item name="email">
        <t-input v-model="formData.email" placeholder="管理员邮箱" clearable size="large">
          <template #prefix-icon>
            <t-icon name="mail" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item name="verificationCode">
        <div class="code-row">
          <t-input v-model="formData.verificationCode" placeholder="邮箱验证码" clearable size="large">
            <template #prefix-icon>
              <t-icon name="secured" />
            </template>
          </t-input>
          <t-button
            variant="outline"
            size="large"
            :disabled="sendingCode || countdown > 0"
            :loading="sendingCode"
            @click="sendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </t-button>
        </div>
      </t-form-item>
      <t-form-item name="newPassword">
        <t-input
          v-model="formData.newPassword"
          type="password"
          placeholder="新密码（至少 8 位）"
          clearable
          size="large"
        >
          <template #prefix-icon>
            <t-icon name="lock-on" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item name="confirmPassword">
        <t-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="再次输入新密码"
          clearable
          size="large"
        >
          <template #prefix-icon>
            <t-icon name="lock-on" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item>
        <t-button theme="primary" type="submit" block size="large" shape="round" :loading="loading">
          重置密码
        </t-button>
      </t-form-item>
    </t-form>
  </auth-layout>
</template>

<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormProps, FormRule } from 'tdesign-vue-next'
import AuthLayout from '@box/ui/layouts/AuthLayout.vue'
import { resetAdminPassword, sendAdminVerificationCode } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const formData = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: '',
})

const confirmRule: FormRule = {
  validator: (val) => val === formData.newPassword,
  message: '两次密码不一致',
}

const rules: FormProps['rules'] = {
  email: [
    { required: true, message: '请输入邮箱' },
    { email: true, message: '邮箱格式不正确' },
  ],
  verificationCode: [{ required: true, message: '请输入验证码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 8, message: '密码至少 8 位' },
  ],
  confirmPassword: [{ required: true, message: '请确认密码' }, confirmRule],
}

function startCountdown() {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function sendCode() {
  if (!formData.email) {
    MessagePlugin.warning('请先填写邮箱')
    return
  }
  sendingCode.value = true
  try {
    const { data } = await sendAdminVerificationCode(formData.email.trim())
    if (data.data?.devCode) {
      MessagePlugin.info(`开发模式验证码：${data.data.devCode}`)
    } else {
      MessagePlugin.success('验证码已发送')
    }
    startCountdown()
  } finally {
    sendingCode.value = false
  }
}

const onSubmit: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  loading.value = true
  try {
    await resetAdminPassword({
      email: formData.email.trim(),
      verificationCode: formData.verificationCode.trim(),
      newPassword: formData.newPassword,
    })
    MessagePlugin.success('密码已重置，请登录')
    await router.push('/login')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.form-badge {
  display: inline-flex;
  margin-bottom: 12px;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--box-ink);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.form-title {
  margin: 0 0 8px;
  font: var(--td-font-title-large);
  color: var(--box-ink);
}

.form-desc {
  margin: 0 0 28px;
  font: var(--td-font-body-medium);
  color: var(--box-muted);
}

.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  width: 100%;
}
</style>
