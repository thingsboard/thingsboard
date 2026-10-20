// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.thingsboard.server.dao.sql.citus.CitusShardLocator;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CitusShardRouterTest {

    private final CitusShardLocator locator = mock(CitusShardLocator.class);
    private final CitusShardPlacement placement = mock(CitusShardPlacement.class);
    private final CitusWorkerRegistry registry = mock(CitusWorkerRegistry.class);
    private final CitusSmartRoutingSettings settings = mock(CitusSmartRoutingSettings.class);

    private final CitusShardRouter router = new CitusShardRouter(locator, placement, registry, settings);

    @Test
    void forKeyDelegatesThroughBucketThenPlacementThenRegistry() {
        UUID key = UUID.randomUUID();
        JdbcTemplate template = mock(JdbcTemplate.class);
        CitusWorkerNode node = new CitusWorkerNode(3, "citus-worker-3", 5432);
        when(locator.bucket(key)).thenReturn(7);
        when(placement.workerForBucket(7)).thenReturn(node);
        when(registry.templateForGroup(3)).thenReturn(template);

        JdbcTemplate result = router.forKey(key);

        assertThat(result).isSameAs(template);
        verify(locator).bucket(key);
        verify(placement).workerForBucket(7);
        verify(registry).templateForGroup(3);
    }

    @Test
    void forBucketDelegatesThroughPlacementThenRegistryWithoutLocator() {
        JdbcTemplate template = mock(JdbcTemplate.class);
        CitusWorkerNode node = new CitusWorkerNode(5, "citus-worker-5", 5432);
        when(placement.workerForBucket(7)).thenReturn(node);
        when(registry.templateForGroup(5)).thenReturn(template);

        JdbcTemplate result = router.forBucket(7);

        assertThat(result).isSameAs(template);
        verify(placement).workerForBucket(7);
        verify(registry).templateForGroup(5);
        verifyNoInteractions(locator);
    }

    @Test
    void shardCountDelegatesToLocator() {
        when(locator.shardCount()).thenReturn(32);

        assertThat(router.shardCount()).isEqualTo(32);

        verify(locator).shardCount();
    }

    @Test
    void isEnabledDelegatesToSettings() {
        when(settings.isEnabled()).thenReturn(true);

        assertThat(router.isEnabled()).isTrue();

        verify(settings).isEnabled();
    }

    // --- isRouting: the centralized null-tolerant guard every consumer relies on ---

    @Test
    void isRoutingNullRouterIsFalse() {
        // The load-bearing branch: the kvShardRouter bean is optional (required = false), so consumers
        // pass null whenever smart routing is off or the bean is unset in sliced tests.
        assertThat(CitusShardRouter.isRouting(null)).isFalse();
    }

    @Test
    void isRoutingDelegatesToEnabledFlag() {
        when(settings.isEnabled()).thenReturn(true);
        assertThat(CitusShardRouter.isRouting(router)).isTrue();

        when(settings.isEnabled()).thenReturn(false);
        assertThat(CitusShardRouter.isRouting(router)).isFalse();
    }

    // --- bucket-alignment invariant guard ---

    @Test
    void assertBucketAlignmentThrowsOnMismatchNamingBothCountsAndAnchorTable() {
        assertThatThrownBy(() -> CitusShardRouter.assertBucketAlignment(32, 8, "attribute_kv"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32")
                .hasMessageContaining("8")
                .hasMessageContaining("attribute_kv");
    }

    @Test
    void assertBucketAlignmentPassesWhenCountsMatch() {
        assertThatCode(() -> CitusShardRouter.assertBucketAlignment(32, 32, "attribute_kv"))
                .doesNotThrowAnyException();
    }

    // --- failover-signature classification ---

    @Test
    void nested25006DeepInADataAccessChainQualifies() {
        // A pinned connection failing on a demoted worker surfaces as a DataAccessException subtype
        // wrapping the SQLException, itself potentially wrapped again by intermediate layers.
        RuntimeException failure = new RuntimeException("flush failed",
                new UncategorizedSQLException("upsert", "INSERT ...",
                        new SQLException("cannot execute INSERT in a read-only transaction", "25006")));

        assertThat(CitusShardRouter.isFailoverFailure(failure)).isTrue();
    }

    @Test
    void cannotGetJdbcConnectionAtTopOfChainQualifies() {
        assertThat(CitusShardRouter.isFailoverFailure(
                new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection"))).isTrue();
    }

    @Test
    void nestedSqlTransientConnectionExceptionQualifies() {
        // Hikari's borrow-timeout type, e.g. after targetServerType=primary refuses every replacement connect.
        RuntimeException failure = new RuntimeException("flush failed",
                new SQLTransientConnectionException("citus-worker-1 - Connection is not available"));

        assertThat(CitusShardRouter.isFailoverFailure(failure)).isTrue();
    }

    @Test
    void constraintViolationSyntaxErrorAndQueryTimeoutDoNotQualify() {
        assertThat(CitusShardRouter.isFailoverFailure(new DataIntegrityViolationException("duplicate key",
                new SQLException("duplicate key value violates unique constraint", "23505")))).isFalse();
        assertThat(CitusShardRouter.isFailoverFailure(new BadSqlGrammarException("query", "SELECT oops",
                new SQLException("syntax error", "42601")))).isFalse();
        assertThat(CitusShardRouter.isFailoverFailure(new QueryTimeoutException("statement timeout",
                new SQLException("canceling statement due to statement timeout", "57014")))).isFalse();
    }

    // --- failover observation on the routed paths ---

    private final CitusFailoverRefreshTrigger trigger = mock(CitusFailoverRefreshTrigger.class);
    private final CitusShardRouter observedRouter = new CitusShardRouter(locator, placement, registry, settings, trigger);
    private final UUID key = UUID.randomUUID();
    private final JdbcTemplate template = mock(JdbcTemplate.class);

    private void stubRouting() {
        when(locator.bucket(key)).thenReturn(7);
        when(placement.workerForBucket(7)).thenReturn(new CitusWorkerNode(3, "citus-worker-3", 5432));
        when(registry.templateForGroup(3)).thenReturn(template);
    }

    @Test
    @SuppressWarnings("unchecked")
    void qualifyingRoutedQueryFailureNotifiesTriggerAndRethrowsOriginal() {
        stubRouting();
        CannotGetJdbcConnectionException failure = new CannotGetJdbcConnectionException("connection refused");
        doThrow(failure).when(template).query(anyString(), any(RowMapper.class), any(Object[].class));

        assertThatThrownBy(() -> observedRouter.routedQuery(key, "SELECT 1", (rs, rowNum) -> 1))
                .as("the original exception must be rethrown unchanged")
                .isSameAs(failure);

        verify(trigger).requestRefresh(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void nonQualifyingRoutedQueryFailureRethrowsWithoutNotifying() {
        stubRouting();
        DataIntegrityViolationException failure = new DataIntegrityViolationException("duplicate key");
        doThrow(failure).when(template).query(anyString(), any(RowMapper.class), any(Object[].class));

        assertThatThrownBy(() -> observedRouter.routedQuery(key, "SELECT 1", (rs, rowNum) -> 1))
                .isSameAs(failure);

        verifyNoInteractions(trigger);
    }

    @Test
    void routedWriteObservesIdentically() {
        when(placement.workerForBucket(7)).thenReturn(new CitusWorkerNode(3, "citus-worker-3", 5432));
        when(registry.templateForGroup(3)).thenReturn(template);
        RuntimeException failure = new RuntimeException("flush failed",
                new SQLTransientConnectionException("pool timeout"));

        assertThatThrownBy(() -> observedRouter.routedWrite(7, t -> {
            throw failure;
        })).isSameAs(failure);

        verify(trigger).requestRefresh(anyString());
    }

    @Test
    void routedWriteRunsWorkOnTheBucketOwnerTemplateAndReturnsItsResult() {
        when(placement.workerForBucket(7)).thenReturn(new CitusWorkerNode(3, "citus-worker-3", 5432));
        when(registry.templateForGroup(3)).thenReturn(template);

        String result = observedRouter.routedWrite(7, t -> {
            assertThat(t).isSameAs(template);
            return "saved";
        });

        assertThat(result).isEqualTo("saved");
        verifyNoInteractions(trigger);
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractorRoutedQueryRunsOnTheBucketOwnerTemplateAndReturnsItsResult() {
        // The ResultSetExtractor overload is the production DELETE ... RETURNING path
        // (JpaAttributeDao / SqlTimeseriesLatestDao) — pin its happy path like the RowMapper sibling's.
        stubRouting();
        when(template.query(anyString(), any(ResultSetExtractor.class), any(Object[].class))).thenReturn("deleted-row");

        String result = observedRouter.routedQuery(key, "DELETE ... RETURNING", rs -> "unused", "arg");

        assertThat(result).isEqualTo("deleted-row");
        verify(template).query(anyString(), any(ResultSetExtractor.class), any(Object[].class));
        verifyNoInteractions(trigger);
    }

    @Test
    @SuppressWarnings("unchecked")
    void qualifyingExtractorRoutedQueryFailureNotifiesTriggerAndRethrowsOriginal() {
        // A regression routing the extractor overload around observed() would slip failover detection
        // on the DELETE ... RETURNING path; pin the observation like the RowMapper sibling's.
        stubRouting();
        CannotGetJdbcConnectionException failure = new CannotGetJdbcConnectionException("connection refused");
        doThrow(failure).when(template).query(anyString(), any(ResultSetExtractor.class), any(Object[].class));

        assertThatThrownBy(() -> observedRouter.routedQuery(key, "DELETE ... RETURNING", rs -> "unused", "arg"))
                .as("the original exception must be rethrown unchanged")
                .isSameAs(failure);

        verify(trigger).requestRefresh(anyString());
    }

    @Test
    void noWorkerPoolFailureNotifiesTriggerAndRethrows() {
        // Template resolution runs inside the observed try: a bucket mapped to a group the registry has
        // no pool for (a reconcile skipped a failed pool build) must fire the failover trigger so the
        // state heals via the debounced refresh instead of waiting out the scheduled tick.
        when(locator.bucket(key)).thenReturn(7);
        when(placement.workerForBucket(7)).thenReturn(new CitusWorkerNode(3, "citus-worker-3", 5432));
        CitusNoWorkerPoolException failure = new CitusNoWorkerPoolException("No Citus worker pool for group 3");
        when(registry.templateForGroup(3)).thenThrow(failure);

        assertThatThrownBy(() -> observedRouter.routedQuery(key, "SELECT 1", (rs, rowNum) -> 1))
                .isSameAs(failure);

        verify(trigger).requestRefresh(anyString());
    }

    @Test
    void noWorkerPoolExceptionQualifiesButGenericIllegalStateDoesNot() {
        assertThat(CitusShardRouter.isFailoverFailure(
                new CitusNoWorkerPoolException("No Citus worker pool for group 3"))).isTrue();
        // Generic IllegalStateException also covers placement-not-loaded and bucket-alignment states a
        // refresh cannot fix — it must NOT trigger one.
        assertThat(CitusShardRouter.isFailoverFailure(
                new IllegalStateException("placement has not been loaded yet"))).isFalse();
    }
}
