# 知识库管线 RAG

矢量图：[`assets/flow-rag.svg`](./assets/flow-rag.svg)

业务元数据在 MySQL，原文件在 MinIO，搜索与向量由 Elasticsearch 承担。

```mermaid
flowchart TB
  subgraph IN["入库 Ingest"]
    U["文档上传 Upload<br/>PDF / DOCX / MD"] --> M["对象存储 MinIO<br/>原始文件 original file"]
    M --> CH["解析分块 Parse / Chunk"]
    CH --> EM["向量化 Embedding"]
    EM --> ES["Elasticsearch<br/>索引 + 向量 index + vector"]
  end

  subgraph Q["查询 Query"]
    Q1["用户问题 Query"] --> HS["混合搜索 Hybrid<br/>向量 vector + 关键词 keyword"]
    HS --> RR["重排序 Rerank"]
    RR --> CTX["上下文注入 Context<br/>引用 Citation"]
    CTX --> LLM["大模型 LLM"]
  end

  ES -.-> HS
```

文档状态机 Document status：`UPLOADING → PARSING → CHUNKING → EMBEDDING → INDEXING → READY / FAILED`。
