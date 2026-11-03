// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting;

import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

public interface EntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> {

    D getExportData(EntitiesExportCtx<?> ctx, I entityId) throws ThingsboardException;

}
