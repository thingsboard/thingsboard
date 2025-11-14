/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class DefaultTokenOutdatingService implements TokenOutdatingService {

    private final TbTransactionalCache<String, Long> cache;
    private final JwtTokenFactory tokenFactory;

    public DefaultTokenOutdatingService(@Qualifier("UsersSessionInvalidation") TbTransactionalCache<String, Long> cache, JwtTokenFactory tokenFactory) {
        this.cache = cache;
        this.tokenFactory = tokenFactory;
    }

    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(UserAuthDataChangedEvent event) {
        if (StringUtils.hasText(event.getId())) {
            cache.put(event.getId(), event.getTs());
        }
    }

    @Override
    public boolean isOutdated(String token, UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getPayload();
        long issueTime = claims.getIssuedAt().getTime();
        String sessionId = claims.get("sessionId", String.class);
        if (isTokenOutdated(issueTime, userId.toString())) {
            return true;
        } else {
            return sessionId != null && isTokenOutdated(issueTime, sessionId);
        }
    }

    private Boolean isTokenOutdated(long issueTime, String sessionId) {
        return Optional.ofNullable(cache.get(sessionId)).map(outdatedTime -> isTokenOutdated(issueTime, outdatedTime.get())).orElse(false);
    }

    private boolean isTokenOutdated(long issueTime, Long outdatageTime) {
        return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime);
    }

}
