package com.boxai.ai;

import java.util.List;

public interface RerankModelGateway {

    List<RerankScore> rerank(ModelRuntimeConfig config, String query, List<String> documents, int topN);
}
