/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.rpc.processor.entityview;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseEntityViewProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<EntityView> entityViewValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateEntityView(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg) {
        boolean created = false;
        boolean entityViewNameUpdated = false;
        EntityView entityView = JacksonUtil.fromString(entityViewUpdateMsg.getEntity(), EntityView.class, true);
        if (entityView == null) {
            throw new RuntimeException("[{" + tenantId + "}] entityViewUpdateMsg {" + entityViewUpdateMsg + "} cannot be converted to entity view");
        }
        EntityView entityViewById = edgeCtx.getEntityViewService().findEntityViewById(tenantId, entityViewId);
        if (entityViewById == null) {
            created = true;
            entityView.setId(null);
        } else {
            entityView.setId(entityViewId);
        }
        String entityViewName = entityView.getName();
        EntityView entityViewByName = edgeCtx.getEntityViewService().findEntityViewByTenantIdAndName(tenantId, entityViewName);
        if (entityViewByName != null && !entityViewByName.getId().equals(entityViewId)) {
            entityViewName = entityViewName + "_" + StringUtils.randomAlphanumeric(15);
            log.warn("[{}] Entity view with name {} already exists. Renaming entity view name to {}",
                    tenantId, entityView.getName(), entityViewName);
            entityViewNameUpdated = true;
        }
        entityView.setName(entityViewName);
        setCustomerId(tenantId, created ? null : entityViewById.getCustomerId(), entityView, entityViewUpdateMsg);

        entityViewValidator.validate(entityView, EntityView::getTenantId);
        if (created) {
            entityView.setId(entityViewId);
        }
        edgeCtx.getEntityViewService().saveEntityView(entityView, false);
        return Pair.of(created, entityViewNameUpdated);
    }

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, EntityView entityView, EntityViewUpdateMsg entityViewUpdateMsg);

}
