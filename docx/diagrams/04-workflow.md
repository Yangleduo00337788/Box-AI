# 工作流执行引擎 Workflow Runtime

矢量图：[`assets/flow-workflow.svg`](./assets/flow-workflow.svg)

图定义与运行时分离。节点执行采用 **策略 + 注册表 Strategy + Registry**。  
实现 Implementation：`box-runtime` · `WorkflowExecutor`

```mermaid
flowchart LR
  V["图校验 Validate<br/>必须有 Start / Output"] --> E["创建执行 Create Execution"]
  E --> S["定位起点 Start Node"]
  S --> N["节点执行器 Node Executor<br/>LLM / HTTP / Code ..."]
  N --> R["路由边 Route<br/>条件分支 Condition"]
  R --> O["输出结果 Output<br/>追踪落库 Trace"]
```

## 时序图 Sequence

```mermaid
sequenceDiagram
  autonumber
  actor User as 用户 User
  participant API as 接口层 API
  participant WF as 工作流定义 box-workflow
  participant RT as 运行时 WorkflowExecutor
  participant Node as 节点执行器 NodeExecutor
  participant DB as 落库 MySQL / Trace

  User->>API: 调试或运行 Debug / Run
  API->>WF: 读取图定义 Load graph
  WF->>WF: 校验图 Validate graph
  API->>RT: 创建执行 Create Execution
  RT->>RT: 定位 Start 节点
  loop 沿边调度 Walk edges
    RT->>Node: 执行当前节点 Execute node
    Node-->>RT: 节点输出 Node output
    RT->>RT: 按条件选下一条边 Route next
  end
  RT->>DB: 写 Trace / 执行状态
  RT-->>API: 输出结果 Output
```

节点类型 Node types：Start / Input / Output / LLM / Agent / Knowledge / HTTP / Condition / Switch / Loop / Code / Variable / Template。
