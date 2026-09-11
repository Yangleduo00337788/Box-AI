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

    <t-form :data="formData" :rules="rules" label-width="0" @submit="onSubmit">
      <t-form-item name="account">
        <t-input v-model="formData.account" placeholder="邮箱 / 用户名" clearable size="large">
          <template #prefix-icon>
            <t-icon name="user" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item name="password">
        <t-input
          v-model="formData.password"
          type="password"
          placeholder="密码"
          clearable
          size="large"
        >
          <template #prefix-icon>
            <t-icon name="lock-on" />
          </template>
        </t-input>
      </t-form-item>
      <div class="form-options">
        <t-checkbox v-model="remember">记住账号</t-checkbox>
        <router-link class="form-link" :to="registerLink">注册新账号</router-link>
      </div>
      <t-form-item>
        <t-button theme="primary" type="submit" block size="large" shape="round" :loading="loading">
          登录
        </t-button>
      </t-form-item>
    </t-form>
  </auth-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormProps } from 'tdesign-vue-next'
import AuthLayout from '@box/ui/layouts/AuthLayout.vue'
import { PORTAL_OPTIONS, type PortalType } from '@/constants/portal'
import { extractApiError } from '@/api/apiError'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const remember = ref(false)
const portal = ref<PortalType>('personal')

const formData = reactive({
  account: '',
  password: '',
})

const rules: FormProps['rules'] = {
  account: [{ required: true, message: '请输入账号' }],
  password: [{ required: true, message: '请输入密码' }],
}

const currentPortalDesc = computed(
  () => PORTAL_OPTIONS.find((item) => item.value === portal.value)?.desc || '',
)

const registerLink = computed(() => ({
  path: '/register',
  query: { portal: portal.value },
}))

function switchPortal(value: PortalType) {
  portal.value = value
  router.replace({ query: { portal: value } })
}

onMounted(() => {
  auth.logout()
  const queryPortal = route.query.portal
  if (queryPortal === 'enterprise' || queryPortal === 'personal') {
    portal.value = queryPortal
  }
})

watch(
  () => route.query.portal,
  (value) => {
    if (value === 'enterprise' || value === 'personal') {
      portal.value = value
    }
  },
)

const onSubmit: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  loading.value = true
  try {
    await auth.login(
      formData.account,
      formData.password,
      portal.value === 'enterprise' ? 'ENTERPRISE' : 'PERSONAL',
    )
    MessagePlugin.success('登录成功')
    await router.push('/chat')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '登录失败'))
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
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.form-link {
  font: var(--td-font-body-medium);
  color: var(--td-brand-color);
}
</style>
