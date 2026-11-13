// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.attributes;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.CitusTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeKvInsertRepositoryCitusSqlTest {

    private AttributeKvInsertRepository repoWith(boolean citus) {
        AttributeKvInsertRepository repo = new AttributeKvInsertRepository();
        ReflectionTestUtils.setField(repo, "citusSettings", CitusTestSupport.citusSettings(citus));
        ReflectionTestUtils.invokeMethod(repo, "initQueries");
        return repo;
    }

    @Test
    void plainModeUsesGlobalSequence() {
        AttributeKvInsertRepository repo = repoWith(false);
        assertThat(repo.getInsertOrUpdateQuery()).contains("nextval('attribute_kv_version_seq')");
        assertThat(repo.getBatchUpdateQuery()).contains("nextval('attribute_kv_version_seq')");
    }

    @Test
    void citusModeUsesPerRowIncrement() {
        AttributeKvInsertRepository repo = repoWith(true);
        assertThat(repo.getInsertOrUpdateQuery())
                .doesNotContain("nextval")
                .contains("version = attribute_kv.version + 1")
                .contains("ON CONFLICT (entity_id, attribute_type, attribute_key)")
                .contains("RETURNING version");
        assertThat(repo.getInsertOrUpdateQuery()).containsPattern("VALUES\\(.*, 1\\) ");
        assertThat(repo.getBatchUpdateQuery())
                .doesNotContain("nextval")
                .contains("version = attribute_kv.version + 1");
    }
}
