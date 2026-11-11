// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The CE {@code tb_cluster} backport is claimed to be a schema no-op on the PE side, save for one deliberate
 * addition. This proves it instead of asserting it: the test rebuilds the CE 4.3.1.4 shape of the table -
 * {@code cluster_id} plus {@code license_claim_token}, none of the four PE-only columns, and no single-row
 * index - then runs the CE-to-PE conversion script exactly as
 * {@code SqlDatabaseUpgradeService.upgradeDatabase(true)} runs it, and checks that the identity the CE
 * instance already had survives untouched while the PE columns come back.
 * <p>
 * The identity surviving is the whole point of the backport: the converted instance keeps the cluster id it
 * was known by and the claim token it wrote, rather than being issued a new identity by
 * {@code generateClusterIdIfNotExist()} at the end of the upgrade.
 * <p>
 * The one thing the conversion does add is {@code tb_cluster_single_row}, the unique index on a constant
 * expression that caps the table at one row. That is intended, not incidental: every statement in
 * {@code TbClusterStore} is written for a single shared row, and the mint is only atomic because the database
 * refuses a second one. A CE instance old enough to predate that index therefore has to acquire it here,
 * which is why this test expects the index to appear rather than expecting the schema to be untouched.
 */
@DaoSqlTest
public class PeUpgradeClusterIdentityIntegrationTest extends AbstractControllerTest {

    private static final String[] PE_ONLY_COLUMNS = {
            "license_secret", "non_production_uptime_ms", "non_production_last_tick", "non_production_confirmed_ts"};
    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";
    private static final String CE_CLAIM_TOKEN_COLUMN = "license_claim_token";
    private static final String SINGLE_ROW_INDEX = "tb_cluster_single_row";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InstallScripts installScripts;

