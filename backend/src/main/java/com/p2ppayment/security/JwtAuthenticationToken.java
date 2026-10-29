package com.p2ppayment.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String email;
    private final String role;
    private final Long userId;

    public JwtAuthenticationToken(String email, String role, Long userId) {
        super(Collections.singleton(new SimpleGrantedAuthority(role)));
        this.email = email;
        this.role = role;
        this.userId = userId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return email;
    }

    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Long getUserId() { return userId; }
}