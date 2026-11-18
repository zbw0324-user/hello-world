package com.jianzhang.service.impl;

import com.jianzhang.dto.LoginDTO;
import com.jianzhang.dto.PasswordDTO;
import com.jianzhang.entity.SysLog;
import com.jianzhang.entity.SysUser;
import com.jianzhang.exception.BusinessException;
import com.jianzhang.mapper.SysLogMapper;
import com.jianzhang.mapper.SysUserMapper;
import com.jianzhang.security.JwtUtil;
import com.jianzhang.security.LoginUser;
import com.jianzhang.security.SecurityUtils;
import com.jianzhang.service.AuthService;
import com.jianzhang.service.SysMenuService;
import com.jianzhang.vo.LoginVO;
import com.jianzhang.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper userMapper;
    private final SysLogMapper sysLogMapper;
    private final SysMenuService menuService;

    @Override
    public LoginVO login(LoginDTO dto) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername().trim(), dto.getPassword()));
        } catch (DisabledException e) {
            throw new BusinessException(401, "账号已被禁用，请联系管理员");
        } catch (BadCredentialsException e) {
            throw new BusinessException(401, "账号或密码错误");
        } catch (Exception e) {
            log.warn("登录失败：{}", e.getMessage());
            throw new BusinessException(401, "登录失败，请稍后重试");
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtUtil.createToken(loginUser.getUserId(), loginUser.getUsername(), loginUser.getRoleCode());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpire(jwtUtil.getExpire());
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setNickname(loginUser.getNickname());
        vo.setRoleId(loginUser.getRoleId());
        vo.setRoleCode(loginUser.getRoleCode());
        vo.setRoleName(loginUser.getRoleName());

        // 记录登录日志
        SysLog sysLog = new SysLog();
        sysLog.setUsername(loginUser.getUsername());
        sysLog.setRoleCode(loginUser.getRoleCode());
        sysLog.setModule("登录认证");
        sysLog.setOperation("登录系统");
        sysLog.setMethod("AuthServiceImpl.login");
        sysLog.setRequestUri("/api/auth/login");
        sysLog.setParams("{\"username\":\"" + loginUser.getUsername() + "\"}");
        sysLog.setStatus(1);
        sysLog.setCostTime(0L);
        sysLogMapper.insert(sysLog);

        return vo;
    }

    @Override
    public UserInfoVO getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setNickname(loginUser.getNickname());
        vo.setRoleId(loginUser.getRoleId());
        vo.setRoleCode(loginUser.getRoleCode());
        vo.setRoleName(loginUser.getRoleName());

        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user != null) {
            vo.setRemark(user.getRemark());
            if (user.getNickname() != null) {
                vo.setNickname(user.getNickname());
            }
        }

        List<String> perms = new ArrayList<>(menuService.getPermsByRole(loginUser.getRoleId()));
        vo.setPerms(perms);
        vo.setMenus(menuService.getMenuTreeByRole(loginUser.getRoleId()));
        return vo;
    }

    @Override
    public void changePassword(PasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        Long userId = SecurityUtils.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }
}
