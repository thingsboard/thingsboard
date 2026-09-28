// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Container-free tests for {@link CitusShardLocator}'s pre-refresh fallback paths: the
 * uniform-partition {@code bucket()} fallback (including the clamp for shard counts that do not divide
 * 2^32 evenly and the non-positive-shard-count guard) and the {@code shardCount()} fallback to the
 * configured value — the paths the install-profile locator bean and
 * {@code CitusQueuePartitioner.queueCount()} rely on before {@code refresh()} has run. The refreshed
 * paths are covered by {@code CitusShardLocatorIntegrationTest}.
 */
class CitusShardLocatorTest {

    /** 48 does not divide 2^32 evenly (2^32 mod 48 = 16), so the top hash values need the clamp. */
    private static final int NON_POWER_OF_TWO_SHARD_COUNT = 48;

    /**
     * Hashes ({@link PostgresHashAny#hashUuid}) to exactly {@link Integer#MAX_VALUE}, the top of the hash
     * range (found by exhaustive search over {@code new UUID(0, i)}). For shardCount 48 the uniform-partition
     * division yields 48 for this hash — one past the last valid bucket — so it exercises the clamp.
     */
    private static final UUID TOP_OF_RANGE_UUID = new UUID(0, 177747923L);

    private CitusShardLocator unrefreshedLocator(JdbcTemplate jdbcTemplate, int configuredShardCount) {
        CitusSettings settings = new CitusSettings();
        ReflectionTestUtils.setField(settings, "shardCount", configuredShardCount);
        return new CitusShardLocator(jdbcTemplate, settings);
    }

    @Test
    void shardCountFallsBackToConfiguredValueBeforeRefresh() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CitusShardLocator locator = unrefreshedLocator(jdbcTemplate, NON_POWER_OF_TWO_SHARD_COUNT);

        assertThat(locator.shardCount()).isEqualTo(NON_POWER_OF_TWO_SHARD_COUNT);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void fallbackBucketsStayInRangeForNonPowerOfTwoShardCount() {
        CitusShardLocator locator = unrefreshedLocator(mock(JdbcTemplate.class), NON_POWER_OF_TWO_SHARD_COUNT);

        for (long i = 0; i < 10_000; i++) {
            UUID entityId = new UUID(i, i * 31 + 7);
            assertThat(locator.bucket(entityId))
                    .as("fallback bucket for %s must stay in [0, %s)", entityId, NON_POWER_OF_TWO_SHARD_COUNT)
                    .isBetween(0, NON_POWER_OF_TWO_SHARD_COUNT - 1);
        }
    }

    @Test
    void topOfRangeHashClampsToLastBucket() {
        // Pin the precondition so a change to the hash function fails here with a clear message instead
        // of silently degrading this test to a mid-range sample.
        assertThat(PostgresHashAny.hashUuid(TOP_OF_RANGE_UUID)).isEqualTo(Integer.MAX_VALUE);
        CitusShardLocator locator = unrefreshedLocator(mock(JdbcTemplate.class), NON_POWER_OF_TWO_SHARD_COUNT);

        // Without the Math.min clamp the uniform-partition division would return 48 for this hash.
        assertThat(locator.bucket(TOP_OF_RANGE_UUID)).isEqualTo(NON_POWER_OF_TWO_SHARD_COUNT - 1);
    }

    @Test
    void nonPositiveShardCountRoutesEverythingToBucketZero() {
        CitusShardLocator zeroShardLocator = unrefreshedLocator(mock(JdbcTemplate.class), 0);
        assertThat(zeroShardLocator.bucket(UUID.randomUUID())).isZero();
        assertThat(zeroShardLocator.bucket(TOP_OF_RANGE_UUID)).isZero();

        CitusShardLocator negativeShardLocator = unrefreshedLocator(mock(JdbcTemplate.class), -5);
        assertThat(negativeShardLocator.bucket(UUID.randomUUID())).isZero();
    }
}
