<template>
  <auth-layout
    slogan="把模型、知识、工具、工作流装进一个 Box"
  >
    <h2 class="form-title">欢迎回来</h2>

    <div class="portal-switch">
      <button
        v-for="item in PORTAL_OPTIONS"
        :key="item.value"
        type="button"
        class="portal-switch__item"
        :class="{ 'portal-switch__item--active': portal === item.value }"
        @click="switchPortal(item.value)"
      >
        {{ item.label }}
      </button>
    </div>
    <p class="form-desc">{{ currentPortalDesc }}</p>

    <div v-if="verifiedOrg && enterpriseView !== 'email'" class="org-banner">
      <t-icon name="check-circle" />
      <div>
        <strong>{{ verifiedOrg.name }}</strong>
        <span>企业标识 {{ verifiedOrg.orgId }}</span>
      </div>
      <t-button variant="text" size="small" @click="backToOrgStep">更换</t-button>
    </div>

    <t-form :data="formData" :rules="activeRules" label-width="0" @submit="onSubmit">
      <t-form-item v-if="enterpriseView === 'employee-org' || enterpriseView === 'join'" name="orgId">
        <t-input
          v-model="formData.orgId"
          placeholder="企业标识（组织 ID）"
          clearable
          size="large"
          :disabled="enterpriseView === 'join' && !!verifiedOrg"
        >
          <template #prefix-icon>
            <t-icon name="root-list" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item v-if="enterpriseView === 'join'" name="inviteCode">
        <t-input v-model="formData.inviteCode" placeholder="邀请码" clearable size="large">
          <template #prefix-icon>
            <t-icon name="secured" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item v-if="enterpriseView !== 'employee-org'" name="account">
        <t-input v-model="formData.account" :placeholder="accountPlaceholder" clearable size="large">
          <template #prefix-icon>
            <t-icon name="user" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item v-if="enterpriseView === 'join'" name="nickname">
        <t-input v-model="formData.nickname" placeholder="昵称（选填）" clearable size="large">
          <template #prefix-icon>
            <t-icon name="user" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item v-if="enterpriseView !== 'employee-org'" name="password">
        <t-input
          v-model="formData.password"
          type="password"
          :placeholder="enterpriseView === 'join' ? '设置密码（至少 8 位）' : '密码'"
          clearable
          size="large"
        >
          <template #prefix-icon>
            <t-icon name="lock-on" />
          </template>
        </t-input>
      </t-form-item>
      <div v-if="enterpriseView !== 'employee-org'" class="form-options">
        <t-checkbox v-if="enterpriseView !== 'join'" v-model="remember">记住账号</t-checkbox>
        <div v-else />
        <div class="form-links">
          <router-link v-if="enterpriseView !== 'join'" class="form-link" to="/forgot-password">忘记密码</router-link>
          <router-link
            v-if="portal === 'personal' || enterpriseView === 'email'"
            class="form-link"
            :to="registerLink"
          >
            {{ portal === 'enterprise' ? '使用企业邮箱注册' : '注册新账号' }}
          </router-link>
        </div>
      </div>
      <auth-agreement v-if="enterpriseView !== 'employee-org'" v-model:agreed="agreed" :action-label="submitLabel" />
      <t-form-item>
        <t-button theme="primary" type="submit" block size="large" shape="round" :loading="loading">
          {{ submitLabel }}
        </t-button>
      </t-form-item>
    </t-form>

    <button
      v-if="portal === 'enterprise' && enterpriseView !== 'email'"
      type="button"
      class="back-email"
      @click="enterEmailLogin"
    >
      返回企业邮箱登录
    </button>

    <button
      v-if="portal === 'enterprise' && enterpriseView === 'email'"
      type="button"
      class="employee-entry"
      @click="enterEmployeeOrg"
    >
      <t-icon name="usergroup" />
      企业员工登录
    </button>

    <p v-if="enterpriseView === 'employee-login'" class="join-hint">
      还没有账号？
      <button type="button" class="inline-link" @click="enterJoin">使用邀请码加入</button>
    </p>
    <p v-if="enterpriseView === 'employee-org'" class="join-hint">
      有邀请码？
      <button type="button" class="inline-link" @click="enterJoin">加入企业</button>
    </p>

    <div v-if="portal === 'personal'" class="oauth-section">
      <div class="oauth-divider">
        <span>或使用第三方登录</span>
      </div>
      <div class="oauth-actions oauth-actions--icons">
        <t-tooltip
          v-for="item in oauthProviders"
          :key="item.provider"
          :content="`使用 ${item.displayName} 登录`"
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
              @click="onOAuthLogin(item)"
            >
              <span class="oauth-logo" :data-provider="item.provider" />
            </t-button>
          </span>
        </t-tooltip>
      </div>
      <p v-if="oauthConfigHint" class="oauth-hint">{{ oauthConfigHint }}</p>
    </div>
  </auth-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormProps } from 'tdesign-vue-next'
