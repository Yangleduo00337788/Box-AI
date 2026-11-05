import { sleep } from 'k6';
import { cfg, scenarioOptions, loginThresholds } from '../lib/config.js';
import { expectOk, jsonHeaders, request } from '../lib/client.js';

/**
 * Personal register: send code then create a unique account.
 * Needs BOX_AUTH_VERIFICATION_EXPOSE_CODE=true (devCode in the JSON).
 * Rate-limit send is 60s per email; unique addresses avoid that.
 */
export const options = scenarioOptions(loginThresholds);

export default function () {
  const email = `k6reg.${__VU}.${__ITER}.${Date.now()}@box.local`;
  const password = cfg.password || 'Loadtest#2026';

  const codeRes = request(
    'POST',
    '/api/v1/auth/verification-code',
    { email, purpose: 'REGISTER' },
    jsonHeaders(),
    cfg.timeout,
    'auth.verification-code',
  );
  const codeBody = expectOk(codeRes, 'auth.verification-code');
  const devCode = codeBody.data && codeBody.data.devCode;
  if (!devCode) {
    return;
  }

  const registerRes = request(
    'POST',
    '/api/v1/auth/register',
    {
      email,
      password,
      verificationCode: devCode,
      nickname: 'k6-register',
      accountType: 'PERSONAL',
    },
    jsonHeaders(),
    cfg.timeout,
    'auth.register',
  );
  expectOk(registerRes, 'auth.register');
  sleep(1);
}
