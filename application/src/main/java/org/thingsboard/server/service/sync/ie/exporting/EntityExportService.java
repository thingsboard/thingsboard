// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting;

import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

public interface EntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> {

    D getExportData(EntitiesExportCtx<?> ctx, E entity) throws ThingsboardException;

    <ID extends EntityId> ID getExternalIdOrElseInternal(EntitiesExportCtx<?> ctx, ID internalId);
}
