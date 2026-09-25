<template>
  <div class="builder">
    <header class="builder-header">
      <div class="builder-header__left">
        <t-button variant="text" shape="square" @click="router.push('/chat')">
          <template #icon><t-icon name="chevron-left" /></template>
        </t-button>
        <div class="builder-header__meta">
          <h1 class="builder-header__title">{{ headerTitle }}</h1>
          <p class="builder-header__desc">Agent Builder · 配置与调试</p>
        </div>
        <t-tag v-if="agent" :theme="agent.status === 'PUBLISHED' ? 'success' : 'default'" variant="light" size="small">
          {{ statusLabel(agent.status) }}
        </t-tag>
        <t-tag v-if="agent" variant="outline" size="small">v{{ agent.draftVersion ?? 1 }} Draft</t-tag>
      </div>
      <t-space>
        <t-button variant="outline" size="small" @click="openVersions">版本</t-button>
        <t-tag v-if="displayModelName" variant="outline" size="small">{{ displayModelName }}</t-tag>
      </t-space>
    </header>

    <t-loading :loading="loading" size="small" class="builder-body">
      <div v-if="loadError" class="builder-error">
        <t-empty :description="loadError">
          <template #title>
            <span>无法加载智能体</span>
          </template>
          <p class="builder-error__hint">请确认当前工作空间是否正确，或从侧栏打开该智能体。</p>
          <template #action>
            <t-button theme="primary" @click="router.push('/chat')">返回对话</t-button>
          </template>
        </t-empty>
      </div>
      <div v-else-if="agent" class="builder-layout">
        <nav class="builder-nav">
          <button
            v-for="item in navItems"
            :key="item.value"
            type="button"
            class="builder-nav__item"
            :class="{ 'builder-nav__item--active': activeTab === item.value }"
            @click="activeTab = item.value"
          >
            <t-icon :name="item.icon" />
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <section class="builder-config">
          <div v-if="activeTab === 'overview'" class="config-panel">
            <h2 class="config-panel__title">概览</h2>
            <p class="config-panel__desc">基础信息与可见性</p>
            <t-form :data="overviewForm" :rules="overviewRules" label-align="top" @submit="saveOverview">
              <t-form-item label="Logo">
                <image-picker v-model="overviewForm.avatarUrl" :fallback-text="(overviewForm.name || '智').slice(0, 1)" hint="上传 Logo" />
              </t-form-item>
              <t-form-item label="名称" name="name">
                <t-input v-model="overviewForm.name" maxlength="128" />
              </t-form-item>
              <t-form-item label="描述" name="description">
                <t-textarea
                  v-model="overviewForm.description"
                  :autosize="{ minRows: 3, maxRows: 6 }"
                  maxlength="500"
                />
              </t-form-item>
              <t-form-item>
                <t-button theme="primary" type="submit" :loading="savingOverview">保存</t-button>
              </t-form-item>
            </t-form>
          </div>

          <div v-else-if="activeTab === 'prompt'" class="config-panel">
            <h2 class="config-panel__title">Prompt</h2>
            <p class="config-panel__desc">定义智能体的系统提示词与角色设定</p>
            <t-form label-align="top">
              <t-form-item label="System Prompt">
                <MonacoEditor
                  v-model="promptForm.systemPrompt"
                  language="markdown"
                  height="420px"
                />
              </t-form-item>
              <t-form-item>
                <t-button theme="primary" :loading="savingPrompt" @click="savePrompt">保存 Prompt</t-button>
              </t-form-item>
            </t-form>
          </div>

          <div v-else-if="activeTab === 'model'" class="config-panel">
            <h2 class="config-panel__title">模型</h2>
            <p class="config-panel__desc">对话模型与生成参数</p>
            <t-form :data="modelForm" :rules="modelRules" label-align="top" @submit="saveModel">
              <t-form-item label="模型来源" name="modelSource">
                <t-radio-group v-model="modelForm.modelSource">
                  <t-radio value="PLATFORM">平台模型（扣配额）</t-radio>
                  <t-radio value="AUTO">智能路由（Auto）</t-radio>
                  <t-radio v-if="byokEnabled" value="BYOK">自带密钥（BYOK）</t-radio>
                </t-radio-group>
              </t-form-item>
              <t-form-item v-if="modelForm.modelSource === 'AUTO'" label="路由策略" name="routingPreference">
                <t-radio-group v-model="modelForm.routingPreference">
                  <t-radio value="BALANCED">均衡</t-radio>
                  <t-radio value="QUALITY">质量优先</t-radio>
                  <t-radio value="COST">成本优先</t-radio>
                </t-radio-group>
                <p class="model-auto-hint">
                  对话时由平台从可用模型池中自动选择，无需手动指定模型；调试与发布均按当前策略路由。
                </p>
              </t-form-item>
              <t-form-item
                v-if="modelForm.modelSource === 'PLATFORM'"
                label="平台模型"
                name="platformModelId"
              >
                <t-select
                  v-model="modelForm.platformModelId"
                  :options="platformModelOptions"
                  placeholder="请选择平台模型"
                />
              </t-form-item>
              <t-form-item v-else-if="modelForm.modelSource === 'BYOK'" label="自带模型" name="modelId">
                <t-select v-model="modelForm.modelId" :options="byokModelOptions" placeholder="请选择模型" />
              </t-form-item>
              <t-form-item label="Temperature">
                <t-slider v-model="modelForm.temperature" :min="0" :max="2" :step="0.1" />
              </t-form-item>
              <t-form-item label="Top P">
                <t-slider v-model="modelForm.topP" :min="0" :max="1" :step="0.05" />
              </t-form-item>
              <t-form-item label="Max Tokens" name="maxTokens">
                <t-input-number v-model="modelForm.maxTokens" :min="1" :max="128000" theme="column" />
              </t-form-item>
              <t-form-item label="流式输出">
                <t-switch v-model="modelForm.streamEnabled" />
              </t-form-item>
              <t-form-item>
                <t-button theme="primary" type="submit" :loading="savingModel">保存模型配置</t-button>
              </t-form-item>
            </t-form>
          </div>

          <div v-else-if="activeTab === 'workflows'" class="config-panel">
            <h2 class="config-panel__title">工作流</h2>
            <p class="config-panel__desc">
              绑定工作流后，可将一条设为「默认」在每次对话前执行，或将工作流暴露为可调用工具供模型按需触发。
            </p>
            <t-select
              v-model="selectedWorkflowId"
              :options="workflowOptions"
              placeholder="选择要绑定的工作流"
              clearable
              style="margin-bottom: 12px"
            />
            <t-space direction="vertical" style="margin-bottom: 12px">
              <t-checkbox v-model="bindWorkflowDefault">设为默认工作流（每条消息前执行）</t-checkbox>
              <t-checkbox v-model="bindWorkflowCallable">暴露为可调用工具</t-checkbox>
            </t-space>
            <t-button theme="primary" :loading="bindingWorkflow" @click="bindWorkflow">绑定工作流</t-button>
            <t-table row-key="id" :data="workflowBindings" :columns="workflowColumns" size="small" style="margin-top: 16px">
              <template #defaultWorkflow="{ row }">
                <t-tag v-if="row.defaultWorkflow" size="small" theme="primary" variant="light">默认</t-tag>
                <span v-else>—</span>
              </template>
              <template #callable="{ row }">
                {{ row.callable ? '是' : '否' }}
              </template>
              <template #op="{ row }">
                <t-button variant="text" theme="danger" @click="unbindWorkflow(row.workflowId)">解除</t-button>
              </template>
            </t-table>
          </div>

          <div v-else-if="activeTab === 'knowledge'" class="config-panel">
            <h2 class="config-panel__title">知识库</h2>
            <p class="config-panel__desc">绑定知识库后，对话将自动 RAG 检索</p>
            <t-select
              v-model="selectedKnowledgeId"
              :options="knowledgeOptions"
              placeholder="选择要绑定的知识库"
              clearable
              style="margin-bottom: 12px"
            />
            <t-button theme="primary" :loading="bindingKnowledge" @click="bindKnowledge">绑定知识库</t-button>
            <t-table row-key="id" :data="knowledgeBindings" :columns="knowledgeColumns" size="small" style="margin-top: 16px">
              <template #op="{ row }">
                <t-button variant="text" theme="danger" @click="unbindKnowledge(row.knowledgeBaseId)">解除</t-button>
              </template>
            </t-table>
          </div>

          <div v-else-if="activeTab === 'memory'" class="config-panel">
            <h2 class="config-panel__title">记忆</h2>
            <p class="config-panel__desc">会话记忆控制当前对话上下文；长期记忆跨会话保存用户信息并在后续对话中检索引用。</p>
            <t-form label-align="top">
              <t-form-item label="启用会话记忆">
                <t-switch v-model="memoryForm.memoryEnabled" />
              </t-form-item>
              <t-form-item label="记忆窗口（最近消息条数）">
                <t-input-number
                  v-model="memoryForm.memoryWindowSize"
                  :min="0"
                  :max="100"
                  :disabled="!memoryForm.memoryEnabled"
                  theme="column"
                />
              </t-form-item>
              <t-form-item label="启用长期记忆">
                <t-switch v-model="memoryForm.longTermMemoryEnabled" />
              </t-form-item>
              <t-form-item>
                <t-button theme="primary" :loading="savingMemory" @click="saveMemory">保存记忆配置</t-button>
              </t-form-item>
            </t-form>
            <template v-if="memoryForm.longTermMemoryEnabled">
              <h3 class="config-subtitle">已保存的长期记忆</h3>
              <t-table
                row-key="id"
                :data="longTermMemories"
                :columns="longTermMemoryColumns"
                size="small"
                :loading="loadingLongTermMemories"
                style="margin-top: 12px"
              >
                <template #op="{ row }">
                  <t-button variant="text" theme="danger" @click="removeLongTermMemory(row.id)">删除</t-button>
                </template>
              </t-table>
            </template>
          </div>

          <div v-else-if="activeTab === 'tools'" class="config-panel">
            <h2 class="config-panel__title">协作</h2>
            <p class="config-panel__desc">
              HTTP 工具、MCP 与技能请在对话页输入框「加号 → 插件」中按消息选用。此处可配置子智能体委派。
            </p>

            <h3 class="config-subtitle">子智能体</h3>
            <p class="config-panel__desc">绑定其他 Agent 后，主 Agent 可通过 Tool Calling 委派子任务</p>
            <t-select
              v-model="selectedSubAgentId"
              :options="subAgentOptions"
              placeholder="选择要绑定的子智能体"
              clearable
              style="margin-bottom: 12px"
            />
            <t-button theme="primary" :loading="bindingSubAgent" @click="bindSubAgent">绑定子智能体</t-button>
            <t-table row-key="id" :data="subAgentBindings" :columns="subAgentColumns" size="small" style="margin-top: 16px">
              <template #op="{ row }">
                <t-button variant="text" theme="danger" @click="unbindSubAgent(row.subAgentId)">解除</t-button>
              </template>
            </t-table>
          </div>

          <div v-else-if="activeTab === 'variables'" class="config-panel">
            <h2 class="config-panel__title">变量</h2>
            <p class="config-panel__desc">定义可在 Prompt 中引用的自定义变量（JSON 数组）</p>
            <t-textarea
              v-model="configForm.variablesJson"
              :autosize="{ minRows: 10, maxRows: 20 }"
              placeholder='[{"name":"user_name","type":"string","defaultValue":"","description":""}]'
            />
            <t-button theme="primary" :loading="savingConfig" style="margin-top: 12px" @click="saveConfig">
              保存变量
            </t-button>
          </div>

          <div v-else-if="activeTab === 'advanced'" class="config-panel">
            <h2 class="config-panel__title">高级</h2>
            <p class="config-panel__desc">运行时限制与高级参数</p>
            <t-form label-align="top">
              <t-form-item label="最大 Tool 调用次数">
                <t-input-number v-model="configForm.maxToolCalls" :min="0" :max="50" theme="column" />
              </t-form-item>
              <t-form-item label="最大执行时间 (ms)">
                <t-input-number v-model="configForm.maxExecutionTimeMs" :min="1000" :max="600000" theme="column" />
              </t-form-item>
              <t-form-item>
                <t-button theme="primary" :loading="savingConfig" @click="saveConfig">保存高级配置</t-button>
              </t-form-item>
            </t-form>
          </div>

          <div v-else-if="activeTab === 'debug'" class="config-panel">
            <h2 class="config-panel__title">调试</h2>
            <p class="config-panel__desc">使用右侧预览面板进行 SSE 对话测试；完整执行记录可在执行记录页查看 Trace。</p>
            <t-space>
              <t-button variant="outline" @click="router.push('/debug')">打开 Debug Console</t-button>
              <t-button variant="outline" @click="router.push('/executions')">查看执行记录</t-button>
              <t-button variant="outline" @click="activeTab = 'overview'">返回概览</t-button>
            </t-space>
          </div>

          <div v-else-if="activeTab === 'publish'" class="config-panel">
            <h2 class="config-panel__title">发布</h2>
            <p class="config-panel__desc">发布后可被对话与开放 API 调用</p>
            <t-descriptions :column="1" bordered>
              <t-descriptions-item label="状态">{{ statusLabel(agent.status) }}</t-descriptions-item>
              <t-descriptions-item label="发布版本">v{{ publishInfo?.publishedVersionNo || agent.publishedVersion || '—' }}</t-descriptions-item>
              <t-descriptions-item label="发布时间">{{ publishInfo?.publishedAt || '—' }}</t-descriptions-item>
            </t-descriptions>
            <t-space style="margin-top: 16px">
              <t-button theme="primary" :loading="publishing" @click="doPublish">发布</t-button>
              <t-button variant="outline" :loading="publishing" @click="doUnpublish">取消发布</t-button>
            </t-space>

            <section v-if="agent.status === 'PUBLISHED'" class="publish-api">
              <h3 class="config-subtitle">集成方式</h3>
              <t-alert
                theme="info"
                message="在「设置 → API 密钥」创建密钥后，使用 Bearer Token 调用开放 API，或嵌入 Web Chat。"
              />
              <t-tabs v-model="publishTab" class="publish-tabs">
                <t-tab-panel value="api" label="API">
                  <div class="api-snippet">
                    <p><strong>POST</strong> <code>{{ publishEndpoint }}</code></p>
                    <pre>{{ publishApiExample }}</pre>
                  </div>
                </t-tab-panel>
                <t-tab-panel value="embed" label="Embed">
                  <div class="api-snippet">
                    <h4 class="config-subtitle">外观定制</h4>
                    <t-form label-align="top" class="embed-form">
                      <t-form-item label="主题色">
                        <t-color-picker v-model="embedForm.themeColor" format="HEX" />
                      </t-form-item>
                      <t-form-item label="Logo URL">
                        <t-input v-model="embedForm.logoUrl" placeholder="https://..." />
                      </t-form-item>
                      <t-form-item label="欢迎语">
                        <t-textarea
                          v-model="embedForm.welcomeMessage"
                          placeholder="你好，有什么可以帮你？"
                          :autosize="{ minRows: 2, maxRows: 4 }"
                          maxlength="500"
                        />
                      </t-form-item>
                      <t-form-item label="推荐问题（每行一条）">
                        <t-textarea
                          v-model="embedForm.suggestedQuestionsText"
                          placeholder="如何开始使用？&#10;有哪些功能？"
                          :autosize="{ minRows: 3, maxRows: 6 }"
                        />
                      </t-form-item>
                      <t-form-item label="自定义域名">
                        <t-input v-model="embedForm.customDomain" placeholder="chat.example.com" />
                      </t-form-item>
                      <t-alert
                        v-if="embedForm.customDomain && embedForm.domainVerifySkipped"
                        theme="warning"
                        message="当前为开发环境，已跳过 DNS / 校验文件检查。生产请将 box.embed.skip-domain-verify 设为 false。"
                        style="margin-bottom: 12px"
                      />
                      <p v-if="embedForm.customDomain" class="embed-domain-hint">
                        将域名 CNAME 到当前站点后，访问根路径会打开嵌入对话。
                        <span v-if="embedForm.domainVerifySkipped && embedForm.domainVerified">本地已跳过校验。</span>
                        <span v-else-if="embedForm.domainVerified">已验证。</span>
                        <span v-else>尚未验证。</span>
                      </p>
                      <p v-if="embedForm.gatewaySetupHint" class="embed-domain-hint">
                        {{ embedForm.gatewaySetupHint }}
                      </p>
                      <p v-if="embedForm.gatewayCnameTarget" class="embed-domain-hint">
                        网关 CNAME：<code>{{ embedForm.gatewayCnameTarget }}</code>
                        <span v-if="embedForm.gatewayTlsMode">（TLS：{{ embedForm.gatewayTlsMode }}）</span>
                      </p>
                      <p v-if="embedForm.domainVerifyToken" class="embed-domain-hint">
                        任选其一完成校验：TXT 记录
                        <code>box-verify={{ embedForm.domainVerifyToken }}</code>
                        （可写在域名或 <code>_box-verify.{{ embedForm.customDomain }}</code>），或将
                        <code>/.well-known/box-domain-verify.txt</code> 内容设为同一令牌。CNAME 到本站时可由平台自动提供该文件。
                      </p>
                      <t-space>
                        <t-button theme="primary" :loading="savingEmbed" @click="saveEmbedConfig">保存 Embed 配置</t-button>
                        <t-button
                          v-if="embedForm.customDomain"
                          variant="outline"
                          :loading="verifyingDomain"
                          @click="verifyEmbedDomain"
                        >
                          验证域名
                        </t-button>
                      </t-space>
                    </t-form>
                    <h4 class="config-subtitle" style="margin-top: 20px">集成代码</h4>
                    <p>Web Chat URL</p>
                    <pre>{{ publishChatUrl }}</pre>
                    <p style="margin-top: 12px">Embed Code</p>
                    <pre>{{ publishEmbedCode }}</pre>
                  </div>
                </t-tab-panel>
                <t-tab-panel value="sdk" label="SDK">
                  <t-radio-group v-model="sdkLang" variant="default-filled" size="small" style="margin-bottom: 12px">
                    <t-radio-button value="javascript">JavaScript</t-radio-button>
                    <t-radio-button value="python">Python</t-radio-button>
                    <t-radio-button value="curl">cURL</t-radio-button>
                  </t-radio-group>
                  <div class="api-snippet">
                    <pre>{{ publishSdkExample }}</pre>
                  </div>
                </t-tab-panel>
              </t-tabs>
              <t-space style="margin-top: 12px">
                <t-button variant="outline" @click="router.push('/settings/api-keys')">管理 API 密钥</t-button>
                <t-button variant="outline" @click="copyPublishSnippet">复制当前示例</t-button>
              </t-space>
            </section>
          </div>
        </section>

        <aside class="builder-preview">
          <div class="preview-header">
            <h3>调试预览</h3>
            <t-button variant="text" size="small" @click="clearChat">清空</t-button>
          </div>
          <box-chat-message-list
            ref="chatListRef"
            class="preview-messages"
            :items="previewChatListItems"
            :actions-disabled="chatting"
            empty-text="发送消息测试当前 Prompt 与模型配置"
          />
          <div class="preview-input">
            <box-chat-sender
              v-model="chatInput"
              :loading="chatting"
              :can-send="Boolean(chatInput.trim())"
              placeholder="输入测试消息…"
              :max-rows="4"
              :show-cloud="false"
              :show-attach="false"
              @send="sendChat"
              @stop="stopChat"
            />
          </div>
        </aside>
      </div>
    </t-loading>

    <t-drawer v-model:visible="versionsVisible" header="版本历史" size="520px">
      <t-space style="margin-bottom: 16px">
        <t-button theme="primary" size="small" :loading="versionLoading" @click="createVersionSnapshot">
          创建版本快照
        </t-button>
        <t-button variant="outline" size="small" :disabled="!compareBaseId || !compareTargetId" @click="runCompare">
          对比选中版本
        </t-button>
      </t-space>
      <t-table
        row-key="id"
        :data="versions"
        :columns="versionColumns"
        :loading="versionLoading"
        bordered
        stripe
        size="small"
      >
        <template #select="{ row }">
          <t-checkbox
            :checked="compareBaseId === row.id"
            @change="(checked: boolean) => { compareBaseId = checked ? row.id : undefined }"
          />
          <t-checkbox
            :checked="compareTargetId === row.id"
            style="margin-left: 8px"
            @change="(checked: boolean) => { compareTargetId = checked ? row.id : undefined }"
          />
        </template>
        <template #status="{ row }">
          <t-tag size="small" variant="light">{{ versionStatusLabel(row) }}</t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-button
              v-if="!row.currentDraft"
              variant="text"
              size="small"
              @click="restoreVersion(row.id)"
            >
              恢复
            </t-button>
            <t-button
              v-if="!row.currentDraft && !row.published && row.status !== 'ARCHIVED'"
              variant="text"
              theme="warning"
              size="small"
              @click="archiveVersion(row.id)"
            >
              归档
            </t-button>
          </t-space>
        </template>
      </t-table>
    </t-drawer>

    <t-dialog v-model:visible="compareVisible" header="版本对比" width="720px" :footer="false">
      <t-table
        row-key="field"
        :data="compareResult"
        :columns="compareColumns"
        bordered
        stripe
        size="small"
      >
        <template #changed="{ row }">
          <t-tag :theme="row.changed ? 'warning' : 'success'" size="small" variant="light">
            {{ row.changed ? '已变更' : '相同' }}
          </t-tag>
        </template>
      </t-table>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'
