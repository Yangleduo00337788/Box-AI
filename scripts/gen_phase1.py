# -*- coding: utf-8 -*-
from pathlib import Path

ROOT = Path(r"d:\yangleduo\Code\Java\AgentX\box-server")


def write(rel: str, content: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


def module_pom(artifact: str, deps: list[str], extra: str = "") -> str:
    dep_xml = []
    for d in deps:
        dep_xml.append(f"""        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>{d}</artifactId>
        </dependency>""")
    extra_xml = extra
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.boxai</groupId>
        <artifactId>box-server</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        {"<relativePath>../../pom.xml</relativePath>" if artifact.startswith("box-") and artifact not in {"box-common","box-security","box-infrastructure","box-domain","box-application","box-bootstrap"} else "<relativePath>../pom.xml</relativePath>"}
    </parent>
    <artifactId>{artifact}</artifactId>
    <name>{artifact}</name>
    <dependencies>
{chr(10).join(dep_xml)}
{extra_xml}
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
"""


# ---------- common ----------
write("box-common/pom.xml", module_pom("box-common", [], """
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>
"""))

write("box-common/src/main/java/com/boxai/common/result/Result.java", """
package com.boxai.common.result;

public record Result<T>(Integer code, String message, T data) {

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> failure(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
""")

write("box-common/src/main/java/com/boxai/common/result/PageQuery.java", """
package com.boxai.common.result;

public record PageQuery(Integer page, Integer pageSize) {

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

    public int offset() {
        return (page - 1) * pageSize;
    }
}
""")

write("box-common/src/main/java/com/boxai/common/result/PageResult.java", """
package com.boxai.common.result;

import java.util.List;

public record PageResult<T>(List<T> records, long total, int page, int pageSize) {}
""")

write("box-common/src/main/java/com/boxai/common/exception/ErrorCode.java", """
package com.boxai.common.exception;

public interface ErrorCode {

    int SUCCESS = 0;
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int INTERNAL_ERROR = 500;

    int USER_NOT_FOUND = 9001;
    int USER_DISABLED = 9002;
    int USER_ALREADY_EXISTS = 9003;
    int INVALID_CREDENTIALS = 9004;

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
""")

write("box-common/src/main/java/com/boxai/common/exception/BusinessException.java", """
package com.boxai.common.exception;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
""")

write("box-common/src/main/java/com/boxai/common/constant/HeaderNames.java", """
package com.boxai.common.constant;

public final class HeaderNames {

    public static final String WORKSPACE_ID = "X-Workspace-Id";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private HeaderNames() {}
}
""")

write("box-common/src/main/java/com/boxai/common/constant/RoleCodes.java", """
package com.boxai.common.constant;

public final class RoleCodes {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String TENANT_ADMIN = "TENANT_ADMIN";
    public static final String DEVELOPER = "DEVELOPER";
    public static final String MEMBER = "MEMBER";

    private RoleCodes() {}
}
""")

# ---------- domain ----------
write("box-domain/pom.xml", module_pom("box-domain", ["box-common"]))

write("box-domain/src/main/java/com/boxai/domain/user/User.java", """
package com.boxai.domain.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class User {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime lastLoginAt;
}
""")

write("box-domain/src/main/java/com/boxai/domain/user/UserRepository.java", """
package com.boxai.domain.user;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    User save(User user);

    void updateLastLogin(Long userId);
}
""")

write("box-domain/src/main/java/com/boxai/domain/workspace/Workspace.java", """
package com.boxai.domain.workspace;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Workspace {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String avatarUrl;
    private Long ownerId;
    private Integer status;
}
""")

write("box-domain/src/main/java/com/boxai/domain/workspace/WorkspaceMember.java", """
package com.boxai.domain.workspace;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceMember {

    private Long id;
    private Long workspaceId;
    private Long userId;
    private Long roleId;
    private Integer status;
    private String roleCode;
    private String workspaceName;
    private String workspaceSlug;
}
""")

write("box-domain/src/main/java/com/boxai/domain/workspace/WorkspaceRepository.java", """
package com.boxai.domain.workspace;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository {

    Workspace save(Workspace workspace);

    Optional<Workspace> findById(Long id);

    Optional<Workspace> findBySlug(String slug);

    List<WorkspaceMember> listMembersByUserId(Long userId);

    WorkspaceMember addMember(WorkspaceMember member);

    Optional<WorkspaceMember> findMember(Long workspaceId, Long userId);
}
""")

write("box-domain/src/main/java/com/boxai/domain/rbac/Role.java", """
package com.boxai.domain.rbac;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Role {

    private Long id;
    private Long workspaceId;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer builtIn;
}
""")

write("box-domain/src/main/java/com/boxai/domain/rbac/RoleRepository.java", """
package com.boxai.domain.rbac;

import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findByWorkspaceAndCode(Long workspaceId, String roleCode);

    Role save(Role role);
}
""")

write("box-domain/src/main/java/com/boxai/ai/LlmProvider.java", """
package com.boxai.ai;

public interface LlmProvider {

    String providerName();

    boolean available();
}
""")

# ---------- application ----------
write("box-application/pom.xml", """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.boxai</groupId>
        <artifactId>box-server</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>box-application</artifactId>
    <name>box-application</name>
    <dependencies>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
""")

write("box-application/src/main/java/com/boxai/application/package-info.java", """
package com.boxai.application;
""")

# ---------- security ----------
write("box-security/pom.xml", """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.boxai</groupId>
        <artifactId>box-server</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>box-security</artifactId>
    <name>box-security</name>
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
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
""")

write("box-security/src/main/java/com/boxai/security/jwt/JwtProperties.java", """
package com.boxai.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.security.jwt")
public class JwtProperties {

    private String secret = "box-dev-jwt-secret-change-me-please-32b";
    private long expireSeconds = 86400;
}
""")

write("box-security/src/main/java/com/boxai/security/jwt/JwtService.java", """
package com.boxai.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generate(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getExpireSeconds())))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey key() {
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
""")

write("box-security/src/main/java/com/boxai/security/context/LoginUser.java", """
package com.boxai.security.context;

public record LoginUser(Long userId, String username) {}
""")

write("box-security/src/main/java/com/boxai/security/context/SecurityContexts.java", """
package com.boxai.security.context;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContexts {

    private SecurityContexts() {}

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return user;
    }
}
""")

write("box-security/src/main/java/com/boxai/security/context/WorkspaceContext.java", """
package com.boxai.security.context;

