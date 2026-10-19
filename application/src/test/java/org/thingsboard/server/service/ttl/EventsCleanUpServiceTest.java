// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.thingsboard.server.service.ttl.EventsCleanUpService.RANDOM_DELAY_INTERVAL_MS_EXPRESSION;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventsCleanUpServiceTest.class)
@Slf4j
public class EventsCleanUpServiceTest {

    @Value(RANDOM_DELAY_INTERVAL_MS_EXPRESSION)
    long randomDelayMs;
    @Value("${sql.ttl.events.execution_interval_ms}")
    long executionIntervalMs;

    @Test
    public void givenInterval_whenRandomDelay_ThenDelayInInterval() {
        log.info("randomDelay {}", randomDelayMs);
        log.info("executionIntervalMs {}", executionIntervalMs);
        assertThat(randomDelayMs, greaterThanOrEqualTo(0L));
        assertThat(randomDelayMs, lessThanOrEqualTo(executionIntervalMs));
    }

}
