// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.relation;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.CitusTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

class JpaRelationDaoCitusSqlTest {

    private JpaRelationDao daoWith(boolean citus) {
        JpaRelationDao dao = new JpaRelationDao();
        ReflectionTestUtils.setField(dao, "citusSettings", CitusTestSupport.citusSettings(citus));
        ReflectionTestUtils.invokeMethod(dao, "initDeleteQuery");
        return dao;
    }

    @Test
    void plainModeUsesGlobalSequence() {
        JpaRelationDao dao = daoWith(false);
        assertThat(dao.getDeleteQuery()).contains("nextval('relation_version_seq') as version");
        assertThat(dao.getDeleteQuery()).contains("DELETE FROM relation WHERE");
    }

    @Test
    void citusModeReturnsStoredVersion() {
        JpaRelationDao dao = daoWith(true);
        assertThat(dao.getDeleteQuery())
                .doesNotContain("nextval")
                .contains("relation_type_group, version");
    }
}