public record WorkspaceContext(Long workspaceId, Long userId, Long roleId, String roleCode) {

    private static final ThreadLocal<WorkspaceContext> HOLDER = new ThreadLocal<>();

    public static void set(WorkspaceContext context) {
        HOLDER.set(context);
    }

    public static WorkspaceContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
""")

write("box-security/src/main/java/com/boxai/security/filter/JwtAuthenticationFilter.java", """
package com.boxai.security.filter;

import com.boxai.common.constant.HeaderNames;
import com.boxai.security.context.LoginUser;
import com.boxai.security.jwt.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HeaderNames.AUTHORIZATION);
        if (header != null && header.startsWith(HeaderNames.BEARER_PREFIX)) {
            String token = header.substring(HeaderNames.BEARER_PREFIX.length());
            try {
                var claims = jwtService.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                var authentication = new UsernamePasswordAuthenticationToken(
                        new LoginUser(userId, username), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
""")

write("box-security/src/main/java/com/boxai/security/interceptor/WorkspaceInterceptor.java", """
package com.boxai.security.interceptor;

import com.boxai.common.constant.HeaderNames;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.SecurityContexts;
import com.boxai.security.context.WorkspaceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WorkspaceInterceptor implements HandlerInterceptor {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceInterceptor(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String workspaceIdHeader = request.getHeader(HeaderNames.WORKSPACE_ID);
        if (workspaceIdHeader == null || workspaceIdHeader.isBlank()) {
            return true;
        }
        LoginUser user = SecurityContexts.currentUser();
        Long workspaceId = Long.valueOf(workspaceIdHeader);
        var member = workspaceRepository.findMember(workspaceId, user.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工作空间"));
        if (member.getStatus() == null || member.getStatus() != 1) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "工作空间成员已禁用");
        }
        WorkspaceContext.set(new WorkspaceContext(workspaceId, user.userId(), member.getRoleId(), member.getRoleCode()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        WorkspaceContext.clear();
    }
}
""")

write("box-security/src/main/java/com/boxai/security/config/SecurityConfig.java", """
package com.boxai.security.config;

import com.boxai.security.filter.JwtAuthenticationFilter;
import com.boxai.security.jwt.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.boxai.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/system/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, e) ->
                        write(response, objectMapper, 401, Result.failure(401, "未登录"))))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void write(HttpServletResponse response, ObjectMapper objectMapper, int status, Object body) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
""")

write("box-security/src/main/java/com/boxai/security/config/WebMvcConfig.java", """
package com.boxai.security.config;

import com.boxai.security.interceptor.WorkspaceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final WorkspaceInterceptor workspaceInterceptor;

    public WebMvcConfig(WorkspaceInterceptor workspaceInterceptor) {
        this.workspaceInterceptor = workspaceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(workspaceInterceptor).addPathPatterns("/api/v1/**");
    }
}
""")

# ---------- infrastructure ----------
write("box-infrastructure/pom.xml", """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.boxai</groupId>
        <artifactId>box-server</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>box-infrastructure</artifactId>
    <name>box-infrastructure</name>
    <dependencies>
        <dependency>
            <groupId>com.boxai</groupId>
            <artifactId>box-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mybatis-flex</groupId>
            <artifactId>mybatis-flex-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
