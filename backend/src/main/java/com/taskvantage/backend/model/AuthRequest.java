package com.taskvantage.backend.model;

public class AuthRequest {

    private String username;
    private String password;
    private String fcmToken;
    private Boolean isPwa;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public Boolean getIsPwa() {
        return isPwa;
    }

    public void setIsPwa(Boolean isPwa) {
        this.isPwa = isPwa;
    }
}
