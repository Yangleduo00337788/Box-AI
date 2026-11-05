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

export function postJson(session, path, body, name, timeout) {
  const res = request('POST', path, body, authHeaders(session), timeout || cfg.timeout, name);
  return expectOk(res, name);
}
