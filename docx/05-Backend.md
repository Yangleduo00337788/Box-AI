Box Backend

后端项目结构、编码规范与 API 设计

Java： 21
Spring Boot： 3.x
ORM： MyBatis-Flex
Database： MySQL 8
Cache： Redis
Object Storage： MinIO
Search： Elasticsearch
AI： LangChain4j
Architecture： Modular Monolith

---

一、后端整体工程结构

项目名称：

box-server

完整结构：

box-server
│
├── pom.xml
│
├── box-bootstrap
│   └── pom.xml
│
├── box-common
│   └── pom.xml
│
├── box-security
│   └── pom.xml
│
├── box-infrastructure
│   └── pom.xml
│
├── box-domain
│   └── pom.xml
│
├── box-application
│   └── pom.xml
│
└── box-modules
    │
    ├── box-user
    ├── box-workspace
    ├── box-tenant
    ├── box-agent
    ├── box-model
    ├── box-knowledge
    ├── box-tool
    ├── box-workflow
    ├── box-conversation
    ├── box-runtime
    ├── box-publish
    ├── box-trace
    └── box-analytics（工作空间统计：`/api/v1/analytics/*`）

**Runtime 分工**：Agent 对话在 `box-agent`（`AgentChatExecutor`）；Workflow 节点执行在 `box-runtime`。

**前端工程**：`box-web`（C 端）· `box-admin-web`（平台管理）· `box-ui`（共享 `@box/ui`）

---

二、Maven 模块依赖关系

不能让所有模块互相依赖。

推荐：

                         bootstrap
                             │
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
       application       security        infrastructure
            │                                 │
            ▼                                 ▼
         domain  ◄────────────────────────────┘
            ▲
            │
       business modules

业务模块：

agent
workflow
knowledge
tool
model
conversation
runtime
publish
trace
analytics

依赖公共层。

---

三、推荐 Maven 依赖方向

box-bootstrap
        │
        ├── box-application
        ├── box-security
        ├── box-infrastructure
        └── modules

业务模块：

box-agent
        │
        ├── box-domain
        ├── box-common
        └── box-application

Infrastructure：

box-infrastructure
        │
        ├── box-domain
        ├── box-common
        ├── MySQL
        ├── Redis
        ├── ES
        └── MinIO

---

四、根 pom.xml

<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.boxai</groupId>

    <artifactId>box-server</artifactId>

    <version>1.0.0-SNAPSHOT</version>

    <packaging>pom</packaging>

    <name>box-server</name>

    <properties>

        <java.version>21</java.version>

        <spring-boot.version>3.x.x</spring-boot.version>

        <mybatis-flex.version>...</mybatis-flex.version>

        <langchain4j.version>...</langchain4j.version>

    </properties>

    <modules>

        <module>box-common</module>

        <module>box-security</module>

        <module>box-infrastructure</module>

        <module>box-domain</module>

        <module>box-application</module>

        <module>box-modules/box-user</module>
        <module>box-modules/box-workspace</module>
        <module>box-modules/box-tenant</module>
        <module>box-modules/box-agent</module>
        <module>box-modules/box-model</module>
        <module>box-modules/box-knowledge</module>
        <module>box-modules/box-tool</module>
        <module>box-modules/box-workflow</module>
        <module>box-modules/box-conversation</module>
        <module>box-modules/box-runtime</module>
        <module>box-modules/box-publish</module>
        <module>box-modules/box-trace</module>
        <module>box-modules/box-analytics</module>

        <module>box-bootstrap</module>

    </modules>

</project>

实际开发时版本号不要盲目复制示例，需要统一通过 Maven properties / BOM 管理。

---

五、box-common

目录：

box-common
└── src/main/java/com/boxai/common
    │
    ├── constant
    │
    ├── enums
    │
    ├── exception
    │
    ├── result
    │
    ├── pagination
    │
    ├── utils
    │
    └── json

---

六、统一 Result

推荐：

public record Result<T>(
        Integer code,
        String message,
        T data
) {

    public static <T> Result<T> success(T data) {
        return new Result<>(
                0,
                "success",
                data
        );
    }

    public static <T> Result<T> success() {
        return new Result<>(
                0,
                "success",
                null
        );
    }

    public static <T> Result<T> failure(
            Integer code,
            String message
    ) {
        return new Result<>(
                code,
                message,
                null
        );
    }
}

---

七、统一错误码

public interface ErrorCode {

    int SUCCESS = 0;

    int BAD_REQUEST = 400;

    int UNAUTHORIZED = 401;

    int FORBIDDEN = 403;

