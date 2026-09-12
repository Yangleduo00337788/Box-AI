package com.boxai.common.constant;

public final class AuditActions {

    private AuditActions() {
    }

    public static final String LOGIN = "auth.login";
    public static final String REGISTER = "auth.register";

    public static final String AGENT_CREATE = "agent.create";
    public static final String AGENT_UPDATE = "agent.update";
    public static final String AGENT_DELETE = "agent.delete";
    public static final String AGENT_PUBLISH = "agent.publish";
    public static final String AGENT_UNPUBLISH = "agent.unpublish";

    public static final String KNOWLEDGE_CREATE = "knowledge.create";
    public static final String KNOWLEDGE_DELETE = "knowledge.delete";
    public static final String KNOWLEDGE_DOCUMENT_DELETE = "knowledge.document.delete";

    public static final String TOOL_CREATE = "tool.create";
    public static final String TOOL_DELETE = "tool.delete";
    public static final String TOOL_EXECUTE = "tool.execute";

    public static final String API_KEY_CREATE = "api_key.create";
    public static final String API_KEY_DELETE = "api_key.delete";
    public static final String API_KEY_ROTATE = "api_key.rotate";
    public static final String API_KEY_DISABLE = "api_key.disable";

    public static final String MEMBER_INVITE = "member.invite";
    public static final String MEMBER_REMOVE = "member.remove";
    public static final String MEMBER_ROLE_UPDATE = "member.role.update";

    public static final String ROLE_CREATE = "role.create";
    public static final String ROLE_UPDATE = "role.update";
    public static final String ROLE_DELETE = "role.delete";
}
