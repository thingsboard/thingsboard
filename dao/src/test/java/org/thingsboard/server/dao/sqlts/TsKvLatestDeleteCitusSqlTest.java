// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sqlts;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.CitusTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

class TsKvLatestDeleteCitusSqlTest {

    private SqlTimeseriesLatestDao daoWith(boolean citus) {
        SqlTimeseriesLatestDao dao = new SqlTimeseriesLatestDao();
        ReflectionTestUtils.setField(dao, "citusSettings", CitusTestSupport.citusSettings(citus));
        ReflectionTestUtils.invokeMethod(dao, "initDeleteQuery");
        return dao;
    }

    @Test
    void plainModeUsesGlobalSequence() {
        SqlTimeseriesLatestDao dao = daoWith(false);
        assertThat(dao.getRemoveLatestWithVersionQuery())
                .contains("nextval('ts_kv_latest_version_seq')")
                .doesNotContain("RETURNING version");
    }

    @Test
    void citusModeReturnsPerRowVersion() {
        SqlTimeseriesLatestDao dao = daoWith(true);
        assertThat(dao.getRemoveLatestWithVersionQuery())
                .doesNotContain("nextval")
                .contains("RETURNING version");
    }
}
