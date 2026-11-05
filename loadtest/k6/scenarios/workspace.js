import { sleep } from 'k6';
import { scenarioOptions, readThresholds } from '../lib/config.js';
import { firstId, getJson, login } from '../lib/client.js';

export const options = scenarioOptions(readThresholds);

export function setup() {
  return login();
}

export default function (session) {
  const me = getJson(session, '/api/v1/auth/me', 'auth.me');
  getJson(session, '/api/v1/workspaces', 'workspaces.list');
  getJson(session, '/api/v1/workspaces/current-permissions', 'workspaces.permissions');
  getJson(session, '/api/v1/sidebar/pins', 'sidebar.pins');
  getJson(session, '/api/v1/sidebar/selection', 'sidebar.selection');
  getJson(session, '/api/v1/notifications/unread-count', 'notifications.unread');
  getJson(session, '/api/v1/notifications?limit=20', 'notifications.list');
  getJson(session, '/api/v1/tenant/quota', 'tenant.quota');

  const agents = getJson(session, '/api/v1/agents', 'agents.list');
  const conversations = getJson(session, '/api/v1/conversations', 'conversations.list');
  getJson(session, '/api/v1/projects', 'projects.list');
  getJson(session, '/api/v1/knowledge-bases', 'knowledge.list');
  getJson(session, '/api/v1/tools', 'tools.list');
  getJson(session, '/api/v1/workflows', 'workflows.list');
  getJson(session, '/api/v1/platform/models', 'platform.models');

  const agentId = firstId(agents.data);
  if (agentId) {
    getJson(session, `/api/v1/agents/${agentId}`, 'agents.detail');
  }

  const conversationId = firstId(conversations.data);
  if (conversationId) {
    getJson(session, `/api/v1/conversations/${conversationId}`, 'conversations.detail');
    getJson(session, `/api/v1/conversations/${conversationId}/messages`, 'conversations.messages');
  }

  if (me.passed) {
    getJson(session, '/api/v1/search?q=box', 'search');
  }

  sleep(0.5);
}
