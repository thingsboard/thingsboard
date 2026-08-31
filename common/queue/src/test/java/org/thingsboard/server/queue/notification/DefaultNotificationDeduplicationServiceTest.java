/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.queue.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultNotificationDeduplicationServiceTest {

    private static final int TIMEOUT = 30;

    private DefaultNotificationDeduplicationService deduplicationService;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        deduplicationService = new DefaultNotificationDeduplicationService();
        deduplicationService.setDeduplicationDurations("");
        cacheManager = new ConcurrentMapCacheManager(CacheConstants.SENT_NOTIFICATIONS_CACHE);
        ReflectionTestUtils.setField(deduplicationService, "cacheManager", cacheManager);
    }

    @Test
    void testFirstTriggerIsNotDeduplicated() {
        NotificationRuleTrigger trigger = mockTrigger(TimeUnit.HOURS.toMillis(1));
        NotificationRule rule = mockRule();

        assertThat(deduplicationService.alreadyProcessed(trigger, rule)).isFalse();
    }

    @Test
    void testSecondTriggerIsDeduplicated() {
        NotificationRuleTrigger trigger = mockTrigger(TimeUnit.HOURS.toMillis(1));
        NotificationRule rule = mockRule();

        assertThat(deduplicationService.alreadyProcessed(trigger, rule)).isFalse();
        assertThat(deduplicationService.alreadyProcessed(trigger, rule)).isTrue();
    }

    @Test
    void testTriggerPassesAfterDeduplicationWindowExpires() {
        NotificationRuleTrigger trigger = mockTrigger(50); // 50ms dedup window
        NotificationRule rule = mockRule();

        assertThat(deduplicationService.alreadyProcessed(trigger, rule)).isFalse();

        try {
            Thread.sleep(200); // wait well past the 50ms window
        } catch (InterruptedException ignored) {}

        assertThat(deduplicationService.alreadyProcessed(trigger, rule)).isFalse();
    }

    @Test
    void testFutureTimestampFromExternalCacheIsDiscarded() {
        NotificationRuleTrigger trigger = mockTrigger(TimeUnit.HOURS.toMillis(1));
        NotificationRule rule = mockRule();
        String dedupKey = DefaultNotificationDeduplicationService.getDeduplicationKey(trigger, rule);

        // Put a timestamp 2 hours in the future into external cache
        Cache externalCache = cacheManager.getCache(CacheConstants.SENT_NOTIFICATIONS_CACHE);
        externalCache.put(dedupKey, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2));

        // Should NOT be deduplicated — future timestamp must be discarded
        assertThat(deduplicationService.alreadyProcessed(trigger, rule)).isFalse();
    }

    @Test
    void testValidTimestampFromExternalCacheIsDeduplicated() {
        NotificationRuleTrigger trigger = mockTrigger(TimeUnit.HOURS.toMillis(1));
        NotificationRule rule = mockRule();
        String dedupKey = DefaultNotificationDeduplicationService.getDeduplicationKey(trigger, rule);

        // Put a recent timestamp into external cache
        Cache externalCache = cacheManager.getCache(CacheConstants.SENT_NOTIFICATIONS_CACHE);
        externalCache.put(dedupKey, System.currentTimeMillis());

        // Should be deduplicated — valid external cache entry
        assertThat(deduplicationService.alreadyProcessed(trigger, rule)).isTrue();
    }

    @Test
    void testConcurrentTriggersProduceExactlyOneNonDeduplicated() throws Exception {
        NotificationRuleTrigger trigger = mockTrigger(TimeUnit.HOURS.toMillis(1));
        NotificationRule rule = mockRule();

        int threadCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Boolean> results = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        barrier.await(TIMEOUT, TimeUnit.SECONDS);
                    } catch (Exception ignored) {}
                    results.add(deduplicationService.alreadyProcessed(trigger, rule));
                });
            }
            executor.shutdown();
            assertThat(executor.awaitTermination(TIMEOUT, TimeUnit.SECONDS)).isTrue();

            assertThat(results).hasSize(threadCount);
            assertThat(results.stream().filter(r -> !r).count())
                    .as("exactly one trigger should pass through deduplication")
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private NotificationRuleTrigger mockTrigger(long deduplicationDurationMs) {
        NotificationRuleTrigger trigger = mock(NotificationRuleTrigger.class);
        when(trigger.getType()).thenReturn(NotificationRuleTriggerType.RESOURCES_SHORTAGE);
        when(trigger.getDeduplicationKey()).thenReturn("test:dedup:key");
        when(trigger.getDefaultDeduplicationDuration()).thenReturn(deduplicationDurationMs);
        when(trigger.getDeduplicationStrategy()).thenReturn(NotificationRuleTrigger.DeduplicationStrategy.ONLY_MATCHING);
        return trigger;
    }

    private NotificationRule mockRule() {
        NotificationRule rule = mock(NotificationRule.class);
        when(rule.getDeduplicationKey()).thenReturn("rule:key");
        return rule;
    }

}