    @Test
    public void ceToPeConversionPreservesTheClusterIdentityAndRestoresEveryPeColumn() {
        // Snapshot everything the shared suite database may already hold in this single row, so the columns
        // this test drops can be put back exactly as they were.
        UUID clusterId = jdbcTemplate.queryForObject("SELECT cluster_id FROM tb_cluster", UUID.class);
        String licenseSecret = jdbcTemplate.queryForObject("SELECT license_secret FROM tb_cluster", String.class);
        Long uptimeMs = jdbcTemplate.queryForObject("SELECT non_production_uptime_ms FROM tb_cluster", Long.class);
        Long lastTick = jdbcTemplate.queryForObject("SELECT non_production_last_tick FROM tb_cluster", Long.class);
        Long confirmedTs = jdbcTemplate.queryForObject("SELECT non_production_confirmed_ts FROM tb_cluster", Long.class);
        String claimToken = jdbcTemplate.queryForObject("SELECT license_claim_token FROM tb_cluster", String.class);

        String ceClaimToken = "ce-claim-" + UUID.randomUUID();
        try {
            // Reconstruct the CE 4.3.1.4 shape: identity + claim token only, and none of the hardening a CE
            // instance of that vintage had yet.
            for (String column : PE_ONLY_COLUMNS) {
                jdbcTemplate.execute("ALTER TABLE tb_cluster DROP COLUMN IF EXISTS " + column);
                assertFalse(columnExists("tb_cluster", column));
            }
            jdbcTemplate.execute("DROP INDEX IF EXISTS " + SINGLE_ROW_INDEX);
            assertFalse(indexExists(SINGLE_ROW_INDEX));
            jdbcTemplate.update("UPDATE tb_cluster SET license_claim_token = ?", ceClaimToken);
            assertTrue(constraintExists("tb_cluster_pkey"));

            runPeSchemaUpdate();

            // Identity untouched: same single row, same cluster id, and the token the CE instance wrote is
            // still there for the converted instance to claim with.
            assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject("SELECT count(*) FROM tb_cluster", Integer.class));
            assertEquals(clusterId, jdbcTemplate.queryForObject("SELECT cluster_id FROM tb_cluster", UUID.class));
            assertEquals(ceClaimToken, jdbcTemplate.queryForObject("SELECT license_claim_token FROM tb_cluster", String.class));

            // Every PE column is back, and the NOT NULL DEFAULT 0 counter is seeded rather than left null.
            for (String column : PE_ONLY_COLUMNS) {
                assertTrue(column + " must exist after the CE-to-PE conversion", columnExists("tb_cluster", column));
            }
            assertEquals(Long.valueOf(0L),
                    jdbcTemplate.queryForObject("SELECT non_production_uptime_ms FROM tb_cluster", Long.class));

            // The single deliberate schema addition: the converted instance comes out with the single-row
            // backstop the PE code assumes, even though the CE instance it came from never had one.
            assertTrue("the CE-to-PE conversion must install the single-row backstop", indexExists(SINGLE_ROW_INDEX));

            // The file is idempotent by design: a re-run changes nothing and throws nothing - including the
            // CREATE UNIQUE INDEX IF NOT EXISTS, which sees the index it created on the first pass.
            runPeSchemaUpdate();
            assertEquals(clusterId, jdbcTemplate.queryForObject("SELECT cluster_id FROM tb_cluster", UUID.class));
            assertEquals(ceClaimToken, jdbcTemplate.queryForObject("SELECT license_claim_token FROM tb_cluster", String.class));
            assertTrue(indexExists(SINGLE_ROW_INDEX));
        } finally {
            // Unconditional: if the conversion threw before its ALTERs ran, the dropped columns would stay
            // missing and break every later test in the shared suite.
            jdbcTemplate.execute("ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS license_secret varchar");
            jdbcTemplate.execute("ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_uptime_ms bigint NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_last_tick bigint");
            jdbcTemplate.execute("ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_confirmed_ts bigint");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS " + SINGLE_ROW_INDEX + " ON tb_cluster ((true))");
            jdbcTemplate.update("UPDATE tb_cluster SET license_secret = ?, non_production_uptime_ms = ?, " +
                            "non_production_last_tick = ?, non_production_confirmed_ts = ?, license_claim_token = ?",
                    licenseSecret, uptimeMs == null ? 0L : uptimeMs, lastTick, confirmedTs, claimToken);
        }
    }

    /**
     * The lists above are a hand-maintained transcription of the {@code tb_cluster} block in
     * {@code upgrade/pe/schema_update.sql}, and nothing else would notice them drifting apart: a sixth
     * PE-only column added to the script is never dropped by the test, so "it comes back" would be vacuously
     * true for it, and the restore block would leave it behind if the conversion failed mid-way - the two
     * failure modes this class exists to rule out. Deriving the lists outright is not possible, because the
     * script's fifth ALTER is {@code license_claim_token}, the CE column the test must keep rather than drop.
     * So the script is parsed only to check the transcription, which turns a new column into "update this
     * test" instead of silent under-coverage.
     */
    @Test
    public void theScriptsTbClusterColumnsAreExactlyTheOnesThisTestAccountsFor() {
        List<String> expected = new ArrayList<>(List.of(PE_ONLY_COLUMNS));
        expected.add(CE_CLAIM_TOKEN_COLUMN);

        assertThat(scriptTbClusterAddedColumns())
                .as("tb_cluster ADD COLUMN statements in upgrade/pe/%s", SCHEMA_UPDATE_SQL)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    private List<String> scriptTbClusterAddedColumns() {
        List<String> columns = new ArrayList<>();
        Matcher matcher = Pattern.compile(
                        "^\\s*ALTER\\s+TABLE\\s+tb_cluster\\s+ADD\\s+COLUMN\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)",
                        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
                .matcher(peSchemaUpdateSql());
        while (matcher.find()) {
            columns.add(matcher.group(1).toLowerCase());
        }
        return columns;
    }

    // Executes upgrade/pe/schema_update.sql the way SqlDatabaseUpgradeService.upgradeDatabase(true) does:
    // the whole file as a single execute.
    private void runPeSchemaUpdate() {
        jdbcTemplate.execute(peSchemaUpdateSql());
    }

    private String peSchemaUpdateSql() {
        try {
            return Files.readString(Paths.get(installScripts.getDataDir(), "upgrade", "pe", SCHEMA_UPDATE_SQL));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the CE-to-PE " + SCHEMA_UPDATE_SQL, e);
        }
    }

    private boolean columnExists(String table, String column) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = ? AND column_name = ?)",
                Boolean.class, table, column);
        return Boolean.TRUE.equals(exists);
    }

    private boolean constraintExists(String constraintName) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ?)", Boolean.class, constraintName);
        return Boolean.TRUE.equals(exists);
    }

    private boolean indexExists(String indexName) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = ?)", Boolean.class, indexName);
        return Boolean.TRUE.equals(exists);
    }
}
