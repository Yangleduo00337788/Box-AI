package com.boxai.common.exception;

import com.boxai.common.guard.ResourceDependency;

import java.util.List;

public class DependencyConflictException extends BusinessException {

    private final List<ResourceDependency> dependencies;

    public DependencyConflictException(String message, List<ResourceDependency> dependencies) {
        super(ErrorCode.CONFLICT, message);
        this.dependencies = dependencies == null ? List.of() : dependencies;
    }

    public List<ResourceDependency> getDependencies() {
        return dependencies;
    }
}
