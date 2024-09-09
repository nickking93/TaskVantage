package com.taskvantage.backend.service;

import com.taskvantage.backend.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom implementation of UserDetails to provide user-specific authentication data.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    /**
     * Constructor to initialize the CustomUserDetails with a User entity.
     *
     * @param user The User entity representing the authenticated user.
     */
    public CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Returns the authorities granted to the user. In this case, it assigns the default "ROLE_USER".
     *
     * @return Collection of granted authorities (roles) for the user.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * Returns the password of the authenticated user.
     *
     * @return User's password.
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Returns the username of the authenticated user.
     *
     * @return User's username.
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Indicates whether the user's account has expired.
     * This implementation always returns true, meaning the account is never expired.
     *
     * @return true if the account is not expired, otherwise false.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user's account is locked.
     * This implementation always returns true, meaning the account is never locked.
     *
     * @return true if the account is not locked, otherwise false.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) have expired.
     * This implementation always returns true, meaning the credentials are never expired.
     *
     * @return true if the credentials are not expired, otherwise false.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled.
     * This implementation always returns true, meaning the user is always enabled.
     *
     * @return true if the user is enabled, otherwise false.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Returns the underlying User entity associated with the authenticated user.
     *
     * @return The User entity.
     */
    public User getUser() {
        return user;
    }
}
