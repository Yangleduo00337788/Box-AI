# -*- coding: utf-8 -*-
from pathlib import Path

ROOT = Path(r"d:\yangleduo\Code\Java\AgentX\box-server")


def write(rel: str, content: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


NESTED_POM_PARENT = """    <parent>
        <groupId>com.boxai</groupId>
        <artifactId>box-server</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>"""

PLACEHOLDERS = [
    "box-agent", "box-model", "box-knowledge", "box-tool", "box-workflow",
    "box-conversation", "box-runtime", "box-publish", "box-trace", "box-analytics",
]

for name in PLACEHOLDERS:
    pkg = name.replace("box-", "").replace("-", "")
    write(f"box-modules/{name}/pom.xml", f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
{NESTED_POM_PARENT}
    <artifactId>{name}</artifactId>
    <name>{name}</name>
    <dependencies>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-domain</artifactId>
        </dependency>
    </dependencies>
</project>
""")
    write(f"box-modules/{name}/src/main/java/com/boxai/{pkg}/package-info.java", f"package com.boxai.{pkg};")

write("box-modules/box-user/pom.xml", f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
{NESTED_POM_PARENT}
    <artifactId>box-user</artifactId>
    <name>box-user</name>
    <dependencies>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-workspace</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
""")

write("box-modules/box-workspace/pom.xml", f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
{NESTED_POM_PARENT}
    <artifactId>box-workspace</artifactId>
    <name>box-workspace</name>
    <dependencies>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
""")

write("box-modules/box-user/src/main/java/com/boxai/user/api/RegisterRequest.java", """
package com.boxai.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        String nickname
) {}
""")

write("box-modules/box-user/src/main/java/com/boxai/user/api/LoginRequest.java", """
package com.boxai.user.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String account,
        @NotBlank String password
) {}
""")

write("box-modules/box-user/src/main/java/com/boxai/user/api/AuthVO.java", """
package com.boxai.user.api;

import java.util.List;

public record AuthVO(
        String token,
        UserVO user,
        List<WorkspaceVO> workspaces
) {}
""")

write("box-modules/box-user/src/main/java/com/boxai/user/api/UserVO.java", """
package com.boxai.user.api;

public record UserVO(
        Long id,
        String username,
        String email,
        String nickname,
        String avatarUrl
) {}
""")

write("box-modules/box-user/src/main/java/com/boxai/user/api/WorkspaceVO.java", """
package com.boxai.user.api;

public record WorkspaceVO(
        Long id,
        String name,
        String slug,
        String roleCode
) {}
""")

write("box-modules/box-user/src/main/java/com/boxai/user/application/AuthApplicationService.java", """
package com.boxai.user.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.security.jwt.JwtService;
import com.boxai.user.api.AuthVO;
import com.boxai.user.api.LoginRequest;
import com.boxai.user.api.RegisterRequest;
import com.boxai.user.api.UserVO;
import com.boxai.user.api.WorkspaceVO;
import com.boxai.workspace.application.WorkspaceApplicationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceApplicationService workspaceApplicationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthApplicationService(UserRepository userRepository,
                                  WorkspaceRepository workspaceRepository,
                                  WorkspaceApplicationService workspaceApplicationService,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceApplicationService = workspaceApplicationService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthVO register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已注册");
        }
        String username = email;
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() == null || request.nickname().isBlank() ? email.split("@")[0] : request.nickname());
        user.setStatus(1);
        userRepository.save(user);
        workspaceApplicationService.createDefaultWorkspace(user);
        return issue(user);
    }

    public AuthVO login(LoginRequest request) {
        String account = request.account().trim();
        User user = userRepository.findByEmail(account.toLowerCase(Locale.ROOT))
                .or(() -> userRepository.findByUsername(account))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "账号或密码错误"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号已禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "账号或密码错误");
        }
        userRepository.updateLastLogin(user.getId());
        return issue(user);
    }

    public AuthVO me(LoginUser loginUser) {
        User user = userRepository.findById(loginUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        return issue(user);
    }

    private AuthVO issue(User user) {
        String token = jwtService.generate(user.getId(), user.getUsername());
        List<WorkspaceMember> members = workspaceRepository.listMembersByUserId(user.getId());
        List<WorkspaceVO> workspaces = members.stream()
                .map(item -> new WorkspaceVO(item.getWorkspaceId(), item.getWorkspaceName(), item.getWorkspaceSlug(), item.getRoleCode()))
                .toList();
        return new AuthVO(token, new UserVO(user.getId(), user.getUsername(), user.getEmail(), user.getNickname(), user.getAvatarUrl()), workspaces);
    }
}
""")

write("box-modules/box-user/src/main/java/com/boxai/user/controller/AuthController.java", """
package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.user.api.AuthVO;
import com.boxai.user.api.LoginRequest;
import com.boxai.user.api.RegisterRequest;
import com.boxai.user.application.AuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/register")
    public Result<AuthVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authApplicationService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authApplicationService.login(request));
    }

    @GetMapping("/me")
    public Result<AuthVO> me() {
        return Result.success(authApplicationService.me(SecurityContexts.currentUser()));
    }
}
""")

write("box-modules/box-workspace/src/main/java/com/boxai/workspace/api/CreateWorkspaceRequest.java", """
package com.boxai.workspace.api;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(
        @NotBlank String name,
        String description
) {}
""")

write("box-modules/box-workspace/src/main/java/com/boxai/workspace/api/WorkspaceDetailVO.java", """
package com.boxai.workspace.api;

