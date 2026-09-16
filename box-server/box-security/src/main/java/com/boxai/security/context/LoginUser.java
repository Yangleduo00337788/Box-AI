package com.boxai.security.context;

public record LoginUser(Long userId, String username, String userType, String platformAdminRole) {

    public LoginUser(Long userId, String username, String userType) {
        this(userId, username, userType, null);
    }
}
