<template>
  <auth-layout
    slogan="把模型、知识、工具、工作流装进一个 Box"
  >
    <h2 class="form-title">{{ portal === 'enterprise' ? '使用企业邮箱注册' : '创建个人账号' }}</h2>
    <p class="form-desc">
      已有账号？
      <router-link :to="{ path: '/login', query: { portal } }">返回登录</router-link>
      <template v-if="portal === 'personal'">
        · 企业请
        <router-link :to="{ path: '/register', query: { portal: 'enterprise' } }">使用企业邮箱注册</router-link>
      </template>
      <template v-else>
        · 个人请
        <router-link :to="{ path: '/register', query: { portal: 'personal' } }">注册个人账号</router-link>
      </template>
    </p>
    <p v-if="portal === 'enterprise'" class="form-hint">
      员工不能自行注册。请使用企业邮箱完成注册，或从登录页进入员工入口。
    </p>

    <t-form :data="formData" :rules="rules" label-width="0" @submit="onSubmit">
      <t-form-item v-if="portal === 'enterprise'" name="companyName">
        <t-input v-model="formData.companyName" placeholder="企业名称" clearable size="large">
          <template #prefix-icon>
            <t-icon name="root-list" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item name="email">
        <t-input
          v-model="formData.email"
          :placeholder="portal === 'enterprise' ? '企业邮箱' : '登录邮箱'"
          clearable
          size="large"
        >
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

    <div v-if="portal === 'personal'" class="oauth-section">
      <div class="oauth-divider"><span>或使用第三方注册</span></div>
      <div class="oauth-actions oauth-actions--icons">
        <t-tooltip
          v-for="item in oauthProviders"
          :key="item.provider"
          :content="`使用 ${item.displayName} 注册`"
          placement="top"
          theme="light"
        >
          <span class="oauth-tip">
            <t-button
              class="oauth-btn"
              variant="outline"
              size="large"
              shape="circle"
              :disabled="loading || !item.enabled"
              @click="onOAuthRegister(item)"
            >
              <span class="oauth-logo" :data-provider="item.provider" />
            </t-button>
          </span>
        </t-tooltip>
      </div>
      <p v-if="oauthConfigHint" class="oauth-hint">{{ oauthConfigHint }}</p>
    </div>

    <router-link
      v-if="portal === 'enterprise'"
      class="employee-entry"
      :to="{ path: '/login', query: { portal: 'enterprise', mode: 'employee' } }"
    >
      <t-icon name="usergroup" />
      企业员工登录
    </router-link>
  </auth-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormProps, FormRule } from 'tdesign-vue-next'
import AuthLayout from '@box/ui/layouts/AuthLayout.vue'
import AuthAgreement from '@/components/AuthAgreement.vue'
import type { PortalType } from '@/constants/portal'
import { notifyApiError } from '@/api/apiError'
import { fetchOAuthProviders, sendVerificationCode, startOAuthLogin, type OAuthProviderVO } from '@/api/auth'
import { mergeOAuthProviders } from '@/constants/oauthProviders'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const sendingCode = ref(false)
const agreed = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
const portal = ref<PortalType>(route.query.portal === 'enterprise' ? 'enterprise' : 'personal')
const oauthProviders = ref<OAuthProviderVO[]>(mergeOAuthProviders([], portal.value))
const oauthConfigHint = computed(() => {
  if (oauthProviders.value.some((item) => item.enabled)) {
    return ''
  }
  return '第三方注册未启用：请在环境变量或管理端填写 GitHub / Google 的 Client ID 与 Secret，并重启后端。'
})

const formData = reactive({
  companyName: '',
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

const companyNameRule: FormRule = {
  validator: (val) => portal.value !== 'enterprise' || Boolean(String(val || '').trim()),
  message: '请填写企业名称',
}

const rules: FormProps['rules'] = {
  companyName: [companyNameRule],
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

async function loadOAuthProviders() {
  if (portal.value !== 'personal') {
    oauthProviders.value = []
    return
  }
  try {
    const { data } = await fetchOAuthProviders()
    oauthProviders.value = mergeOAuthProviders(data.data, 'personal')
  } catch {
    oauthProviders.value = mergeOAuthProviders([], 'personal')
  }
}

function onOAuthRegister(item: OAuthProviderVO) {
  if (!item.enabled) {
    MessagePlugin.warning('该方式尚未配置，请检查环境变量或管理端 OAuth 配置后重启后端')
    return
  }
  if (!agreed.value) {
    MessagePlugin.warning('请先阅读并同意用户协议和隐私政策')
    return
  }
  startOAuthLogin(item.provider, 'PERSONAL')
}

function applyPortalFromRoute() {
  const queryPortal = route.query.portal
  portal.value = queryPortal === 'enterprise' ? 'enterprise' : 'personal'
}

onMounted(async () => {
  auth.logout()
  applyPortalFromRoute()
  await loadOAuthProviders()
})

watch(
  () => route.query.portal,
  async () => {
    applyPortalFromRoute()
    await loadOAuthProviders()
  },
)

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
    notifyApiError(error, '发送验证码失败')
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
      accountType: portal.value === 'enterprise' ? 'ENTERPRISE' : 'PERSONAL',
      companyName: portal.value === 'enterprise' ? formData.companyName.trim() : undefined,
    })
    MessagePlugin.success('注册成功')
    await router.push('/chat')
  } catch (error) {
    notifyApiError(error, '注册失败')
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

.form-desc {
  margin: 0 0 12px;
  font: var(--td-font-body-medium);
  color: var(--box-muted);
}

.form-hint {
  margin: 0 0 24px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  line-height: 1.5;
}

.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  width: 100%;
}

.oauth-section {
  margin-top: 24px;
}

.oauth-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  color: var(--box-muted);
  font: var(--td-font-body-small);
}

.oauth-divider::before,
.oauth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--box-border);
}

.oauth-actions--icons {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.oauth-tip {
  display: inline-flex;
}

.oauth-actions :deep(.oauth-btn .t-button__text) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.oauth-logo {
  display: inline-block;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.oauth-logo[data-provider='github'] {
  background-image: url('../assets/oauth/github.svg');
}

.oauth-logo[data-provider='google'] {
  background-image: url('../assets/oauth/google.svg');
}

:global(html[data-box-theme='dark']) .oauth-logo[data-provider='github'] {
  filter: invert(1);
}

.oauth-hint {
  margin: 12px 0 0;
  text-align: center;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  line-height: 1.5;
}

.employee-entry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 16px;
  color: var(--box-ink);
  font: var(--td-font-body-medium);
  text-decoration: none;
}

.employee-entry :deep(.t-icon) {
  font-size: 18px;
}

.employee-entry:hover {
  color: var(--td-brand-color);
}
</style>