import AuthLayout from '@box/ui/layouts/AuthLayout.vue'
import AuthAgreement from '@/components/AuthAgreement.vue'
import { PORTAL_OPTIONS, type PortalType } from '@/constants/portal'
import { notifyApiError, notifyError } from '@/api/apiError'
import {
  fetchOAuthProviders,
  lookupEnterpriseOrg,
  startOAuthLogin,
  type EnterpriseOrgLookupVO,
  type OAuthProviderVO,
} from '@/api/auth'
import { mergeOAuthProviders } from '@/constants/oauthProviders'
import { useAuthStore } from '@/stores/auth'

type EnterpriseView = 'email' | 'employee-org' | 'employee-login' | 'join'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const remember = ref(false)
const agreed = ref(false)
const portal = ref<PortalType>(route.query.portal === 'enterprise' ? 'enterprise' : 'personal')
const enterpriseView = ref<EnterpriseView>('email')
const verifiedOrg = ref<EnterpriseOrgLookupVO | null>(null)
const oauthProviders = ref<OAuthProviderVO[]>(mergeOAuthProviders([], portal.value))
const oauthConfigHint = computed(() => {
  if (oauthProviders.value.some((item) => item.enabled)) {
    return ''
  }
  return '第三方登录未启用：请在环境变量或管理端填写 GitHub / Google 的 Client ID 与 Secret，并重启后端。'
})

const formData = reactive({
  orgId: '',
  inviteCode: '',
  account: '',
  nickname: '',
  password: '',
})

const accountPlaceholder = computed(() => {
  if (portal.value !== 'enterprise') return '邮箱 / 用户名'
  if (enterpriseView.value === 'join') return '设置登录账号'
  if (enterpriseView.value === 'employee-login') return '账号 / 用户名'
  return '企业邮箱'
})

const submitLabel = computed(() => {
  if (enterpriseView.value === 'employee-org') return '下一步'
  if (enterpriseView.value === 'join') return '加入企业'
  return '登录'
})

const currentPortalDesc = computed(() => {
  if (portal.value !== 'enterprise') {
    return PORTAL_OPTIONS.find((item) => item.value === portal.value)?.desc || ''
  }
  if (enterpriseView.value === 'employee-org') {
    return '请先输入企业标识，校验通过后再登录。'
  }
  if (enterpriseView.value === 'employee-login') {
    return '使用管理员开通的账号或用户名登录。'
  }
  if (enterpriseView.value === 'join') {
    return '使用管理员提供的企业标识与邀请码创建员工账号。'
  }
  return '使用企业邮箱登录。员工请从下方入口进入。'
})

const registerLink = computed(() => ({
  path: '/register',
  query: { portal: portal.value },
}))

const activeRules = computed<FormProps['rules']>(() => {
  if (enterpriseView.value === 'employee-org') {
    return { orgId: [{ required: true, message: '请输入企业标识' }] }
  }
  const rules: FormProps['rules'] = {
    account: [
      {
        required: true,
        message:
          portal.value === 'enterprise' && enterpriseView.value === 'email' ? '请输入企业邮箱' : '请输入账号',
      },
    ],
    password: [
      { required: true, message: '请输入密码' },
      ...(enterpriseView.value === 'join' ? [{ min: 8, message: '密码至少 8 位' }] : []),
    ],
  }
  if (enterpriseView.value === 'join') {
    rules.orgId = [{ required: true, message: '请输入企业标识' }]
    rules.inviteCode = [{ required: true, message: '请输入邀请码' }]
  }
  return rules
})

function syncQuery() {
  const query: Record<string, string> = { portal: portal.value }
  if (portal.value === 'enterprise' && enterpriseView.value !== 'email') {
    query.mode = enterpriseView.value === 'join' ? 'join' : 'employee'
  }
  void router.replace({ query })
}

function enterEmailLogin() {
  enterpriseView.value = 'email'
  verifiedOrg.value = null
  syncQuery()
}

function enterEmployeeOrg() {
  enterpriseView.value = 'employee-org'
  if (!verifiedOrg.value) {
    formData.account = ''
    formData.password = ''
  }
  syncQuery()
}

