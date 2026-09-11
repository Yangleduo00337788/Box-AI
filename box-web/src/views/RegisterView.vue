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
      <t-form-item>
        <t-button theme="primary" type="submit" block size="large" shape="round" :loading="loading">
          注册
        </t-button>
      </t-form-item>
    </t-form>
  </auth-layout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormProps, FormRule } from 'tdesign-vue-next'
import AuthLayout from '@box/ui/layouts/AuthLayout.vue'
import { PORTAL_OPTIONS, type PortalType } from '@/constants/portal'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const accountType = ref<PortalType>('personal')

const formData = reactive({
  companyName: '',
  contactEmail: '',
  email: '',
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

const onSubmit: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  loading.value = true
  try {
    await auth.register({
      email: formData.email,
      password: formData.password,
      nickname: formData.nickname,
      accountType: accountType.value === 'enterprise' ? 'ENTERPRISE' : 'PERSONAL',
      companyName: accountType.value === 'enterprise' ? formData.companyName : undefined,
      contactEmail: accountType.value === 'enterprise' ? formData.contactEmail : undefined,
    })
    MessagePlugin.success('注册成功')
    await router.push('/chat')
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
</style>
