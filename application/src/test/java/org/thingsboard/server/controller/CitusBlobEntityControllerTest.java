// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.thingsboard.server.dao.service.CitusDaoSqlTest;

/**
 * Reruns {@link BlobEntityControllerTest} against a real Citus cluster. The {@code blob_entity} table is a
 * partitioned, coordinator-local table (Citus rejects {@code create_reference_table} on partitioned tables), so it is
 * never distributed and stays on the coordinator. This guards its CRUD and query paths under Citus mode.
 */
@CitusDaoSqlTest
public class CitusBlobEntityControllerTest extends BlobEntityControllerTest {
}
