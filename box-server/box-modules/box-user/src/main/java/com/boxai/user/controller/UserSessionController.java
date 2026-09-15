package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.security.jwt.JwtService;
import com.boxai.user.api.UserSessionVO;
import com.boxai.user.application.UserSessionApplicationService;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/sessions")
public class UserSessionController {

    private final UserSessionApplicationService userSessionApplicationService;
    private final JwtService jwtService;

    public UserSessionController(UserSessionApplicationService userSessionApplicationService, JwtService jwtService) {
        this.userSessionApplicationService = userSessionApplicationService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public Result<List<UserSessionVO>> list(@RequestHeader("Authorization") String authorization) {
        Long userId = SecurityContexts.currentUser().userId();
        String currentSessionId = extractSessionId(authorization);
        return Result.success(userSessionApplicationService.listSessions(userId, currentSessionId));
    }

    @DeleteMapping("/{sessionId}")
    public Result<Void> revoke(@PathVariable String sessionId) {
        userSessionApplicationService.revokeSession(SecurityContexts.currentUser().userId(), sessionId);
        return Result.success(null);
    }

    private String extractSessionId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            Claims claims = jwtService.parse(authorization.substring(7));
            Object sid = claims.get("sid");
            return sid == null ? null : String.valueOf(sid);
        } catch (Exception ex) {
            return null;
        }
    }
}
