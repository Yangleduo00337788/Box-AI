import { sleep } from 'k6';
import { cfg, scenarioOptions, llmThresholds } from '../lib/config.js';
import { firstId, getJson, login, postJson } from '../lib/client.js';

export const options = scenarioOptions(llmThresholds);

export function setup() {
  const session = login();
  let knowledgeBaseId = cfg.knowledgeBaseId;
  if (!knowledgeBaseId) {
    const list = getJson(session, '/api/v1/knowledge-bases', 'knowledge.list');
    knowledgeBaseId = firstId(list.data);
  }
  if (!knowledgeBaseId) {
    throw new Error('KNOWLEDGE_BASE_ID (or an existing knowledge base) is required');
  }
  return Object.assign({}, session, { knowledgeBaseId });
}

export default function (session) {
  postJson(
    session,
    `/api/v1/knowledge-bases/${session.knowledgeBaseId}/search`,
    { query: cfg.ragQuery, topK: 5 },
    'knowledge.search',
    cfg.chatTimeout,
  );
  sleep(0.5);
}
