#!/usr/bin/env bash
set -euo pipefail

SCENARIO="${1:-}"
ENV_FILE="${ENV_FILE:-$(dirname "$0")/.env}"
PROFILE="${PROFILE:-}"
OUT_DIR="${OUT_DIR:-$(dirname "$0")/results}"

if [[ -z "$SCENARIO" ]]; then
  echo "usage: $0 <smoke|auth|workspace|mixed|chat|chat-sse|rag|published|register|workflow|embed|chat-tool|chat-mcp>" >&2
  exit 1
fi

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is not on PATH. Install from https://k6.io/docs/get-started/installation/" >&2
  exit 1
fi

args=(run)
if [[ -f "$ENV_FILE" ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%"${line##*[![:space:]]}"}"
    [[ -z "$line" || "$line" == \#* ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    key="${key%"${key##*[![:space:]]}"}"
    args+=(-e "${key}=${value}")
  done < "$ENV_FILE"
fi

if [[ -n "$PROFILE" ]]; then
  args+=(-e "PROFILE=${PROFILE}")
fi

mkdir -p "$OUT_DIR"
stamp="$(date +%Y%m%d-%H%M%S)"
summary="${OUT_DIR}/${SCENARIO}-${stamp}.json"
args+=(--summary-export "$summary")
args+=("$(dirname "$0")/scenarios/${SCENARIO}.js")

echo "k6 ${args[*]}"
k6 "${args[@]}"
echo "summary: ${summary}"
