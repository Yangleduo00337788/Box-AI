import { sleep } from 'k6';
import { cfg, scenarioOptions, llmThresholds } from '../lib/config.js';
import { firstId, getJson, login, postJson, postSse } from '../lib/client.js';

/**
 * Console chat path: SSE stream=true. Each VU creates its own conversation
 * so 200 concurrent users are not serialized on one row.
 */
export const options = scenarioOptions(llmThresholds);

let conversationId;

export function setup() {
  const session = login();
  let agentId = cfg.agentId;
  if (!agentId) {
    const agents = getJson(session, '/api/v1/agents', 'agents.list');
    agentId = firstId(agents.data);
  }
  if (!agentId) {
    throw new Error('AGENT_ID (or an existing agent) is required');
  }
  return Object.assign({}, session, { agentId: String(agentId) });
}

export default function (session) {
  if (!conversationId) {
    const created = postJson(
      session,
      '/api/v1/conversations',
      { agentId: Number(session.agentId), title: 'k6 sse loadtest' },
      'conversations.create',
    );
    conversationId =
      created.data && created.data.id != null ? String(created.data.id) : '';
    if (!conversationId) {
      throw new Error('failed to create conversation for SSE VU');
    }
  }
  postSse(
    session,
    `/api/v1/conversations/${conversationId}/messages`,
    {
      message: cfg.chatMessage,
      stream: true,
    },
    'conversations.sse',
    cfg.chatTimeout,
  );
  sleep(1);
}
