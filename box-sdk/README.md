# Box SDK

面向已发布 Agent 的开放集成客户端，封装 Published API（`/api/v1/published/agents/{id}/chat`）的 JSON 与 SSE 流式调用。

## JavaScript / TypeScript

```bash
# 本地引用
# npm install ../box-sdk/js
```

```js
import { BoxClient } from '@box/sdk'

const box = new BoxClient({
  baseUrl: 'https://your-box-host',
  apiKey: 'ax_live_xxx',
})

const answer = await box.chat(12, '你好', {
  stream: true,
  onDelta: (chunk) => process.stdout.write(chunk),
})
```

## Python

```python
from boxai import BoxClient

box = BoxClient('https://your-box-host', 'ax_live_xxx')
print(box.chat(12, '你好', stream=True))
```

鉴权：`Authorization: Bearer <API Key>`。流式对话使用 `stream: true`，事件类型与主站 Chat 一致（`delta` / `done` / `error`）。
