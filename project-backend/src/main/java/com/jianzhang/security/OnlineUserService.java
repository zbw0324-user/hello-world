package com.jianzhang.security;

import com.jianzhang.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 在线用户管理（内存实现）：
 * 1. 登录成功后登记在线状态；登出 / 令牌过期自动清理；
 * 2. 管理员可强制下线（令牌进黑名单，下次请求即 401）；
 * 3. 单机内存方案，重启即清空；集群部署可替换为 Redis 实现。
 */
@Component
@RequiredArgsConstructor
public class OnlineUserService {

    private final JwtUtil jwtUtil;

    /** token -> 在线用户 */
    private final Map<String, OnlineUser> onlineUsers = new ConcurrentHashMap<>();

    /** 已强制下线的令牌黑名单（到期自动失效，无需手动清理也可定时清理） */
    private final Set<String> blacklist = new CopyOnWriteArraySet<>();

    /** 登录成功后登记 */
    public void record(String token, LoginVO vo, String ip) {
        OnlineUser user = new OnlineUser();
        user.setTokenId(tokenId(token));
        user.setToken(token);
        user.setUsername(vo.getUsername());
        user.setNickname(vo.getNickname());
        user.setRoleName(vo.getRoleName());
        user.setIp(ip);
        user.setLoginTime(LocalDateTime.now());
        user.setExpireTime(LocalDateTime.now().plusSeconds(vo.getExpire() == null ? 7200 : vo.getExpire()));

        // 同一账号重复登录时，踢掉旧令牌（单点登录语义）
        onlineUsers.values().removeIf(u -> u.getUsername().equals(vo.getUsername()));
        onlineUsers.put(token, user);

        // 清理已过期的在线记录与黑名单
        evictExpired();
    }

    /** 登出时移除 */
    public void remove(String token) {
        onlineUsers.remove(token);
    }

    /** 强制下线（按会话标识定位）：移除在线状态并拉黑令牌 */
    public boolean kick(String tokenId) {
        for (Map.Entry<String, OnlineUser> entry : onlineUsers.entrySet()) {
            if (tokenId(entry.getKey()).equals(tokenId)) {
                onlineUsers.remove(entry.getKey());
                blacklist.add(entry.getKey());
                return true;
            }
        }
        return false;
    }

    /** 令牌是否已被强制下线 */
    public boolean isBlocked(String token) {
        return blacklist.contains(token);
    }

    /** 在线用户列表（token 脱敏） */
    public List<OnlineUser> list() {
        evictExpired();
        List<OnlineUser> result = new ArrayList<>(onlineUsers.values());
        result.sort(Comparator.comparing(OnlineUser::getLoginTime).reversed());
        for (OnlineUser u : result) {
            u.setToken(maskToken(u.getToken()));
        }
        return result;
    }

    /** 当前在线人数 */
    public int count() {
        return onlineUsers.size();
    }

    private void evictExpired() {
        LocalDateTime now = LocalDateTime.now();
        onlineUsers.values().removeIf(u -> u.getExpireTime() != null && u.getExpireTime().isBefore(now));
        blacklist.removeIf(t -> {
            try {
                return jwtUtil.parseToken(t).getExpiration().toInstant()
                        .isBefore(now.atZone(java.time.ZoneId.systemDefault()).toInstant());
            } catch (Exception e) {
                return true;
            }
        });
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 16) {
            return "****";
        }
        return token.substring(0, 12) + "****" + token.substring(token.length() - 6);
    }

    /** 令牌摘要（SHA-256 前 16 位），作为对外暴露的会话标识 */
    private String tokenId(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(token.hashCode());
        }
    }
}