import type { FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import MonacoEditor from '@/components/MonacoEditor.vue'
import ImagePicker from '@/components/ImagePicker.vue'
import { extractApiError } from '@/api/apiError'
import { parseAgentVariablesJson } from '@/utils/agentConfigBuilder'
import { promptToolConfirmation } from '@/composables/useToolConfirmation'
import {
  archiveAgentVersion,
  bindAgentKnowledge,
  bindAgentWorkflow,
  bindAgentSubAgent,
  chatAgent,
  chatAgentStream,
  compareAgentVersions,
  createAgentVersion,
  getAgent,
  getAgentPublishStatus,
  listAgentKnowledge,
  listAgentWorkflows,
  listAgentLongTermMemories,
  listAgentSubAgents,
  listAgentVersions,
  listAgents,
  deleteAgentLongTermMemory,
  publishAgent,
  restoreAgentVersion,
  unbindAgentKnowledge,
  unbindAgentWorkflow,
  unbindAgentSubAgent,
  unpublishAgent,
  getAgentEmbedConfig,
  type AgentEmbedConfigVO,
  updateAgent,
  updateAgentConfig,
  updateAgentEmbedConfig,
  updateAgentMemory,
  updateAgentModel,
  updateAgentPrompt,
  verifyAgentEmbedDomain,
  type AgentKnowledgeBindingVO,
  type AgentWorkflowBindingVO,
  type AgentLongTermMemoryVO,
  type AgentPublishVO,
  type AgentSubAgentBindingVO,
  type AgentVersionDiffVO,
  type AgentVersionVO,
  type AgentVO,
} from '@/api/agent'
import { listKnowledgeBases, type KnowledgeBaseVO } from '@/api/knowledge'
import { listWorkflows, type WorkflowVO } from '@/api/workflow'
import { listModels, type ModelVO } from '@/api/model'
import { groupedPlatformModelOptions } from '@/utils/modelOptions'
import {
  fetchPlatformCapabilities,
  listPlatformModels,
  type PlatformModelVO,
} from '@/api/platform'
import type { KnowledgeCitation, ModelSource } from '@/api/agent'
import BoxChatMessageList from '@/components/BoxChatMessageList.vue'
import BoxChatSender from '@/components/BoxChatSender.vue'
import { buildLocalListItems } from '@/utils/boxChatListItems'
import type { LocalChatMessage } from '@/utils/chatMessageAdapter'

interface BuilderPreviewMessage extends LocalChatMessage {
  citations?: KnowledgeCitation[]
}

const route = useRoute()
const router = useRouter()
const agentId = computed(() => Number(route.params.id))

const loading = ref(true)
const loadError = ref('')
const agent = ref<AgentVO | null>(null)

const headerTitle = computed(() => {
  if (agent.value?.name) {
    return agent.value.name
  }
  if (loadError.value) {
    return '无法加载智能体'
  }
  if (loading.value) {
    return '加载中…'
  }
  return '智能体不存在'
})
const models = ref<ModelVO[]>([])
const platformModels = ref<PlatformModelVO[]>([])
const byokEnabled = ref(false)
const activeTab = ref('overview')

const savingOverview = ref(false)
const savingPrompt = ref(false)
const savingModel = ref(false)
const chatting = ref(false)
const chatInput = ref('')
const messages = ref<BuilderPreviewMessage[]>([])
const chatListRef = ref<InstanceType<typeof BoxChatMessageList> | null>(null)
const previewChatListItems = computed(() =>
  buildLocalListItems(
    messages.value,
    chatting.value,
    {
      enableRegenerate: false,
      enableDelete: false,
      enableFullActions: true,
    },
    {
      userName: '我',
      agentName: headerTitle.value,
    },
  ),
)

const navItems = [
  { value: 'overview', label: '概览', icon: 'home' },
  { value: 'prompt', label: 'Prompt', icon: 'edit' },
  { value: 'model', label: '模型', icon: 'cpu' },
  { value: 'memory', label: '记忆', icon: 'time' },
  { value: 'knowledge', label: '知识库', icon: 'book' },
  { value: 'workflows', label: '工作流', icon: 'tree-square-dot' },
  { value: 'tools', label: '协作', icon: 'usergroup' },
  { value: 'variables', label: '变量', icon: 'data' },
  { value: 'advanced', label: '高级', icon: 'setting' },
  { value: 'debug', label: '调试', icon: 'bug' },
  { value: 'publish', label: '发布', icon: 'upload' },
]

const knowledgeBases = ref<KnowledgeBaseVO[]>([])
const workflows = ref<WorkflowVO[]>([])
const knowledgeBindings = ref<AgentKnowledgeBindingVO[]>([])
const workflowBindings = ref<AgentWorkflowBindingVO[]>([])
const subAgentBindings = ref<AgentSubAgentBindingVO[]>([])
const allAgents = ref<AgentVO[]>([])
const publishInfo = ref<AgentPublishVO | null>(null)
const selectedKnowledgeId = ref<number | undefined>()
const selectedWorkflowId = ref<number | undefined>()
const selectedSubAgentId = ref<number | undefined>()
const bindWorkflowDefault = ref(false)
const bindWorkflowCallable = ref(true)
const bindingKnowledge = ref(false)
const bindingWorkflow = ref(false)
const bindingSubAgent = ref(false)
const savingMemory = ref(false)
const savingConfig = ref(false)
const savingEmbed = ref(false)
const verifyingDomain = ref(false)
const publishing = ref(false)
const publishTab = ref<'api' | 'embed' | 'sdk'>('api')
const sdkLang = ref<'javascript' | 'python' | 'curl'>('javascript')
const versionsVisible = ref(false)
const versionLoading = ref(false)
const versions = ref<AgentVersionVO[]>([])
const compareBaseId = ref<number | undefined>()
const compareTargetId = ref<number | undefined>()
const compareVisible = ref(false)
const compareResult = ref<AgentVersionDiffVO[]>([])
let streamAbortController: AbortController | null = null
const stoppedByUser = ref(false)

const versionColumns: PrimaryTableCol<AgentVersionVO>[] = [
  { colKey: 'select', title: '对比', width: 90 },
  { colKey: 'versionName', title: '版本', width: 80 },
  { colKey: 'status', title: '状态', width: 100 },
  { colKey: 'updatedAt', title: '更新时间', width: 160 },
  { colKey: 'op', title: '操作', width: 140 },
]

const compareColumns: PrimaryTableCol<AgentVersionDiffVO>[] = [
  { colKey: 'label', title: '字段', width: 140 },
  { colKey: 'baseValue', title: '基准版本', ellipsis: true },
  { colKey: 'targetValue', title: '目标版本', ellipsis: true },
  { colKey: 'changed', title: '差异', width: 90 },
]

const knowledgeOptions = computed(() =>
  knowledgeBases.value.map((item) => ({ label: item.name, value: item.id })),
)
const workflowOptions = computed(() =>
  workflows.value
    .filter((item) => !workflowBindings.value.some((binding) => binding.workflowId === item.id))
    .map((item) => ({ label: item.name, value: item.id })),
)
const subAgentOptions = computed(() =>
  allAgents.value
    .filter((item) => item.id !== agentId.value)
    .filter((item) => !subAgentBindings.value.some((binding) => binding.subAgentId === item.id))
    .map((item) => ({ label: item.name, value: item.id })),
)
const publishEndpoint = computed(() => `/api/v1/published/agents/${agentId.value}/chat`)
const publishChatUrl = computed(() => {
  const title = encodeURIComponent(agent.value?.name || 'Box Agent')
  if (embedForm.customDomain && embedForm.domainVerified) {
    return `https://${embedForm.customDomain}/embed?apiKey=YOUR_API_KEY&title=${title}`
  }
  return `${window.location.origin}/embed/agents/${agentId.value}?apiKey=YOUR_API_KEY&title=${title}`
})
const publishEmbedCode = computed(
  () => `<iframe
  src="${publishChatUrl.value}"
  width="100%"
  height="600"
  frameborder="0"
  allow="clipboard-write"
></iframe>`,
)
const publishApiExample = computed(() => {
  const base =
    typeof window !== 'undefined' && window.location.origin
      ? window.location.origin
      : 'http://127.0.0.1:8080'
  const url = `${base}${publishEndpoint.value}`
  return `curl -X POST "${url}" \\
  -H "Authorization: Bearer ax_live_你的密钥" \\
  -H "Content-Type: application/json" \\
  -d '{"message":"你好","stream":false}'`
})
const publishJsExample = computed(
  () => `import { BoxClient } from '@box/sdk'

const box = new BoxClient({
  baseUrl: window.location.origin,
  apiKey: 'ax_live_你的密钥',
})
const answer = await box.chat(${agentId.value}, '你好', {
  stream: true,
  onDelta: (chunk) => console.log(chunk),
})
console.log(answer)`,
)
const publishPythonExample = computed(
  () => `from boxai import BoxClient

box = BoxClient('${typeof window !== 'undefined' ? window.location.origin : 'https://your-box-host'}', 'ax_live_你的密钥')
print(box.chat(${agentId.value}, '你好', stream=True))`,
)
const publishSdkExample = computed(() => {
  if (sdkLang.value === 'python') {
    return publishPythonExample.value
  }
  if (sdkLang.value === 'curl') {
    return publishApiExample.value
  }
  return publishJsExample.value
})
const knowledgeColumns = [
  { colKey: 'knowledgeBaseId', title: '知识库 ID' },
  { colKey: 'topK', title: 'Top K', width: 80 },
  { colKey: 'op', title: '操作', width: 100 },
]
const workflowColumns = [
  { colKey: 'workflowName', title: '工作流' },
  { colKey: 'workflowId', title: 'ID', width: 90 },
  { colKey: 'defaultWorkflow', title: '默认', width: 80 },
  { colKey: 'callable', title: '可调用', width: 80 },
  { colKey: 'op', title: '操作', width: 100 },
]
const subAgentColumns = [
  { colKey: 'subAgentName', title: '子智能体' },
  { colKey: 'subAgentId', title: 'ID', width: 80 },
  { colKey: 'enabled', title: '启用', width: 80 },
  { colKey: 'op', title: '操作', width: 100 },
]

const overviewForm = reactive({ name: '', description: '', avatarUrl: '' })
const promptForm = reactive({ systemPrompt: '' })
const modelForm = reactive({
  modelSource: 'PLATFORM' as ModelSource,
  platformModelId: undefined as number | undefined,
  modelId: undefined as number | undefined,
  routingPreference: 'BALANCED',
  temperature: 0.7,
  topP: 1,
  maxTokens: 4096,
  streamEnabled: true,
})
const memoryForm = reactive({
  memoryEnabled: true,
  memoryWindowSize: 20,
  longTermMemoryEnabled: false,
})
const longTermMemories = ref<AgentLongTermMemoryVO[]>([])
const loadingLongTermMemories = ref(false)
const longTermMemoryColumns: PrimaryTableCol<AgentLongTermMemoryVO>[] = [
  { colKey: 'content', title: '记忆内容', ellipsis: true },
  { colKey: 'createdAt', title: '创建时间', width: 180 },
  { colKey: 'op', title: '操作', width: 80 },
]
const configForm = reactive({
  variablesJson: '[]',
  maxToolCalls: 10,
  maxExecutionTimeMs: 120000,
})

const embedForm = reactive({
  themeColor: '#0052d9',
  logoUrl: '',
  welcomeMessage: '',
  suggestedQuestionsText: '',
  customDomain: '',
  domainVerified: false,
  domainVerifyToken: '',
  domainVerifySkipped: false,
  gatewayCnameTarget: '',
  gatewayTlsMode: '',
  gatewaySetupHint: '',
})

interface AgentConfigPayload {
  variables?: unknown[]
  advanced?: { maxToolCalls?: number; maxExecutionTimeMs?: number }
  embed?: {
    themeColor?: string
    logoUrl?: string
    welcomeMessage?: string
    suggestedQuestions?: string[]
    customDomain?: string
  }
}

const overviewRules: FormProps['rules'] = {
  name: [{ required: true, message: '请输入名称' }],
}

const modelRules: FormProps['rules'] = {
  modelSource: [{ required: true, message: '请选择模型来源' }],
  maxTokens: [{ required: true, message: '请输入 Max Tokens' }],
}

const platformModelOptions = computed(() => groupedPlatformModelOptions(platformModels.value))

const byokModelOptions = computed(() =>
  models.value
    .filter((item) => item.status === 1 && item.modelType === 'CHAT')
    .map((item) => ({
      label: item.providerName ? `${item.modelName}（${item.providerName}）` : item.modelName,
      value: item.id,
    })),
)

const displayModelName = computed(() => {
  if (!agent.value) return ''
  if (agent.value.modelSource === 'BYOK') {
    return agent.value.modelName ? `${agent.value.modelName}（BYOK）` : '自带密钥'
  }
  if (agent.value.modelSource === 'AUTO') {
    return `智能路由（${agent.value.routingPreference || 'BALANCED'}）`
  }
  return agent.value.platformModelName || agent.value.modelName || ''
})

function statusLabel(status: string) {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'ARCHIVED') return '已归档'
  return '草稿'
}

