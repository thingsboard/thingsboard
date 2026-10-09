// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.tracecoverage;

import org.springframework.test.context.TestPropertySource;

/**
 * The events datasource points at a separate (empty) database here, so a setting written to one database and read
 * from the other is caught; the sibling *_DedicatedEventsDataSource tests share one database and can't see that.
 */
@TestPropertySource(properties = {
        "spring.datasource.events.enabled=true",
        "spring.datasource.events.url=jdbc:tc:postgresql:18:///thingsboard_events?TC_DAEMON=true&TC_TMPFS=/testtmpfs:rw",
        "spring.datasource.events.driverClassName=${spring.datasource.driverClassName}"
})
public class JpaTraceCoverageSettingDaoTest_DedicatedEventsDataSource extends JpaTraceCoverageSettingDaoTest {
}
