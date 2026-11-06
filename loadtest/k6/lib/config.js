const profiles = {
  smoke: {
    vus: 2,
    duration: '30s',
  },
  load: {
    stages: [
      { duration: '1m', target: 20 },
      { duration: '3m', target: 50 },
      { duration: '1m', target: 0 },
    ],
  },
  stress: {
    stages: [
      { duration: '1m', target: 50 },
      { duration: '2m', target: 100 },
      { duration: '2m', target: 200 },
      { duration: '1m', target: 0 },
    ],
  },
  heavy: {
    stages: [
      { duration: '20s', target: 1000 },
      { duration: '2m', target: 1000 },
      { duration: '20s', target: 0 },
    ],
  },
};

function env(name, fallback) {
  const value = __ENV[name];
  if (value === undefined || value === '') {
    return fallback;
  }
  return value;
}

function envInt(name, fallback) {
  const raw = env(name, String(fallback));
  const parsed = parseInt(raw, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

export const cfg = {
  baseUrl: env('BASE_URL', 'http://localhost:8080').replace(/\/$/, ''),
  account: env('ACCOUNT', ''),
  password: env('PASSWORD', ''),
  accountType: env('ACCOUNT_TYPE', 'PERSONAL'),
  orgId: env('ORG_ID', ''),
  workspaceId: env('WORKSPACE_ID', ''),
  agentId: env('AGENT_ID', ''),
  conversationId: env('CONVERSATION_ID', ''),
  knowledgeBaseId: env('KNOWLEDGE_BASE_ID', ''),
  apiKey: env('API_KEY', ''),
  publishedAgentId: env('PUBLISHED_AGENT_ID', ''),
  workflowId: env('WORKFLOW_ID', ''),
  toolAgentId: env('TOOL_AGENT_ID', ''),
  mcpAgentId: env('MCP_AGENT_ID', ''),
  mcpEndpoint: env('MCP_ENDPOINT', 'https://aisenseapi.com/mcp'),
  platformModelCode: env('PLATFORM_MODEL_CODE', 'qwen3.8-max-0902'),
  chatMessage: env('CHAT_MESSAGE', '请用一句话介绍你自己'),
  toolChatMessage: env('TOOL_CHAT_MESSAGE', '请调用健康检查工具，再用一句话说明服务是否可用'),
  ragQuery: env('RAG_QUERY', '什么是 Box'),
  timeout: env('HTTP_TIMEOUT', '60s'),
  chatTimeout: env('CHAT_TIMEOUT', '120s'),
  profile: env('PROFILE', 'smoke'),
};

export function scenarioOptions(thresholds) {
  const profile = profiles[cfg.profile] || profiles.smoke;
  const extraVus = envInt('VUS', 0);
  const extraDuration = env('DURATION', '');
  const options = {
    thresholds,
    ...profile,
  };
  if (extraVus > 0) {
    delete options.stages;
    options.vus = extraVus;
    options.duration = extraDuration || '1m';
  } else if (extraDuration) {
    delete options.stages;
    options.duration = extraDuration;
    options.vus = options.vus || 2;
  }
  return options;
}

export const readThresholds = {
  http_req_failed: ['rate<0.01'],
  http_req_duration: ['p(95)<800'],
  checks: ['rate>0.99'],
};

export const loginThresholds = {
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<1500'],
  checks: ['rate>0.95'],
};

export const llmThresholds = {
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<30000'],
  checks: ['rate>0.90'],
};
