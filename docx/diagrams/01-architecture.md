# 架构图 Architecture

矢量图：[`assets/architecture.svg`](./assets/architecture.svg)

V1 采用 **模块化单体 Modular Monolith**：前端多应用共用 `@box/ui`，后端一个 Spring Boot 进程按领域模块拆分。

```mermaid
flowchart TB
  subgraph FE["前端 FRONTEND · Vue 3 + TDesign + SSE 流式输出"]
    WEB["用户工作台 box-web"]
    ADM["管理控制台 box-admin-web"]
    UI["共享组件库 box-ui"]
    SDK["开放客户端 box-sdk"]
  end

  subgraph SRV["后端 BOX-SERVER · Spring Boot 3 · 接口前缀 /api/v1"]
    AG["智能体 box-agent"]
    WF["工作流定义 box-workflow"]
    KB["知识库 box-knowledge"]
    TOOL["工具 box-tool"]
    MODEL["模型 box-model"]
    CONV["会话 box-conversation"]
    RT["工作流运行时 box-runtime"]
    TRACE["链路追踪 box-trace"]
    PUB["发布 box-publish"]
    TEN["租户 box-tenant"]
    USER["用户 box-user"]
    WS["工作空间 box-workspace"]
    ANA["统计分析 box-analytics"]
    LC["大模型编排 LangChain4j Runtime"]
  end

  subgraph INFRA["基础设施 Infrastructure"]
    MY[("MySQL 8<br/>业务数据与数据库迁移 Flyway")]
    RD[("Redis<br/>会话、缓存与分布式锁 session / cache / lock")]
    ES[("Elasticsearch<br/>全文、向量与混合检索 full-text / vector / hybrid")]
    MO[("MinIO<br/>原文件与文档 object storage")]
  end

  FE --> SRV
  AG --> LC
  WF --> RT
  KB --> ES
  KB --> MO
  SRV --> MY
  SRV --> RD
```

| 层 Layer | 内容 |
|----------|------|
| 接入 Access | `box-web`、`box-admin-web`、Open API / Embed / `box-sdk` |
| 应用 Application | 单进程 `box-server`：对话在 `box-agent`（`AgentChatExecutor`），工作流节点在 `box-runtime`（`WorkflowExecutor`） |
| 存储 Storage | MySQL 库名 `box`、Redis 前缀 `box:`、Elasticsearch、MinIO |
