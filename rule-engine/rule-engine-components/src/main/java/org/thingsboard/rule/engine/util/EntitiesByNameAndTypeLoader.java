/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.rule.engine.util;

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Optional;

public class EntitiesByNameAndTypeLoader {

    public static EntityId findEntityId(TbContext ctx, EntityType entityType, String entityName) {
        EntityId targetEntityId = null;
        switch (entityType) {
            case DEVICE:
                Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(ctx.getTenantId(), entityName);
                if (device != null) {
                    targetEntityId = device.getId();
                }
                break;
            case ASSET:
                Asset asset = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), entityName);
                if (asset != null) {
                    targetEntityId = asset.getId();
                }
                break;
            case ENTITY_VIEW:
                EntityView entityView = ctx.getEntityViewService().findEntityViewByTenantIdAndName(ctx.getTenantId(), entityName);
                if (entityView != null) {
                    targetEntityId = entityView.getId();
                }
                break;
            case EDGE:
                Edge edge = ctx.getEdgeService().findEdgeByTenantIdAndName(ctx.getTenantId(), entityName);
                if (edge != null) {
                    targetEntityId = edge.getId();
                }
                break;
            case USER:
                User user = ctx.getUserService().findUserByTenantIdAndEmail(ctx.getTenantId(), entityName);
                if (user != null) {
                    targetEntityId = user.getId();
                }
                break;
            default:
                throw new IllegalStateException("Unexpected entity type " + entityType.name());
        }
        if (targetEntityId == null) {
            throw new IllegalStateException("Failed to found " + entityType.name() + "  entity by name: '" + entityName + "'!");
        }
        return targetEntityId;
    }

}