function versionStatusLabel(row: AgentVersionVO) {
  if (row.currentDraft) return '当前草稿'
  if (row.published) return '已发布'
  if (row.status === 'ARCHIVED') return '已归档'
  return row.status
}

async function loadVersions() {
  versionLoading.value = true
  try {
    const { data } = await listAgentVersions(agentId.value)
    versions.value = data.data || []
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '加载版本失败'))
  } finally {
    versionLoading.value = false
  }
}

async function openVersions() {
  versionsVisible.value = true
  compareBaseId.value = undefined
  compareTargetId.value = undefined
  await loadVersions()
}

async function createVersionSnapshot() {
  versionLoading.value = true
  try {
    await createAgentVersion(agentId.value)
    MessagePlugin.success('版本快照已创建')
    await loadVersions()
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '创建版本失败'))
  } finally {
    versionLoading.value = false
  }
}

async function runCompare() {
  if (!compareBaseId.value || !compareTargetId.value) return
  try {
    const { data } = await compareAgentVersions(agentId.value, compareBaseId.value, compareTargetId.value)
    compareResult.value = data.data?.diffs || []
    compareVisible.value = true
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '对比失败'))
  }
}

function restoreVersion(versionId: number) {
  const dialog = DialogPlugin.confirm({
    header: '恢复版本',
    body: '将把选中版本覆盖当前草稿，是否继续？',
    onConfirm: async () => {
      try {
        await restoreAgentVersion(agentId.value, versionId)
        MessagePlugin.success('版本已恢复')
        versionsVisible.value = false
        await loadAgent()
        dialog.destroy()
      } catch (error) {
        MessagePlugin.error(extractApiError(error, '恢复失败'))
      }
    },
  })
}

