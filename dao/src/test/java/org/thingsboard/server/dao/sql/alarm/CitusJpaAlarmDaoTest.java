// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.alarm;

import org.thingsboard.server.dao.service.CitusDaoSqlTest;

/**
 * Reruns {@link JpaAlarmDaoTest} against a real Citus cluster. The {@code alarm} table is distributed on
 * {@code originator_id}, so this pins the alarm DAO paths (including the raw Hibernate {@code save()} update, which
 * must not touch the immutable distribution column) as an always-on Citus regression rather than relying on the
 * {@code -Dtb.citus.alltests=true} sweep.
 */
@CitusDaoSqlTest
public class CitusJpaAlarmDaoTest extends JpaAlarmDaoTest {
}
