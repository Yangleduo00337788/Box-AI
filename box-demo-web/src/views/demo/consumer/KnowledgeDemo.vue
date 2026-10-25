<script setup lang="ts">
import { ref } from 'vue'

type Doc = { name: string; format: string; status: string; chunks: number }

const docs = ref<Doc[]>([
  { name: '产品 FAQ.pdf', format: 'PDF', status: 'READY', chunks: 42 },
  { name: '定价说明.docx', format: 'Word', status: 'READY', chunks: 18 },
  { name: '工单导出.xlsx', format: 'Excel', status: 'PROCESSING', chunks: 0 },
])

const testQuestion = ref('企业版是否支持 SSO？')
const testAnswer = ref('')

function runTest() {
  testAnswer.value =
    '（模拟检索）企业版支持对接企业 IdP 进行 SSO；OAuth 占位可在系统配置中启用 GitHub / Google 或接入 SAML。（演示数据）'
}

const columns = [
  { colKey: 'name', title: '文档' },
  { colKey: 'format', title: '格式', width: 80 },
  { colKey: 'status', title: '状态', width: 120 },
  { colKey: 'chunks', title: '分块数', width: 90 },
]
</script>

<template>
  <div class="demo-page">
    <header class="demo-page__header">
      <h1>知识库 RAG</h1>
      <p>文档处理状态机、分块统计与 Test Answer 问答模拟。</p>
    </header>

    <t-card title="文档列表" style="margin-bottom: 16px">
      <template #actions>
        <t-button theme="primary" variant="outline">上传文档</t-button>
      </template>
      <t-table :data="docs" :columns="columns" row-key="name">
        <template #status="{ row }">
          <t-tag :theme="row.status === 'READY' ? 'success' : 'warning'" variant="light" size="small">
            {{ row.status }}
          </t-tag>
        </template>
      </t-table>
    </t-card>

    <t-card title="Test Answer">
      <t-input v-model="testQuestion" placeholder="输入测试问题" style="margin-bottom: 12px" />
      <t-button theme="primary" @click="runTest">检索并生成</t-button>
      <p v-if="testAnswer" class="test-answer">{{ testAnswer }}</p>
    </t-card>
  </div>
</template>

<style scoped>
.test-answer {
  margin-top: 16px;
  padding: 12px;
  border-radius: 8px;
  background: var(--td-bg-color-secondarycontainer);
  line-height: 1.7;
  font-size: 14px;
}
</style>
