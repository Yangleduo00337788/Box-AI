# Box AI 测试夹具与发布门禁

本目录用于 **上线前验收**：自动化门禁 + 手工用例 + 固定测试数据。  
**全部通过** 表示工程与核心链路达到 V1 可上线标准（仍须按环境做安全与容量评估）。

---

## 快速开始

### 1. 自动化门禁（本地，约 15–30 分钟）

需已安装：JDK 21、Maven、Node 20+。

```powershell
# 仓库根目录
.\test-fixtures\scripts\run-release-gate.ps1
```

```bash
./test-fixtures/scripts/run-release-gate.sh
```

仅跑后端单测：

```powershell
.\test-fixtures\scripts\run-release-gate.ps1 -SkipFrontend
```

中间件 + 后端已启动时，追加健康检查：

```powershell
$env:BOX_SMOKE_BASE_URL = "http://127.0.0.1:8080"
.\test-fixtures\scripts\run-release-gate.ps1 -SkipBackend -SkipFrontend
```

栈已起时，再跑 **Phase 1 可脚本项**（MySQL/Flyway、Redis、health、存储 schema、插件 SQL、5.6 等）：

```powershell
$env:BOX_SMOKE_BASE_URL = "http://127.0.0.1:8080"
# 非 compose 时：$env:MYSQL_PORT=3306; $env:MYSQL_PASSWORD=root
.\test-fixtures\scripts\run-automated-infra.ps1
```

### 2. 完整发布流程（含手工）

按 **[RELEASE-GATE.md](./RELEASE-GATE.md)** 从 Phase 0 做到 Phase 5，逐项勾选。

### 3. 专项夹具（嵌在 Phase 4）

| 目录 | 用途 |
|------|------|
| [knowledge-base/](./knowledge-base/) | RAG 多样式文档 + 标准问法 |
| [conversation-plugins/](./conversation-plugins/) | 对话加号 Skill / HTTP / MCP 用例 |

---

## 目录结构

```
test-fixtures/
├── README.md                 # 本文件
├── RELEASE-GATE.md           # 发布门禁总清单（自动化 + 手工）
├── scripts/
│   ├── run-release-gate.ps1  # Windows 一键门禁
│   ├── run-release-gate.sh   # Linux/macOS/CI 一键门禁
│   ├── smoke-stack.ps1       # 依赖栈健康检查
│   └── config.example.env    # 冒烟与环境变量示例
├── manual/
│   ├── consumer-checklist.md # C 端 box-web 手工项
│   ├── admin-checklist.md    # B 端 box-admin-web 手工项
│   └── storage-checklist.md  # MySQL / 对象存储 / ES 验收
├── knowledge-base/           # RAG 样例文件
└── conversation-plugins/     # 插件对话样例 + SQL
```

---

## 与 CI 的关系

GitHub Actions [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) 覆盖 **Phase 0 的子集**（`mvn test`、box-web test/build、admin build）。  
**发布门禁** = CI 通过 + 本地/预发 **Phase 1–5** 手工项 + 可选 `BOX_SMOKE_BASE_URL` 健康检查。

---

## 环境建议

| 场景 | 做法 |
|------|------|
| 本地开发 | `deploy/docker compose up -d`，再启 `box-server` / 前端 |
| 预发全栈 | `deploy/docker compose --profile app up -d --build` |
| 对象存储 | MinIO（Compose）或管理端配置 R2；见 [manual/storage-checklist.md](./manual/storage-checklist.md) |

配置复制：`scripts/config.example.env` → 本地 `config.env`（勿提交密钥）。
