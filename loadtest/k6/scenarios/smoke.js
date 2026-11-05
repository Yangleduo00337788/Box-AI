import { sleep } from 'k6';
import { cfg, scenarioOptions, readThresholds } from '../lib/config.js';
import { jsonHeaders, request, expectOk } from '../lib/client.js';

export const options = scenarioOptions(readThresholds);

export default function () {
  const health = request(
    'GET',
    '/api/v1/system/health',
    null,
    jsonHeaders(),
    cfg.timeout,
    'system.health',
  );
  expectOk(health, 'health');

  const oauth = request(
    'GET',
    '/api/v1/auth/oauth/providers',
    null,
    jsonHeaders(),
    cfg.timeout,
    'auth.oauth.providers',
  );
  expectOk(oauth, 'oauth.providers');

  sleep(1);
}
