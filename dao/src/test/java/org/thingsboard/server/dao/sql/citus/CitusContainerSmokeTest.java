// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CitusContainerSmokeTest extends AbstractCitusContainerTest {

    @Test
    void citusExtensionIsInstalled() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from pg_extension where extname = 'citus'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void atLeastOneWorkerIsRegistered() {
        Integer workers = jdbcTemplate.queryForObject(
                "select count(*) from pg_dist_node where noderole = 'primary' and groupid <> 0", Integer.class);
        assertThat(workers).isGreaterThanOrEqualTo(1);
    }
}
