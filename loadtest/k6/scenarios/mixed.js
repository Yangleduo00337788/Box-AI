import { sleep } from 'k6';
import { cfg, scenarioOptions, readThresholds } from '../lib/config.js';
import { firstId, getJson, login } from '../lib/client.js';

/**
 * Console-like mix: list/detail reads that dominate after login.
 * Does not call LLM. Use chat.js / rag.js / published.js separately.
 */
export const options = scenarioOptions(readThresholds);

export function setup() {
  const session = login();
  const agents = getJson(session, '/api/v1/agents', 'agents.list');
  const conversations = getJson(session, '/api/v1/conversations', 'conversations.list');
  return Object.assign({}, session, {
    agentId: cfg.agentId || firstId(agents.data),
    conversationId: cfg.conversationId || firstId(conversations.data),
  });
}

export default function (session) {
  const roll = Math.random();

  if (roll < 0.35) {
    getJson(session, '/api/v1/conversations', 'conversations.list');
    getJson(session, '/api/v1/agents', 'agents.list');
  } else if (roll < 0.55) {
    getJson(session, '/api/v1/auth/me', 'auth.me');
    getJson(session, '/api/v1/notifications/unread-count', 'notifications.unread');
    getJson(session, '/api/v1/sidebar/pins', 'sidebar.pins');
  } else if (roll < 0.7 && session.conversationId) {
    getJson(session, `/api/v1/conversations/${session.conversationId}`, 'conversations.detail');
    getJson(session, `/api/v1/conversations/${session.conversationId}/messages`, 'conversations.messages');
  } else if (roll < 0.82) {
    getJson(session, '/api/v1/knowledge-bases', 'knowledge.list');
    getJson(session, '/api/v1/tools', 'tools.list');
    getJson(session, '/api/v1/workflows', 'workflows.list');
  } else if (roll < 0.92) {
    getJson(session, '/api/v1/search?q=box', 'search');
    getJson(session, '/api/v1/platform/models', 'platform.models');
  } else {
    getJson(session, '/api/v1/workspaces', 'workspaces.list');
    getJson(session, '/api/v1/tenant/quota', 'tenant.quota');
    if (session.agentId) {
      getJson(session, `/api/v1/agents/${session.agentId}`, 'agents.detail');
    }
  }

  sleep(0.3);
}
