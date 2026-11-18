package com.jianzhang.security;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户身份（Spring Security 主体）
 */
@Data
@NoArgsConstructor
public class LoginUser implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private Long roleId;
    private String roleCode;
    private String roleName;
    /** 账号状态 0禁用 1正常 */
    private Integer status;
    /** 权限标识集合，如 finance:record:add */
    private Set<String> perms;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 角色权限：ROLE_ADMIN / ROLE_FINANCE / ROLE_WAREHOUSE
        if (roleCode != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase()));
        }
        // 按钮级权限标识
        if (perms != null) {
            authorities.addAll(perms.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
        }
        return authorities;
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
        return status != null && status == 1;
    }
}
