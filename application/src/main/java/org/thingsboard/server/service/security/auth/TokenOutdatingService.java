/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.security.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.config.JwtSettings;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Service
@RequiredArgsConstructor
public class TokenOutdatingService {
    private final CacheManager cacheManager;
    private final JwtTokenFactory tokenFactory;
    private final JwtSettings jwtSettings;
    private Cache tokenOutdatageTimeCache;

    @PostConstruct
    protected void initCache() {
        tokenOutdatageTimeCache = cacheManager.getCache(CacheConstants.TOKEN_OUTDATAGE_TIME_CACHE);
    }

    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(UserAuthDataChangedEvent userAuthDataChangedEvent) {
        outdateTokens(userAuthDataChangedEvent.getUserId().getId().toString());
    }

    public boolean isOutdated(JwtToken token, UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getBody();
        long issueTime = claims.getIssuedAt().getTime();

        Jws<Claims> claimsJws = tokenFactory.parseTokenClaims(token);
        Optional<String> tokenId = Optional.ofNullable(claimsJws.getBody().get(JwtTokenFactory.TOKEN_ID)).map(Object::toString);

        boolean isTokenExpired = true;
        if (tokenId.isPresent()) {
            isTokenExpired = Optional.ofNullable(tokenOutdatageTimeCache.get(tokenId.get(), Long.class))
                    .map(time -> checkIfOutdated(time, issueTime, userId)).orElse(false);
        }

        Boolean isUserExpired = Optional.ofNullable(tokenOutdatageTimeCache.get(toKey(userId), Long.class))
                .map(time -> checkIfOutdated(time, issueTime, userId)).orElse(false);

        return isTokenExpired || isUserExpired;
    }

    public void outdateTokens(String id) {
        tokenOutdatageTimeCache.put(id, System.currentTimeMillis());
    }

    public void outdateTokens(RawAccessJwtToken token) {
        Jws<Claims> claimsJws = tokenFactory.parseTokenClaims(token);
        Optional.ofNullable(claimsJws.getBody().get(JwtTokenFactory.TOKEN_ID))
                .map(Object::toString)
                .ifPresent(this::outdateTokens);
    }

    private boolean checkIfOutdated(Long outdatageTime, long issueTime, UserId userId) {
        if (System.currentTimeMillis() - outdatageTime <= SECONDS.toMillis(jwtSettings.getRefreshTokenExpTime())) {
            return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime);
        } else {
            /*
             * Means that since the outdating has passed more than
             * the lifetime of refresh token (the longest lived)
             * and there is no need to store outdatage time anymore
             * as all the tokens issued before the outdatage time
             * are now expired by themselves
             * */
            tokenOutdatageTimeCache.evict(toKey(userId));
            return false;
        }
    }

    private String toKey(UserId userId) {
        return userId.getId().toString();
    }
}
