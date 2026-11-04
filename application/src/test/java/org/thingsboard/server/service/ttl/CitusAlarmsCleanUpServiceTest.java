// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ttl;

import org.thingsboard.server.dao.service.CitusDaoSqlTest;

/**
 * Reruns the TTL cleanup flow -- the scatter-gather AlarmRef projection over the distributed alarm table plus the
 * per-ref delete by (originator_id, id) -- against a real Citus cluster.
 */
@CitusDaoSqlTest
public class CitusAlarmsCleanUpServiceTest extends AlarmsCleanUpServiceTest {

}
