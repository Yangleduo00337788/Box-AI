import { sleep } from 'k6';
import { cfg, scenarioOptions, llmThresholds } from '../lib/config.js';
import { firstId, getJson, login, postJson } from '../lib/client.js';

/**
 * Hits LLM. Chat is rate-limited to 60 req / user / minute.
 * Prefer PROFILE=smoke and a dedicated staging agent.
 */
export const options = scenarioOptions(llmThresholds);

export function setup() {
  const session = login();
  let agentId = cfg.agentId;
  let conversationId = cfg.conversationId;

  if (!agentId) {
    const agents = getJson(session, '/api/v1/agents', 'agents.list');
    agentId = firstId(agents.data);
  }
  if (!conversationId && agentId) {
    const created = postJson(
      session,
      '/api/v1/conversations',
      { agentId: Number(agentId), title: 'k6 loadtest' },
      'conversations.create',
    );
    conversationId = created.data && created.data.id != null ? String(created.data.id) : '';
  }
  if (!agentId || !conversationId) {
    throw new Error('AGENT_ID (or an existing agent) and a conversation are required');
  }
  return Object.assign({}, session, { agentId, conversationId });
}

export default function (session) {
  postJson(
    session,
    `/api/v1/conversations/${session.conversationId}/messages`,
    {
      message: cfg.chatMessage,
      stream: false,
    },
    'conversations.send',
    cfg.chatTimeout,
  );
  sleep(1);
}
