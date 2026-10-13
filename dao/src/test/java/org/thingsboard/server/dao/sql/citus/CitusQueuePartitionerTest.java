// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CitusQueuePartitionerTest {

    @Test
    void enabledFlagReflectsSettings() {
        CitusSettings off = new CitusSettings();
        assertThat(new CitusQueuePartitioner(off, mock(CitusShardLocator.class)).isEnabled()).isFalse();

        CitusSettings on = new CitusSettings();
        ReflectionTestUtils.setField(on, "enabled", true);
        assertThat(new CitusQueuePartitioner(on, mock(CitusShardLocator.class)).isEnabled()).isTrue();
    }

    @Test
    void queueCountEqualsLocatorShardCount() {
        CitusShardLocator locator = mock(CitusShardLocator.class);
        when(locator.shardCount()).thenReturn(16);
        CitusSettings on = new CitusSettings();
        ReflectionTestUtils.setField(on, "enabled", true);
        assertThat(new CitusQueuePartitioner(on, locator).queueCount()).isEqualTo(16);
    }

    @Test
    void resolverDelegatesToLocatorViaExtractor() {
        UUID e = UUID.randomUUID();
        CitusShardLocator locator = mock(CitusShardLocator.class);
        when(locator.bucket(e)).thenReturn(7);
        CitusSettings on = new CitusSettings();
        ReflectionTestUtils.setField(on, "enabled", true);
        CitusQueuePartitioner p = new CitusQueuePartitioner(on, locator);

        Function<String, Integer> resolver = p.bucketResolver(s -> e);
        assertThat(resolver.apply("ignored")).isEqualTo(7);
    }
}
