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
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.usersUpdateTime.UsersUpdateTimeCacheEvictEvent;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.config.JwtSettings;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.HashMap;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Service
@RequiredArgsConstructor
public class TokenOutdatingService extends AbstractCachedEntityService<UserId, HashMap<String, Long>, UsersUpdateTimeCacheEvictEvent> {
    private final JwtTokenFactory tokenFactory;
    private final JwtSettings jwtSettings;

    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(UserAuthDataChangedEvent event) {
        processUserSessions(event);
    }

    public boolean isOutdated(JwtToken token, UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getBody();
        long issueTime = claims.getIssuedAt().getTime();

        String sessionId = claims.get("sessionId", String.class);
        return Optional.ofNullable(cache.get(userId))
                .map(outdatageTime -> {
                    if (outdatageTime.get().get(sessionId) != null && System.currentTimeMillis() - outdatageTime.get().get(sessionId) <= SECONDS.toMillis(jwtSettings.getRefreshTokenExpTime())) {
                        return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime.get().get(sessionId));
                    } else {
                        /*
                         * Means that since the outdating has passed more than
                         * the lifetime of refresh token (the longest lived)
                         * and there is no need to store outdatage time anymore
                         * as all the tokens issued before the outdatage time
                         * are now expired by themselves
                         * */
                        handleEvictEvent(new UsersUpdateTimeCacheEvictEvent(userId, sessionId));
                        return false;
                    }
                })
                .orElse(false);
    }

    @TransactionalEventListener(classes = UsersUpdateTimeCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(UsersUpdateTimeCacheEvictEvent event) {
        HashMap<String, Long> userSessions = cache.get(event.getUserId()).get();
        if (userSessions != null) {
            userSessions.remove(event.getSessionId());
            cache.put(event.getUserId(), userSessions);
        }
    }

    private void processUserSessions(UserAuthDataChangedEvent event) {
        if (cache.get(event.getUserId()) != null) {
            HashMap<String, Long> userSessions = cache.get(event.getUserId()).get();
            if (event.isDropAllSessions()) {
                userSessions.replaceAll((k, v) -> event.getTs());
            } else {
                userSessions.put(event.getSessionId(), event.getTs());
            }
            cache.put(event.getUserId(), userSessions);
        } else {
            cache.put(event.getUserId(), new HashMap<>() {{
                put(event.getSessionId(), event.getTs());
            }});
        }
    }
}
