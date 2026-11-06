import { check, sleep } from 'k6';
import { cfg, scenarioOptions, llmThresholds } from '../lib/config.js';
import { getJson, login, postJson, postSse, putJson, resolvePreferredPlatformModel } from '../lib/client.js';

/**
 * Console SSE with a bound HTTP tool. Expects tool.start / tool.end.
 * VU should stay below plain chat (tools add a model round-trip).
 */
export const options = scenarioOptions(llmThresholds);

const TOOL_NAME = 'k6-ping';
const TOOL_KEY = 'k6_ping';
const AGENT_NAME = 'k6-tool-agent';

let conversationId;


function ensureHttpTool(session) {
  const listed = getJson(session, '/api/v1/tools', 'tools.list');
  const existing = (listed.data || []).find((item) => item && item.toolKey === TOOL_KEY);
  if (existing && existing.id != null) {
    return Number(existing.id);
  }
  const created = postJson(
    session,
    '/api/v1/tools',
    {
      name: TOOL_NAME,
      toolKey: TOOL_KEY,
      description: '查询公开健康检查，无参数。必须在回答前调用。',
      type: 'HTTP',
      inputSchemaJson: '{"type":"object","properties":{}}',
      httpConfig: {
        method: 'GET',
        url: 'https://jsonplaceholder.typicode.com/todos/1',
        timeoutMs: 8000,
        allowRedirect: true,
      },
    },
    'tools.create',
  );
  if (!created.data || created.data.id == null) {
    throw new Error('failed to create HTTP tool');
  }
  return Number(created.data.id);
}

function ensureToolAgent(session, toolId, platformModelId) {
  if (cfg.toolAgentId) {
    return cfg.toolAgentId;
  }
  const listed = getJson(session, '/api/v1/agents', 'agents.list');
  const existing = (listed.data || []).find((item) => item && item.name === AGENT_NAME);
  let agentId = existing && existing.id != null ? String(existing.id) : '';
  if (!agentId) {
    const created = postJson(
      session,
      '/api/v1/agents',
      {
        name: AGENT_NAME,
        description: 'HTTP tool loadtest',
        platformModelId,
      },
      'agents.create',
    );
    agentId = created.data && created.data.id != null ? String(created.data.id) : '';
  }
  if (!agentId) {
    throw new Error('failed to create tool agent');
  }
  putJson(
    session,
    `/api/v1/agents/${agentId}/model`,
    {
      modelSource: 'PLATFORM',
      platformModelId,
      temperature: 0.2,
      maxTokens: 128,
      streamEnabled: true,
    },
    'agents.model',
  );
  putJson(
    session,
    `/api/v1/agents/${agentId}/prompt`,
    {
      systemPrompt:
        '你是压测助手。用户提问时必须先调用工具 k6_ping，再根据工具结果用一句话回答。不要跳过工具。',
    },
    'agents.prompt',
  );
  const bindings = getJson(session, `/api/v1/agents/${agentId}/tools`, 'agents.tools');
  const bound = (bindings.data || []).some((item) => item && Number(item.toolId) === toolId);
  if (!bound) {
    postJson(
      session,
      `/api/v1/agents/${agentId}/tools`,
      { toolId, enabled: true, requireConfirmation: false },
      'agents.bindTool',
    );
  }
  postJson(session, `/api/v1/agents/${agentId}/publish`, null, 'agents.publish');
  return agentId;
}

export function setup() {
  const session = login();
  const platformModelId = Number(resolvePreferredPlatformModel(session).id);
  const toolId = ensureHttpTool(session);
  const agentId = ensureToolAgent(session, toolId, platformModelId);
  return Object.assign({}, session, { agentId: String(agentId) });
}

export default function (session) {
  if (!conversationId) {
    const created = postJson(
      session,
      '/api/v1/conversations',
      { agentId: Number(session.agentId), title: 'k6 tool loadtest' },
      'conversations.create',
    );
    conversationId =
      created.data && created.data.id != null ? String(created.data.id) : '';
    if (!conversationId) {
      throw new Error('failed to create conversation for tool SSE VU');
    }
  }
  const sse = postSse(
    session,
    `/api/v1/conversations/${conversationId}/messages`,
    {
      message: cfg.toolChatMessage,
      stream: true,
    },
    'conversations.tool.sse',
    cfg.chatTimeout,
  );
  const text = sse.body || '';
  check(sse.res, {
    'conversations.tool.start': () => text.indexOf('"type":"tool.start"') !== -1,
    'conversations.tool.end': () => text.indexOf('"type":"tool.end"') !== -1,
  });
  sleep(1);
}
