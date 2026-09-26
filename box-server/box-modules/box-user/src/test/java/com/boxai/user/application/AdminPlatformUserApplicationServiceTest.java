package com.boxai.user.application;

import com.boxai.common.constant.PlatformAdminRoles;
import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plan.TenantUsageRepository;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.user.api.CreatePlatformAdminRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPlatformUserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private TenantUsageRepository tenantUsageRepository;
    @Mock
    private ExecutionRepository executionRepository;

    @InjectMocks
    private AdminPlatformUserApplicationService service;

    @Test
    void createAdminRejectsDuplicateEmail() {
        when(userRepository.findByEmail("ops@example.com")).thenReturn(Optional.of(new User()));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createAdmin(
                new CreatePlatformAdminRequest("Ops@Example.com", "password1", null, null)));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createAdminRejectsInvalidRole() {
        when(userRepository.findByEmail("ops@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("ops@example.com")).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createAdmin(
                new CreatePlatformAdminRequest("ops@example.com", "password1", "Ops", "HACKER")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void createAdminDefaultsToOpsRole() {
        when(userRepository.findByEmail("ops@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("ops@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1")).thenReturn("hash");

        var vo = service.createAdmin(new CreatePlatformAdminRequest("Ops@Example.com", "password1", "  ", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("ops@example.com", captor.getValue().getEmail());
        assertEquals("ops", captor.getValue().getNickname());
        assertEquals(UserTypes.PLATFORM_ADMIN, captor.getValue().getUserType());
        assertEquals(PlatformAdminRoles.OPS, captor.getValue().getPlatformAdminRole());
        assertEquals("hash", captor.getValue().getPasswordHash());
        assertEquals(PlatformAdminRoles.OPS, vo.platformAdminRole());
    }

    @Test
    void updateStatusRejectsSelfDisable() {
        User user = admin(8L, 1);
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStatus(new LoginUser(8L, "me", UserTypes.PLATFORM_ADMIN), 8L, 0));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(userRepository, never()).updateStatus(anyLong(), anyInt());
    }

    @Test
    void updateStatusRejectsDisablingLastAdmin() {
        User user = admin(9L, 1);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.countByUserTypeAndStatus(UserTypes.PLATFORM_ADMIN, 1)).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStatus(new LoginUser(8L, "me", UserTypes.PLATFORM_ADMIN), 9L, 0));
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
        verify(userRepository, never()).updateStatus(anyLong(), anyInt());
    }

    @Test
    void updateStatusDisablesAdminWhenOthersRemain() {
        User user = admin(9L, 1);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.countByUserTypeAndStatus(UserTypes.PLATFORM_ADMIN, 1)).thenReturn(2L);

        var vo = service.updateStatus(new LoginUser(8L, "me", UserTypes.PLATFORM_ADMIN), 9L, 0);

        verify(userRepository).updateStatus(9L, 0);
        assertEquals(0, vo.status());
    }

    private static User admin(Long id, int status) {
        User user = new User();
        user.setId(id);
        user.setUserType(UserTypes.PLATFORM_ADMIN);
        user.setStatus(status);
        user.setEmail("admin@example.com");
        user.setUsername("admin@example.com");
        return user;
    }
}
