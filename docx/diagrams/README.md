# Box 图册 Diagrams

本目录集中存放架构图、流程图、时序图。节点用 **中文（English）** 标注。

矢量原图在 [`assets/`](./assets/)，与仓库根目录 `assets/readme/` 同源，供 README 渲染。文字源（Mermaid）在本目录 `.md` 文件中，便于改图。

| 文件 | 类型 Type | 说明 |
|------|-----------|------|
| [01-architecture.md](./01-architecture.md) | 架构图 Architecture | 模块化单体分层 |
| [02-agent-chat.md](./02-agent-chat.md) | 流程图 + 时序图 Flow / Sequence | 智能体对话 SSE |
| [03-rag.md](./03-rag.md) | 流程图 Flow | 知识库入库与检索 |
| [04-workflow.md](./04-workflow.md) | 流程图 Flow | 工作流执行引擎 |
| [05-lifecycle.md](./05-lifecycle.md) | 流程图 Flow | Agent 配置与发布 |
| [06-product.md](./06-product.md) | 能力全景 Product map | C 端 / 管理端 / 开放集成 |

对应设计文档：[`03-Architecture.md`](../03-Architecture.md)、[`06-Runtime.md`](../06-Runtime.md)。
