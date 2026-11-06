import http from 'k6/http';
import { check } from 'k6';
import { cfg } from './config.js';

export function jsonHeaders(extra) {
  return Object.assign(
    {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    extra || {},
  );
}

export function authHeaders(session) {
  const headers = jsonHeaders({
    Authorization: `Bearer ${session.token}`,
  });
  if (session.workspaceId) {
    headers['X-Workspace-Id'] = String(session.workspaceId);
  }
  return headers;
}

export function apiKeyHeaders() {
  return jsonHeaders({
    Authorization: `Bearer ${cfg.apiKey}`,
  });
}

export function apiKeySseHeaders() {
  const headers = apiKeyHeaders();
  headers.Accept = 'text/event-stream';
  return headers;
}

export function getPublic(path, name) {
  const res = request('GET', path, null, jsonHeaders(), cfg.timeout, name);
  return expectOk(res, name);
}

export function request(method, path, body, headers, timeout, name) {
  const params = {
    headers,
    timeout: timeout || cfg.timeout,
    tags: { name: name || `${method} ${path}` },
  };
  const url = `${cfg.baseUrl}${path}`;
  if (body === null || body === undefined) {
    return http.request(method, url, null, params);
  }
  return http.request(method, url, JSON.stringify(body), params);
}

export function expectOk(res, name) {
  let payload = null;
  try {
    payload = res.json();
  } catch (e) {
    payload = null;
  }
  const passed = check(res, {
    [`${name} HTTP 200`]: (r) => r.status === 200,
    [`${name} code=0`]: () => payload != null && payload.code === 0,
  });
  return { passed, payload, data: payload ? payload.data : null };
}

export function login() {
  requireAccount();
  const body = {
    account: cfg.account,
    password: cfg.password,
    accountType: cfg.accountType,
  };
  if (cfg.orgId) {
    body.orgId = cfg.orgId;
  }
  const res = request('POST', '/api/v1/auth/login', body, jsonHeaders(), cfg.timeout, 'auth.login');
  const result = expectOk(res, 'login');
  if (!result.passed || !result.data || !result.data.token) {
    throw new Error(`login failed: status=${res.status} body=${res.body}`);
  }
  return {
    token: result.data.token,
    workspaceId: cfg.workspaceId || result.data.currentWorkspaceId,
    user: result.data.user,
  };
}

function requireAccount() {
  if (!cfg.account || !cfg.password) {
    throw new Error('ACCOUNT and PASSWORD are required for this scenario');
  }
}

export function firstId(list) {
  if (!Array.isArray(list) || list.length === 0) {
    return '';
  }
  const item = list[0];
  return item && item.id != null ? String(item.id) : '';
}

export function getJson(session, path, name) {
  const res = request('GET', path, null, authHeaders(session), cfg.timeout, name);
  return expectOk(res, name);
}

/** Prefer Aliyun MaaS (provider 4) when the same modelCode exists on two providers. */
export function resolvePreferredPlatformModel(session) {
  const code = cfg.platformModelCode;
  const models = getJson(session, '/api/v1/platform/models', 'platform.models');
  const list = Array.isArray(models.data) ? models.data : [];
  const matches = list.filter((item) => item && item.modelCode === code);
  const match = matches.find((item) => Number(item.providerId) === 4) || matches[0];
  if (match && match.id != null) {
    return match;
  }
  throw new Error('platform model ' + code + ' is required');
}

export function postJson(session, path, body, name, timeout) {
  const res = request('POST', path, body, authHeaders(session), timeout || cfg.timeout, name);
  return expectOk(res, name);
}

export function putJson(session, path, body, name, timeout) {
  const res = request('PUT', path, body, authHeaders(session), timeout || cfg.timeout, name);
  return expectOk(res, name);
}

export function sseHeaders(session) {
  const headers = authHeaders(session);
  headers.Accept = 'text/event-stream';
  return headers;
}

/** k6 waits until the SSE connection closes; success is HTTP 200 + a done event. */
export function postSse(session, path, body, name, timeout) {
  return postSseWithHeaders(path, body, sseHeaders(session), name, timeout);
}

export function postSseWithHeaders(path, body, headers, name, timeout) {
  const res = request('POST', path, body, headers, timeout || cfg.chatTimeout, name);
  const text = res.body ? String(res.body) : '';
  const passed = check(res, {
    [`${name} HTTP 200`]: (r) => r.status === 200,
    [`${name} sse done`]: () => text.indexOf('"type":"done"') !== -1,
    [`${name} sse not error`]: () => text.indexOf('"type":"error"') === -1,
  });
  return { passed, res, body: text };
}
