import { check, sleep } from 'k6';
import { cfg, scenarioOptions, llmThresholds } from '../lib/config.js';
import { authHeaders, expectOk, firstId, getJson, login, postJson, putJson, request, resolvePreferredPlatformModel } from '../lib/client.js';

/**
 * Full published graph: Start → Knowledge → LLM → Output.
 * Hits embedding + chat; VU should stay at chat admission scale, not mixed 1000.
 */
export const options = scenarioOptions(llmThresholds);

function definitionJson(knowledgeBaseId, platformModelId) {
  return JSON.stringify({
    nodes: [
      { id: 'start', type: 'Start' },
      {
        id: 'kb',
        type: 'Knowledge',
        config: {
          knowledgeBaseId: Number(knowledgeBaseId),
          query: '{{ query }}',
          topK: 5,
          outputVariable: 'knowledgeResult',
        },
      },
      {
        id: 'llm',
        type: 'LLM',
        config: {
          platformModelId: Number(platformModelId),
          userPrompt: '用一句话回答：{{ query }}\n资料：{{ knowledgeResult.context }}',
          outputVariable: 'llmResult',
          maxTokens: 64,
          temperature: 0.7,
        },
      },
      {
        id: 'out',
        type: 'Output',
        config: { outputKey: 'answer', variable: 'llmResult' },
      },
    ],
    edges: [
      { source: 'start', target: 'kb' },
      { source: 'kb', target: 'llm' },
      { source: 'llm', target: 'out' },
    ],
    variables: [{ name: 'query' }],
  });
}

function ensurePublishedWorkflow(session) {
  let workflowId = cfg.workflowId;
  if (!workflowId) {
    const listed = getJson(session, '/api/v1/workflows', 'workflows.list');
    const existing = (listed.data || []).find((item) => item && item.name === 'k6-full-workflow');
    workflowId = existing && existing.id != null ? String(existing.id) : '';
  }
  if (!workflowId) {
    const created = postJson(
      session,
      '/api/v1/workflows',
      { name: 'k6-full-workflow', description: 'Start-Knowledge-LLM-Output loadtest' },
      'workflows.create',
    );
    workflowId = created.data && created.data.id != null ? String(created.data.id) : '';
  }
  if (!workflowId) {
    throw new Error('failed to create workflow');
  }
  let knowledgeBaseId = cfg.knowledgeBaseId;
  if (!knowledgeBaseId) {
    const kbs = getJson(session, '/api/v1/knowledge-bases', 'knowledge.list');
    knowledgeBaseId = firstId(kbs.data);
  }
  const platformModelId = String(resolvePreferredPlatformModel(session).id);
  if (!knowledgeBaseId || !platformModelId) {
    throw new Error('KNOWLEDGE_BASE_ID and a platform chat model are required');
  }
  putJson(
    session,
    `/api/v1/workflows/${workflowId}/definition`,
    { definitionJson: definitionJson(knowledgeBaseId, platformModelId) },
    'workflows.definition',
  );
  const validated = postJson(session, `/api/v1/workflows/${workflowId}/validate`, null, 'workflows.validate');
  if (!validated.data || validated.data.valid !== true) {
    throw new Error(`workflow invalid: ${JSON.stringify(validated.data)}`);
  }
  postJson(session, `/api/v1/workflows/${workflowId}/publish`, null, 'workflows.publish');
  return workflowId;
}

export function setup() {
  const session = login();
  const workflowId = ensurePublishedWorkflow(session);
  return Object.assign({}, session, { workflowId });
}

export default function (session) {
  const res = request(
    'POST',
    `/api/v1/runtime/workflows/${session.workflowId}/execute`,
    { inputs: { query: cfg.ragQuery } },
    authHeaders(session),
    cfg.chatTimeout,
    'workflow.execute',
  );
  const result = expectOk(res, 'workflow.execute');
  const status = result.data ? result.data.status : '';
  check(res, {
    'workflow.execute SUCCEEDED': () => status === 'SUCCEEDED',
  });
  sleep(1);
}
