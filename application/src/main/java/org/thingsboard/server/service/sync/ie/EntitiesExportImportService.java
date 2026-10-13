// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.ie;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public interface EntitiesExportImportService {

    <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(EntitiesExportCtx<?> ctx, I entityId) throws ThingsboardException;

    <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(EntitiesExportCtx<?> ctx, E entity) throws ThingsboardException;

    <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(EntitiesImportCtx ctx, EntityExportData<E> exportData) throws Exception;

    /**
     * Imports a batch of entities in dependency order with two-pass resolution.
     * Sorts entity types by {@link #getEntityTypeComparatorForImport()}, imports each entity,
     * then reimports any entities with unresolved forward/circular references,
     * and finally saves all deferred references and relations.
     */
    void importEntities(EntitiesImportCtx ctx, Map<EntityType, List<EntityExportData<?>>> entities) throws ThingsboardException;

    void saveReferencesAndRelations(EntitiesImportCtx ctx) throws ThingsboardException;

    Comparator<EntityType> getEntityTypeComparatorForImport();

}
