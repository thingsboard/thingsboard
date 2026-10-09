// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sqlts.insert.latest.sql;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.CitusTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

class SqlLatestInsertTsRepositoryCitusSqlTest {

    private SqlLatestInsertTsRepository repoWith(boolean citus, boolean byLatestTs) {
        SqlLatestInsertTsRepository repo = new SqlLatestInsertTsRepository();
        ReflectionTestUtils.setField(repo, "citusSettings", CitusTestSupport.citusSettings(citus));
        ReflectionTestUtils.setField(repo, "updateByLatestTs", byLatestTs);
        ReflectionTestUtils.invokeMethod(repo, "init");
        return repo;
    }

    @Test
    void plainModeUsesGlobalSequence() {
        SqlLatestInsertTsRepository repo = repoWith(false, true);
        assertThat(repo.getInsertOrUpdateQuery()).contains("nextval('ts_kv_latest_version_seq')");
    }

    @Test
    void citusModeUsesPerRowIncrementAndKeepsLatestTsGuard() {
        SqlLatestInsertTsRepository repo = repoWith(true, true);
        assertThat(repo.getInsertOrUpdateQuery())
                .doesNotContain("nextval")
                .contains("version = ts_kv_latest.version + 1")
                .contains("ON CONFLICT (entity_id, key)")
                .contains("WHERE ts_kv_latest.ts <= ?")
                .endsWith(" RETURNING version");
        // the INSERT side must start every fresh row at version 1 (the per-row version contract)
        assertThat(repo.getInsertOrUpdateQuery()).containsPattern("VALUES\\(.*, 1\\) ");
        assertThat(repo.getBatchUpdateQuery())
                .doesNotContain("nextval")
                .contains("version = ts_kv_latest.version + 1");
    }

    @Test
    void citusModeWithoutLatestTsGuard() {
        SqlLatestInsertTsRepository repo = repoWith(true, false);
        assertThat(repo.getInsertOrUpdateQuery())
                .doesNotContain("nextval")
                .doesNotContain("ts_kv_latest.ts <=")
                .contains("version = ts_kv_latest.version + 1");
    }
}
