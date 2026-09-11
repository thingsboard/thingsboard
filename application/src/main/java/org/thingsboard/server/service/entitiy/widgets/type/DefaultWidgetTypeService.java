// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.widgets.type;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.resource.TbResourceService;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultWidgetTypeService extends AbstractTbEntityService implements TbWidgetTypeService {

    private final WidgetTypeService widgetTypeService;
    private final TbResourceService tbResourceService;

    @Override
    public WidgetTypeDetails save(WidgetTypeDetails entity, SecurityUser user) throws Exception {
        return this.save(entity, false, user);
    }

    @Override
    public WidgetTypeDetails save(WidgetTypeDetails widgetTypeDetails, boolean updateExistingByFqn, SecurityUser user) throws Exception {
        TenantId tenantId = widgetTypeDetails.getTenantId();
        if (widgetTypeDetails.getId() == null && StringUtils.isNotEmpty(widgetTypeDetails.getFqn()) && updateExistingByFqn) {
            WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdAndFqn(tenantId, widgetTypeDetails.getFqn());
            if (widgetType != null) {
                widgetTypeDetails.setId(widgetType.getId());
            }
        }
        if (CollectionUtils.isNotEmpty(widgetTypeDetails.getResources())) {
            tbResourceService.importResources(widgetTypeDetails.getResources(), user);
        }

        ActionType actionType = widgetTypeDetails.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            WidgetTypeDetails savedWidgetTypeDetails = checkNotNull(widgetTypeService.saveWidgetType(widgetTypeDetails));
            autoCommit(user, savedWidgetTypeDetails.getId());
            logEntityActionService.logEntityAction(tenantId, savedWidgetTypeDetails.getId(), savedWidgetTypeDetails,
                    null, actionType, user);
            return savedWidgetTypeDetails;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.WIDGET_TYPE), widgetTypeDetails, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(WidgetTypeDetails widgetTypeDetails, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = widgetTypeDetails.getTenantId();
        try {
            widgetTypeService.deleteWidgetType(widgetTypeDetails.getTenantId(), widgetTypeDetails.getId());
            logEntityActionService.logEntityAction(tenantId, widgetTypeDetails.getId(), widgetTypeDetails, null, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.WIDGET_TYPE), actionType, user, e, widgetTypeDetails.getId());
            throw e;
        }
    }

}
