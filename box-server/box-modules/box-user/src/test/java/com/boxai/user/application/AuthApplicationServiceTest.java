package com.boxai.user.application;

import com.boxai.common.constant.TenantTypes;
import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.jwt.JwtService;
import com.boxai.security.ratelimit.RateLimitService;
import com.boxai.tenant.api.TenantVO;
import com.boxai.tenant.application.TenantApplicationService;
import com.boxai.user.api.AuthVO;
import com.boxai.user.api.ChangePasswordRequest;
import com.boxai.user.api.LoginRequest;
import com.boxai.user.api.RegisterRequest;
import com.boxai.workspace.api.WorkspaceDetailVO;
import com.boxai.workspace.application.WorkspaceApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceApplicationService workspaceApplicationService;
    @Mock
    private TenantApplicationService tenantApplicationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private VerificationCodeService verificationCodeService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private UserPreferenceApplicationService userPreferenceApplicationService;
    @Mock
    private UserSessionApplicationService userSessionApplicationService;

    @InjectMocks
    private AuthApplicationService service;

    @Test
    void registerRejectsEnterpriseWithoutCompanyName() {
        RegisterRequest request = new RegisterRequest(
                "a@example.com", "password1", "123456", null, TenantTypes.ENTERPRISE, "  ", null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "A@Example.com", "password1", "123456", "Nick", TenantTypes.PERSONAL, null, null);
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(new User()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(request));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getCode());
        verify(verificationCodeService).verify("a@example.com",
                com.boxai.user.support.VerificationCodePurpose.REGISTER, "123456");
    }

    @Test
    void loginRejectsPlatformAdminOnConsumerPortal() {
        User user = user(1L, UserTypes.PLATFORM_ADMIN, 1);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(
                new LoginRequest("admin@example.com", "secret", TenantTypes.PERSONAL)));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void loginRejectsDisabledAccount() {
        User user = user(2L, UserTypes.TENANT_USER, 0);
        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(
                new LoginRequest("disabled@example.com", "secret", TenantTypes.PERSONAL)));
        assertEquals(ErrorCode.USER_DISABLED, ex.getCode());
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = user(3L, UserTypes.TENANT_USER, 1);
        user.setPasswordHash("hash");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(
                new LoginRequest("user@example.com", "wrong", TenantTypes.PERSONAL)));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getCode());
    }

    @Test
    void loginRejectsEnterpriseAccountOnPersonalPortal() {
        User user = user(4L, UserTypes.TENANT_USER, 1);
        user.setPasswordHash("hash");
        when(userRepository.findByEmail("corp@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(tenantApplicationService.findPrimaryByUserId(4L)).thenReturn(tenant(TenantTypes.ENTERPRISE));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(
                new LoginRequest("corp@example.com", "secret", TenantTypes.PERSONAL)));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void loginIssuesTokenForMatchingAccountType() {
        User user = user(5L, UserTypes.TENANT_USER, 1);
        user.setPasswordHash("hash");
        user.setUsername("ok@example.com");
        user.setEmail("ok@example.com");
        user.setNickname("Ok");
        when(userRepository.findByEmail("ok@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(tenantApplicationService.findPrimaryByUserId(5L)).thenReturn(tenant(TenantTypes.PERSONAL));
        when(userSessionApplicationService.createSession(5L)).thenReturn("sess-1");
        when(jwtService.generate(5L, "ok@example.com", UserTypes.TENANT_USER, "sess-1")).thenReturn("jwt-token");
        when(workspaceApplicationService.listMineByUserId(5L)).thenReturn(List.of(
                new WorkspaceDetailVO(9L, "Default", "default", null, null, 1, "OWNER")));
        when(userPreferenceApplicationService.getCurrentWorkspaceId(5L)).thenReturn(9L);

        AuthVO auth = service.login(new LoginRequest("ok@example.com", "secret", TenantTypes.PERSONAL));

        assertEquals("jwt-token", auth.token());
        assertEquals(9L, auth.currentWorkspaceId());
        assertEquals(5L, auth.user().id());
        verify(userRepository).updateLastLogin(5L);
        verify(rateLimitService).assertAllowed(anyString(), anyString(), anyInt(), any());
    }

    @Test
    void updatePasswordRejectsWrongOldPassword() {
        User user = user(5L, UserTypes.TENANT_USER, 1);
        user.setPasswordHash("old-hash");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updatePassword(5L, new ChangePasswordRequest("wrong", "newpass1")));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getCode());
        verify(userRepository, never()).updatePasswordHash(any(), anyString());
    }

    @Test
    void updatePasswordRejectsSamePassword() {
        User user = user(5L, UserTypes.TENANT_USER, 1);
        user.setPasswordHash("old-hash");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldpass", "old-hash")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updatePassword(5L, new ChangePasswordRequest("oldpass", "oldpass")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void updatePasswordPersistsEncodedHash() {
        User user = user(5L, UserTypes.TENANT_USER, 1);
        user.setPasswordHash("old-hash");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldpass", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("newpass1", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("newpass1")).thenReturn("new-hash");

        service.updatePassword(5L, new ChangePasswordRequest("oldpass", "newpass1"));

        verify(userRepository).updatePasswordHash(5L, "new-hash");
    }

    private static User user(Long id, String userType, int status) {
        User user = new User();
        user.setId(id);
        user.setUserType(userType);
        user.setStatus(status);
        user.setEmail("user@example.com");
        user.setUsername("user@example.com");
        return user;
    }

    private static TenantVO tenant(String type) {
        return new TenantVO(1L, "T", "t", type, 1L, "plan", null, 1, 1L, null);
    }
}
