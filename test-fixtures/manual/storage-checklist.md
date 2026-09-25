# 数据存储验收清单

验证 **MySQL / 对象存储 / Elasticsearch** 分工是否正确（参见 `docx/04-Database.md`）。

---

## 1. MySQL（库 `box`）

| ID | 检查项 | 方法 | ✓ |
|----|--------|------|---|
| S-01 | 业务表有数据 | 注册后查 `sys_user` | ☐ |
| S-02 | 对话在库 | `message` 有 content，非空 | ☐ |
| S-03 | 知识库元数据 | `knowledge_document` 有 `storage_key`、`storage_backend` | ☐ |
| S-04 | Chunk 文本在库 | `knowledge_chunk.content` 有值 | ☐ |
| S-05 | 向量不在 MySQL | `knowledge_chunk` 无 embedding 大字段 | ☑ |
| S-06 | 对象存储配置 | `system_config` 含 `platform.object_storage.settings` | ☑ |

---

## 2. 对象存储（MinIO / R2）

| ID | 检查项 | 方法 | ✓ |
|----|--------|------|---|
| S-10 | 知识库原文件 | 桶内路径 `knowledge/{kbId}/{docId}/...` | ☐ |
| S-11 | 公开图 | `public-assets/{uuid}.png` 等 | ☐ |
| S-12 | 切换 R2 后新文件 | 新文档 `storage_backend=R2`，R2 控制台可见 | ☐ |
| S-13 | 删文档 | MySQL 删行后对象可被删（或记录 warn） | ☐ |

---

## 3. Elasticsearch

| ID | 检查项 | 方法 | ✓ |
|----|--------|------|---|
| S-20 | 知识库索引 | 文档 READY 后 `_cat/indices` 有 box 相关 index | ☐ |
| S-21 | 混合检索 | 知识库 Search 有结果 | ☐ |
| S-22 | Agent 长期记忆（若启用） | 记忆写入后可语义召回 | ☐ |

---

## 4. Redis

| ID | 检查项 | 方法 | ✓ |
|----|--------|------|---|
| S-30 | 健康检查 | health 中 redis=true | ☑ |
| S-31 | 限流 / 会话 | 频繁登录不拖垮 DB（主观） | ☐ |

---

## 5. 一致性场景（推荐走查）

1. 上传 PDF → MySQL `UPLOADING→…→READY` → MinIO/R2 有对象 → ES 可搜。
2. 换 R2 为写入后端 → 新上传只在 R2 → 旧文档仍可读（`storage_backend` 为空或 MINIO）。
3. 头像 URL `/api/v1/public-assets/xxx.png` 可 200。

| ID | 场景 | ✓ |
|----|------|---|
| S-40 | 端到端一致 | ☐ |
