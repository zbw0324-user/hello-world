package com.jianzhang.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianzhang.entity.SysMenu;
import com.jianzhang.entity.SysRole;
import com.jianzhang.entity.SysUser;
import com.jianzhang.mapper.SysMenuMapper;
import com.jianzhang.mapper.SysRoleMapper;
import com.jianzhang.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录认证：从数据库加载用户、角色、权限
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user == null) {
            throw new UsernameNotFoundException("账号或密码错误");
        }

        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            throw new UsernameNotFoundException("账号未分配有效角色，请联系管理员");
        }

        // 加载该角色被授权的菜单与按钮权限
        List<SysMenu> menus = menuMapper.selectByRoleId(role.getRoleId());
        Set<String> perms = new HashSet<>();
        for (SysMenu menu : menus) {
            if (StringUtils.hasText(menu.getPerms())) {
                perms.add(menu.getPerms());
            }
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setNickname(user.getNickname());
        loginUser.setRoleId(role.getRoleId());
        loginUser.setRoleCode(role.getRoleCode());
        loginUser.setRoleName(role.getRoleName());
        loginUser.setStatus(user.getStatus());
        loginUser.setPerms(perms);
        return loginUser;
    }
}