    int NOT_FOUND = 404;

    int INTERNAL_ERROR = 500;

    int AGENT_NOT_FOUND = 10001;

    int AGENT_VERSION_NOT_FOUND = 10002;

    int WORKSPACE_NOT_FOUND = 11001;

    int WORKSPACE_ACCESS_DENIED = 11002;

    int MODEL_NOT_FOUND = 12001;

    int KNOWLEDGE_NOT_FOUND = 13001;

    int TOOL_NOT_FOUND = 14001;

    int WORKFLOW_NOT_FOUND = 15001;

    int EXECUTION_FAILED = 16001;
}

实际项目建议最终使用：

enum BusinessErrorCode

统一维护。

---

八、BusinessException

public class BusinessException
        extends RuntimeException {

    private final int code;

    public BusinessException(
            int code,
            String message
    ) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

使用：

if (agent == null) {
    throw new BusinessException(
        ErrorCode.AGENT_NOT_FOUND,
        "Agent 不存在"
    );
}

---

九、全局异常处理

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(
            BusinessException e
    ) {

        return Result.failure(
                e.getCode(),
                e.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(
            MethodArgumentNotValidException e
    ) {

        return Result.failure(
                400,
                "请求参数校验失败"
        );
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(
            Exception e
    ) {

        return Result.failure(
                500,
                "服务器内部错误"
        );
    }
}

生产环境：

«不应该把 "Exception#getMessage()" 原样返回给用户。»

---

十、分页模型

请求：

public record PageQuery(

        Integer page,

        Integer pageSize

) {

    public PageQuery {

        if (page == null || page < 1) {
            page = 1;
        }

        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }

        if (pageSize > 100) {
            pageSize = 100;
        }
    }
}

响应：

public record PageResult<T>(

        List<T> records,

        long total,

        int page,

        int pageSize

) {}

---

十一、Controller 规范

Controller 只负责：

参数接收
参数校验
调用 Application Service
返回 Result

不要在 Controller 写：

SQL
AI调用
Redis
ES
复杂业务逻辑
Workflow执行

错误：

@PostMapping
public Result<?> create() {

    // 100行代码

}

正确：

@PostMapping
public Result<AgentVO> create(
        @Valid @RequestBody CreateAgentRequest request
) {

    return Result.success(
        agentApplicationService.create(request)
    );
}

---

十二、Agent 模块结构

box-agent
│
└── src/main/java/com/boxai/agent
    │
    ├── controller
    │
    ├── application
    │   ├── service
    │   └── command
    │
    ├── domain
    │   ├── entity
    │   ├── repository
    │   └── service
    │
    ├── infrastructure
    │   ├── mapper
    │   └── repository
    │
    └── dto
        ├── request
        ├── response
        └── query

---

十三、Agent Controller

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentApplicationService
            agentApplicationService;

    @PostMapping
    public Result<AgentVO> create(
            @Valid
            @RequestBody
            CreateAgentRequest request
    ) {

        return Result.success(
            agentApplicationService.create(request)
        );
    }

    @GetMapping
    public Result<PageResult<AgentVO>> page(
            AgentPageQuery query
    ) {

        return Result.success(
            agentApplicationService.page(query)
        );
    }

    @GetMapping("/{id}")
    public Result<AgentVO> detail(
            @PathVariable Long id
    ) {

        return Result.success(
            agentApplicationService.detail(id)
        );
    }

    @PutMapping("/{id}")
    public Result<Void> update(
            @PathVariable Long id,
            @Valid
            @RequestBody
            UpdateAgentRequest request
    ) {

        agentApplicationService.update(
            id,
            request
        );

        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id
    ) {

        agentApplicationService.delete(id);

        return Result.success();
    }
}

---

十四、Agent Request DTO

CreateAgentRequest

public record CreateAgentRequest(

        @NotBlank
        @Size(max = 128)
        String name,

        @Size(max = 512)
        String description,

        String avatar

) {}

---

十五、Agent VO

public record AgentVO(

        Long id,

        String name,

        String description,

        String avatar,

        String status,

        Integer draftVersion,

        Integer publishedVersion,

        Long createdBy,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {}

---

十六、Agent Application Service

@Service
@RequiredArgsConstructor
@Transactional
public class AgentApplicationService {

    private final AgentRepository agentRepository;

    private final AgentVersionRepository
            agentVersionRepository;

    private final WorkspaceContext
            workspaceContext;

    public AgentVO create(
            CreateAgentRequest request
    ) {

        Long workspaceId =
                workspaceContext.requireWorkspaceId();

        Agent agent = new Agent();

        agent.setWorkspaceId(workspaceId);

        agent.setName(request.name());

        agent.setDescription(
                request.description()
        );

        agent.setAvatar(
                request.avatar()
        );

        agent.setStatus("DRAFT");

        agentRepository.save(agent);

        return AgentVOAssembler.toVO(agent);
    }
}

---

十七、WorkspaceContext

这是整个系统非常重要的组件。

public interface WorkspaceContext {

    Long requireWorkspaceId();

    Long getWorkspaceId();

    void setWorkspaceId(Long workspaceId);
}

实现：

@Component
public class WorkspaceContextHolder
        implements WorkspaceContext {

    private static final ThreadLocal<Long>
            CONTEXT = new ThreadLocal<>();

    @Override
    public Long requireWorkspaceId() {

        Long workspaceId = CONTEXT.get();

        if (workspaceId == null) {
            throw new BusinessException(
                    401,
                    "Workspace 未选择"
            );
        }

        return workspaceId;
    }

    @Override
    public Long getWorkspaceId() {
        return CONTEXT.get();
    }

    @Override
    public void setWorkspaceId(
            Long workspaceId
    ) {
        CONTEXT.set(workspaceId);
    }

    public void clear() {
        CONTEXT.remove();
    }
}

注意：

«如果未来大量使用异步 / 虚拟线程 / Reactor，需要重新设计 Context 传递方式，不能简单依赖 ThreadLocal。»

---

十八、Workspace 拦截器

请求：

Authorization
+
Workspace ID

例如 Header：

X-Workspace-Id: 10001

流程：

Request
 ↓
JWT
 ↓
User
 ↓
X-Workspace-Id
 ↓
Workspace Member
 ↓
Permission
 ↓
WorkspaceContext
 ↓
Controller

---

十九、Workspace 权限验证

绝对不能只相信：

X-Workspace-Id

必须查询：

SELECT *
FROM workspace_member
WHERE workspace_id = ?
  AND user_id = ?
  AND status = 1
  AND deleted = 0;

验证成功后：

WorkspaceContext

才允许继续执行。

---

二十、JWT 架构

登录：

POST /api/v1/auth/login

流程：

Username
Password
   ↓
User
   ↓
Password Verify
   ↓
Generate JWT
   ↓
Response

JWT Payload：

{
  "sub": "10001",
  "username": "admin",
  "iat": 123456,
  "exp": 123999
}

不要把大量权限数据塞进 JWT。

---

二十一、Spring Security

过滤链：

Request
   ↓
JwtAuthenticationFilter
   ↓
JWT Verify
   ↓
SecurityContext
   ↓
WorkspaceInterceptor
   ↓
Controller

公开接口：

/api/v1/auth/login
/api/v1/auth/register

其他 API：

authenticated()

---

二十二、RBAC 校验

权限判断：

User
 ↓
Workspace Member
 ↓
Role
 ↓
Permission

例如：

@PreAuthorize(
    "hasAuthority('agent:create')"
)
@PostMapping
public Result<AgentVO> create(...) {
    ...
}

但是：

«RBAC 只解决“能不能做”。»

还需要 Workspace Resource Check：

«“能不能操作这个具体 Agent”。»

因此：

Permission
+
Workspace
+
Resource Ownership

三层都需要。

---

二十三、Repository

Domain：

public interface AgentRepository {

    Agent findById(Long id);

    void save(Agent agent);

    void update(Agent agent);

    void delete(Long id);

    PageResult<Agent> page(
        AgentQuery query
    );
}

Domain 不知道：

MyBatis
MySQL
SQL
Mapper

---

二十四、MyBatis-Flex Mapper

Infrastructure：

@Mapper
public interface AgentMapper
        extends BaseMapper<AgentDO> {
}

DO：

@Data
@Table("agent")
public class AgentDO {

    private Long id;

    private Long workspaceId;

    private String name;

    private String description;

    private String avatar;

    private String status;

    private Long draftVersionId;

    private Long publishedVersionId;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    private Integer deleted;
}

---

二十五、为什么 Entity 和 DO 分开

不要：

Domain Entity = MyBatis Entity

推荐：

Domain
    ↓
Agent

Infrastructure
    ↓
AgentDO

Database
    ↓
agent

这样以后数据库变化不会污染 Domain。

---

二十六、DTO / DO / Entity / VO

四种对象职责：

Request DTO
    ↓
Controller 输入

Command
    ↓
Application 业务意图

Domain Entity
    ↓
核心业务模型

DO
    ↓
数据库映射

VO
    ↓
API 输出

完整：

HTTP Request
     ↓
Request DTO
     ↓
Application Service
     ↓
Domain Entity
     ↓
Repository
     ↓
DO
     ↓
MySQL

反向：

MySQL
 ↓
DO
 ↓
Repository
 ↓
Domain Entity
 ↓
Application
 ↓
VO
 ↓
HTTP Response

---

二十七、Agent API

V1：

POST   /api/v1/agents

GET    /api/v1/agents

GET    /api/v1/agents/{id}

PUT    /api/v1/agents/{id}

DELETE /api/v1/agents/{id}

版本：

GET  /api/v1/agents/{id}/versions

POST /api/v1/agents/{id}/versions

GET  /api/v1/agents/{id}/versions/{versionId}

POST /api/v1/agents/{id}/versions/{versionId}/publish

POST /api/v1/agents/{id}/versions/{versionId}/rollback

---

二十八、Agent 配置 API

Prompt：

PUT /api/v1/agents/{id}/prompt

Model：

PUT /api/v1/agents/{id}/model

Knowledge：

POST   /api/v1/agents/{id}/knowledge
DELETE /api/v1/agents/{id}/knowledge/{knowledgeId}

Tool：

POST   /api/v1/agents/{id}/tools
DELETE /api/v1/agents/{id}/tools/{toolId}

Memory：

PUT /api/v1/agents/{id}/memory

Variables：

GET    /api/v1/agents/{id}/variables
POST   /api/v1/agents/{id}/variables
PUT    /api/v1/agents/{id}/variables/{variableId}
DELETE /api/v1/agents/{id}/variables/{variableId}

---

二十九、Model API

GET    /api/v1/model-providers

POST   /api/v1/model-providers

PUT    /api/v1/model-providers/{id}

DELETE /api/v1/model-providers/{id}

Model：

GET    /api/v1/models

POST   /api/v1/models

PUT    /api/v1/models/{id}

DELETE /api/v1/models/{id}

API Key：

POST   /api/v1/model-api-keys

DELETE /api/v1/model-api-keys/{id}

---

三十、Knowledge API

GET    /api/v1/knowledge-bases

POST   /api/v1/knowledge-bases

GET    /api/v1/knowledge-bases/{id}

PUT    /api/v1/knowledge-bases/{id}

DELETE /api/v1/knowledge-bases/{id}

Document：

POST   /api/v1/knowledge-bases/{id}/documents

GET    /api/v1/knowledge-bases/{id}/documents

GET    /api/v1/documents/{id}

DELETE /api/v1/documents/{id}

RAG Test：

POST /api/v1/knowledge-bases/{id}/search

---

三十一、Tool API

GET    /api/v1/tools

POST   /api/v1/tools

GET    /api/v1/tools/{id}

PUT    /api/v1/tools/{id}

DELETE /api/v1/tools/{id}

Tool 测试：

POST /api/v1/tools/{id}/test

---

三十二、Workflow API

GET    /api/v1/workflows

POST   /api/v1/workflows

GET    /api/v1/workflows/{id}

PUT    /api/v1/workflows/{id}

DELETE /api/v1/workflows/{id}

保存：

PUT /api/v1/workflows/{id}/definition

校验：

POST /api/v1/workflows/{id}/validate

调试：

POST /api/v1/workflows/{id}/debug

发布：

POST /api/v1/workflows/{id}/publish

---

三十三、Conversation API

创建：

POST /api/v1/conversations

列表：

GET /api/v1/conversations

详情：

GET /api/v1/conversations/{id}

消息：

GET /api/v1/conversations/{id}/messages

聊天：

POST /api/v1/conversations/{id}/messages

---

三十四、Agent Chat API

调试 Agent：

POST /api/v1/agents/{id}/chat

请求：

{
  "conversationId": "10001",
  "message": "你好",
  "stream": true,
  "variables": {
    "user_name": "张三"
  }
}

响应：

Content-Type: text/event-stream

---

三十五、Runtime API

内部 Runtime：

POST /api/v1/runtime/agents/{agentId}/execute

POST /api/v1/runtime/workflows/{workflowId}/execute

但是正式对外发布后：

POST /api/v1/published/agents/{agentId}/chat

两者不要混在一起。

---

三十六、Publish API

POST /api/v1/agents/{id}/publish

GET  /api/v1/agents/{id}/publish

POST /api/v1/agents/{id}/unpublish

发布时：

Draft
 ↓
Validate
 ↓
Snapshot
 ↓
Create Version
 ↓
Publish

---

三十七、API Key API

GET    /api/v1/api-keys

POST   /api/v1/api-keys

DELETE /api/v1/api-keys/{id}

POST   /api/v1/api-keys/{id}/disable

POST   /api/v1/api-keys/{id}/enable

创建响应：

{
  "id": 10001,
  "name": "生产环境",
  "apiKey": "ax_live_xxxxxxxxx"
}

注意：

«"apiKey" 只返回一次。»

---

三十八、Trace API

GET /api/v1/executions

GET /api/v1/executions/{id}

GET /api/v1/executions/{id}/nodes

GET /api/v1/executions/{id}/trace

GET /api/v1/traces/{traceId}

前端调试界面：

Execution
│
├── Input
├── Output
├── Duration
├── Tokens
│
└── Trace
    ├── Prompt
    ├── Memory
    ├── RAG
    ├── Tool
    └── LLM

---

三十九、API URL 规范

统一：

/api/v1

资源使用复数：

/agents
/workflows
/models
/tools
/conversations

不要：

/getAgent
/createAgent
/deleteAgent

REST 风格：

GET
POST
PUT
PATCH
DELETE

---

四十、状态码

HTTP 状态码：

200 SUCCESS

201 CREATED

400 BAD REQUEST

401 UNAUTHORIZED

403 FORBIDDEN

404 NOT FOUND

409 CONFLICT

422 VALIDATION ERROR

429 TOO MANY REQUESTS

500 INTERNAL SERVER ERROR

业务错误码单独维护。

---

四十一、Request ID

每一次 HTTP 请求生成：

X-Request-Id

例如：

req_01HXYZ...

然后贯穿：

Request
 ↓
Application
 ↓
Execution
 ↓
Trace
 ↓
Log

日志：

requestId
userId
workspaceId
executionId
traceId

这样出现问题时可以完整追踪。

---

四十二、日志规范

不要：

System.out.println();

统一：

@Slf4j

例如：

log.info(
    "Agent created, workspaceId={}, agentId={}",
    workspaceId,
    agentId
);

错误：

log.error("error", e);

应该：

log.error(
    "Agent execution failed, executionId={}",
    executionId,
    e
);

---

四十三、敏感数据日志规范

禁止日志打印：

API Key
JWT
Password
Database Password
Authorization
完整用户隐私数据

例如：

❌ apiKey=sk-xxxx

✅ apiKey=sk-****xxxx

---

四十四、数据库事务

Application Service 控制事务：

@Transactional
public void publish(...) {
    ...
}

不要：

Controller @Transactional

也不要在 Domain Entity 中处理事务。

---

四十五、Agent 创建事务

POST /agents
        │
        ▼
Create Agent
        │
        ▼
Create Agent Version 1
        │
        ▼
Create Default Variables
        │
        ▼
Commit

如果任何一步失败：

Rollback

---

四十六、Agent 发布事务

Publish
   │
   ▼
Validate Version
   │
   ▼
Create Snapshot
   │
   ▼
Update agent.published_version_id
   │
   ▼
Create publish record
   │
   ▼
Audit Log
   │
   ▼
Commit

---

四十七、文件上传 API

不要直接把文件二进制存 MySQL。

流程：

Browser
 ↓
POST /files/upload
 ↓
Spring Boot
 ↓
MinIO
 ↓
File Metadata
 ↓
Return fileId

响应：

{
  "fileId": 10001,
  "fileName": "example.pdf",
  "size": 102400,
  "status": "UPLOADED"
}

知识库再：

fileId
 ↓
Document
 ↓
Parse

---

四十八、MinIO Service

public interface FileStorageService {

    String upload(
        String bucket,
        String objectName,
        InputStream inputStream,
        long size,
        String contentType
    );

    InputStream download(
        String bucket,
        String objectName
    );

    void delete(
        String bucket,
        String objectName
    );

    String getPresignedUrl(
        String bucket,
        String objectName,
        Duration duration
    );
}

业务模块只依赖：

FileStorageService

不直接依赖：

MinioClient

---

四十九、Redis Service

统一：

public interface CacheService {

    <T> T get(
        String key,
        Class<T> type
    );

    void set(
        String key,
        Object value,
        Duration ttl
    );

    void delete(String key);

    boolean exists(String key);
}

业务：

cacheService.set(
    "box:rate_limit:" + userId,
    count,
    Duration.ofMinutes(1)
);

---

五十、限流

V1 不需要复杂的 Sentinel。

可以先：

Redis
+
Lua

实现：

登录限流
API Key 限流
Chat 限流
Tool 限流

例如：

User
 ↓
1分钟
 ↓
60 requests

超过：

429 Too Many Requests

---

五十一、API Key 鉴权

公开 API：

POST /api/v1/published/{agentId}/chat

请求：

Authorization: Bearer ax_live_xxxxxxxxx

流程：

API Key
 ↓
Hash
 ↓
Redis Cache
 ↓
MySQL
 ↓
Workspace
 ↓
Permission
 ↓
Agent
 ↓
Published Version

---

五十二、前端工程结构

box-web
│
├── src
│   │
│   ├── api
│   │   ├── auth.ts
│   │   ├── agent.ts
│   │   ├── model.ts
│   │   ├── knowledge.ts
│   │   ├── tool.ts
│   │   ├── workflow.ts
│   │   ├── conversation.ts
│   │   └── publish.ts
│   │
│   ├── components
│   │
│   ├── layouts
│   │
│   ├── router
│   │
│   ├── stores
│   │
│   ├── types
│   │
│   ├── utils
│   │
│   ├── views
│   │
│   └── App.vue
│
└── package.json

---

五十三、Frontend API 封装

例如：

export interface AgentVO {
  id: number
  name: string
  description?: string
  avatar?: string
  status: string
}

export function getAgents(params: AgentQuery) {
  return request.get<PageResult<AgentVO>>(
    '/api/v1/agents',
    { params }
  )
}

export function createAgent(
  data: CreateAgentRequest
) {
  return request.post<AgentVO>(
    '/api/v1/agents',
    data
  )
}

页面不要直接写 Axios。

---

五十四、前端状态管理

Pinia：

stores
├── auth.ts
├── workspace.ts
├── agent.ts
├── model.ts
├── conversation.ts
└── app.ts

Workspace Store：

currentWorkspace
workspaceList
members
permissions

---

五十五、前端路由（box-web）

公开页：

`/login` · `/register` · `/forgot-password` · `/embed` · `/embed/agents/:id` · `/invite/:token` · `/legal/:doc`

登录后默认：`/` → `/chat`

**C 端侧栏（真实结构）**：新任务 `/chat`、插件市场 `/plugin-market`、模型 `/models`，以及智能体 / 项目 / 任务列表。  
知识库 · 工具 · MCP · 工作流 · 市场 **不在侧栏**，从插件市场或全局搜索进入。

**头像菜单 Dialog（不以侧栏整页为主）**：概览、分析（可切到执行记录）、团队。对应路由 `/dashboard`、`/analytics`、`/team` 仍存在。团队弹窗需企业租户 + `member:manage`。

| 路由 | 说明 |
|------|------|
| `/chat`、`/chat/:id` | 对话工作台（默认首页） |
| `/dashboard` | 概览（主入口为弹窗） |
| `/agents` | ↪️ `/chat` |
| `/agents/:id/builder` | Agent Builder |
| `/workflows`、`/workflows/:id/editor` | 工作流（非侧栏） |
| `/knowledge` · `/tools` · `/mcp` | 资源（非侧栏） |
| `/models` | 模型（侧栏） |
| `/plugin-market` · `/market` | 插件市场（侧栏） / Agent 市场（非侧栏） |
| `/executions` | ↪️ `/debug` |
| `/debug` | Debug Console（无侧栏入口） |
| `/analytics` | 分析（主入口为弹窗） |
| `/team` | 团队（主入口为弹窗；`member:manage`） |
| `/settings/profile` · `security` · `preferences` | 账号与偏好 |
| `/settings/api-keys` · `roles` · `audit-logs` | 需对应权限 |
| `/settings/plan` | 套餐与额度（含账单 Tab） |
| `/settings/about` · `legal` | 关于与协议 |
| `/forbidden` | 无权限 |

设置重定向：`appearance` · `general` → `preferences`；`quota` · `capacity` · `billing` → `plan`。

已废弃重定向：`/conversations`、`/chat/logs` → `/chat`

**box-admin-web**（独立应用，默认 `/dashboard`）：

侧栏显隐与路由守卫共用 `box-admin-web/src/constants/rbac.ts` 的 `ADMIN_ROUTE_ROLES`（与后端 `PlatformAdminAccess` 对齐）。`menu.ts` 只描述文案与路径，不重复写角色。

| 路由 | 说明 | 角色 |
|------|------|------|
| `/dashboard` | 工作台 | SUPER / OPS / FINANCE / CONTENT |
| `/analytics` | 平台分析 | SUPER / OPS / FINANCE |
| `/tenants` | 租户管理 | SUPER / OPS / FINANCE |
| `/users` | 平台用户 | SUPER / OPS（新建/启停仅 SUPER） |
| `/plans` | 套餐 | SUPER / OPS / FINANCE |
| `/billing-invoices` | 账单对账 | SUPER / FINANCE |
| `/ops-placements` | 运营位 | SUPER / OPS / CONTENT |
| `/audit-logs` | 全平台审计 | SUPER / OPS / FINANCE |
| `/platform-models` | 平台模型池 | SUPER / OPS |
| `/agent-templates` | 智能体市场 | SUPER / OPS / CONTENT |
| `/plugin-catalog` · `/platform-tools` · `/platform-mcp` | 插件 / 官方工具 / MCP | SUPER / OPS / CONTENT |
| `/system-config` | SMTP / OAuth / 协议 | SUPER |
| `/forbidden` | 无权限 | 已登录 |

---

五十五点一、平台 Admin API

```
POST       /api/v1/admin/auth/login
GET        /api/v1/admin/auth/me
POST       /api/v1/admin/auth/verification-code
POST       /api/v1/admin/auth/password/reset

GET        /api/v1/admin/analytics/overview
GET        /api/v1/admin/analytics/trends
GET        /api/v1/admin/analytics/tenants/{tenantId}

GET/POST   /api/v1/admin/tenants
PUT        /api/v1/admin/tenants/{id}/status
PUT        /api/v1/admin/tenants/{id}/plan
GET        /api/v1/admin/tenants/{id}/quota
GET        /api/v1/admin/tenants/{id}/workspaces
GET/POST   /api/v1/admin/tenants/{tenantId}/members
PUT        /api/v1/admin/tenants/{tenantId}/members/{userId}/status

GET        /api/v1/admin/users
POST       /api/v1/admin/users                 （仅 SUPER_ADMIN）
PUT        /api/v1/admin/users/{id}/status     （仅 SUPER_ADMIN）
GET        /api/v1/admin/users/{id}/context

GET/POST   /api/v1/admin/plans
GET/PUT/DELETE /api/v1/admin/plans/{id}

GET        /api/v1/admin/billing/invoices

GET        /api/v1/admin/audit-logs

GET/POST/PUT/DELETE /api/v1/admin/ops/placements

GET/POST/PUT/DELETE /api/v1/admin/platform/providers
GET/POST/PUT/DELETE /api/v1/admin/platform/models
GET/POST/DELETE     /api/v1/admin/platform/credentials

GET/POST/PUT/DELETE /api/v1/admin/agent-templates
PUT        /api/v1/admin/agent-templates/{id}/review
GET/POST/PUT/DELETE /api/v1/admin/plugins
PUT        /api/v1/admin/plugins/{id}/review
GET/POST/PUT/DELETE /api/v1/admin/plugin-categories

GET/PUT    /api/v1/admin/system/config
POST       /api/v1/admin/assets/images
```

平台角色 `platformAdminRole`：`SUPER_ADMIN` · `OPS` · `FINANCE` · `CONTENT`。  
`PlatformAdminInterceptor` 对 `/api/v1/admin/**` 校验 userType + 角色路径矩阵（见 `PlatformAdminAccess`）。无 claim 的旧 Token 暂按 SUPER_ADMIN，重新登录后写入真实角色。

租户成员等见 `box-tenant` / `box-user` 下 Admin Controller。

---

五十五点二、通知 / 账单 / 侧栏 API

```
GET        /api/v1/notifications
GET        /api/v1/notifications/unread-count
POST       /api/v1/notifications/{id}/read
POST       /api/v1/notifications/read-all

GET        /api/v1/billing/...

GET        /api/v1/sidebar
```

---

五十六、前后端权限模型

前端：

Permission
 ↓
按钮是否显示

后端：

Permission
 ↓
请求是否允许

例如：

<Button
  v-if="hasPermission('agent:create')"
>
  创建 Agent
</Button>

但是：

«前端权限只是 UX 控制，不能作为安全边界。»

真正权限必须由后端执行。

平台管理端另有 `platformAdminRole`（SUPER_ADMIN / OPS / FINANCE / CONTENT）：侧栏与路由守卫只做显隐，接口由 `PlatformAdminInterceptor` 拒绝 403。

---

五十七、统一 API 调用链

最终：

Vue
 ↓
Axios
 ↓
Nginx
 ↓
Spring Security
 ↓
JWT
 ↓
Workspace
 ↓
Permission
 ↓
Controller
 ↓
Application Service
 ↓
Domain
 ↓
Repository
 ↓
Infrastructure
 ↓
MySQL / Redis / ES / MinIO

AI：

Application
 ↓
Agent Runtime
 ↓
LangChain4j
 ↓
Model

---

五十八、V1 第一阶段实际开发顺序

不要一上来开发 Workflow。

建议严格按照：

Phase 01
项目骨架

Phase 02
MySQL + Flyway

Phase 03
用户注册 / 登录

Phase 04
Workspace

Phase 05
RBAC

Phase 06
Agent CRUD

Phase 07
Agent Version

Phase 08
Model Provider

Phase 09
LangChain4j

Phase 10
Agent Runtime

Phase 11
Conversation

Phase 12
SSE Chat

Phase 13
Knowledge

Phase 14
RAG

Phase 15
Tool

Phase 16
Workflow

Phase 17
Workflow Runtime

Phase 18
Trace

Phase 19
Publish

Phase 20
Analytics

---

五十九、第一阶段代码完成标准

完成以下内容：

✓ Maven 多模块
✓ Spring Boot 启动
✓ MySQL
✓ Flyway
✓ Redis
✓ MyBatis-Flex
✓ 全局异常
✓ Result
✓ JWT
✓ Security
✓ User
✓ Workspace
✓ RBAC

然后启动：

./mvnw spring-boot:run

能够：

注册
 ↓
登录
 ↓
获取 JWT
 ↓
创建 Workspace
 ↓
切换 Workspace
 ↓
创建 Agent

才进入下一阶段。

---

六十、第二阶段代码完成标准

Agent：

✓ Agent CRUD
✓ Agent Version
✓ Prompt
✓ Model
✓ Variable
✓ Knowledge Relation
✓ Tool Relation
✓ Draft
✓ Publish

最终可以：

创建 Agent
 ↓
配置 Prompt
 ↓
选择 Model
 ↓
保存 Draft
 ↓
创建 Version
 ↓
Publish

---

六十一、第三阶段 AI 完成标准

接入 LangChain4j：

Agent
 ↓
Published Version
 ↓
Model Config
 ↓
Prompt
 ↓
LangChain4j
 ↓
LLM
 ↓
SSE

实现：

普通聊天
Streaming
Conversation
Token Statistics
Execution
Trace

完成这一步之后：

«Box 才真正从“后台管理系统”变成“AI Agent 平台”。»

---

六十二、第五部分最终架构

后端最终形成：

                         Vue 3
                           │
                           ▼
                         Nginx
                           │
                           ▼
                  Spring Boot 3
                           │
              ┌────────────┴────────────┐
              │                         │
         Security                  Controller
              │                         │
              │                    Application
              │                         │
              └──────────────┬──────────┘
                             ▼
                          Domain
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
          Repository      Runtime       Services
              │              │
              ▼              ▼
        Infrastructure   LangChain4j
              │              │
       ┌──────┼──────┐       ▼
       ▼      ▼      ▼      LLM
     MySQL  Redis   ES
              │
              ▼
            MinIO

---

六十三、第五部分核心结论

Box 后端开发必须坚持：

Controller
    ↓
Application
    ↓
Domain
    ↓
Repository
    ↓
Infrastructure

而 AI Runtime 单独：

Agent
 ↓
Agent Version
 ↓
Agent Runtime
 ↓
LangChain4j
 ↓
LLM

Workflow 单独：

Workflow
 ↓
Workflow Version
 ↓
Workflow Runtime
 ↓
Node Executor

RAG 单独：

Knowledge
 ↓
Document
 ↓
Chunk
 ↓
Embedding
 ↓
Elasticsearch

文件：

MinIO

缓存：

Redis

业务：

MySQL

这样整个 Box 的代码边界就基本确定了。

六十四、第五部分完成后的项目形态

box-server
│
├── common
├── security
├── infrastructure
├── domain
├── application
│
└── modules
    │
    ├── user
    ├── workspace
    ├── tenant
    ├── agent
    ├── model
    ├── knowledge
    ├── tool
    ├── workflow
    ├── conversation
    ├── runtime
    ├── publish
    ├── trace
    └── analytics

核心业务链：

User
 ↓
Workspace
 ↓
Agent
 ↓
Agent Version
 ↓
Runtime
 ↓
LangChain4j
 ↓
LLM

扩展能力：

          Agent Runtime
          /     |      \
         /      |       \
       RAG     Tool    Workflow
        │        │        │
        ▼        ▼        ▼
       ES      HTTP/MCP   Node
                         Executor

最终形成：

«一个真正具备 Agent 创建、配置、运行、编排、RAG、Tool、Workflow、发布和可观测能力的 AI Agent 平台后端。»