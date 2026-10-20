// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.thingsboard.server.service.install.AbstractPostgresContainerTest;
import org.thingsboard.server.service.install.TbClusterSchema;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getClusterId()} against a real PostgreSQL, over the states a deployment patched in place from a version
 * that predates {@code tb_cluster} can be in while {@code SystemPatchApplier} is still working in the background.
 */
class DefaultTbLicenseCtxClusterIdTest extends AbstractPostgresContainerTest {

    private JdbcTemplate jdbcTemplate;
    private DefaultTbLicenseCtx licenseCtx;

    @BeforeEach
    void setUp() throws SQLException {
        execute("DROP TABLE IF EXISTS tb_cluster;");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        licenseCtx = new DefaultTbLicenseCtx(null, null, null, jdbcTemplate);
    }

    @Test
    void testCreatesTheTableAndMintsTheIdWhenTbClusterIsMissing() {
        UUID clusterId = licenseCtx.getClusterId();

        assertThat(clusterId).isNotNull();
        assertThat(readClusterIds()).containsExactly(clusterId);
        // The single-row backstop must come with the table: it is what keeps a second node from minting a second id.
        assertThat(indexExists("tb_cluster_single_row")).isTrue();
    }

    /**
     * The table is there and only its row is missing - the window between {@code schema_update.sql} committing and
     * {@code V4_3_1_6Migration.apply()} minting. The index still has to be created before the mint: the table can
     * predate it, and without it the mint's {@code ON CONFLICT DO NOTHING} has nothing to conflict on.
     */
    @Test
    void testCreatesTheSingleRowIndexBeforeMintingWhenOnlyTheRowIsMissing() throws SQLException {
        execute(TbClusterSchema.CREATE_CLUSTER_TABLE_QUERY);

        UUID clusterId = licenseCtx.getClusterId();

        assertThat(clusterId).isNotNull();
        assertThat(readClusterIds()).containsExactly(clusterId);
        assertThat(indexExists("tb_cluster_single_row")).isTrue();
    }

    @Test
    void testReturnsTheStoredIdAndMintsNothingWhenTheRowIsAlreadyThere() throws SQLException {
        execute(TbClusterSchema.CREATE_CLUSTER_TABLE_QUERY);
        execute(TbClusterSchema.CREATE_CLUSTER_SINGLE_ROW_INDEX_QUERY);
        UUID existing = UUID.randomUUID();
        execute("INSERT INTO tb_cluster (cluster_id) VALUES ('" + existing + "');");

        assertThat(licenseCtx.getClusterId()).isEqualTo(existing);
        assertThat(readClusterIds()).containsExactly(existing);
    }

    /**
     * A {@code tb_cluster} carrying more than one row - reachable only where the single-row index never got
     * created - must still answer with one id: a bare {@code SELECT} raises
     * {@code IncorrectResultSizeDataAccessException} there, and no retry can repair a second row.
     */
    @Test
    void testReadsOneIdFromATableThatSomehowHoldsTwoRows() throws SQLException {
        execute(TbClusterSchema.CREATE_CLUSTER_TABLE_QUERY);
        UUID lowest = new UUID(0L, 1L);
        UUID highest = new UUID(-1L, -1L);
        execute("INSERT INTO tb_cluster (cluster_id) VALUES ('" + lowest + "'), ('" + highest + "');");

        assertThat(licenseCtx.getClusterId()).isEqualTo(lowest);
    }

    /**
     * Two nodes self-healing at once, which is what a rolling restart onto the new package produces. The identity
     * the portal knows a deployment by must be one id, so whichever node loses the race has to come back with the
     * winner's - never a second one, and never an exception.
     */
    @Test
    void testConcurrentSelfHealAgreesOnOneClusterId() throws Exception {
        DefaultTbLicenseCtx otherNode = new DefaultTbLicenseCtx(null, null, null, jdbcTemplate);

        List<UUID> results = runTogether(licenseCtx::getClusterId, otherNode::getClusterId);

        assertThat(results.get(0)).isNotNull().isEqualTo(results.get(1));
        assertThat(readClusterIds()).containsExactly(results.get(0));
    }

    /**
     * The same race one state later, against a table that predates the single-row index. Each minter offers a
     * cluster id of its own, so the primary key cannot collide and only the index stops both rows from landing.
     */
    @Test
    void testConcurrentMintAgainstATableWithoutTheSingleRowIndexLeavesOneRow() throws Exception {
        execute(TbClusterSchema.CREATE_CLUSTER_TABLE_QUERY);
        DefaultTbLicenseCtx otherNode = new DefaultTbLicenseCtx(null, null, null, jdbcTemplate);

        List<UUID> results = runTogether(licenseCtx::getClusterId, otherNode::getClusterId);

        assertThat(readClusterIds()).hasSize(1);
        assertThat(results.get(0)).isNotNull().isEqualTo(results.get(1));
        assertThat(indexExists("tb_cluster_single_row")).isTrue();
    }

    private static List<UUID> runTogether(Supplier<UUID> first, Supplier<UUID> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier startTogether = new CyclicBarrier(2);
            Future<UUID> firstResult = executor.submit(() -> {
                startTogether.await();
                return first.get();
            });
            Future<UUID> secondResult = executor.submit(() -> {
                startTogether.await();
                return second.get();
            });
            return List.of(firstResult.get(), secondResult.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private List<UUID> readClusterIds() {
        return jdbcTemplate.queryForList("SELECT cluster_id FROM tb_cluster ORDER BY cluster_id", UUID.class);
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = ?", Integer.class, indexName);
        return count != null && count > 0;
    }

}
