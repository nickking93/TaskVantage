package com.taskvantage.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // Field for storing FCM token for push notifications
    @Column(nullable = true)
    private String token;

    // Field to track if the user's email is verified
    @Column(nullable = false)
    private boolean emailVerified = false;

    // Token used for email verification
    @Column(nullable = true, unique = true)
    private String verificationToken;

    // Expiry time for the verification token
    @Column(nullable = true)
    private LocalDateTime tokenExpiry;

    // Field for storing password reset token
    @Column(nullable = true, unique = true)
    private String passwordResetToken;

    // Expiry time for the password reset token
    @Column(nullable = true)
    private LocalDateTime passwordResetTokenExpiry;

    public String getGoogleAccessToken() {
        return googleAccessToken;
    }

    // New field for storing Google account email
    @Column(nullable = true)
    private String googleEmail;

    @Column(nullable = false)
    private boolean taskSyncEnabled = false; // Default value can be set as needed

    @Column(nullable = false)
    private boolean isAdmin = false; // Admin flag for privileged operations

    @Column(nullable = true)
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getGoogleEmail() {
        return googleEmail;
    }

    public void setGoogleEmail(String googleEmail) {
        this.googleEmail = googleEmail;
    }

    // Getter and Setter for taskSyncEnabled
    public boolean isTaskSyncEnabled() {
        return taskSyncEnabled;
    }

    public void setTaskSyncEnabled(boolean taskSyncEnabled) {
        this.taskSyncEnabled = taskSyncEnabled;
    }

    // Getter and Setter for Google Access Token
    public void setGoogleAccessToken(String googleAccessToken) {
        this.googleAccessToken = googleAccessToken;
    }

    // Getter and Setter for Google Refresh Token
    public String getGoogleRefreshToken() {
        return googleRefreshToken;
    }

    public void setGoogleRefreshToken(String googleRefreshToken) {
        this.googleRefreshToken = googleRefreshToken;
    }

    // Google Calendar tokens
    @Column(nullable = true)
    private String googleAccessToken;

    @Column(nullable = true)
    private String googleRefreshToken;

    // Outlook Calendar tokens
    @Column(nullable = true)
    private String outlookAccessToken;

    @Column(nullable = true)
    private String outlookRefreshToken;

    // Apple Calendar tokens
    @Column(nullable = true)
    private String appleAccessToken;

    @Column(nullable = true)
    private String appleRefreshToken;

    // Getter and Setter for id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Getter and Setter for username
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // Getter and Setter for password
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Getter and Setter for FCM token
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // Getter and Setter for emailVerified
    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    // Getter and Setter for verificationToken
    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    // Getter and Setter for tokenExpiry
    public LocalDateTime getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(LocalDateTime tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    // Getter and Setter for passwordResetToken
    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    // Getter and Setter for passwordResetTokenExpiry
    public LocalDateTime getPasswordResetTokenExpiry() {
        return passwordResetTokenExpiry;
    }

    public void setPasswordResetTokenExpiry(LocalDateTime passwordResetTokenExpiry) {
        this.passwordResetTokenExpiry = passwordResetTokenExpiry;
    }

    // Getter and Setter for isAdmin
    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}