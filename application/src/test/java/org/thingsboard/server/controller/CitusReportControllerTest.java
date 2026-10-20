// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.thingsboard.server.dao.service.CitusDaoSqlTest;

/**
 * Reruns {@link ReportControllerTest} against a real Citus cluster. The {@code report} table is a partitioned,
 * coordinator-local table (Citus rejects {@code create_reference_table} on partitioned tables), so it lives only on
 * the coordinator while {@code report_template} is a reference table and the entities a report scans are distributed.
 * This exercises the mixed-placement query paths that only the Citus planner has to resolve.
 */
@CitusDaoSqlTest
public class CitusReportControllerTest extends ReportControllerTest {
}
