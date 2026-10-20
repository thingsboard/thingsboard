// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.StatementCallback;
import org.thingsboard.server.dao.sql.citus.routing.CitusWorkerRegistry.HostPort;
import org.thingsboard.server.dao.sql.citus.routing.CitusWorkerRegistry.WorkerPool;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitusWorkerRegistryTest {

    private static final CitusWorkerNode WORKER_1 = new CitusWorkerNode(1, "citus-worker-1", 5432);
    private static final CitusWorkerNode WORKER_2 = new CitusWorkerNode(2, "citus-worker-2", 5432);

    // --- effectiveHostPort: override mapping (design-mandated unit test) ---

    @Test
    void overridePresentWinsOverCatalogValues() {
        Map<String, String> overrides = Map.of("citus-worker-1", "10.0.0.5:6543");
        HostPort hp = CitusWorkerRegistry.effectiveHostPort(WORKER_1, overrides);
        assertThat(hp.host()).isEqualTo("10.0.0.5");
        assertThat(hp.port()).isEqualTo(6543);
    }

    @Test
    void overrideAbsentFallsBackToNodeNameAndPort() {
        HostPort hp = CitusWorkerRegistry.effectiveHostPort(WORKER_2, Map.of("citus-worker-1", "10.0.0.5:6543"));
        assertThat(hp.host()).isEqualTo("citus-worker-2");
        assertThat(hp.port()).isEqualTo(5432);
    }

    @Test
    void emptyOverridesFallsBackToCatalogValues() {
        HostPort hp = CitusWorkerRegistry.effectiveHostPort(WORKER_1, Map.of());
        assertThat(hp.host()).isEqualTo("citus-worker-1");
        assertThat(hp.port()).isEqualTo(5432);
    }

    @Test
    void overrideWhitespaceIsTolerated() {
        Map<String, String> overrides = Map.of("citus-worker-1", "  localhost : 7000 ");
        HostPort hp = CitusWorkerRegistry.effectiveHostPort(WORKER_1, overrides);
        assertThat(hp.host()).isEqualTo("localhost");
        assertThat(hp.port()).isEqualTo(7000);
    }

    @Test
    void overrideIpv6FormIsParsedOnLastColon() {
        Map<String, String> overrides = Map.of("citus-worker-1", "[::1]:5555");
        HostPort hp = CitusWorkerRegistry.effectiveHostPort(WORKER_1, overrides);
        assertThat(hp.host()).isEqualTo("[::1]");
        assertThat(hp.port()).isEqualTo(5555);
    }

    @Test
    void overrideMissingPortThrowsNamingNodeAndValue() {
        Map<String, String> overrides = Map.of("citus-worker-1", "10.0.0.5");
        assertThatThrownBy(() -> CitusWorkerRegistry.effectiveHostPort(WORKER_1, overrides))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("citus-worker-1")
                .hasMessageContaining("10.0.0.5");
    }

    @Test
    void overrideNonNumericPortThrowsNamingNodeAndValue() {
        Map<String, String> overrides = Map.of("citus-worker-1", "host:notaport");
        assertThatThrownBy(() -> CitusWorkerRegistry.effectiveHostPort(WORKER_1, overrides))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("citus-worker-1")
                .hasMessageContaining("host:notaport");
    }

    @Test
    void overrideBlankHostThrows() {
        Map<String, String> overrides = Map.of("citus-worker-1", ":5432");
        assertThatThrownBy(() -> CitusWorkerRegistry.effectiveHostPort(WORKER_1, overrides))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("citus-worker-1");
    }

    @Test
    void overrideBareTrailingColonThrows() {
        // "host:" has its last colon as the final char (lastColon == length-1): no port component.
        Map<String, String> overrides = Map.of("citus-worker-1", "host:");
        assertThatThrownBy(() -> CitusWorkerRegistry.effectiveHostPort(WORKER_1, overrides))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("citus-worker-1")
                .hasMessageContaining("host:");
    }

    // --- dbName parsing from jdbc url ---

    @Test
    void databaseNameParsedFromSimpleUrl() {
        assertThat(CitusWorkerRegistry.databaseName("jdbc:postgresql://localhost:5432/thingsboard_pe"))
                .isEqualTo("thingsboard_pe");
    }

    @Test
    void databaseNameParsedStrippingQueryParams() {
        assertThat(CitusWorkerRegistry.databaseName("jdbc:postgresql://host:5432/tb?ssl=true&foo=bar"))
                .isEqualTo("tb");
    }

    @Test
    void databaseNameTrailingSlashWithNoDbThrows() {
        // Trailing slash -> last slash is the final char: no db segment to extract.
        assertThatThrownBy(() -> CitusWorkerRegistry.databaseName("jdbc:postgresql://h:5432/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jdbc:postgresql://h:5432/");
    }

    @Test
    void databaseNameMalformedUrlThrows() {
        // A string with no slash at all cannot yield a db name.
        assertThatThrownBy(() -> CitusWorkerRegistry.databaseName("garbage"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("garbage");
    }

    // --- worker jdbc url derivation from coordinator url ---

    @Test
    void workerJdbcUrlSubstitutesAuthorityPreservingDbPathAndPinsPrimary() {
        // Only the host:port authority is replaced; the database path is preserved. With no query string,
        // one is created to carry the unconditional targetServerType=primary write-safety pin and the
        // socketTimeout frozen-host bound.
        assertThat(CitusWorkerRegistry.workerJdbcUrl("jdbc:postgresql://coordinator:5432/thingsboard_pe", "10.0.0.7:6543", 30))
                .isEqualTo("jdbc:postgresql://10.0.0.7:6543/thingsboard_pe?targetServerType=primary&socketTimeout=30");
    }

    @Test
    void workerJdbcUrlPreservesQueryStringOntoNewAuthorityAndAppendsPrimary() {
        // The load-bearing case: connection params on the coordinator url must survive onto the worker pool url.
        assertThat(CitusWorkerRegistry.workerJdbcUrl("jdbc:postgresql://coordinator:5432/tb?sslmode=require&ApplicationName=tb", "citus-worker-1:5432", 30))
                .isEqualTo("jdbc:postgresql://citus-worker-1:5432/tb?sslmode=require&ApplicationName=tb&targetServerType=primary&socketTimeout=30");
    }

    @Test
    void workerJdbcUrlOverridesCoordinatorSuppliedTargetServerType() {
        // A coordinator url may legitimately carry its own targetServerType (e.g. 'any' against a proxy);
        // the worker pool value MUST win — a worker pool never talks to a non-writable server.
        assertThat(CitusWorkerRegistry.workerJdbcUrl("jdbc:postgresql://coordinator:5432/tb?targetServerType=any&sslmode=require", "citus-worker-1:5432", 30))
                .isEqualTo("jdbc:postgresql://citus-worker-1:5432/tb?sslmode=require&targetServerType=primary&socketTimeout=30");
    }

    @Test
    void workerJdbcUrlKeepsCoordinatorSuppliedSocketTimeout() {
        // socketTimeout is merged as a DEFAULT: an operator who tuned it on the coordinator url keeps their value.
        assertThat(CitusWorkerRegistry.workerJdbcUrl("jdbc:postgresql://coordinator:5432/tb?socketTimeout=120", "citus-worker-1:5432", 30))
                .isEqualTo("jdbc:postgresql://citus-worker-1:5432/tb?socketTimeout=120&targetServerType=primary");
    }

    @Test
    void workerJdbcUrlWithQueryButNoDbPathStillSubstitutesAuthority() {
        // The authority terminates at '?' even when there is no '/db' segment, and the query is preserved.
        assertThat(CitusWorkerRegistry.workerJdbcUrl("jdbc:postgresql://coordinator:5432?sslmode=require", "citus-worker-2:5432", 30))
                .isEqualTo("jdbc:postgresql://citus-worker-2:5432?sslmode=require&targetServerType=primary&socketTimeout=30");
    }

    @Test
    void workerJdbcUrlWithoutAuthorityMarkerThrows() {
        // No '//' authority marker -> cannot locate the host:port to substitute.
        assertThatThrownBy(() -> CitusWorkerRegistry.workerJdbcUrl("jdbc:postgresql:garbage", "citus-worker-1:5432", 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jdbc:postgresql:garbage");
    }

    // --- node discovery row mapping (mocked JdbcTemplate) ---

    @Test
    @SuppressWarnings("unchecked")
    void discoverWorkersMapsRowsWithRealRowMapper() throws SQLException {
        // Drive the ACTUAL production WORKER_ROW_MAPPER over a mocked ResultSet instead of returning a canned list,
        // so the discovery query's column names AND the mapper's (groupid, nodename, nodeport) argument order are
        // both pinned: a rename/reorder that broke discovery would fail here.
        ResultSet firstRow = mock(ResultSet.class);
        when(firstRow.getInt("groupid")).thenReturn(1);
        when(firstRow.getString("nodename")).thenReturn("citus-worker-1");
        when(firstRow.getInt("nodeport")).thenReturn(5432);
        ResultSet secondRow = mock(ResultSet.class);
        when(secondRow.getInt("groupid")).thenReturn(2);
        when(secondRow.getString("nodename")).thenReturn("citus-worker-2");
        when(secondRow.getInt("nodeport")).thenReturn(5432);

        JdbcTemplate coordinator = mock(JdbcTemplate.class);
        // The stub answers by running the captured RowMapper over the mocked rows, exactly as JdbcTemplate.query would.
        when(coordinator.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<CitusWorkerNode> rowMapper = invocation.getArgument(1);
            return List.of(rowMapper.mapRow(firstRow, 0), rowMapper.mapRow(secondRow, 1));
        });

        List<CitusWorkerNode> workers = CitusWorkerRegistry.discoverWorkers(coordinator);

        assertThat(workers).containsExactly(WORKER_1, WORKER_2);
    }

    // --- reconcile: endpoint-drift self-healing (mocked catalog, stubbed pool building) ---

    /**
     * Registry with {@code buildAndProbe} stubbed out: pools are mock-backed instead of real Hikari
     * pools, endpoints can be marked unreachable to simulate probe failures, and every build attempt
     * is recorded — so reconcile()'s add/retire/swap logic is unit-testable without worker databases.
     */
    private static class TestableWorkerRegistry extends CitusWorkerRegistry {

        final Set<String> unreachableEndpoints = new HashSet<>();
        final Set<Integer> nonWritableGroups = new HashSet<>();
        final List<String> builtEndpoints = new ArrayList<>();
        final List<Integer> probedGroups = new ArrayList<>();
        final Map<String, HikariDataSource> dataSourcesByEndpoint = new HashMap<>();

        TestableWorkerRegistry(JdbcTemplate coordinatorJdbcTemplate) {
            super(coordinatorJdbcTemplate, new CitusSmartRoutingSettings(),
                    "jdbc:postgresql://coordinator:5432/thingsboard_pe", "postgres", "postgres");
        }

        @Override
        WorkerPool buildAndProbe(CitusWorkerNode worker, HostPort hostPort) {
            String endpoint = hostPort.host() + ":" + hostPort.port();
            builtEndpoints.add(endpoint);
            if (unreachableEndpoints.contains(endpoint)) {
                throw new IllegalStateException("probe failed for " + endpoint);
            }
            HikariDataSource dataSource = mock(HikariDataSource.class);
            dataSourcesByEndpoint.put(endpoint, dataSource);
            return new WorkerPool(worker.groupId(), endpoint, dataSource, mock(JdbcTemplate.class));
        }

        @Override
        boolean probeWritable(WorkerPool pool) {
            probedGroups.add(pool.groupId());
            return !nonWritableGroups.contains(pool.groupId());
        }
    }

    @SuppressWarnings("unchecked")
    private static void stubCatalog(JdbcTemplate coordinator, CitusWorkerNode... workers) {
        when(coordinator.query(anyString(), any(RowMapper.class))).thenReturn(List.of(workers));
    }

    @Test
    void endpointDriftRebuildsPoolAndClosesOldOne() {
        JdbcTemplate coordinator = mock(JdbcTemplate.class);
        TestableWorkerRegistry registry = new TestableWorkerRegistry(coordinator);
        stubCatalog(coordinator, WORKER_1);
        registry.start();
        JdbcTemplate oldTemplate = registry.templateForGroup(1);
        HikariDataSource oldDataSource = registry.dataSourcesByEndpoint.get("citus-worker-1:5432");

        // same groupId, new advertised endpoint (e.g. the worker pod restarted on a new address)
        stubCatalog(coordinator, new CitusWorkerNode(1, "citus-worker-1", 5433));
        registry.refresh();

        assertThat(registry.builtEndpoints).containsExactly("citus-worker-1:5432", "citus-worker-1:5433");
        assertThat(registry.templateForGroup(1))
                .as("lock-free readers must now get the rebuilt pool's template")
                .isNotSameAs(oldTemplate);
        verify(oldDataSource).close();
        verify(registry.dataSourcesByEndpoint.get("citus-worker-1:5433"), never()).close();
        assertThat(registry.workerGroupIds()).containsExactly(1);
    }

    @Test
    void driftWithUnreachableNewEndpointKeepsOldPoolAndHealsOnLaterRefresh() {
        JdbcTemplate coordinator = mock(JdbcTemplate.class);
        TestableWorkerRegistry registry = new TestableWorkerRegistry(coordinator);
        stubCatalog(coordinator, WORKER_1);
        registry.start();
        JdbcTemplate oldTemplate = registry.templateForGroup(1);
        HikariDataSource oldDataSource = registry.dataSourcesByEndpoint.get("citus-worker-1:5432");

        // group 1 drifts to an endpoint whose probe fails; group 2 appears alongside it
        stubCatalog(coordinator, new CitusWorkerNode(1, "citus-worker-1", 5433), WORKER_2);
        registry.unreachableEndpoints.add("citus-worker-1:5433");
        assertThatCode(registry::refresh)
                .as("a failed swap probe must not escape a scheduled refresh")
                .doesNotThrowAnyException();

        assertThat(registry.templateForGroup(1))
                .as("old pool must survive until a swap probe succeeds")
                .isSameAs(oldTemplate);
        verify(oldDataSource, never()).close();
        assertThat(registry.workerGroupIds())
                .as("one bad group must not block the rest of the reconcile")
                .containsExactlyInAnyOrder(1, 2);

        // the new endpoint becomes reachable: the next refresh heals the drift
        registry.unreachableEndpoints.clear();
        registry.refresh();

        assertThat(registry.templateForGroup(1)).isNotSameAs(oldTemplate);
        verify(oldDataSource).close();
        verify(registry.dataSourcesByEndpoint.get("citus-worker-1:5433"), never()).close();
    }

    @Test
    void unchangedEndpointLeavesPoolInstanceUntouched() {
        JdbcTemplate coordinator = mock(JdbcTemplate.class);
        TestableWorkerRegistry registry = new TestableWorkerRegistry(coordinator);
        stubCatalog(coordinator, WORKER_1, WORKER_2);
        registry.start();
        JdbcTemplate template1 = registry.templateForGroup(1);
        JdbcTemplate template2 = registry.templateForGroup(2);

        registry.refresh();

        assertThat(registry.templateForGroup(1)).isSameAs(template1);
        assertThat(registry.templateForGroup(2)).isSameAs(template2);
        assertThat(registry.builtEndpoints)
                .as("an unchanged refresh must not rebuild anything")
                .containsExactly("citus-worker-1:5432", "citus-worker-2:5432");
        assertThat(registry.probedGroups)
                .as("every unchanged-endpoint group must be writability-probed on refresh")
                .containsExactly(1, 2);
        verify(registry.dataSourcesByEndpoint.get("citus-worker-1:5432"), never()).close();
        verify(registry.dataSourcesByEndpoint.get("citus-worker-2:5432"), never()).close();
    }

    // --- reconcile: writability probe on unchanged endpoints (failover behind a stable name) ---

    @Test
    void failedWritabilityProbeOnUnchangedEndpointRebuildsPool() {
        JdbcTemplate coordinator = mock(JdbcTemplate.class);
        TestableWorkerRegistry registry = new TestableWorkerRegistry(coordinator);
        stubCatalog(coordinator, WORKER_1);
        registry.start();
        JdbcTemplate oldTemplate = registry.templateForGroup(1);
        HikariDataSource oldDataSource = registry.dataSourcesByEndpoint.get("citus-worker-1:5432");

        // The endpoint string is UNCHANGED but the pooled server got demoted (VIP/proxy topology, or a
        // re-promotion behind the same DNS name); the rebuild through the same endpoint succeeds (the
        // name now resolves to the new leader).
        registry.nonWritableGroups.add(1);
        registry.refresh();

        assertThat(registry.builtEndpoints)
                .as("a failed writability probe must trigger a rebuild through the same endpoint")
                .containsExactly("citus-worker-1:5432", "citus-worker-1:5432");
        assertThat(registry.templateForGroup(1)).isNotSameAs(oldTemplate);
        verify(oldDataSource).close();
        verify(registry.dataSourcesByEndpoint.get("citus-worker-1:5432"), never()).close();
        assertThat(registry.workerGroupIds()).containsExactly(1);
    }

    @Test
    void failedWritabilityProbeWithFailedRebuildKeepsOldPool() {
        JdbcTemplate coordinator = mock(JdbcTemplate.class);
        TestableWorkerRegistry registry = new TestableWorkerRegistry(coordinator);
        stubCatalog(coordinator, WORKER_1);
        registry.start();
        JdbcTemplate oldTemplate = registry.templateForGroup(1);
        HikariDataSource oldDataSource = registry.dataSourcesByEndpoint.get("citus-worker-1:5432");

        // Demoted behind an unchanged name AND the endpoint still resolves to the standby: the rebuild is
        // refused too. The old pool must survive (keep-a-running-app) and the next cycle retries.
        registry.nonWritableGroups.add(1);
        registry.unreachableEndpoints.add("citus-worker-1:5432");
        assertThatCode(registry::refresh)
                .as("a failed probe-driven rebuild must not escape a scheduled refresh")
                .doesNotThrowAnyException();

        assertThat(registry.templateForGroup(1))
                .as("old pool must survive until a rebuild succeeds")
                .isSameAs(oldTemplate);
        verify(oldDataSource, never()).close();

        // the endpoint becomes writable again (catalog flipped / leader re-promoted): the next refresh heals
        registry.nonWritableGroups.clear();
        registry.unreachableEndpoints.clear();
        registry.refresh();

        assertThat(registry.templateForGroup(1))
                .as("a healthy probe must keep the pool untouched again")
                .isSameAs(oldTemplate);
        verify(oldDataSource, never()).close();
    }

    // --- start(): fail-fast rollback of pools opened during a failed startup pass ---

    @Test
    void failedStartRollsBackPoolsOpenedDuringTheFailedPass() {
        JdbcTemplate coordinator = mock(JdbcTemplate.class);
        TestableWorkerRegistry registry = new TestableWorkerRegistry(coordinator);
        // Catalog order guarantees worker-1's pool is opened before worker-2's build throws.
        stubCatalog(coordinator, WORKER_1, WORKER_2);
        registry.unreachableEndpoints.add("citus-worker-2:5432");

        assertThatThrownBy(registry::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("citus-worker-2");

        assertThat(registry.workerGroupIds())
                .as("a failed fail-fast boot must not leave any pool registered")
                .isEmpty();
        // the pool opened before the failure must be closed by the rollback (leak-free failed boot)
        verify(registry.dataSourcesByEndpoint.get("citus-worker-1:5432")).close();
    }

    // --- refresh(): retirement of a group that disappeared from pg_dist_node ---

    @Test
    void groupGoneFromCatalogIsRetiredWhileSurvivorPoolIsUntouched() {
        JdbcTemplate coordinator = mock(JdbcTemplate.class);
        TestableWorkerRegistry registry = new TestableWorkerRegistry(coordinator);
        stubCatalog(coordinator, WORKER_1, WORKER_2);
        registry.start();
        JdbcTemplate survivorTemplate = registry.templateForGroup(1);
        HikariDataSource survivorDataSource = registry.dataSourcesByEndpoint.get("citus-worker-1:5432");
        HikariDataSource retiredDataSource = registry.dataSourcesByEndpoint.get("citus-worker-2:5432");

        // worker-2 disappears from pg_dist_node (removed / rebalanced away): refresh must retire its pool.
        stubCatalog(coordinator, WORKER_1);
        registry.refresh();

        assertThat(registry.workerGroupIds()).containsExactly(1);
        verify(retiredDataSource).close();
        verify(survivorDataSource, never()).close();
        assertThat(registry.templateForGroup(1))
                .as("the surviving pool instance must be untouched by the retirement")
                .isSameAs(survivorTemplate);
        assertThatThrownBy(() -> registry.templateForGroup(2))
                .as("routing to the retired group must surface the no-pool error, not fall back")
                .isInstanceOf(CitusNoWorkerPoolException.class);
    }

    // --- probeWritable: false ONLY on a 25006 read-only demotion; every other failure keeps the pool ---

    private static CitusWorkerRegistry baseRegistry() {
        return new CitusWorkerRegistry(mock(JdbcTemplate.class), new CitusSmartRoutingSettings(),
                "jdbc:postgresql://coordinator:5432/thingsboard_pe", "postgres", "postgres");
    }

    @Test
    @SuppressWarnings("unchecked")
    void probeWritableKeepsPoolOnBorrowTimeout() {
        JdbcTemplate template = mock(JdbcTemplate.class);
        // A Hikari borrow timeout: the probe statement never executed, so this is saturation, not a demotion.
        when(template.execute(any(StatementCallback.class)))
                .thenThrow(new CannotGetJdbcConnectionException("pool exhausted",
                        new SQLTransientConnectionException("HikariPool - Connection is not available")));
        WorkerPool pool = new WorkerPool(1, "citus-worker-1:5432", mock(HikariDataSource.class), template);

        assertThat(baseRegistry().probeWritable(pool))
                .as("an inconclusive borrow timeout must keep the pool (no rebuild)")
                .isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void probeWritableRebuildsOnReadOnlySqlTransaction() {
        JdbcTemplate template = mock(JdbcTemplate.class);
        // The probe executed and the server refused the write with read_only_sql_transaction (25006): a real demotion.
        when(template.execute(any(StatementCallback.class)))
                .thenThrow(new UncategorizedSQLException("probe", CitusWorkerRegistry.WRITABILITY_PROBE_QUERY,
                        new SQLException("cannot execute in a read-only transaction",
                                CitusWorkerRegistry.READ_ONLY_SQL_TRANSACTION_SQL_STATE)));
        WorkerPool pool = new WorkerPool(1, "citus-worker-1:5432", mock(HikariDataSource.class), template);

        assertThat(baseRegistry().probeWritable(pool))
                .as("a definitive 25006 read-only error must send the group to the rebuild path")
                .isFalse();
    }

    @Test
    void isReadOnlySqlTransactionFindsStateDeepInCauseChain() {
        SQLException readOnly = new SQLException("read only", CitusWorkerRegistry.READ_ONLY_SQL_TRANSACTION_SQL_STATE);
        Throwable wrapped = new RuntimeException("outer", new IllegalStateException("mid", readOnly));
        assertThat(CitusWorkerRegistry.isReadOnlySqlTransaction(wrapped)).isTrue();
    }

    @Test
    void isReadOnlySqlTransactionFindsStateOnChainedNextException() {
        // The PostgreSQL batch shape: 25006 sits only on the getNextException chain, not on the head or a cause.
        SQLException head = new SQLException("batch failed", "00000");
        head.setNextException(new SQLException("read only", CitusWorkerRegistry.READ_ONLY_SQL_TRANSACTION_SQL_STATE));
        assertThat(CitusWorkerRegistry.isReadOnlySqlTransaction(new RuntimeException("outer", head))).isTrue();
    }

    @Test
    void isReadOnlySqlTransactionFalseForOtherSqlState() {
        SQLException queryCancelled = new SQLException("statement timeout", "57014");
        assertThat(CitusWorkerRegistry.isReadOnlySqlTransaction(new RuntimeException(queryCancelled))).isFalse();
    }
}