async function archiveVersion(versionId: number) {
  versionLoading.value = true
  try {
    await archiveAgentVersion(agentId.value, versionId)
    MessagePlugin.success('版本已归档')
    await loadVersions()
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '归档失败'))
  } finally {
    versionLoading.value = false
  }
}

function applyAgent(data: AgentVO) {
  agent.value = data
  overviewForm.name = data.name
  overviewForm.description = data.description || ''
  overviewForm.avatarUrl = data.avatarUrl || ''
  promptForm.systemPrompt = data.systemPrompt || ''
  modelForm.modelSource =
    data.modelSource === 'BYOK' ? 'BYOK' : data.modelSource === 'AUTO' ? 'AUTO' : 'PLATFORM'
  modelForm.routingPreference = data.routingPreference || 'BALANCED'
  modelForm.platformModelId = data.platformModelId
  modelForm.modelId = data.modelId
  modelForm.temperature = data.temperature ?? 0.7
  modelForm.topP = data.topP ?? 1
  modelForm.maxTokens = data.maxTokens ?? 4096
  modelForm.streamEnabled = data.streamEnabled ?? true
  memoryForm.memoryEnabled = data.memoryEnabled ?? true
  memoryForm.memoryWindowSize = data.memoryWindowSize ?? 20
  memoryForm.longTermMemoryEnabled = data.longTermMemoryEnabled ?? false
  applyConfigJson(data.configJson)
  void loadEmbedConfig()
  if (memoryForm.longTermMemoryEnabled) {
    loadLongTermMemories()
  } else {
    longTermMemories.value = []
  }
}

