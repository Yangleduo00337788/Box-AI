import http from 'node:http';

const PORT = Number(process.env.MCP_MOCK_PORT || 18765);
const TOOL = {
  name: 'ping',
  description: '健康检查，无参数。必须在回答前调用。',
  inputSchema: { type: 'object', properties: {} },
};

function jsonRpcResult(id, result) {
  return JSON.stringify({ jsonrpc: '2.0', id, result });
}

function handle(body) {
  let msg;
  try {
    msg = JSON.parse(body);
  } catch {
    return { status: 400, payload: '{"error":"invalid json"}' };
  }
  const method = msg.method;
  const id = msg.id;
  if (method === 'initialize') {
    return {
      status: 200,
      payload: jsonRpcResult(id, {
        protocolVersion: '2024-11-05',
        capabilities: { tools: {} },
        serverInfo: { name: 'k6-mcp-mock', version: '1.0.0' },
      }),
    };
  }
  if (method === 'notifications/initialized' || method === 'notifications/cancelled') {
    return { status: 202, payload: '' };
  }
  if (method === 'tools/list') {
    return { status: 200, payload: jsonRpcResult(id, { tools: [TOOL] }) };
  }
  if (method === 'tools/call') {
    const name = msg.params && msg.params.name;
    if (name !== 'ping') {
      return {
        status: 200,
        payload: JSON.stringify({
          jsonrpc: '2.0',
          id,
          result: { isError: true, content: [{ type: 'text', text: 'unknown tool' }] },
        }),
      };
    }
    return {
      status: 200,
      payload: jsonRpcResult(id, {
        content: [{ type: 'text', text: 'ok' }],
      }),
    };
  }
  if (method === 'ping') {
    return { status: 200, payload: jsonRpcResult(id, {}) };
  }
  return {
    status: 200,
    payload: JSON.stringify({
      jsonrpc: '2.0',
      id,
      error: { code: -32601, message: `Method not found: ${method}` },
    }),
  };
}

const server = http.createServer((req, res) => {
  if (req.method !== 'POST') {
    res.writeHead(405, { Allow: 'POST' });
    res.end();
    return;
  }
  const chunks = [];
  req.on('data', (c) => chunks.push(c));
  req.on('end', () => {
    const { status, payload } = handle(Buffer.concat(chunks).toString('utf8'));
    res.writeHead(status, {
      'Content-Type': 'application/json',
      'Mcp-Session-Id': 'k6-mcp-session',
    });
    res.end(payload);
  });
});

server.listen(PORT, '127.0.0.1', () => {
  process.stdout.write(`k6-mcp-mock listening on http://127.0.0.1:${PORT}\n`);
});