function enterJoin() {
  enterpriseView.value = 'join'
  if (verifiedOrg.value) {
    formData.orgId = verifiedOrg.value.orgId
  }
  syncQuery()
}

function backToOrgStep() {
  verifiedOrg.value = null
  formData.password = ''
  enterEmployeeOrg()
}

function switchPortal(value: PortalType) {
  portal.value = value
  enterpriseView.value = 'email'
  verifiedOrg.value = null
  void router.replace({ query: { portal: value } })
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

function onOAuthLogin(item: OAuthProviderVO) {
  if (!item.enabled) {
    MessagePlugin.warning('该登录方式尚未配置，请检查环境变量或管理端 OAuth 配置后重启后端')
    return
  }
  if (!agreed.value) {
    MessagePlugin.warning('请先阅读并同意用户协议和隐私政策')
    return
  }
  startOAuthLogin(item.provider, 'PERSONAL')
}

onMounted(async () => {
  auth.logout()
  const queryPortal = route.query.portal
  if (queryPortal === 'enterprise' || queryPortal === 'personal') {
    portal.value = queryPortal
  }
  const mode = route.query.mode
  if (mode === 'employee') {
    enterpriseView.value = 'employee-org'
  } else if (mode === 'join') {
    enterpriseView.value = 'join'
  }
  const oauthError = route.query.oauth_error
  if (typeof oauthError === 'string' && oauthError.trim()) {
    MessagePlugin.error(oauthError.trim())
    void router.replace({ path: route.path, query: { portal: portal.value } })
  }
  await loadOAuthProviders()
})

watch(
  () => route.query.portal,
  (value) => {
    if (value === 'enterprise' || value === 'personal') {
      portal.value = value
    }
  },
)

watch(portal, () => {
  void loadOAuthProviders()
})

const onSubmit: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  if (enterpriseView.value === 'employee-org') {
    loading.value = true
    try {
      const { data } = await lookupEnterpriseOrg(formData.orgId.trim())
      const org = data.data
      if (!org?.orgId) {
        notifyError('企业标识不存在')
        return
      }
      verifiedOrg.value = org
      formData.orgId = org.orgId
      enterpriseView.value = 'employee-login'
      syncQuery()
    } catch (error) {
      verifiedOrg.value = null
      notifyApiError(error, '企业标识不存在')
    } finally {
      loading.value = false
    }
    return
  }
  if (!agreed.value) {
    MessagePlugin.warning('请先阅读并同意用户协议和隐私政策')
    return
  }
  loading.value = true
  try {
    if (portal.value === 'enterprise' && enterpriseView.value === 'join') {
      await auth.joinEnterprise({
        orgId: formData.orgId.trim(),
        inviteCode: formData.inviteCode.trim(),
        account: formData.account.trim(),
        password: formData.password,
        nickname: formData.nickname.trim() || undefined,
      })
      MessagePlugin.success('已加入企业')
    } else {
      await auth.login(
        formData.account,
        formData.password,
        portal.value === 'enterprise' ? 'ENTERPRISE' : 'PERSONAL',
        enterpriseView.value === 'employee-login' ? formData.orgId.trim() : undefined,
      )
      MessagePlugin.success('登录成功')
    }
    await router.push('/chat')
  } catch (error) {
    notifyApiError(error, enterpriseView.value === 'join' ? '加入失败' : '登录失败')
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
  margin: 0 0 16px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.org-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 10px 12px;
  border: 1px solid var(--box-border);
  border-radius: 12px;
  background: var(--box-surface);
  color: var(--box-ink);
}

.org-banner strong {
  display: block;
  font: var(--td-font-body-medium);
}

.org-banner span {
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.org-banner :deep(.t-icon) {
  color: var(--td-success-color);
  font-size: 20px;
}

.org-banner :deep(.t-button) {
  margin-left: auto;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.form-links {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-link {
  font: var(--td-font-body-medium);
  color: var(--td-brand-color);
}

.employee-entry,
.back-email {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 8px;
  padding: 8px 0;
  border: 0;
  background: transparent;
  color: var(--box-ink);
  font: var(--td-font-body-medium);
  cursor: pointer;
}

.employee-entry :deep(.t-icon) {
  font-size: 18px;
}

.employee-entry:hover,
.back-email:hover,
.inline-link:hover {
  color: var(--td-brand-color);
}

.join-hint {
  margin: 8px 0 0;
  text-align: center;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.inline-link {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--td-brand-color);
  cursor: pointer;
  font: inherit;
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
</style>
