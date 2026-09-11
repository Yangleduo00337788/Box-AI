package com.boxai.ai;

import java.util.List;

public interface EmbeddingModelGateway {

    float[] embed(ModelRuntimeConfig config, String text);

    List<float[]> embedAll(ModelRuntimeConfig config, List<String> texts);
}
