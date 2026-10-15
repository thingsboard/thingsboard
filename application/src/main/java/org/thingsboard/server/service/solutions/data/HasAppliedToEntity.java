// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data;

import org.thingsboard.server.common.data.id.EntityId;

import java.util.UUID;

public interface HasAppliedToEntity {

    EntityId entityId();

    String getCfPageLink(UUID cfId);

    default String getEntityPageLink() {
        EntityId id = entityId();
        if (id == null) {
            return null;
        }
        String idStr = id.getId().toString();
        return switch (id.getEntityType()) {
            case DEVICE_PROFILE -> "/profiles/deviceProfiles/" + idStr;
            case ASSET_PROFILE -> "/profiles/assetProfiles/" + idStr;
            case DEVICE -> "/entities/devices/all/" + idStr;
            case ASSET -> "/entities/assets/all/" + idStr;
            default -> null;
        };
    }

}
