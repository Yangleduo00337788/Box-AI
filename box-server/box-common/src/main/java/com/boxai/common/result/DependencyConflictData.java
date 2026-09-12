package com.boxai.common.result;

import com.boxai.common.guard.ResourceDependency;

import java.util.List;

public record DependencyConflictData(List<ResourceDependency> dependencies) {
}
