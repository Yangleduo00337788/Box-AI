package com.boxai.model.platform;

import com.boxai.ai.ModelRuntimeConfig;

public record ResolvedPlatformModel(
        ModelRuntimeConfig runtimeConfig,
        Long platformCredentialId,
        Long platformModelId
) {}
