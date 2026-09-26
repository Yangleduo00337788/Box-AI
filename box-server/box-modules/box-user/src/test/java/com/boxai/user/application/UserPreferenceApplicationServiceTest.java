package com.boxai.user.application;

import com.boxai.domain.user.UserPreference;
import com.boxai.domain.user.UserPreferenceRepository;
import com.boxai.user.api.UpdateUserPreferenceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferenceApplicationServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @InjectMocks
    private UserPreferenceApplicationService service;

    @Test
    void getCreatesDefaultPreference() {
        when(userPreferenceRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var vo = service.get(3L);

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(userPreferenceRepository).save(captor.capture());
        assertEquals(3L, captor.getValue().getUserId());
        assertEquals("light", vo.theme());
        assertTrue(vo.sendWithEnter());
    }

    @Test
    void updatePatchesThemeOnly() {
        UserPreference existing = new UserPreference();
        existing.setUserId(3L);
        existing.setTheme("light");
        existing.setSendWithEnter(true);
        when(userPreferenceRepository.findByUserId(3L)).thenReturn(Optional.of(existing));

        var vo = service.update(3L, new UpdateUserPreferenceRequest("dark", null));

        verify(userPreferenceRepository).update(existing);
        assertEquals("dark", vo.theme());
        assertTrue(vo.sendWithEnter());
    }

    @Test
    void setCurrentWorkspaceIdPersistsSelection() {
        when(userPreferenceRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.setCurrentWorkspaceId(3L, 7L);

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(userPreferenceRepository).update(captor.capture());
        assertEquals(7L, captor.getValue().getCurrentWorkspaceId());
    }

    @Test
    void getCurrentWorkspaceIdReturnsNullWhenMissing() {
        when(userPreferenceRepository.findByUserId(3L)).thenReturn(Optional.empty());
        assertNull(service.getCurrentWorkspaceId(3L));
    }
}