public record WorkspaceDetailVO(
        Long id,
        String name,
        String slug,
        String description,
        String roleCode
) {}
""")

write("box-modules/box-workspace/src/main/java/com/boxai/workspace/application/WorkspaceApplicationService.java", """
package com.boxai.workspace.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.workspace.api.CreateWorkspaceRequest;
import com.boxai.workspace.api.WorkspaceDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceApplicationService {

    private final WorkspaceRepository workspaceRepository;
    private final RoleRepository roleRepository;

    public WorkspaceApplicationService(WorkspaceRepository workspaceRepository, RoleRepository roleRepository) {
        this.workspaceRepository = workspaceRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public Workspace createDefaultWorkspace(User user) {
        return createWorkspace(user.getId(), user.getNickname() + " 的工作空间", "个人默认工作空间");
    }

    @Transactional
    public WorkspaceDetailVO create(LoginUser loginUser, CreateWorkspaceRequest request) {
        Workspace workspace = createWorkspace(loginUser.userId(), request.name(), request.description());
        return new WorkspaceDetailVO(workspace.getId(), workspace.getName(), workspace.getSlug(), workspace.getDescription(), RoleCodes.TENANT_ADMIN);
    }

    public List<WorkspaceDetailVO> listMine(LoginUser loginUser) {
        return workspaceRepository.listMembersByUserId(loginUser.userId()).stream()
                .map(item -> new WorkspaceDetailVO(
                        item.getWorkspaceId(),
                        item.getWorkspaceName(),
                        item.getWorkspaceSlug(),
                        null,
                        item.getRoleCode()))
                .toList();
    }

    private Workspace createWorkspace(Long ownerId, String name, String description) {
        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setSlug(uniqueSlug(name));
        workspace.setDescription(description);
        workspace.setOwnerId(ownerId);
        workspace.setStatus(1);
        workspaceRepository.save(workspace);

        Role admin = saveRole(workspace.getId(), RoleCodes.TENANT_ADMIN, "工作空间管理员");
        saveRole(workspace.getId(), RoleCodes.DEVELOPER, "开发者");
        saveRole(workspace.getId(), RoleCodes.MEMBER, "成员");

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspace.getId());
        member.setUserId(ownerId);
        member.setRoleId(admin.getId());
        workspaceRepository.addMember(member);
        return workspace;
    }

    private Role saveRole(Long workspaceId, String code, String name) {
        Role role = new Role();
        role.setWorkspaceId(workspaceId);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setBuiltIn(1);
        return roleRepository.save(role);
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "workspace";
        }
        String slug = base;
        int i = 1;
        while (workspaceRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + i++;
            if (i > 20) {
                slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return slug;
    }

    public WorkspaceDetailVO requireAccess(Long workspaceId, Long userId) {
        WorkspaceMember member = workspaceRepository.findMember(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工作空间"));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "工作空间不存在"));
        return new WorkspaceDetailVO(workspace.getId(), workspace.getName(), workspace.getSlug(), workspace.getDescription(), member.getRoleCode());
    }
}
""")

write("box-modules/box-workspace/src/main/java/com/boxai/workspace/controller/WorkspaceController.java", """
package com.boxai.workspace.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.workspace.api.CreateWorkspaceRequest;
import com.boxai.workspace.api.WorkspaceDetailVO;
import com.boxai.workspace.application.WorkspaceApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceApplicationService workspaceApplicationService;

    public WorkspaceController(WorkspaceApplicationService workspaceApplicationService) {
        this.workspaceApplicationService = workspaceApplicationService;
    }

    @GetMapping
    public Result<List<WorkspaceDetailVO>> list() {
        return Result.success(workspaceApplicationService.listMine(SecurityContexts.currentUser()));
    }

    @PostMapping
    public Result<WorkspaceDetailVO> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return Result.success(workspaceApplicationService.create(SecurityContexts.currentUser(), request));
    }
}
""")

print("user/workspace/placeholders written")
