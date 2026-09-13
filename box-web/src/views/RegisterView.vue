<template>
  <auth-layout
    slogan="把模型、知识、工具、工作流装进一个 Box"
  >
    <h2 class="form-title">创建账号</h2>

    <div class="portal-switch">
      <button
        v-for="item in PORTAL_OPTIONS"
        :key="item.value"
        type="button"
        class="portal-switch__item"
        :class="{ 'portal-switch__item--active': accountType === item.value }"
        @click="accountType = item.value"
      >
        {{ item.label }}
      </button>
    </div>
    <p class="form-desc">
      已有账号？
      <router-link :to="{ path: '/login', query: { portal: accountType } }">返回登录</router-link>
    </p>

    <t-form :data="formData" :rules="rules" label-width="0" @submit="onSubmit">
      <template v-if="accountType === 'enterprise'">
        <t-form-item name="companyName" :rules="companyNameRules">
          <t-input v-model="formData.companyName" placeholder="企业名称" clearable size="large">
            <template #prefix-icon>
              <t-icon name="city" />
            </template>
          </t-input>
        </t-form-item>
        <t-form-item name="contactEmail">
          <t-input v-model="formData.contactEmail" placeholder="企业联系邮箱（选填）" clearable size="large">
            <template #prefix-icon>
              <t-icon name="mail" />
            </template>
          </t-input>
        </t-form-item>
      </template>

      <t-form-item name="email">
        <t-input v-model="formData.email" placeholder="登录邮箱" clearable size="large">
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
      <t-form-item name="nickname">
        <t-input v-model="formData.nickname" placeholder="昵称（选填）" clearable size="large">
          <template #prefix-icon>
            <t-icon name="user" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item name="password">
        <t-input
          v-model="formData.password"
          type="password"
          placeholder="至少 8 位密码"
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
          placeholder="再次输入密码"
          clearable
          size="large"
        >
          <template #prefix-icon>
            <t-icon name="lock-on" />
          </template>
        </t-input>
      </t-form-item>
      <auth-agreement v-model:agreed="agreed" action-label="注册" />
      <t-form-item>
        <t-button theme="primary" type="submit" block size="large" shape="round" :loading="loading">
          注册
        </t-button>
      </t-form-item>
    </t-form>
  </auth-layout>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormProps, FormRule } from 'tdesign-vue-next'
import AuthLayout from '@box/ui/layouts/AuthLayout.vue'
import AuthAgreement from '@/components/AuthAgreement.vue'
import { PORTAL_OPTIONS, type PortalType } from '@/constants/portal'
import { extractApiError } from '@/api/apiError'
import { sendVerificationCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const sendingCode = ref(false)
const agreed = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
const accountType = ref<PortalType>('personal')

const formData = reactive({
  companyName: '',
  contactEmail: '',
  email: '',
  verificationCode: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const confirmRule: FormRule = {
  validator: (val) => val === formData.password,
  message: '两次密码不一致',
}

const rules: FormProps['rules'] = {
  email: [
    { required: true, message: '请输入邮箱' },
    { email: true, message: '邮箱格式不正确' },
  ],
  verificationCode: [{ required: true, message: '请输入验证码' }],
  password: [
    { required: true, message: '请输入密码' },
    { min: 8, message: '密码至少 8 位' },
  ],
  confirmPassword: [{ required: true, message: '请确认密码' }, confirmRule],
}

const companyNameRules: FormRule[] = [{ required: true, message: '请输入企业名称' }]

onMounted(() => {
  auth.logout()
  const queryPortal = route.query.portal
  if (queryPortal === 'enterprise' || queryPortal === 'personal') {
    accountType.value = queryPortal
  }
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})

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
    const { data } = await sendVerificationCode(formData.email, 'REGISTER')
    if (data.data.devCode) {
      MessagePlugin.info(`开发模式验证码：${data.data.devCode}`)
    } else {
      MessagePlugin.success('验证码已发送')
    }
    startCountdown()
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '发送验证码失败'))
  } finally {
    sendingCode.value = false
  }
}

const onSubmit: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  if (!agreed.value) {
    MessagePlugin.warning('请先阅读并同意用户协议和隐私政策')
    return
  }
  loading.value = true
  try {
    await auth.register({
      email: formData.email,
      password: formData.password,
      verificationCode: formData.verificationCode,
      nickname: formData.nickname,
      accountType: accountType.value === 'enterprise' ? 'ENTERPRISE' : 'PERSONAL',
      companyName: accountType.value === 'enterprise' ? formData.companyName : undefined,
      contactEmail: accountType.value === 'enterprise' ? formData.contactEmail : undefined,
    })
    MessagePlugin.success('注册成功')
    await router.push('/chat')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '注册失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.form-title {
  margin: 0 0 16px;
  font: var(--td-font-title-large);
  color: var(--box-ink);
}

.portal-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.portal-switch__item {
  padding: 10px 12px;
  border: 1px solid var(--box-border);
  border-radius: 12px;
  background: var(--box-surface);
  color: var(--box-muted);
  font: var(--td-font-body-medium);
  cursor: pointer;
  transition: all 0.2s;
}

.portal-switch__item--active {
  border-color: var(--box-ink);
  background: var(--box-ink);
  color: #fff;
  font-weight: 600;
}

.form-desc {
  margin: 0 0 24px;
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
