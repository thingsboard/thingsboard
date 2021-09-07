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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.thingsboard.server.common.data.id.UserId;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggedInUsersLimitTest {

    private UserActiveSessionsLimitService userActiveSessionsLimitService;

    private UserId userId;

    private final int maxLoginUsers = 1;

    @BeforeEach
    public void setUp() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        userActiveSessionsLimitService = new UserActiveSessionsLimitService(cacheManager, (long) maxLoginUsers);
        userActiveSessionsLimitService.initCache();

        userId = new UserId(UUID.randomUUID());

    }

    @Test
    public void testIsOverLimit(){
        assertFalse(userActiveSessionsLimitService.isOverLimit(userId));
        for (int i = 0; i < maxLoginUsers; i++)
            userActiveSessionsLimitService.increaseCurrentActiveSessions(userId);

        boolean isOverLimit = userActiveSessionsLimitService.isOverLimit(userId);
        assertTrue(isOverLimit);
    }

    @Test
    public void testDecreaseCurrentLoginAmount() {
        userActiveSessionsLimitService.increaseCurrentActiveSessions(userId);
        userActiveSessionsLimitService.decreaseCurrentActiveSessions(userId);
        long currentAmount1 = userActiveSessionsLimitService.getCurrentAmount(userId);
        assertEquals(0, currentAmount1);

        for (int i = 0; i < 2; i++)
            userActiveSessionsLimitService.increaseCurrentActiveSessions(userId);

        userActiveSessionsLimitService.decreaseCurrentActiveSessions(userId);
        long currentAmount2 = userActiveSessionsLimitService.getCurrentAmount(userId);
        assertEquals(currentAmount2, 1L);
    }

    @Test
    public void testIncreaseCurrentLoggedInUsers() {
        userActiveSessionsLimitService.increaseCurrentActiveSessions(userId);
        long currentAmount1 = userActiveSessionsLimitService.getCurrentAmount(userId);
        assertEquals(currentAmount1, 1L);

        userActiveSessionsLimitService.increaseCurrentActiveSessions(userId);
        long currentAmount2 = userActiveSessionsLimitService.getCurrentAmount(userId);
        assertEquals(currentAmount2, 2L);

    }
}
