// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.service.install.TbClusterSchema;

import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The self-heal, driven through a stubbed {@link JdbcTemplate}.
 */
class DefaultTbLicenseCtxSelfHealTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final DefaultTbLicenseCtx licenseCtx = new DefaultTbLicenseCtx(null, null, null, jdbcTemplate);

    /**
     * The outage this whole change exists to prevent: node B loses the CREATE TABLE race (PostgreSQL's
     * {@code IF NOT EXISTS} is not atomic and raises a duplicate key on {@code pg_type}).
     */
    @Test
    void testReturnsTheWinnersIdAfterLosingTheCreateRace() {
        UUID winnersId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(TbClusterSchema.SELECT_CLUSTER_ID_QUERY, UUID.class))
                .thenThrow(missingTable())
                .thenReturn(winnersId);
        doThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"pg_type_typname_nsp_index\""))
                .when(jdbcTemplate).execute(TbClusterSchema.CREATE_CLUSTER_TABLE_QUERY);

        assertThat(licenseCtx.getClusterId()).isEqualTo(winnersId);

        // A failed CREATE TABLE must not skip the statements after it.
        verify(jdbcTemplate).execute(TbClusterSchema.CREATE_CLUSTER_SINGLE_ROW_INDEX_QUERY);
        verify(jdbcTemplate).update(eq(TbClusterSchema.INSERT_CLUSTER_ID_QUERY), any(Object.class));
    }

    /**
     * An empty table means someone else owns the {@code CREATE TABLE}, but the single-row index still has to be
     * there before the mint - the table may predate it - so that is the one statement this branch issues.
     */
    @Test
    void testCreatesOnlyTheSingleRowIndexWhenOnlyTheRowIsMissing() {
        UUID minted = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(TbClusterSchema.SELECT_CLUSTER_ID_QUERY, UUID.class))
                .thenThrow(new EmptyResultDataAccessException(1))
                .thenReturn(minted);

        assertThat(licenseCtx.getClusterId()).isEqualTo(minted);

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).execute(TbClusterSchema.CREATE_CLUSTER_SINGLE_ROW_INDEX_QUERY);
        inOrder.verify(jdbcTemplate).update(eq(TbClusterSchema.INSERT_CLUSTER_ID_QUERY), any(Object.class));
        verify(jdbcTemplate, never()).execute(TbClusterSchema.CREATE_CLUSTER_TABLE_QUERY);
    }

    /** An id still missing after one self-heal pass is a genuine fault, so it propagates and fails the node. */
    @Test
    void testGivesUpAndPropagatesWhenTheIdNeverAppears() {
        when(jdbcTemplate.queryForObject(TbClusterSchema.SELECT_CLUSTER_ID_QUERY, UUID.class))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(licenseCtx::getClusterId).isInstanceOf(EmptyResultDataAccessException.class);

        verify(jdbcTemplate).update(eq(TbClusterSchema.INSERT_CLUSTER_ID_QUERY), any(Object.class));
    }

    private static BadSqlGrammarException missingTable() {
        return new BadSqlGrammarException("read cluster id", TbClusterSchema.SELECT_CLUSTER_ID_QUERY,
                new SQLException("relation \"tb_cluster\" does not exist", "42P01"));
    }

}
