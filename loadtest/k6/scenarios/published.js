import { sleep } from 'k6';
import { cfg, scenarioOptions, llmThresholds } from '../lib/config.js';
import { apiKeyHeaders, expectOk, request } from '../lib/client.js';

/**
 * Open API path: Authorization Bearer ax_live_*.
 * API Key filter allows 120 req / key / minute; published chat 60 / IP / agent / minute.
 */
export const options = scenarioOptions(llmThresholds);

export function setup() {
  if (!cfg.apiKey || !cfg.publishedAgentId) {
    throw new Error('API_KEY and PUBLISHED_AGENT_ID are required');
  }
  return {
    apiKey: cfg.apiKey,
    publishedAgentId: cfg.publishedAgentId,
  };
}

export default function (data) {
  const res = request(
    'POST',
    `/api/v1/published/agents/${data.publishedAgentId}/chat`,
    { message: cfg.chatMessage, stream: false },
    apiKeyHeaders(),
    cfg.chatTimeout,
    'published.chat',
  );
  expectOk(res, 'published.chat');
  sleep(1);
}
