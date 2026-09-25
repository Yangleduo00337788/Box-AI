package com.boxai.knowledge.support;

public final class KnowledgeDocumentProgress {

    /** 处理失败时 progress 归零，与 {@code KnowledgeDocumentProcessingService} 一致 */
    public static final int FAILED = 0;

    public static final int UPLOADING = 5;
    public static final int QUEUED = 10;
    public static final int PARSING = 20;
    public static final int OCR = 45;
    public static final int CHUNKING = 60;
    public static final int EMBEDDING = 75;
    public static final int INDEXING = 90;
    public static final int READY = 100;

    private KnowledgeDocumentProgress() {
    }
}
