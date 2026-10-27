// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.tools;

import org.awaitility.pollinterval.FixedPollInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

public class TbRateLimitsTest {

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
        int gap = 1000;

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
        int gap = 1000;

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

    @Test
    @DisplayName("TbRateLimits should construct with single rate limit")
    void testSingleLimitConstructor() {
        TbRateLimits limits = new TbRateLimits("10:1", false);
        assertThat(limits.getConfiguration()).isEqualTo("10:1");
    }

    @Test
    @DisplayName("TbRateLimits should construct with multiple rate limits")
    void testMultipleLimitConstructor() {
        String config = "10:1,100:10";
        TbRateLimits limits = new TbRateLimits(config, false);
        assertThat(limits.getConfiguration()).isEqualTo(config);
    }

    @Test
    @DisplayName("TbRateLimits should throw IllegalArgumentException on empty string")
    void testEmptyConfigThrows() {
        assertThatThrownBy(() -> new TbRateLimits("", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse rate limits configuration: ");
    }

    @Test
    @DisplayName("TbRateLimits should throw NumberFormatException on malformed value")
    void testMalformedConfigThrows() {
        assertThatThrownBy(() -> new TbRateLimits("not_a_number:second", false))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("TbRateLimits should throw ArrayIndexOutOfBoundsException on missing colon")
    void testColonMissingThrows() {
        assertThatThrownBy(() -> new TbRateLimits("100", false))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

}
