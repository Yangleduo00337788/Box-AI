package com.boxai.infrastructure.persistence.repository;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.constant.RoleCodes;
import com.boxai.infrastructure.persistence.entity.PermissionDO;
import com.boxai.infrastructure.persistence.entity.RoleDO;
import com.boxai.infrastructure.persistence.mapper.PermissionMapper;
import com.boxai.infrastructure.persistence.mapper.RoleMapper;
import com.boxai.infrastructure.persistence.mapper.RolePermissionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionQueryImplTest {

    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;
    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RolePermissionQueryImpl query;

    @Test
    void builtInRoleDoesNotReadRolePermissionRows() {
        RoleDO role = new RoleDO();
        role.setId(8L);
        role.setRoleCode(RoleCodes.DEVELOPER);
        role.setBuiltIn(1);
        when(roleMapper.selectOneById(8L)).thenReturn(role);

        List<String> codes = query.listPermissionCodes(8L);

        assertTrue(codes.contains(PermissionCodes.AGENT_PUBLISH));
        verify(rolePermissionMapper, never()).selectListByQuery(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tenantAdminUsesAllPermissionCodes() {
        RoleDO role = new RoleDO();
        role.setId(1L);
        role.setRoleCode(RoleCodes.TENANT_ADMIN);
        role.setBuiltIn(1);
        when(roleMapper.selectOneById(1L)).thenReturn(role);
        PermissionDO permission = new PermissionDO();
        permission.setPermissionCode(PermissionCodes.ROLE_MANAGE);
        when(permissionMapper.selectAll()).thenReturn(List.of(permission));

        assertEquals(List.of(PermissionCodes.ROLE_MANAGE), query.listPermissionCodes(1L));
    }
}