""")

infra_java = {
"src/main/java/com/boxai/infrastructure/persistence/entity/UserDO.java": """
package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("sys_user")
public class UserDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String username;
    private String email;
    private String phone;
    @Column("password_hash")
    private String passwordHash;
    private String nickname;
    @Column("avatar_url")
    private String avatarUrl;
    private Integer status;
    @Column("last_login_at")
    private LocalDateTime lastLoginAt;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("created_by")
    private Long createdBy;
    @Column("updated_by")
    private Long updatedBy;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
""",
"src/main/java/com/boxai/infrastructure/persistence/entity/WorkspaceDO.java": """
package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workspace")
public class WorkspaceDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String name;
    private String slug;
    private String description;
    @Column("avatar_url")
    private String avatarUrl;
    @Column("owner_id")
    private Long ownerId;
    private Integer status;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("created_by")
    private Long createdBy;
    @Column("updated_by")
    private Long updatedBy;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
""",
"src/main/java/com/boxai/infrastructure/persistence/entity/WorkspaceMemberDO.java": """
package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workspace_member")
public class WorkspaceMemberDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("user_id")
    private Long userId;
    @Column("role_id")
    private Long roleId;
    private Integer status;
    @Column("joined_at")
    private LocalDateTime joinedAt;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("created_by")
    private Long createdBy;
    @Column("updated_by")
    private Long updatedBy;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
""",
"src/main/java/com/boxai/infrastructure/persistence/entity/RoleDO.java": """
package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("sys_role")
public class RoleDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("role_code")
    private String roleCode;
    @Column("role_name")
    private String roleName;
    private String description;
    @Column("built_in")
    private Integer builtIn;
    private Integer status;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
