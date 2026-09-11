package com.boxai.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelDefinition {

    private Long id;
    private Long providerId;
    private String modelCode;
    private String modelName;
    private String modelType;
    private Boolean supportStreaming;
    private Boolean supportToolCalling;
    private Boolean supportVision;
    private Integer contextWindow;
    private Integer maxOutputTokens;
    private Integer status;
    private String configJson;
}
