/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.config.JwtSettings;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Service
@RequiredArgsConstructor
public class TokenOutdatingService {
    private final TbTransactionalCache<String, Long> cache;
    private final JwtTokenFactory tokenFactory;
    private final JwtSettings jwtSettings;

    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(UserAuthDataChangedEvent event) {
        cache.put(event.getId(), event.getTs());
    }

    public boolean isOutdated(JwtToken token, UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getBody();
        long issueTime = claims.getIssuedAt().getTime();

        String sessionId = claims.get("sessionId", String.class);

        Boolean isUserIdOutdated = Optional.ofNullable(cache.get(userId.toString()))
                .map(outdatageTimeByUserId -> {
                    if (refreshTokenNotExpired(outdatageTimeByUserId.get(), System.currentTimeMillis())) {
                        return accessTokenNotExpired(issueTime, outdatageTimeByUserId.get());
                    } else {
                        return false;
                    }
                })
                .orElse(false);

        if (!isUserIdOutdated) {
            return Optional.ofNullable(cache.get(sessionId)).map(outdatageTimeBySessionId -> {
                        if (refreshTokenNotExpired(outdatageTimeBySessionId.get(), System.currentTimeMillis())) {
                            return accessTokenNotExpired(issueTime, outdatageTimeBySessionId.get());
                        } else {
                            return false;
                        }
                    }
            ).orElse(false);
        }

        return isUserIdOutdated;
    }

    private boolean accessTokenNotExpired(long issueTime, Long outdatageTime) {
        return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime);
    }

    private boolean refreshTokenNotExpired(Long outdatageTime, long currentTime) {
        return currentTime - outdatageTime <= SECONDS.toMillis(jwtSettings.getRefreshTokenExpTime());
    }
}
