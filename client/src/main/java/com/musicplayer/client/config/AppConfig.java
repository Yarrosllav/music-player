package com.musicplayer.client.config;

public class AppConfig {
    private static AppConfig instance;
    private String serverUrl = "http://localhost:8080";
    private Long currentUserId;
    private String currentUsername;
    private String userRole;

    private AppConfig() {}

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    public String getServerUrl() {
        return serverUrl;
    }
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    public Long getCurrentUserId() {
        return currentUserId;
    }
    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }
    public String getCurrentUsername() {
        return currentUsername;
    }
    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }
    public String getUserRole() {
        return userRole;
    }
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
    public boolean isLoggedIn() {
        return currentUserId != null;
    }
    public boolean isAdmin() {
        return "ADMIN".equals(userRole);
    }
}