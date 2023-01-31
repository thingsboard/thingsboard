/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.tools;

import org.awaitility.pollinterval.FixedPollInterval;
import org.awaitility.pollinterval.PollInterval;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class RateLimitsTest {

    @Test
    public void testRateLimits_greedyRefill() {
        testRateLimitWithGreedyRefill(3, 10);
        testRateLimitWithGreedyRefill(3, 3);
        testRateLimitWithGreedyRefill(4, 2);
    }

    private void testRateLimitWithGreedyRefill(int capacity, int period) {
        String rateLimitConfig = capacity + ":" + period;
        TbRateLimits rateLimits = new TbRateLimits(rateLimitConfig);

        rateLimits.tryConsume(capacity);
        assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();

        int expectedRefillTime = (int) (((double) period / capacity) * 1000);
        int gap = 500;

        for (int i = 0; i < capacity; i++) {
            await("token refill for rate limit " + rateLimitConfig)
                    .pollInterval(new FixedPollInterval(10, TimeUnit.MILLISECONDS))
                    .atLeast(expectedRefillTime - gap, TimeUnit.MILLISECONDS)
                    .atMost(expectedRefillTime + gap, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(rateLimits.tryConsume()).as("token is available").isTrue();
                    });
            assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();
        }
    }

    @Test
    public void testRateLimits_intervalRefill() {
        testRateLimitWithIntervalRefill(10, 5);
        testRateLimitWithIntervalRefill(3, 3);
        testRateLimitWithIntervalRefill(4, 2);
    }

    private void testRateLimitWithIntervalRefill(int capacity, int period) {
        String rateLimitConfig = capacity + ":" + period;
        TbRateLimits rateLimits = new TbRateLimits(rateLimitConfig, true);

        rateLimits.tryConsume(capacity);
        assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();

        int expectedRefillTime = period * 1000;
        int gap = 500;

        await("tokens refill for rate limit " + rateLimitConfig)
                .pollInterval(new FixedPollInterval(10, TimeUnit.MILLISECONDS))
                .atLeast(expectedRefillTime - gap, TimeUnit.MILLISECONDS)
                .atMost(expectedRefillTime + gap, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    for (int i = 0; i < capacity; i++) {
                        assertThat(rateLimits.tryConsume()).as("token is available").isTrue();
                    }
                    assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();
                });
    }

}
