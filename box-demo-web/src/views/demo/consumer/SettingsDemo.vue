<script setup lang="ts">
import { ref } from 'vue'

const tab = ref('api-keys')

const apiKeys = ref([{ name: '生产集成', prefix: 'box_live_••••7f3a', created: '2026-10-01' }])

const audit = [
  { time: '2026-10-25 11:20', user: '张明', action: '创建 API Key' },
  { time: '2026-10-24 16:02', user: '李悦', action: '更新角色「编辑」权限' },
]
</script>

<template>
  <div class="demo-page">
    <header class="demo-page__header">
      <h1>设置与治理</h1>
      <p>API Key、角色权限、审计与套餐入口（模拟）。</p>
    </header>

    <t-tabs v-model="tab">
      <t-tab-panel value="api-keys" label="API 密钥">
        <t-table
          :data="apiKeys"
          :columns="[
            { colKey: 'name', title: '名称' },
            { colKey: 'prefix', title: '密钥前缀' },
            { colKey: 'created', title: '创建时间' },
          ]"
          row-key="name"
        />
        <t-button theme="primary" style="margin-top: 12px">创建密钥（演示）</t-button>
        <t-alert theme="warning" title="安全提示" style="margin-top: 12px">
          真实环境中完整密钥仅在创建时展示一次。
        </t-alert>
      </t-tab-panel>
      <t-tab-panel value="roles" label="角色与权限">
        <t-tag theme="primary" variant="light">管理员</t-tag>
        <t-tag variant="light" style="margin-left: 8px">编辑</t-tag>
        <t-tag variant="light" style="margin-left: 8px">只读</t-tag>
      </t-tab-panel>
      <t-tab-panel value="audit" label="审计日志">
        <t-table
          :data="audit"
          :columns="[
            { colKey: 'time', title: '时间', width: 160 },
            { colKey: 'user', title: '用户', width: 100 },
            { colKey: 'action', title: '操作' },
          ]"
          row-key="time"
        />
      </t-tab-panel>
      <t-tab-panel value="plan" label="套餐与额度">
        <t-descriptions :column="1" bordered>
          <t-descriptions-item label="当前套餐">团队版</t-descriptions-item>
          <t-descriptions-item label="本月 Token">1.2M / 5M</t-descriptions-item>
          <t-descriptions-item label="席位">8 / 20</t-descriptions-item>
        </t-descriptions>
      </t-tab-panel>
    </t-tabs>
  </div>
</template>
