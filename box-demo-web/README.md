# Box 静态演示站

**一比一**打包正式 `box-web`（用户端）与 `box-admin-web`（管理端），通过 `@box/demo-mock` 在前端模拟 `/api`，无需 `box-server`。用于 Cloudflare 等静态托管对外演示。

## 本地预览（推荐）

用户端与管理端各开一个 dev 进程（真实 UI + 模拟 API）：

```bash
cd box-web && npm run dev:demo
# http://localhost:5173  任意账号密码登录

cd box-admin-web && npm run dev:demo
# http://localhost:5174/admin/  任意账号密码登录
```

## 合并构建（与线上静态部署一致）

```bash
cd box-demo-web
npm install
npm run build
```

产物在 `box-demo-web/dist`：

- `/` → 用户端 SPA
- `/admin/` → 管理端 SPA

## 部署 Cloudflare Pages（推荐）

纯静态站点，用 **Pages**，不要用 Workers 托管 `dist`。

```bash
cd box-demo-web
npm install
npm run deploy
```

首次会创建 Pages 项目 **`box-ai-demo`**。自定义域在控制台绑定：

**Workers & Pages → box-ai-demo → Custom domains → `bai.mars-studio.asia`**

（`mars-studio.asia` 须在同一 Cloudflare 账号下。）

构建产物里的 `_redirects` 负责：

- `/` → 用户端 SPA
- `/admin/*` → 管理端 SPA

也可用 Dashboard **Create project → Direct Upload**，上传 `dist` 目录。

## 实现说明

- 演示开关：`box-web/.env.demo`、`box-admin-web/.env.demo`（`VITE_BOX_DEMO=true`）
- Mock 逻辑：`box-demo-mock/`（种子数据 + 会话内状态；详见该目录 README）
- 非演示构建不受影响，勿带 `--mode demo`

演示环境 **所有业务能力均为模拟**，界面与正式版一致，但不连接真实模型与数据库。
