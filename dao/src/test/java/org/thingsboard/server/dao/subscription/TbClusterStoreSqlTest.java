// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The claim token and the license secret are read and written through hand-written SQL against column names
 * that live in three separate schema files, and every other test of those paths mocks this store away.
 * Without a real round-trip a typo between the statement and the schema - or a column missing from one of
 * those files - would only surface during a customer's first-time setup.
 * <p>
 * Only the round-trips that genuinely need the schema live here; the store's outcome handling is pinned
 * without a database by {@link TbClusterStoreTest}.
 */
@DaoSqlTest
public class TbClusterStoreSqlTest extends AbstractServiceTest {

    @Autowired
    private TbClusterStore tbClusterStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @After
    public void tearDown() {
        // tb_cluster is the single shared row: reset the columns these tests write directly rather than
        // through the store, whose clear methods swallow every failure and would leave a value behind for
        // whichever test happens to run next.
        jdbcTemplate.update("UPDATE tb_cluster SET license_claim_token = NULL, license_secret = NULL");
    }

    @Test
    public void licenseClaimTokenRoundTripsThroughTheRealSchema() {
        tbClusterStore.saveLicenseClaimToken("claim-token");

        assertThat(tbClusterStore.getLicenseClaimToken()).contains("claim-token");
    }

    /**
     * The same hand-written SQL risk, on the higher-stakes column: the license secret is how a whole cluster
     * shares one activation, and every other test that touches it mocks this store away.
     */
    @Test
    public void licenseSecretRoundTripsThroughTheRealSchema() {
        assertThat(tbClusterStore.getLicenseSecret()).isEmpty();

        tbClusterStore.saveLicenseSecret("license-secret");

        assertThat(tbClusterStore.getLicenseSecret()).contains("license-secret");
    }

    @Test
    public void compareAndClearLeavesADifferentStoredTokenIntact() {
        tbClusterStore.saveLicenseClaimToken("fresh-claim-token");

        // The whole point of the compare-and-clear: a claim that comes back terminal must not wipe the token
        // a fresh claim request stored while it was out.
        tbClusterStore.clearLicenseClaimToken("superseded-claim-token");

        assertThat(tbClusterStore.getLicenseClaimToken()).contains("fresh-claim-token");
    }

    @Test
    public void compareAndClearDropsAMatchingStoredToken() {
        tbClusterStore.saveLicenseClaimToken("claim-token");

        tbClusterStore.clearLicenseClaimToken("claim-token");

        assertThat(tbClusterStore.getLicenseClaimToken()).isEmpty();
    }

    @Test
    public void unconditionalClearDropsWhateverIsStored() {
        tbClusterStore.saveLicenseClaimToken("claim-token");

        tbClusterStore.forceClearLicenseClaimToken();

        assertThat(tbClusterStore.getLicenseClaimToken()).isEmpty();
    }

    /**
     * Every statement in this store is written for the single shared row - no {@code WHERE}, no
     * {@code ORDER BY} - so "there is exactly one row" is an invariant the class depends on rather than one
     * it checks. {@code schema-entities.sql} enforces it with a unique index on a constant expression; this
     * proves the enforcement is really in the schema the store runs against, not only in the file, by trying
     * the one thing that would break the invariant.
     */
    @Test
    public void secondRowInsertIsRejectedByTheSingleRowIndex() {
        UUID rejectedRowId = UUID.randomUUID();
        try {
            assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id) VALUES (?)", rejectedRowId))
                    .isInstanceOf(DataIntegrityViolationException.class);

            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tb_cluster", Integer.class)).isEqualTo(1);
        } finally {
            // If the insert above unexpectedly succeeded (the unique index failed to enforce the single-row
            // invariant), this leaves a stray second row behind. TbClusterStore.getColumn() uses LIMIT 1 with
            // no ORDER BY and updateColumn() has no WHERE, so a stray row would make unrelated tests
            // (NonProductionUptimeTest, NonProductionConfirmedTsTest, BasicLicenseActivationServiceTest) fail
            // nondeterministically. Clean up only the row this test tried to insert - never the seeded row.
            jdbcTemplate.update("DELETE FROM tb_cluster WHERE cluster_id = ?", rejectedRowId);
        }
    }

}
