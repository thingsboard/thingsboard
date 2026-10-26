package com.iot.thingmodel.auth;

import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.config.ThingModelProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.User;

@Component
public class TbUserVerifier {

    private final ThingModelProperties properties;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public TbUserVerifier(ThingModelProperties properties) {
        this.properties = properties;
    }

    public CurrentUser verify(String rawToken) {
        String token = normalize(rawToken);
        String key = sha256(token);
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.expiresAt() > now) {
            return entry.user();
        }
        if (cache.size() > Constants.Auth.MAX_CACHE_SIZE) {
            cache.clear();
        }
        try {
            ThingsboardClient client = ThingsboardClient.builder()
                    .url(properties.getTb().getBaseUrl())
                    .retryOnRateLimit(false)
                    .build();
            client.setToken(token);
            User user = client.getUser();
            CurrentUser currentUser = toCurrentUser(user);
            cache.put(key, new CacheEntry(
                    currentUser,
                    now + properties.getAuth().getCacheSeconds() * Constants.Auth.MILLIS_PER_SECOND));
            return currentUser;
        } catch (org.thingsboard.client.ApiException ex) {
            if (ex.getCode() == HttpStatus.UNAUTHORIZED.value()) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED, "登录状态无效或已过期");
            }
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, Constants.ErrorCode.TB_UNAVAILABLE, "无法校验 ThingsBoard 用户");
        }
    }

    private CurrentUser toCurrentUser(User user) {
        String userId = user.getId() == null || user.getId().getId() == null
                ? null : user.getId().getId().toString();
        String tenantId = user.getTenantId() == null || user.getTenantId().getId() == null
                ? null : user.getTenantId().getId().toString();
        String authority = user.getAuthority() == null ? null : user.getAuthority().toString();
        return new CurrentUser(userId, tenantId, authority, user.getEmail());
    }

    private String normalize(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED, "缺少访问令牌");
        }
        String value = token.trim();
        if (value.regionMatches(
                true, 0, Constants.Auth.BEARER_PREFIX, 0, Constants.Auth.BEARER_PREFIX_LENGTH)) {
            value = value.substring(Constants.Auth.BEARER_PREFIX_LENGTH).trim();
        }
        if (value.isEmpty()) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED, "缺少访问令牌");
        }
        return value;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(Constants.Auth.SHA_256);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(Constants.Auth.SHA_256 + " is not available", ex);
        }
    }

    private record CacheEntry(CurrentUser user, long expiresAt) {
    }
}
