package com.gatelms.security;

import com.gatelms.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class GateLmsUserDetails implements UserDetails {
    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public GateLmsUserDetails(
        UUID userId,
        String username,
        String passwordHash,
        String displayName,
        String role,
        Collection<? extends GrantedAuthority> authorities
    ) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.authorities = authorities;
    }

    public static GateLmsUserDetails fromUser(User user) {
        String roleName = "ROLE_" + user.getRole().name();
        return new GateLmsUserDetails(
            user.getId() != null ? user.getId() : UUID.fromString("00000000-0000-0000-0000-000000000001"),
            user.getUsername(),
            user.getPasswordHash(),
            user.getDisplayName(),
            user.getRole().name(),
            List.of(new SimpleGrantedAuthority(roleName))
        );
    }

    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
