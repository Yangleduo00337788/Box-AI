<template>
  <auth-layout slogan="平台运营与租户治理" :show-mascot="false">
    <span class="form-badge">管理端</span>
    <h2 class="form-title">平台登录</h2>
    <p class="form-desc">B 端管理后台 · 仅限 Box 平台管理员访问</p>

    <t-form :data="formData" :rules="rules" label-width="0" @submit="onSubmit">
      <t-form-item name="account">
        <t-input v-model="formData.account" placeholder="管理员账号" clearable size="large">
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
      <div class="form-extra">
        <router-link class="form-link" to="/forgot-password">忘记密码</router-link>
      </div>
      <t-form-item>
        <t-button theme="primary" type="submit" block size="large" shape="round" :loading="loading">
          登录管理端
        </t-button>
      </t-form-item>
    </t-form>
  </auth-layout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormProps } from 'tdesign-vue-next'
import AuthLayout from '@box/ui/layouts/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)

const formData = reactive({
  account: '',
  password: '',
})

const rules: FormProps['rules'] = {
  account: [{ required: true, message: '请输入账号' }],
  password: [{ required: true, message: '请输入密码' }],
}

onMounted(() => {
  auth.logout()
})

const onSubmit: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  loading.value = true
  try {
    await auth.login(formData.account, formData.password)
    MessagePlugin.success('登录成功')
    await router.push(auth.homePath)
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败'
    if (message) {
      MessagePlugin.error(message)
    }
  } finally {
    loading.value = false
  }
}
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

.form-extra {
  display: flex;
  justify-content: flex-end;
  margin: -8px 0 16px;
}

.form-link {
  font-size: 13px;
  color: var(--td-brand-color);
  text-decoration: none;
}

.form-link:hover {
  text-decoration: underline;
}
</style>
