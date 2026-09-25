#!/usr/bin/env bash
# Box AI Phase 0 发布门禁
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SKIP_BACKEND=false
SKIP_FRONTEND=false
SKIP_SMOKE=false

for arg in "$@"; do
  case "$arg" in
    --skip-backend) SKIP_BACKEND=true ;;
    --skip-frontend) SKIP_FRONTEND=true ;;
    --skip-smoke) SKIP_SMOKE=true ;;
  esac
done

step() { echo ""; echo "==> $1"; }

if [ "$SKIP_BACKEND" != true ]; then
  step "box-server: mvn test"
  (cd "$ROOT/box-server" && mvn -B test)
fi

if [ "$SKIP_FRONTEND" != true ]; then
  step "box-ui: npm ci + typecheck"
  (cd "$ROOT/box-ui" && npm ci && npm run typecheck)
  step "box-web: npm ci + test + build"
  (cd "$ROOT/box-web" && npm ci && npm run test && npm run build)
  step "box-admin-web: npm ci + build"
  (cd "$ROOT/box-admin-web" && npm ci && npm run build)
fi

if [ "$SKIP_SMOKE" != true ]; then
  if [ -z "${BOX_SMOKE_BASE_URL:-}" ]; then
    echo ""
    echo "Skip smoke: export BOX_SMOKE_BASE_URL=http://127.0.0.1:8080"
  else
    bash "$ROOT/test-fixtures/scripts/smoke-stack.sh" "$BOX_SMOKE_BASE_URL"
  fi
fi

echo ""
echo "Phase 0 automated gate: PASSED"
echo "Next: complete test-fixtures/RELEASE-GATE.md Phase 1-5 (manual)."
