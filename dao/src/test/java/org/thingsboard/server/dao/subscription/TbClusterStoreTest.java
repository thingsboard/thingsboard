// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The half of {@link TbClusterStore}'s contract that is about how a statement's outcome is treated rather
 * than about the schema it runs against, so it is driven off a {@link JdbcTemplate} mock instead of the real
 * database - see {@link TbClusterStoreSqlTest} for the round-trips that need the schema.
 * <p>
 * Driving these off a mock is not a shortcut: there is no way to make the real template fail, or to present
 * an empty {@code tb_cluster}, without damaging the single seeded row every other test in that suite shares.
 * Keeping them out of the Spring context also means they cost nothing to run.
 */
public class TbClusterStoreTest {

    /**
     * The bite behind both clear methods' "Never throws" javadoc. They are called on paths whose outcome does
     * not depend on them - an activation that already put a license in place, a claim the portal rejected -
     * so a database that is down or a statement that is rejected must not turn best-effort hygiene into a
     * failed user action. Removing either try/catch fails this.
     */
    @Test
    public void clearSwallowsAStoreFailureRatherThanPropagatingIt() {
        JdbcTemplate failing = mock(JdbcTemplate.class, invocation -> {
            throw new DataAccessResourceFailureException("the database is unreachable");
        });
        TbClusterStore store = new TbClusterStore(failing);

        assertThatCode(store::forceClearLicenseClaimToken).doesNotThrowAnyException();
        assertThatCode(() -> store.clearLicenseClaimToken("claim-token")).doesNotThrowAnyException();
    }

    /**
     * The other half of the contract the clear methods relax: a save that writes nothing has silently lost a
     * value every other node was meant to read, so it must fail loudly rather than return as if it worked.
     * An empty {@code tb_cluster} is the way that happens, and the update count is how the store detects it.
     */
    @Test
    public void saveFailsLoudlyWhenNoRowWasWritten() {
        JdbcTemplate empty = mock(JdbcTemplate.class);
        TbClusterStore store = new TbClusterStore(empty);

        assertThatThrownBy(() -> store.saveLicenseClaimToken("claim-token"))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * The same zero-row update, through the two clear methods, which must stay silent where the save above
     * throws. In PostgreSQL an {@code UPDATE} matching no row is not an error, so neither clear ever reaches
     * its catch block here - this pins the difference in treatment, not the exception handling.
     */
    @Test
    public void clearStaysSilentWhenNoRowWasWritten() {
        JdbcTemplate empty = mock(JdbcTemplate.class);
        TbClusterStore store = new TbClusterStore(empty);

        assertThatCode(store::forceClearLicenseClaimToken).doesNotThrowAnyException();
        assertThatCode(() -> store.clearLicenseClaimToken("claim-token")).doesNotThrowAnyException();
    }

    /**
     * The read side of the invariant the saves declare. An absent row and a NULL column are different facts:
     * callers read a NULL column as a value the cluster deliberately cleared and give the licence up on it, so
     * an empty table - a stale replica, a restored dump, an install that did not finish - must reach their
     * error handling instead of their success path.
     */
    @Test
    public void readFailsLoudlyOnAMissingRowButAnswersEmptyForANullColumn() {
        assertThatThrownBy(storeReading(null, false)::getLicenseSecret).isInstanceOf(IllegalStateException.class);

        assertThat(storeReading(null, true).getLicenseSecret()).isEmpty();
        assertThat(storeReading("license-secret", true).getLicenseSecret()).contains("license-secret");
    }

    /** A store whose reads see either no row at all, or one row holding {@code value}. */
    @SuppressWarnings("unchecked")
    private static TbClusterStore storeReading(String value, boolean rowPresent) {
        try {
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(rowPresent);
            when(resultSet.getString(anyString())).thenReturn(value);
            JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
            when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenAnswer(invocation ->
                    ((ResultSetExtractor<Optional<String>>) invocation.getArgument(1)).extractData(resultSet));
            return new TbClusterStore(jdbcTemplate);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
