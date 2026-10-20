// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class ConverterImportService extends BaseEntityImportService<ConverterId, Converter, EntityExportData<Converter>> {

    private final ConverterService converterService;

    @Override
    protected void setOwner(TenantId tenantId, Converter converter, IdProvider idProvider) {
        converter.setTenantId(tenantId);
    }

    @Override
    protected Converter prepare(EntitiesImportCtx ctx, Converter entity, Converter oldEntity, EntityExportData<Converter> exportData, IdProvider idProvider) {
        return entity;
    }

    @Override
    protected Converter deepCopy(Converter converter) {
        return new Converter(converter);
    }

    @Override
    protected Converter saveOrUpdate(EntitiesImportCtx ctx, Converter entity, EntityExportData<Converter> exportData, IdProvider idProvider, CompareResult compareResult) {
        return converterService.saveConverter(entity);
    }

    @Override
    protected void onEntitySaved(User user, Converter savedConverter, Converter oldConverter) throws ThingsboardException {
        super.onEntitySaved(user, savedConverter, oldConverter);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CONVERTER;
    }

}