function applyConfigJson(raw?: string) {
  if (!raw) {
    configForm.variablesJson = '[]'
    configForm.maxToolCalls = 10
    configForm.maxExecutionTimeMs = 120000
    embedForm.themeColor = '#0052d9'
    embedForm.logoUrl = ''
    embedForm.welcomeMessage = ''
    embedForm.suggestedQuestionsText = ''
    embedForm.customDomain = ''
    embedForm.domainVerified = false
    embedForm.domainVerifyToken = ''
    embedForm.domainVerifySkipped = false
    return
  }
  try {
    const parsed = JSON.parse(raw) as AgentConfigPayload
    configForm.variablesJson = JSON.stringify(parsed.variables || [], null, 2)
    configForm.maxToolCalls = parsed.advanced?.maxToolCalls ?? 10
    configForm.maxExecutionTimeMs = parsed.advanced?.maxExecutionTimeMs ?? 120000
    embedForm.themeColor = parsed.embed?.themeColor || '#0052d9'
    embedForm.logoUrl = parsed.embed?.logoUrl || ''
    embedForm.welcomeMessage = parsed.embed?.welcomeMessage || ''
    embedForm.suggestedQuestionsText = (parsed.embed?.suggestedQuestions || []).join('\n')
    embedForm.customDomain = parsed.embed?.customDomain || ''
  } catch {
    configForm.variablesJson = '[]'
  }
}

