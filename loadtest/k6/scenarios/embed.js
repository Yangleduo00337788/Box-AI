import { sleep } from 'k6';
import { cfg, scenarioOptions, llmThresholds } from '../lib/config.js';
import { apiKeySseHeaders, getPublic, postSseWithHeaders } from '../lib/client.js';

/**
 * Public Embed page: anonymous GET embed-config, then SSE chat with API Key.
 * Matches /embed/agents/:id (not custom-domain resolve).
 */
export const options = scenarioOptions(llmThresholds);

export function setup() {
  if (!cfg.apiKey || !cfg.publishedAgentId) {
    throw new Error('API_KEY and PUBLISHED_AGENT_ID are required');
  }
  const config = getPublic(
    `/api/v1/published/agents/${cfg.publishedAgentId}/embed-config`,
    'embed.config.setup',
  );
  if (!config.passed) {
    throw new Error('embed-config probe failed');
  }
  return { publishedAgentId: cfg.publishedAgentId };
}

export default function (data) {
  getPublic(
    `/api/v1/published/agents/${data.publishedAgentId}/embed-config`,
    'embed.config',
  );
  postSseWithHeaders(
    `/api/v1/published/agents/${data.publishedAgentId}/chat`,
    { message: cfg.chatMessage, stream: true },
    apiKeySseHeaders(),
    'embed.chat.sse',
    cfg.chatTimeout,
  );
  sleep(1);
}
