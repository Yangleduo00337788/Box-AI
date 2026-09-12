package com.boxai.common.constant;

public final class PermissionCodes {

    private PermissionCodes() {
    }

    public static final String AGENT_CREATE = "agent:create";
    public static final String AGENT_READ = "agent:read";
    public static final String AGENT_UPDATE = "agent:update";
    public static final String AGENT_DELETE = "agent:delete";
    public static final String AGENT_PUBLISH = "agent:publish";

    public static final String WORKFLOW_CREATE = "workflow:create";
    public static final String WORKFLOW_READ = "workflow:read";
    public static final String WORKFLOW_UPDATE = "workflow:update";
    public static final String WORKFLOW_EXECUTE = "workflow:execute";
    public static final String WORKFLOW_DELETE = "workflow:delete";

    public static final String KNOWLEDGE_CREATE = "knowledge:create";
    public static final String KNOWLEDGE_READ = "knowledge:read";
    public static final String KNOWLEDGE_UPDATE = "knowledge:update";
    public static final String KNOWLEDGE_UPLOAD = "knowledge:upload";
    public static final String KNOWLEDGE_DELETE = "knowledge:delete";

    public static final String TOOL_CREATE = "tool:create";
    public static final String TOOL_UPDATE = "tool:update";
    public static final String TOOL_DELETE = "tool:delete";
    public static final String TOOL_EXECUTE = "tool:execute";

    public static final String MODEL_CREATE = "model:create";
    public static final String MODEL_UPDATE = "model:update";

    public static final String MEMBER_MANAGE = "member:manage";
    public static final String ROLE_MANAGE = "role:manage";
    public static final String API_KEY_MANAGE = "api_key:manage";
    public static final String AUDIT_READ = "audit:read";
}