function parseSuggestedQuestions(text: string) {
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
}

function buildConfigJson() {
  const variables = parseAgentVariablesJson(configForm.variablesJson || '[]')
  const payload: AgentConfigPayload = {
    variables,
    advanced: {
      maxToolCalls: configForm.maxToolCalls,
      maxExecutionTimeMs: configForm.maxExecutionTimeMs,
    },
  }
  const questions = parseSuggestedQuestions(embedForm.suggestedQuestionsText)
  if (
    embedForm.themeColor ||
    embedForm.logoUrl ||
    embedForm.welcomeMessage ||
    questions.length ||
    embedForm.customDomain
  ) {
    payload.embed = {
      themeColor: embedForm.themeColor || '#0052d9',
      logoUrl: embedForm.logoUrl.trim(),
      welcomeMessage: embedForm.welcomeMessage.trim(),
      suggestedQuestions: questions,
      customDomain: embedForm.customDomain.trim(),
    }
  }
  return JSON.stringify(payload)
}

function applyEmbedVo(data: AgentEmbedConfigVO) {
  embedForm.themeColor = data.themeColor || '#0052d9'
  embedForm.logoUrl = data.logoUrl || ''
  embedForm.welcomeMessage = data.welcomeMessage || ''
  embedForm.suggestedQuestionsText = (data.suggestedQuestions || []).join('\n')
  embedForm.customDomain = data.customDomain || ''
  embedForm.domainVerified = Boolean(data.domainVerified)
  embedForm.domainVerifyToken = data.domainVerifyToken || ''
  embedForm.domainVerifySkipped = Boolean(data.domainVerifySkipped)
  embedForm.gatewayCnameTarget = data.gatewayCnameTarget || ''
  embedForm.gatewayTlsMode = data.gatewayTlsMode || ''
  embedForm.gatewaySetupHint = data.gatewaySetupHint || ''
}

async function loadEmbedConfig() {
  try {
    const { data } = await getAgentEmbedConfig(agentId.value)
    if (data.data) {
      applyEmbedVo(data.data)
    }
  } catch {
    // keep parsed configJson values
  }
}

async function saveEmbedConfig() {
  savingEmbed.value = true
  try {
    const { data } = await updateAgentEmbedConfig(agentId.value, {
      themeColor: embedForm.themeColor,
      logoUrl: embedForm.logoUrl.trim(),
      welcomeMessage: embedForm.welcomeMessage.trim(),
      suggestedQuestions: parseSuggestedQuestions(embedForm.suggestedQuestionsText),
      customDomain: embedForm.customDomain.trim(),
    })
    if (data.data) {
      applyEmbedVo(data.data)
    }
    MessagePlugin.success('Embed 配置已保存，发布后将对外生效')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '保存失败'))
  } finally {
    savingEmbed.value = false
  }
}

async function verifyEmbedDomain() {
  verifyingDomain.value = true
  try {
    const { data } = await verifyAgentEmbedDomain(agentId.value)
    if (data.data) {
      applyEmbedVo(data.data)
    }
    MessagePlugin.success(
      embedForm.domainVerifySkipped ? '开发环境已跳过校验' : embedForm.domainVerified ? '域名已验证' : '验证已提交',
    )
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '域名验证失败'))
  } finally {
    verifyingDomain.value = false
  }
}

