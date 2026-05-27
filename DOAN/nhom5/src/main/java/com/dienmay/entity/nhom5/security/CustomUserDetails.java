package com.dienmay.entity.nhom5.security;

import com.dienmay.entity.nhom5.entity.Role;
import com.dienmay.entity.nhom5.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public static CustomUserDetails fromUser(User user) {
        return new CustomUserDetails(user);
    }

    public User getUser() {
        return user;
    }

    public String getUid() {
        return user.getUid();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public Role getRole() {
        return user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role role = user.getRole() == null ? Role.CUSTOMER : user.getRole();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.getUid();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getIsActive() == null || user.getIsActive();
    }
}
