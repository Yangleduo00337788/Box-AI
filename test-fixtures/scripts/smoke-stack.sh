#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-${BOX_SMOKE_BASE_URL:-http://127.0.0.1:8080}}"
BASE="${BASE%/}"
URI="$BASE/api/v1/system/health"
echo "Smoke: GET $URI"

body="$(curl -fsS "$URI")"
echo "$body" | grep -q '"code":0' || { echo "API code != 0"; exit 1; }
echo "$body" | grep -q '"mysql":true' || { echo "mysql not true"; exit 1; }
echo "$body" | grep -q '"redis":true' || { echo "redis not true"; exit 1; }
echo "Stack smoke: PASSED"
