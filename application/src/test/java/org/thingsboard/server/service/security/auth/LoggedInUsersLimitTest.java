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

    private LoggedInUsersLimitService loggedInUsersLimitService;

    private UserId userId;

    private final int maxLoginUsers = 1;

    @BeforeEach
    public void setUp() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        loggedInUsersLimitService = new LoggedInUsersLimitService(cacheManager, (long) maxLoginUsers);
        loggedInUsersLimitService.initCache();

        userId = new UserId(UUID.randomUUID());

    }

    @Test
    public void testIsOverLimit(){
        assertFalse(loggedInUsersLimitService.isOverLimit(userId));
        for (int i = 0; i < maxLoginUsers; i++)
            loggedInUsersLimitService.increaseCurrentLoggedInUsers(userId);

        boolean isOverLimit = loggedInUsersLimitService.isOverLimit(userId);
        assertTrue(isOverLimit);
    }

    @Test
    public void testDecreaseCurrentLoginAmount() {
        loggedInUsersLimitService.increaseCurrentLoggedInUsers(userId);
        loggedInUsersLimitService.decreaseCurrentLoggedInUsers(userId);
        Optional<Long> currentAmount1 = loggedInUsersLimitService.getCurrentAmount(userId);
        assertTrue(currentAmount1.isEmpty());

        for (int i = 0; i < 2; i++)
            loggedInUsersLimitService.increaseCurrentLoggedInUsers(userId);

        loggedInUsersLimitService.decreaseCurrentLoggedInUsers(userId);
        Optional<Long> currentAmount2 = loggedInUsersLimitService.getCurrentAmount(userId);
        assertTrue(currentAmount2.isPresent());
        assertEquals(currentAmount2.get(), 1L);
    }

    @Test
    public void testIncreaseCurrentLoggedInUsers() {
        loggedInUsersLimitService.increaseCurrentLoggedInUsers(userId);
        Optional<Long> currentAmount1 = loggedInUsersLimitService.getCurrentAmount(userId);
        assertTrue(currentAmount1.isPresent());
        assertEquals(currentAmount1.get(), 1L);

        loggedInUsersLimitService.increaseCurrentLoggedInUsers(userId);
        Optional<Long> currentAmount2 = loggedInUsersLimitService.getCurrentAmount(userId);
        assertTrue(currentAmount2.isPresent());
        assertEquals(currentAmount2.get(), 2L);

    }
}
