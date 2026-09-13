from __future__ import annotations

import json
from collections.abc import Callable
from urllib.request import Request, urlopen


class BoxClient:
    def __init__(self, base_url: str, api_key: str) -> None:
        if not base_url:
            raise ValueError("base_url is required")
        if not api_key:
            raise ValueError("api_key is required")
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key

    def chat(self, agent_id: int, message: str, stream: bool = False, on_delta: Callable[[str], None] | None = None) -> str:
        if stream:
            return self._chat_stream(agent_id, message, on_delta)
        payload = self._request(
            f"/api/v1/published/agents/{agent_id}/chat",
            {"message": message, "stream": False},
        )
        return str(payload.get("data", {}).get("content") or "")

    def embed_config(self, agent_id: int) -> dict:
        payload = self._request(f"/api/v1/published/agents/{agent_id}/embed-config", None, method="GET")
        return payload.get("data") or {}

    def _chat_stream(self, agent_id: int, message: str, on_delta: Callable[[str], None] | None) -> str:
        request = Request(
            f"{self.base_url}/api/v1/published/agents/{agent_id}/chat",
            data=json.dumps({"message": message, "stream": True}).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
                "Accept": "text/event-stream, application/json",
            },
            method="POST",
        )
        content = []
        with urlopen(request) as response:
            buffer = ""
            while True:
                chunk = response.readline()
                if not chunk:
                    break
                line = chunk.decode("utf-8")
                buffer += line
                if not line.strip():
                    for event_line in buffer.splitlines():
                        if not event_line.startswith("data:"):
                            continue
                        event = json.loads(event_line[5:].strip())
                        if event.get("type") == "delta" and event.get("content"):
                            content.append(event["content"])
                            if on_delta:
                                on_delta(event["content"])
                        if event.get("type") == "error":
                            raise RuntimeError(event.get("message") or "stream error")
                        if event.get("type") == "done":
                            return "".join(content)
                    buffer = ""
        return "".join(content)

    def _request(self, path: str, body: dict | None, method: str = "POST") -> dict:
        data = None if body is None else json.dumps(body).encode("utf-8")
        request = Request(
            f"{self.base_url}{path}",
            data=data,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            method=method,
        )
        with urlopen(request) as response:
            payload = json.loads(response.read().decode("utf-8"))
        if payload.get("code") not in (0, None):
            raise RuntimeError(payload.get("message") or "Box API error")
        return payload
