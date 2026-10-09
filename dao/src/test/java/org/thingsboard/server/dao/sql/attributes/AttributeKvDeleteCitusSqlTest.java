// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.attributes;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.CitusTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeKvDeleteCitusSqlTest {

    private JpaAttributeDao daoWith(boolean citus) {
        JpaAttributeDao dao = new JpaAttributeDao();
        ReflectionTestUtils.setField(dao, "citusSettings", CitusTestSupport.citusSettings(citus));
        ReflectionTestUtils.invokeMethod(dao, "initDeleteQuery");
        return dao;
    }

    @Test
    void plainModeUsesGlobalSequence() {
        JpaAttributeDao dao = daoWith(false);
        assertThat(dao.getRemoveWithVersionQuery())
                .contains("nextval('attribute_kv_version_seq')")
                .doesNotContain("RETURNING version");
    }

    @Test
    void citusModeReturnsPerRowVersion() {
        JpaAttributeDao dao = daoWith(true);
        assertThat(dao.getRemoveWithVersionQuery())
                .doesNotContain("nextval")
                .contains("RETURNING version");
    }
}
