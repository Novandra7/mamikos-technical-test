package com.mamikos.kostapi.auth.security;

import com.mamikos.kostapi.user.entity.User;
import com.mamikos.kostapi.user.entity.UserRole;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapts our {@link User} to what Spring Security expects, and carries the numeric user id
 * (and role, so services do not need a fresh database read just to branch on it) through
 * the security context.
 */
public class SecurityUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final UserRole role;
    private final List<GrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole().authority()));
    }

    public Long id() {
        return id;
    }

    public UserRole role() {
        return role;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
