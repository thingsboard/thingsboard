// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.converter;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbConverterService extends AbstractTbEntityService implements TbConverterService {

    private final ConverterService converterService;

    @Override
    public Converter save(Converter converter, User user) throws Exception {
        ActionType actionType = converter.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = converter.getTenantId();
        try {
            Converter savedConverter = checkNotNull(converterService.saveConverter(converter));

            autoCommit(user, savedConverter.getId());

            logEntityActionService.logEntityAction(tenantId, savedConverter.getId(), savedConverter, null, actionType, user);
            return savedConverter;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CONVERTER), converter,
                    actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(Converter converter, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = converter.getTenantId();
        ConverterId converterId = converter.getId();
        try {
            converterService.deleteConverter(tenantId, converterId);

            logEntityActionService.logEntityAction(tenantId, converter.getId(), converter, null,
                    actionType, user, converter.getId().toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CONVERTER),
                    actionType, user, e, converterId.getId().toString());
            throw e;
        }
    }
}