async function saveConfig() {
  savingConfig.value = true
  try {
    const configJson = buildConfigJson()
    const { data } = await updateAgentConfig(agentId.value, { configJson })
    if (data.data) applyAgent(data.data)
    MessagePlugin.success('配置已保存')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '保存失败'))
  } finally {
    savingConfig.value = false
  }
}

function stopChat() {
  stoppedByUser.value = true
  streamAbortController?.abort()
}

function beginStream() {
  stoppedByUser.value = false
  streamAbortController?.abort()
  streamAbortController = new AbortController()
  return streamAbortController
}

function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === 'AbortError'
}


async function loadAgent() {
  loading.value = true
  loadError.value = ''
  agent.value = null
  try {
    const { data: agentRes } = await getAgent(agentId.value)
    if (!agentRes.data) {
      loadError.value = '智能体不存在'
      return
    }
    applyAgent(agentRes.data)

    try {
      const [
        { data: modelRes },
        { data: platformRes },
        { data: capRes },
        { data: kbRes },
        { data: wfRes },
        { data: agentsRes },
        { data: bindKbRes },
        { data: bindWorkflowRes },
        { data: bindSubAgentRes },
        { data: publishRes },
      ] = await Promise.all([
        listModels(),
        listPlatformModels(),
        fetchPlatformCapabilities(),
        listKnowledgeBases(),
        listWorkflows(),
        listAgents(),
        listAgentKnowledge(agentId.value),
        listAgentWorkflows(agentId.value),
        listAgentSubAgents(agentId.value),
        getAgentPublishStatus(agentId.value),
      ])
      models.value = modelRes.data || []
      platformModels.value = platformRes.data || []
      byokEnabled.value = capRes.data?.byokEnabled === true
      knowledgeBases.value = kbRes.data || []
      workflows.value = wfRes.data || []
      allAgents.value = agentsRes.data || []
      knowledgeBindings.value = bindKbRes.data || []
      workflowBindings.value = bindWorkflowRes.data || []
      subAgentBindings.value = bindSubAgentRes.data || []
      publishInfo.value = publishRes.data || null
    } catch (error) {
      MessagePlugin.warning(extractApiError(error, '部分配置加载失败'))
    }
  } catch (error) {
    loadError.value = extractApiError(error, '加载智能体失败')
  } finally {
    loading.value = false
  }
}

async function bindKnowledge() {
  if (!selectedKnowledgeId.value) return
  bindingKnowledge.value = true
  try {
    await bindAgentKnowledge(agentId.value, { knowledgeBaseId: selectedKnowledgeId.value, topK: 5 })
    const { data } = await listAgentKnowledge(agentId.value)
    knowledgeBindings.value = data.data || []
    MessagePlugin.success('知识库已绑定')
  } finally {
    bindingKnowledge.value = false
  }
}

async function unbindKnowledge(knowledgeBaseId: number) {
  await unbindAgentKnowledge(agentId.value, knowledgeBaseId)
  knowledgeBindings.value = knowledgeBindings.value.filter((item) => item.knowledgeBaseId !== knowledgeBaseId)
}

async function bindWorkflow() {
  if (!selectedWorkflowId.value) return
  bindingWorkflow.value = true
  try {
    await bindAgentWorkflow(agentId.value, {
      workflowId: selectedWorkflowId.value,
      defaultWorkflow: bindWorkflowDefault.value,
      callable: bindWorkflowCallable.value,
    })
    const { data } = await listAgentWorkflows(agentId.value)
    workflowBindings.value = data.data || []
    selectedWorkflowId.value = undefined
    bindWorkflowDefault.value = false
    bindWorkflowCallable.value = true
    MessagePlugin.success('工作流已绑定')
  } finally {
    bindingWorkflow.value = false
  }
}

async function unbindWorkflow(workflowId: number) {
  await unbindAgentWorkflow(agentId.value, workflowId)
  workflowBindings.value = workflowBindings.value.filter((item) => item.workflowId !== workflowId)
}

async function bindSubAgent() {
  if (!selectedSubAgentId.value) return
  bindingSubAgent.value = true
  try {
    await bindAgentSubAgent(agentId.value, { subAgentId: selectedSubAgentId.value, enabled: true })
    const { data } = await listAgentSubAgents(agentId.value)
    subAgentBindings.value = data.data || []
    selectedSubAgentId.value = undefined
    MessagePlugin.success('子智能体已绑定')
  } finally {
    bindingSubAgent.value = false
  }
}

async function unbindSubAgent(subAgentId: number) {
  await unbindAgentSubAgent(agentId.value, subAgentId)
  subAgentBindings.value = subAgentBindings.value.filter((item) => item.subAgentId !== subAgentId)
}

async function loadLongTermMemories() {
  if (!memoryForm.longTermMemoryEnabled) {
    longTermMemories.value = []
    return
  }
  loadingLongTermMemories.value = true
  try {
    const { data } = await listAgentLongTermMemories(agentId.value)
    longTermMemories.value = data.data || []
  } finally {
    loadingLongTermMemories.value = false
  }
}

async function saveMemory() {
  savingMemory.value = true
  try {
    const { data } = await updateAgentMemory(agentId.value, {
      memoryEnabled: memoryForm.memoryEnabled,
      memoryWindowSize: memoryForm.memoryWindowSize,
      longTermMemoryEnabled: memoryForm.longTermMemoryEnabled,
    })
    if (data.data) applyAgent(data.data)
    if (memoryForm.longTermMemoryEnabled) {
      await loadLongTermMemories()
    } else {
      longTermMemories.value = []
    }
    MessagePlugin.success('记忆配置已保存')
  } finally {
    savingMemory.value = false
  }
}

async function removeLongTermMemory(memoryId: number) {
  await deleteAgentLongTermMemory(agentId.value, memoryId)
  longTermMemories.value = longTermMemories.value.filter((item) => item.id !== memoryId)
  MessagePlugin.success('记忆已删除')
}

async function copyPublishSnippet() {
  const text =
    publishTab.value === 'embed'
      ? publishEmbedCode.value
      : publishTab.value === 'sdk'
        ? publishSdkExample.value
        : publishApiExample.value
  try {
    await navigator.clipboard.writeText(text)
    MessagePlugin.success('已复制到剪贴板')
  } catch {
    MessagePlugin.warning('复制失败，请手动复制')
  }
}

async function doPublish() {
  publishing.value = true
  try {
    const { data } = await publishAgent(agentId.value)
    publishInfo.value = data.data || null
    if (agent.value && data.data) {
      agent.value.status = data.data.status
    }
    MessagePlugin.success('发布成功')
  } finally {
    publishing.value = false
  }
}

async function doUnpublish() {
  publishing.value = true
  try {
    const { data } = await unpublishAgent(agentId.value)
    publishInfo.value = data.data || null
    if (agent.value && data.data) {
      agent.value.status = data.data.status
    }
    MessagePlugin.success('已取消发布')
  } finally {
    publishing.value = false
  }
}

const saveOverview: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true || !agent.value) return
  savingOverview.value = true
  try {
    const { data } = await updateAgent(agentId.value, {
      name: overviewForm.name.trim(),
      description: overviewForm.description.trim() || undefined,
      avatarUrl: overviewForm.avatarUrl.trim() || undefined,
      modelId: agent.value.modelId,
    })
    if (data.data) applyAgent(data.data)
    MessagePlugin.success('基础信息已保存')
  } finally {
    savingOverview.value = false
  }
}

