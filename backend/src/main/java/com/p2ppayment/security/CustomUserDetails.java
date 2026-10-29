package com.p2ppayment.security;

import com.p2ppayment.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public Long getId() { return user.getId(); }
    public String getPhoneNumber() { return user.getPhoneNumber(); }
    public String getName() { return user.getName(); }
    public String getRole() { return user.getRole().name(); }
    public boolean isEnabled() { return user.getEnabled(); }
    public boolean isAccountLocked() { return user.getAccountLocked(); }
    public java.time.LocalDateTime getLockedUntil() { return user.getLockedUntil(); }
}