/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
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
        if (isSaveRequired(entityViewById, entityView)) {
            entityViewNameUpdated = updateEntityViewNameIfDuplicateExists(tenantId, entityViewId, entityView);
            setCustomerId(tenantId, created ? null : entityViewById.getCustomerId(), entityView, entityViewUpdateMsg);

            entityViewValidator.validate(entityView, EntityView::getTenantId);
            if (created) {
                entityView.setId(entityViewId);
            }
            edgeCtx.getEntityViewService().saveEntityView(entityView, false);
        }
        return Pair.of(created, entityViewNameUpdated);
    }

    private boolean updateEntityViewNameIfDuplicateExists(TenantId tenantId, EntityViewId entityViewId, EntityView entityView) {
        EntityView entityViewByName = edgeCtx.getEntityViewService().findEntityViewByTenantIdAndName(tenantId, entityView.getName());

        return generateUniqueNameIfDuplicateExists(tenantId, entityViewId, entityView, entityViewByName).map(uniqueName -> {
            entityView.setName(uniqueName);
            return true;
        }).orElse(false);
    }

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, EntityView entityView, EntityViewUpdateMsg entityViewUpdateMsg);

    protected void deleteEntityView(TenantId tenantId, EntityViewId entityViewId) {
        deleteEntityView(tenantId, null, entityViewId);
    }

    protected void deleteEntityView(TenantId tenantId, Edge edge, EntityViewId entityViewId) {
        EntityView entityViewById = edgeCtx.getEntityViewService().findEntityViewById(tenantId, entityViewId);
        if (entityViewById != null) {
            edgeCtx.getEntityViewService().deleteEntityView(tenantId, entityViewId);
            pushEntityEventToRuleEngine(tenantId, edge, entityViewById, TbMsgType.ENTITY_DELETED);
        }
    }
}