async function savePrompt() {
  if (!agent.value) return
  savingPrompt.value = true
  try {
    const { data } = await updateAgentPrompt(agentId.value, {
      systemPrompt: promptForm.systemPrompt.trim() || undefined,
    })
    if (data.data) applyAgent(data.data)
    MessagePlugin.success('Prompt 已保存')
  } finally {
    savingPrompt.value = false
  }
}

const saveModel: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  if (modelForm.modelSource === 'PLATFORM' && !modelForm.platformModelId) return
  if (modelForm.modelSource === 'BYOK' && !modelForm.modelId) return
  savingModel.value = true
  try {
    const { data } = await updateAgentModel(agentId.value, {
      modelSource: modelForm.modelSource,
      platformModelId: modelForm.modelSource === 'PLATFORM' ? modelForm.platformModelId : undefined,
      modelId: modelForm.modelSource === 'BYOK' ? modelForm.modelId : undefined,
      routingPreference: modelForm.modelSource === 'AUTO' ? modelForm.routingPreference : undefined,
      temperature: modelForm.temperature,
      topP: modelForm.topP,
      maxTokens: modelForm.maxTokens,
      streamEnabled: modelForm.streamEnabled,
    })
    if (data.data) applyAgent(data.data)
    MessagePlugin.success('模型配置已保存')
  } finally {
    savingModel.value = false
  }
}

function clearChat() {
  messages.value = []
}

async function scrollChatToBottom() {
  await nextTick()
  chatListRef.value?.scrollToBottom()
}

function buildServerHistory() {
  return messages.value
    .filter((item) => item.content?.trim())
    .map((item) => ({
      role: item.role === 'user' ? 'USER' as const : 'ASSISTANT' as const,
      content: item.content,
    }))
}

async function sendChat(raw?: string) {
  const text = (raw ?? chatInput.value).trim()
  if (!text || chatting.value) return
  const history = buildServerHistory()
  messages.value.push({ role: 'user', content: text })
  chatInput.value = ''
  await scrollChatToBottom()
  chatting.value = true
  const useStream = modelForm.streamEnabled !== false
  const controller = beginStream()
  let assistantIndex = -1
  try {
    if (useStream) {
      assistantIndex = messages.value.length
      messages.value.push({ role: 'assistant', content: '' })
      await scrollChatToBottom()
      await chatAgentStream(agentId.value, text, (delta) => {
        messages.value[assistantIndex].content += delta
        scrollChatToBottom()
      }, history, {
        signal: controller.signal,
        onCitations: (citations) => {
          messages.value[assistantIndex].citations = citations
        },
        onToolConfirm: async (payload) => {
          const confirmed = await promptToolConfirmation(agentId.value, payload)
          if (confirmed && assistantIndex >= 0) {
            const note = `\n\n[已确认执行工具 ${payload.toolName || payload.toolKey}]`
            messages.value[assistantIndex].content += note
          }
        },
      })
      if (stoppedByUser.value && !messages.value[assistantIndex].content) {
        messages.value[assistantIndex].content = '（已停止生成）'
      } else if (!messages.value[assistantIndex].content) {
        messages.value[assistantIndex].content = '（无回复）'
      }
    } else {
      const { data } = await chatAgent(agentId.value, text, history)
      messages.value.push({
        role: 'assistant',
        content: data.data?.content || '（无回复）',
        citations: data.data?.citations,
      })
      await scrollChatToBottom()
    }
  } catch (error) {
    if (isAbortError(error)) {
      if (useStream && assistantIndex >= 0 && !messages.value[assistantIndex]?.content) {
        messages.value[assistantIndex].content = '（已停止生成）'
      }
    } else {
      MessagePlugin.error(extractApiError(error, '对话失败'))
      if (useStream && assistantIndex >= 0 && !messages.value[assistantIndex]?.content) {
        messages.value.splice(assistantIndex, 1)
      }
    }
  } finally {
    chatting.value = false
    streamAbortController = null
  }
}

watch(
  () => route.params.id,
  () => {
    if (route.name === 'agent-builder') {
      loadAgent()
    }
  },
)

watch(activeTab, (tab) => {
  if (tab === 'memory' && memoryForm.longTermMemoryEnabled) {
    loadLongTermMemories()
  }
})

onMounted(loadAgent)
</script>

<style scoped>
.model-auto-hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--td-text-color-secondary);
}

.builder {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 16px 20px;
  box-sizing: border-box;
  background: #fff;
}

.builder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--box-border);
  flex-shrink: 0;
}

.builder-header__left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.builder-header__meta {
  min-width: 0;
}

.builder-header__title {
  margin: 0;
  font: var(--td-font-title-medium);
  color: var(--box-ink);
}

.builder-header__desc {
  margin: 2px 0 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.builder-error {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 320px;
  padding: 24px;
}

.builder-error__hint {
  margin: 0 0 16px;
  color: var(--td-text-color-secondary);
  font-size: 14px;
  line-height: 1.6;
  max-width: 360px;
}

.builder-body {
  flex: 1;
  min-height: 0;
}

.builder-layout {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr) 360px;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.builder-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-md);
  background: var(--box-surface);
}

.builder-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--box-muted);
  font: var(--td-font-body-medium);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.builder-nav__item:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}

.builder-nav__item--active {
  background: var(--box-active);
  color: var(--box-ink);
  font-weight: 500;
}

.builder-config,
.builder-preview {
  min-height: 0;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-md);
  background: var(--box-surface);
}

.builder-config {
  overflow-y: auto;
  padding: 24px;
}

.config-panel__title {
  margin: 0 0 4px;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.config-panel__desc {
  margin: 0 0 20px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.config-subtitle {
  margin: 24px 0 12px;
  font: var(--td-font-title-small);
}

.config-subtitle:first-of-type {
  margin-top: 0;
}

.publish-api {
  margin-top: 24px;
}

.embed-domain-hint {
  margin: 0 0 12px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.publish-tabs {
  margin-top: 12px;
}

.api-snippet {
  margin: 12px 0 16px;
  padding: 12px;
  border-radius: 8px;
  background: #f7f8fa;
  border: 1px solid var(--box-border);
}

.api-snippet pre {
  margin: 8px 0 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.builder-preview {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--box-border);
  flex-shrink: 0;
}

.preview-header h3 {
  margin: 0;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.preview-messages {
  padding: 16px;
}

.chat-citation-popup {
  max-width: 360px;
}

.chat-citation-popup__title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.chat-citation-popup__body {
  font-size: 12px;
  line-height: 1.5;
  color: var(--td-text-color-secondary);
  white-space: pre-wrap;
  max-height: 160px;
  overflow: auto;
}

.preview-input {
  padding: 12px 16px 16px;
  border-top: 1px solid var(--box-border);
  flex-shrink: 0;
}

@media (max-width: 1100px) {
  .builder-layout {
    grid-template-columns: 180px minmax(0, 1fr);
    grid-template-rows: minmax(0, 1fr) 320px;
  }

  .builder-preview {
    grid-column: 1 / -1;
  }
}
</style>
