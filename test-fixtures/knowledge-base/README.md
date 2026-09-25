# 知识库测试样例文件

> 上线验收：见 [../RELEASE-GATE.md](../RELEASE-GATE.md) Phase 4A。

本目录由 `generate_samples.py` 生成，各文件包含**同一套事实**，便于 RAG 测试。

## 建议测试问题

| 问题 | 期望答案要点 |
|------|----------------|
| 企业版多少钱？ | 9999 元/年 |
| 免费版能建几个智能体？ | 3 个 |
| 专业版知识库多大？ | 50 GB |
| 退款几天内？ | 7 天 |
| 客服邮箱？ | support@xingyun-tech.com |
| SLA 是多少？ | 99.9% |

## 页面上传说明

当前知识库页「上传文档」与后端白名单一致，见 `box-web/src/constants/knowledgeDocumentUpload.ts`。

## 重新生成

```bash
python test-fixtures/knowledge-base/generate_samples.py
```
