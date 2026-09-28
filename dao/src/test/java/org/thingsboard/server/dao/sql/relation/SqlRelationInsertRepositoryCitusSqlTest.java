// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.relation;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.CitusTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

class SqlRelationInsertRepositoryCitusSqlTest {

    private SqlRelationInsertRepository repoWith(boolean citus) {
        SqlRelationInsertRepository repo = new SqlRelationInsertRepository();
        ReflectionTestUtils.setField(repo, "citusSettings", CitusTestSupport.citusSettings(citus));
        ReflectionTestUtils.invokeMethod(repo, "initQueries");
        return repo;
    }

    @Test
    void plainModeUsesGlobalSequence() {
        SqlRelationInsertRepository repo = repoWith(false);
        assertThat(repo.getInsertJpaQuery()).contains("nextval('relation_version_seq')");
        assertThat(repo.getInsertJdbcQuery()).contains("nextval('relation_version_seq')");
    }

    // Asserts only the behavioral invariants of the Citus query shape (no global sequence; an upsert that
    // increments the per-row version), tolerant of column ordering and whitespace. The concrete per-row
    // version contract (save=1, save=2, delete+recreate restarts at 1) is verified end-to-end against a real
    // Citus cluster by CitusRelationServiceTest#testPerRowVersionContract.
    @Test
    void citusModeUsesPerRowIncrement() {
        SqlRelationInsertRepository repo = repoWith(true);
        assertThat(repo.getInsertJpaQuery())
                .doesNotContain("nextval")
                .containsIgnoringCase("on conflict")
                .containsIgnoringWhitespaces("version = relation.version + 1");
        assertThat(repo.getInsertJdbcQuery())
                .doesNotContain("nextval")
                .containsIgnoringCase("on conflict")
                .containsIgnoringWhitespaces("version = relation.version + 1");
    }
}
