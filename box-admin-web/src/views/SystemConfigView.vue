<template>
  <div class="system-config-page admin-page">
    <page-header title="系统配置" desc="管理 C 端关于、协议、客服邮箱等产品文案与版本信息。">
      <template #actions>
        <t-button theme="primary" :loading="saving" @click="onSave">保存全部</t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <t-form label-width="140px" class="config-form">
        <t-card :bordered="false" title="基础信息" class="config-card">
          <t-form-item
            v-for="item in basicItems"
            :key="item.configKey"
            :label="item.label"
          >
            <t-textarea
              v-if="item.multiline"
              v-model="form[item.configKey]"
              :autosize="{ minRows: 3, maxRows: 8 }"
              :placeholder="item.placeholder"
            />
            <t-input
              v-else
              v-model="form[item.configKey]"
              :placeholder="item.placeholder"
            />
          </t-form-item>
        </t-card>

        <t-card :bordered="false" title="法律文档" class="config-card">
          <p class="config-group__hint">左侧输入 HTML 或纯文本，右侧即时预览 C 端展示效果。</p>
          <t-form-item label="隐私政策">
            <legal-split-editor v-model="form['legal.privacy']" placeholder="可写 HTML，例如 <h2>引言</h2><p>……</p>" />
          </t-form-item>
          <t-form-item label="服务协议">
            <legal-split-editor v-model="form['legal.terms']" placeholder="可写 HTML，例如 <h2>服务内容</h2><p>……</p>" />
          </t-form-item>
          <t-form-item label="最后更新日期">
            <t-input v-model="form['legal.updated_at']" placeholder="2026-09-11" />
          </t-form-item>
        </t-card>

        <t-card :bordered="false" title="邮件 SMTP" class="config-card">
          <p class="config-group__hint">用于验证码与通知邮件。生产环境建议配合 application.yml 或密钥管理。</p>
          <t-form-item
            v-for="item in mailItems"
            :key="item.configKey"
            :label="item.label"
          >
            <t-input
              v-model="form[item.configKey]"
              :type="item.secret ? 'password' : 'text'"
              :placeholder="item.placeholder"
            />
          </t-form-item>
        </t-card>

        <t-card :bordered="false" title="OAuth 登录" class="config-card">
          <p class="config-group__hint">开关与 Client 信息供 C 端 OAuth 入口读取；回调实现仍依赖后端 box.oauth 配置。</p>
          <t-form-item
            v-for="item in oauthItems"
            :key="item.configKey"
            :label="item.label"
          >
            <t-select
              v-if="item.options"
              v-model="form[item.configKey]"
              :options="item.options"
            />
            <t-input
              v-else
              v-model="form[item.configKey]"
              :type="item.secret ? 'password' : 'text'"
              :placeholder="item.placeholder"
            />
          </t-form-item>
        </t-card>
      </t-form>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import LegalSplitEditor from '@/components/LegalSplitEditor.vue'
import { fetchSystemConfigs, upsertSystemConfig } from '@/api/systemConfig'
import { legalValueToSource } from '@/utils/legalHtml'

const basicItems = [
  { configKey: 'support.email', label: '客服邮箱', placeholder: 'support@example.com' },
  { configKey: 'about.product_name', label: '产品名称' },
  { configKey: 'about.slogan', label: '产品 Slogan', multiline: true },
  { configKey: 'about.positioning', label: '产品定位' },
  { configKey: 'app.version', label: '客户端版本' },
]

const legalKeys = ['legal.privacy', 'legal.terms', 'legal.updated_at']

const mailItems = [
  { configKey: 'mail.smtp.host', label: 'SMTP 主机', placeholder: 'smtp.example.com' },
  { configKey: 'mail.smtp.port', label: 'SMTP 端口', placeholder: '465' },
  { configKey: 'mail.smtp.username', label: 'SMTP 用户名', placeholder: 'noreply@example.com' },
  { configKey: 'mail.smtp.password', label: 'SMTP 密码', placeholder: '******', secret: true },
  { configKey: 'mail.from', label: '发件人地址', placeholder: 'noreply@box.ai' },
]

const oauthItems = [
  {
    configKey: 'oauth.github.enabled',
    label: 'GitHub 登录',
    options: [
      { label: '关闭', value: 'false' },
      { label: '开启', value: 'true' },
    ],
  },
  { configKey: 'oauth.github.client_id', label: 'GitHub Client ID', placeholder: 'Ov23li...' },
  { configKey: 'oauth.github.client_secret', label: 'GitHub Client Secret', placeholder: '******', secret: true },
  {
    configKey: 'oauth.google.enabled',
    label: 'Google 登录',
    options: [
      { label: '关闭', value: 'false' },
      { label: '开启', value: 'true' },
    ],
  },
  { configKey: 'oauth.google.client_id', label: 'Google Client ID', placeholder: 'xxx.apps.googleusercontent.com' },
  { configKey: 'oauth.google.client_secret', label: 'Google Client Secret', placeholder: '******', secret: true },
]

const loading = ref(false)
const saving = ref(false)
const form = reactive<Record<string, string>>({})

function initForm(configs: { configKey: string; configValue?: string }[]) {
  for (const item of [...basicItems, ...mailItems, ...oauthItems]) {
    const found = configs.find((cfg) => cfg.configKey === item.configKey)
    form[item.configKey] = found?.configValue || ''
  }
  for (const key of legalKeys) {
    const found = configs.find((cfg) => cfg.configKey === key)
    const raw = found?.configValue || ''
    form[key] = key === 'legal.updated_at' ? raw : legalValueToSource(raw)
  }
}

async function loadConfigs() {
  loading.value = true
  try {
    const { data } = await fetchSystemConfigs()
    initForm(data.data || [])
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const keys = [
      ...basicItems.map((item) => item.configKey),
      ...legalKeys,
      ...mailItems.map((item) => item.configKey),
      ...oauthItems.map((item) => item.configKey),
    ]
    for (const key of keys) {
      await upsertSystemConfig({
        configKey: key,
        configValue: form[key],
      })
    }
    MessagePlugin.success('已保存')
    await loadConfigs()
  } finally {
    saving.value = false
  }
}

onMounted(loadConfigs)
</script>

<style scoped>
.config-form {
  width: 100%;
}

.config-card {
  margin-bottom: 16px;
}

.config-group__hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--td-text-color-placeholder);
}
</style>
