<template>
  <div class="system-config-page">
    <page-header title="系统配置" desc="管理 C 端关于、协议、客服邮箱等产品文案与版本信息。" />

    <t-loading :loading="loading" size="small">
      <t-form label-width="140px" class="config-form">
        <section v-for="group in configGroups" :key="group.title" class="config-group">
          <h3 class="config-group__title">{{ group.title }}</h3>
          <t-form-item
            v-for="item in group.items"
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
        </section>

        <t-form-item>
          <t-button theme="primary" :loading="saving" @click="onSave">保存全部</t-button>
        </t-form-item>
      </t-form>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import { fetchSystemConfigs, upsertSystemConfig } from '@/api/systemConfig'

interface ConfigField {
  configKey: string
  label: string
  placeholder?: string
  multiline?: boolean
}

const configGroups: { title: string; items: ConfigField[] }[] = [
  {
    title: '基础信息',
    items: [
      { configKey: 'support.email', label: '客服邮箱', placeholder: 'support@example.com' },
      { configKey: 'about.product_name', label: '产品名称' },
      { configKey: 'about.slogan', label: '产品 Slogan', multiline: true },
      { configKey: 'about.positioning', label: '产品定位' },
      { configKey: 'app.version', label: '客户端版本' },
    ],
  },
  {
    title: '法律文档',
    items: [
      {
        configKey: 'legal.privacy',
        label: '隐私政策',
        multiline: true,
        placeholder: 'JSON 数组，每行一段。例如 ["段落1","段落2"]',
      },
      {
        configKey: 'legal.terms',
        label: '服务协议',
        multiline: true,
        placeholder: 'JSON 数组，每行一段',
      },
      { configKey: 'legal.updated_at', label: '最后更新日期', placeholder: '2026-09-11' },
    ],
  },
]

const loading = ref(false)
const saving = ref(false)
const form = reactive<Record<string, string>>({})

function initForm(configs: { configKey: string; configValue?: string }[]) {
  for (const group of configGroups) {
    for (const item of group.items) {
      const found = configs.find((cfg) => cfg.configKey === item.configKey)
      form[item.configKey] = found?.configValue || ''
    }
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
    for (const group of configGroups) {
      for (const item of group.items) {
        await upsertSystemConfig({
          configKey: item.configKey,
          configValue: form[item.configKey],
        })
      }
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
  max-width: 760px;
}

.config-group {
  margin-bottom: 28px;
  padding: 20px;
  border: 1px solid var(--box-border, #e5e6eb);
  border-radius: 12px;
  background: #fff;
}

.config-group__title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
}
</style>
