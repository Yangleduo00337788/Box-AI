import { check, sleep } from 'k6';
import { cfg, scenarioOptions, llmThresholds } from '../lib/config.js';
import { getJson, login, postJson, postSse, putJson, resolvePreferredPlatformModel } from '../lib/client.js';

/**
 * Console SSE with a bound MCP server. Expects tool.start / tool.end.
 * Uses public Streamable HTTP MCP (aisense service_health); VU below plain chat.
 */
export const options = scenarioOptions(llmThresholds);

const SERVER_NAME = 'k6-aisense';
const SERVER_KEY = 'k6_aisense';
const AGENT_NAME = 'k6-mcp-agent';

let conversationId;

function ensureMcpServer(session) {
  const listed = getJson(session, '/api/v1/mcp-servers', 'mcp.list');
  let existing = (listed.data || []).find((item) => item && item.serverKey === SERVER_KEY);
  let serverId = existing && existing.id != null ? Number(existing.id) : 0;
  if (!serverId) {
    const created = postJson(
      session,
      '/api/v1/mcp-servers',
      {
        name: SERVER_NAME,
        serverKey: SERVER_KEY,
        description: 'k6 MCP loadtest via public Streamable HTTP',
        endpointUrl: cfg.mcpEndpoint,
        transportType: 'HTTP',
        authType: 'NONE',
      },
      'mcp.create',
    );
    if (!created.data || created.data.id == null) {
      throw new Error('failed to create MCP server');
    }
    serverId = Number(created.data.id);
  }
  const synced = postJson(session, `/api/v1/mcp-servers/${serverId}/sync`, null, 'mcp.sync', cfg.chatTimeout);
  if (!synced.passed) {
    throw new Error('MCP sync failed');
  }
  return serverId;
}

function ensureMcpAgent(session, mcpServerId, platformModelId) {
  if (cfg.mcpAgentId) {
    return cfg.mcpAgentId;
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
        description: 'MCP loadtest',
        platformModelId,
      },
      'agents.create',
    );
    agentId = created.data && created.data.id != null ? String(created.data.id) : '';
  }
  if (!agentId) {
    throw new Error('failed to create MCP agent');
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
        '你是压测助手。用户提问时必须先调用 MCP 工具 service_health，再根据工具结果用一句话回答。不要跳过工具，不要调用其他工具。',
    },
    'agents.prompt',
  );
  const bindings = getJson(session, `/api/v1/agents/${agentId}/mcp`, 'agents.mcp');
  const bound = (bindings.data || []).some((item) => item && Number(item.mcpServerId) === mcpServerId);
  if (!bound) {
    postJson(
      session,
      `/api/v1/agents/${agentId}/mcp`,
      { mcpServerId, enabled: true },
      'agents.bindMcp',
    );
  }
  postJson(session, `/api/v1/agents/${agentId}/publish`, null, 'agents.publish');
  return agentId;
}

export function setup() {
  const session = login();
  const platformModelId = Number(resolvePreferredPlatformModel(session).id);
  const mcpServerId = ensureMcpServer(session);
  const agentId = ensureMcpAgent(session, mcpServerId, platformModelId);
  return Object.assign({}, session, { agentId: String(agentId) });
}

export default function (session) {
  if (!conversationId) {
    const created = postJson(
      session,
      '/api/v1/conversations',
      { agentId: Number(session.agentId), title: 'k6 mcp loadtest' },
      'conversations.create',
    );
    conversationId =
      created.data && created.data.id != null ? String(created.data.id) : '';
    if (!conversationId) {
      throw new Error('failed to create conversation for MCP SSE VU');
    }
  }
  const sse = postSse(
    session,
    `/api/v1/conversations/${conversationId}/messages`,
    {
      message: '请调用 service_health 检查服务是否可用，再用一句话说明结果',
      stream: true,
    },
    'conversations.mcp.sse',
    cfg.chatTimeout,
  );
  const text = sse.body || '';
  check(sse.res, {
    'conversations.mcp.tool.start': () => text.indexOf('"type":"tool.start"') !== -1,
    'conversations.mcp.tool.end': () => text.indexOf('"type":"tool.end"') !== -1,
  });
  sleep(1);
}
