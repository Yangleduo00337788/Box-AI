package com.boxai.knowledge.support;

public final class KnowledgeDocumentProgress {

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
