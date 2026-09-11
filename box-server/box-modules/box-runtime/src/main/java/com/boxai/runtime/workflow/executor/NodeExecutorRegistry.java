package com.boxai.runtime.workflow.executor;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NodeExecutorRegistry {

    private final Map<String, NodeExecutor> executors = new HashMap<>();

    public NodeExecutorRegistry(List<NodeExecutor> executorList) {
        for (NodeExecutor executor : executorList) {
            executors.put(normalize(executor.nodeType()), executor);
        }
    }

    public NodeExecutor get(String nodeType) {
        NodeExecutor executor = executors.get(normalize(nodeType));
        if (executor == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的节点类型: " + nodeType);
        }
        return executor;
    }

    private String normalize(String nodeType) {
        return nodeType == null ? "" : nodeType.trim().toLowerCase();
    }
}