""",
}

for rel, content in infra_java.items():
    write("box-infrastructure/" + rel, content)

write("box-infrastructure/src/main/java/com/boxai/infrastructure/persistence/mapper/UserMapper.java", """
package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.UserDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/persistence/mapper/WorkspaceMapper.java", """
package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.WorkspaceDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkspaceMapper extends BaseMapper<WorkspaceDO> {}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/persistence/mapper/WorkspaceMemberMapper.java", """
package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.WorkspaceMemberDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkspaceMemberMapper extends BaseMapper<WorkspaceMemberDO> {}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/persistence/mapper/RoleMapper.java", """
package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.RoleDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/persistence/repository/UserRepositoryImpl.java", """
package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.infrastructure.persistence.entity.UserDO;
import com.boxai.infrastructure.persistence.mapper.UserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        UserDO row = userMapper.selectOneByQuery(QueryWrapper.create().eq("email", email));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserDO row = userMapper.selectOneByQuery(QueryWrapper.create().eq("username", username));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserDO row = new UserDO();
        row.setUsername(user.getUsername());
        row.setEmail(user.getEmail());
        row.setPhone(user.getPhone());
        row.setPasswordHash(user.getPasswordHash());
        row.setNickname(user.getNickname());
        row.setAvatarUrl(user.getAvatarUrl());
        row.setStatus(user.getStatus() == null ? 1 : user.getStatus());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        userMapper.insert(row);
        user.setId(row.getId());
        return user;
    }

    @Override
    public void updateLastLogin(Long userId) {
        UserDO patch = new UserDO();
        patch.setId(userId);
        patch.setLastLoginAt(LocalDateTime.now());
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.update(patch);
    }

    private User toDomain(UserDO row) {
        User user = new User();
        user.setId(row.getId());
        user.setUsername(row.getUsername());
        user.setEmail(row.getEmail());
        user.setPhone(row.getPhone());
        user.setPasswordHash(row.getPasswordHash());
        user.setNickname(row.getNickname());
        user.setAvatarUrl(row.getAvatarUrl());
        user.setStatus(row.getStatus());
        user.setLastLoginAt(row.getLastLoginAt());
        return user;
    }
}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/persistence/repository/WorkspaceRepositoryImpl.java", """
package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.infrastructure.persistence.entity.RoleDO;
import com.boxai.infrastructure.persistence.entity.WorkspaceDO;
import com.boxai.infrastructure.persistence.entity.WorkspaceMemberDO;
import com.boxai.infrastructure.persistence.mapper.RoleMapper;
import com.boxai.infrastructure.persistence.mapper.WorkspaceMapper;
import com.boxai.infrastructure.persistence.mapper.WorkspaceMemberMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkspaceRepositoryImpl implements WorkspaceRepository {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper memberMapper;
    private final RoleMapper roleMapper;

    public WorkspaceRepositoryImpl(WorkspaceMapper workspaceMapper, WorkspaceMemberMapper memberMapper, RoleMapper roleMapper) {
        this.workspaceMapper = workspaceMapper;
        this.memberMapper = memberMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public Workspace save(Workspace workspace) {
        WorkspaceDO row = new WorkspaceDO();
        row.setName(workspace.getName());
        row.setSlug(workspace.getSlug());
        row.setDescription(workspace.getDescription());
        row.setAvatarUrl(workspace.getAvatarUrl());
        row.setOwnerId(workspace.getOwnerId());
        row.setStatus(workspace.getStatus() == null ? 1 : workspace.getStatus());
        row.setCreatedBy(workspace.getOwnerId());
        row.setUpdatedBy(workspace.getOwnerId());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        workspaceMapper.insert(row);
        workspace.setId(row.getId());
        return workspace;
    }

    @Override
    public Optional<Workspace> findById(Long id) {
        return Optional.ofNullable(workspaceMapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Workspace> findBySlug(String slug) {
        WorkspaceDO row = workspaceMapper.selectOneByQuery(QueryWrapper.create().eq("slug", slug));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public List<WorkspaceMember> listMembersByUserId(Long userId) {
        List<WorkspaceMemberDO> rows = memberMapper.selectListByQuery(QueryWrapper.create().eq("user_id", userId).eq("status", 1));
        List<WorkspaceMember> result = new ArrayList<>();
        for (WorkspaceMemberDO row : rows) {
            WorkspaceMember member = toMember(row);
            workspaceMapper.selectOneById(row.getWorkspaceId());
            Optional.ofNullable(workspaceMapper.selectOneById(row.getWorkspaceId())).ifPresent(ws -> {
                member.setWorkspaceName(ws.getName());
                member.setWorkspaceSlug(ws.getSlug());
            });
            Optional.ofNullable(roleMapper.selectOneById(row.getRoleId())).ifPresent(role -> member.setRoleCode(role.getRoleCode()));
            result.add(member);
        }
        return result;
    }

    @Override
    public WorkspaceMember addMember(WorkspaceMember member) {
        WorkspaceMemberDO row = new WorkspaceMemberDO();
        row.setWorkspaceId(member.getWorkspaceId());
        row.setUserId(member.getUserId());
        row.setRoleId(member.getRoleId());
        row.setStatus(1);
        row.setJoinedAt(LocalDateTime.now());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setCreatedBy(member.getUserId());
        row.setUpdatedBy(member.getUserId());
        row.setDeleted(0);
        memberMapper.insert(row);
        member.setId(row.getId());
        return member;
    }

    @Override
    public Optional<WorkspaceMember> findMember(Long workspaceId, Long userId) {
        WorkspaceMemberDO row = memberMapper.selectOneByQuery(
                QueryWrapper.create().eq("workspace_id", workspaceId).eq("user_id", userId));
        if (row == null) {
            return Optional.empty();
        }
        WorkspaceMember member = toMember(row);
        RoleDO role = roleMapper.selectOneById(row.getRoleId());
        if (role != null) {
            member.setRoleCode(role.getRoleCode());
        }
        return Optional.of(member);
    }

    private Workspace toDomain(WorkspaceDO row) {
        Workspace workspace = new Workspace();
        workspace.setId(row.getId());
        workspace.setName(row.getName());
        workspace.setSlug(row.getSlug());
        workspace.setDescription(row.getDescription());
        workspace.setAvatarUrl(row.getAvatarUrl());
        workspace.setOwnerId(row.getOwnerId());
        workspace.setStatus(row.getStatus());
        return workspace;
    }

    private WorkspaceMember toMember(WorkspaceMemberDO row) {
        WorkspaceMember member = new WorkspaceMember();
        member.setId(row.getId());
        member.setWorkspaceId(row.getWorkspaceId());
        member.setUserId(row.getUserId());
        member.setRoleId(row.getRoleId());
        member.setStatus(row.getStatus());
        return member;
    }
}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/persistence/repository/RoleRepositoryImpl.java", """
package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.infrastructure.persistence.entity.RoleDO;
import com.boxai.infrastructure.persistence.mapper.RoleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;

    public RoleRepositoryImpl(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public Optional<Role> findByWorkspaceAndCode(Long workspaceId, String roleCode) {
        RoleDO row = roleMapper.selectOneByQuery(
                QueryWrapper.create().eq("workspace_id", workspaceId).eq("role_code", roleCode));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public Role save(Role role) {
        RoleDO row = new RoleDO();
        row.setWorkspaceId(role.getWorkspaceId());
        row.setRoleCode(role.getRoleCode());
        row.setRoleName(role.getRoleName());
        row.setDescription(role.getDescription());
        row.setBuiltIn(role.getBuiltIn() == null ? 1 : role.getBuiltIn());
        row.setStatus(1);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        roleMapper.insert(row);
        role.setId(row.getId());
        return role;
    }

    private Role toDomain(RoleDO row) {
        Role role = new Role();
        role.setId(row.getId());
        role.setWorkspaceId(row.getWorkspaceId());
        role.setRoleCode(row.getRoleCode());
        role.setRoleName(row.getRoleName());
        role.setDescription(row.getDescription());
        role.setBuiltIn(row.getBuiltIn());
        return role;
    }
}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/redis/RedisService.java", """
package com.boxai.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean ping() {
        String pong = redisTemplate.getConnectionFactory().getConnection().ping();
        return pong != null;
    }
}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/minio/MinioProperties.java", """
package com.boxai.infrastructure.minio;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.minio")
public class MinioProperties {

    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey = "box";
    private String secretKey = "boxsecret1";
    private String bucket = "box";
}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/minio/MinioConfig.java", """
package com.boxai.infrastructure.minio;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/minio/MinioService.java", """
package com.boxai.infrastructure.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.stereotype.Service;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public boolean ping() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/elasticsearch/ElasticsearchProperties.java", """
package com.boxai.infrastructure.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.elasticsearch")
public class ElasticsearchProperties {

    private String host = "127.0.0.1";
    private int port = 9200;
}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/elasticsearch/ElasticsearchService.java", """
package com.boxai.infrastructure.elasticsearch;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchService {

    private final ElasticsearchProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public ElasticsearchService(ElasticsearchProperties properties) {
        this.properties = properties;
    }

    public boolean ping() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + properties.getHost() + ":" + properties.getPort()))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }
}
""")

write("box-infrastructure/src/main/java/com/boxai/infrastructure/ai/OpenAiCompatibleLlmProvider.java", """
package com.boxai.infrastructure.ai;

import com.boxai.ai.LlmProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleLlmProvider implements LlmProvider {

    private final String apiKey;

    public OpenAiCompatibleLlmProvider(@Value("${box.ai.openai.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String providerName() {
        return "openai-compatible";
    }

    @Override
    public boolean available() {
        return apiKey != null && !apiKey.isBlank();
    }
}
""")

print("core modules written")
