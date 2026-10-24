// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trips {@link TbClusterStore#getNonProductionConfirmedTs} and
 * {@link TbClusterStore#saveNonProductionConfirmedTs} against the real {@code tb_cluster} column created by
 * {@code dao/src/main/resources/sql/schema-entities.sql} - the only schema source {@code PostgreSqlInitializer}
 * builds the test database from. It does not exercise either upgrade script
 * ({@code application/src/main/data/upgrade/lts/4.4.0.0/schema_update.sql} or
 * {@code application/src/main/data/upgrade/pe/schema_update.sql}): a column present here but missing, or
 * differently defined, in one of those would not be caught by this test.
 * <p>
 * A real {@link ResultSet} is what makes this worth having on top of the mocked unit test: the
 * column name is a bare string literal repeated across three SQL statements, and {@code wasNull()} is only
 * reachable against a real result set, not a mock.
 */
@DaoSqlTest
public class NonProductionConfirmedTsTest extends AbstractServiceTest {

    @Autowired
    private TbClusterStore tbClusterStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @After
    public void tearDown() {
        // tb_cluster is the single shared row: reset the column so the next test starts from the same null
        // baseline the installer leaves behind.
        jdbcTemplate.update("UPDATE tb_cluster SET non_production_confirmed_ts = NULL");
    }

    @Test
    public void anUnsetColumnIsEmpty() {
        assertThat(tbClusterStore.getNonProductionConfirmedTs()).isEmpty();
    }

    @Test
    public void aSavedTimestampIsReadBack() {
        long confirmedTs = 1_700_000_000_000L;
        tbClusterStore.saveNonProductionConfirmedTs(confirmedTs);

        assertThat(tbClusterStore.getNonProductionConfirmedTs()).contains(confirmedTs);
    }

    @Test
    public void aTimestampOfZeroIsStillPresent() {
        // The case that proves wasNull() is doing the work rather than ResultSet#getLong's zero default:
        // a confirmation recorded at the epoch must read back as present, not as "never confirmed".
        tbClusterStore.saveNonProductionConfirmedTs(0L);

        assertThat(tbClusterStore.getNonProductionConfirmedTs()).contains(0L);
    }

}
