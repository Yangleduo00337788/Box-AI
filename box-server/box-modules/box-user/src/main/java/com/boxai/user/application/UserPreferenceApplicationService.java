package com.boxai.user.application;

import com.boxai.domain.user.UserPreference;
import com.boxai.domain.user.UserPreferenceRepository;
import com.boxai.user.api.UpdateUserPreferenceRequest;
import com.boxai.user.api.UserPreferenceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceApplicationService {

    private final UserPreferenceRepository userPreferenceRepository;

    public UserPreferenceApplicationService(UserPreferenceRepository userPreferenceRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
    }

    public UserPreferenceVO get(Long userId) {
        return toVO(userPreferenceRepository.findByUserId(userId).orElseGet(() -> createDefault(userId)));
    }

    @Transactional
    public UserPreferenceVO update(Long userId, UpdateUserPreferenceRequest request) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId).orElseGet(() -> createDefault(userId));
        if (request.theme() != null) {
            preference.setTheme(request.theme());
        }
        if (request.sendWithEnter() != null) {
            preference.setSendWithEnter(request.sendWithEnter());
        }
        userPreferenceRepository.update(preference);
        return toVO(preference);
    }

    private UserPreference createDefault(Long userId) {
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setTheme("light");
        preference.setSendWithEnter(true);
        return userPreferenceRepository.save(preference);
    }

    private UserPreferenceVO toVO(UserPreference preference) {
        return new UserPreferenceVO(preference.getTheme(), preference.getSendWithEnter());
    }
}
