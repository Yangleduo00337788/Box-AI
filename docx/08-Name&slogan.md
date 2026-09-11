📦 项目品牌正式定为：Box

品牌定位

> Box — AI Agent 开发与工作流编排平台

核心理念：

> 把模型、知识、工具、工作流装进一个 Box，让任何人都能构建自己的 AI Agent。

---

Logo 核心概念

我建议不要直接画一个普通的“纸箱”，而是做成一个极简、科技感的抽象盒子。

可以把 Logo 设计成：

一个打开的 Box + 中间一个 AI 核心节点

---

产品命名体系

后面整个产品可以统一：

Box
│
├── Box Studio       AI Agent / Workflow 编辑器
├── Box Agent        智能体
├── Box Workflow     工作流
├── Box Knowledge    知识库
├── Box Tools        工具
├── Box Models       模型
├── Box MCP          MCP
├── Box Runtime      Agent 运行时
├── Box API          开放 API
└── Box SDK          开发 SDK

后台左上角直接：

> ▣ Box

非常干净。

---

已落地决策

工程与包名统一为 Box，避免与 Java / 企业现有 `com.box` 冲突：

| 项 | 取值 |
|---|---|
| 产品名称 | Box |
| 项目名称 | Box AI |
| 定位 | AI Agent & Workflow Platform |
| 后端工程 | box-server |
| Java 包名 | com.boxai |
| 启动类 | BoxApplication |
| 数据库名 | box |
| Redis / ES 前缀 | box: |

历史名称已废弃：`agentx-server` / `com.agentx` / `AgentXApplication`。

前端 UI 已落地：

- 组件库：TDesign Vue Next（`tdesign-vue-next`）
- 图标：`tdesign-icons-vue-next`
- 禁止 Semi Design Vue / Semi UI
- 组件选型、API、DOM 结构、图标查询以 TDesign MCP 为准，framework 固定 `vue-next`

后台左上角：

> ▣ Box