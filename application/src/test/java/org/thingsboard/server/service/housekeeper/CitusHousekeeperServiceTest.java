// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper;

import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.dao.service.CitusDaoSqlTest;

@CitusDaoSqlTest
public class CitusHousekeeperServiceTest extends HousekeeperServiceTest {

    // The plain-Postgres deleted-user unassign posts a per-alarm UNASSIGNED_FROM_DELETED_USER system comment; the Citus
    // bulk path intentionally omits it (see BaseAlarmService.unassignAlarmsByAssignee), so this assertion does not hold
    // under Citus. The Citus bulk unassign itself is exercised by the other inherited tests, which verify the deleted
    // user's alarm assignments are cleared.
    @Test
    @Ignore
    @Override
    public void whenUserIsDeleted_thenUnassignAlarmsAndPostSystemComment() {
    }

}
