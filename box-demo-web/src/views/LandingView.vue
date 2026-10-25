<script setup lang="ts">
import { readmeImage } from '@/utils/assets'
import { adminNav, consumerNav } from '@/data/demoRegistry'

const logo = readmeImage('logo-readme.png')
const flowProduct = readmeImage('flow-product.png')

const pillars = [
  { title: 'Agent 全生命周期', desc: 'Builder 调试、版本发布、Embed 与 Open API' },
  { title: '企业级 RAG', desc: '文档管线、混合检索、对话引用溯源' },
  { title: '双 Runtime', desc: 'Agent Chat 与 Workflow 编排分离演进' },
  { title: '多租户治理', desc: 'RBAC、套餐额度、审计与全链路 Trace' },
]
</script>

<template>
  <div class="landing">
    <section class="landing-hero">
      <img class="landing-hero__logo" :src="logo" alt="Box" />
      <h1>Box AI 产品演示</h1>
      <p class="landing-hero__lead">
        企业级 AI Agent 与工作流平台能力全景。本站在 Cloudflare 托管，纯前端模拟交互与官方界面截图，无需部署后端即可对外展示。
      </p>
      <t-space size="medium" break-line>
        <t-button theme="primary" size="large" @click="$router.push('/demo/web/chat')">进入用户工作台演示</t-button>
        <t-button theme="default" size="large" variant="outline" @click="$router.push('/demo/admin/tenants')">
          进入管理台演示
        </t-button>
      </t-space>
    </section>

    <section class="landing-section">
      <h2>产品能力全景</h2>
      <div class="demo-shot">
        <img :src="flowProduct" alt="Box 产品能力全景" loading="lazy" />
      </div>
    </section>

    <section class="landing-section">
      <h2>为什么选择 Box</h2>
      <t-row :gutter="[16, 16]">
        <t-col v-for="p in pillars" :key="p.title" :xs="12" :md="6">
          <t-card :title="p.title" hover-shadow>
            <p class="pillar-desc">{{ p.desc }}</p>
          </t-card>
        </t-col>
      </t-row>
    </section>

    <section class="landing-section">
      <h2>演示模块一览</h2>
      <t-tabs default-value="web">
        <t-tab-panel value="web" label="C 端 box-web">
          <t-list split>
            <t-list-item v-for="item in consumerNav" :key="item.id">
              <t-list-item-meta :title="item.title" :description="item.summary" />
              <template #action>
                <t-button variant="text" theme="primary" @click="$router.push(item.path)">打开</t-button>
              </template>
            </t-list-item>
          </t-list>
        </t-tab-panel>
        <t-tab-panel value="admin" label="B 端 box-admin-web">
          <t-list split>
            <t-list-item v-for="item in adminNav" :key="item.id">
              <t-list-item-meta :title="item.title" :description="item.summary" />
              <template #action>
                <t-button variant="text" theme="primary" @click="$router.push(item.path)">打开</t-button>
              </template>
            </t-list-item>
          </t-list>
        </t-tab-panel>
      </t-tabs>
    </section>

    <footer class="landing-footer">
      Box · 开源企业级 Agent 平台 · 演示站数据均为虚构
    </footer>
  </div>
</template>

<style scoped>
.pillar-desc {
  margin: 0;
  color: var(--td-text-color-secondary);
  font-size: 14px;
  line-height: 1.6;
}
</style>
