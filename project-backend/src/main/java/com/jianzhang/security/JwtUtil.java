package com.jianzhang.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：生成、解析、校验令牌
 */
@Component
public class JwtUtil {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE_CODE = "roleCode";

    @Value("${business.jwt.secret}")
    private String secret;

    @Value("${business.jwt.expire:7200}")
    private Long expire;

    @Value("${business.jwt.header:Authorization}")
    private String header;

    @Value("${business.jwt.prefix:Bearer }")
    private String prefix;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成令牌
     */
    public String createToken(Long userId, String username, String roleCode) {
        Map<String, Object> claims = new HashMap<>(4);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ROLE_CODE, roleCode);
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expire * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析令牌，非法或过期会抛出异常
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从请求头中提取令牌
     */
    public String resolveToken(HttpServletRequest request) {
        String value = request.getHeader(header);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String prefixTrim = prefix == null ? "" : prefix.trim();
        if (StringUtils.hasText(prefixTrim) && value.startsWith(prefixTrim)) {
            return value.substring(prefixTrim.length()).trim();
        }
        return value.trim();
    }

    public Long getExpire() {
        return expire;
    }
}
