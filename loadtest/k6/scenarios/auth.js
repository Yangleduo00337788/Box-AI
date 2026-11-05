import { sleep } from 'k6';
import { cfg, scenarioOptions, loginThresholds } from '../lib/config.js';
import { authHeaders, expectOk, login, request } from '../lib/client.js';

/**
 * Login is rate-limited to 20 attempts / IP / minute.
 * Keep PROFILE=smoke or VUS low unless staging limits are raised.
 */
export const options = scenarioOptions(loginThresholds);

export default function () {
  const session = login();
  const me = request(
    'GET',
    '/api/v1/auth/me',
    null,
    authHeaders(session),
    cfg.timeout,
    'auth.me',
  );
  expectOk(me, 'auth.me');
  sleep(1);
}
