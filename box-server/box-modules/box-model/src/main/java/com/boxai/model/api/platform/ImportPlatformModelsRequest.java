package com.boxai.model.api.platform;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportPlatformModelsRequest(
        @NotEmpty List<String> modelCodes
) {}
