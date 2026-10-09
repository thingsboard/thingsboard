// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatedEntityInfo {

    private String name;
    private EntityType type;
    private String owner;

    public String getEntityPageLink(UUID id) {
        return switch (type) {
            case DEVICE -> "/entities/devices/all/" + id;
            case ASSET -> "/entities/assets/all/" + id;
            case DEVICE_PROFILE -> "/profiles/deviceProfiles/" + id;
            case ASSET_PROFILE -> "/profiles/assetProfiles/" + id;
            case USER -> "/users/all/" + id;
            case CUSTOMER -> "/customers/all/" + id;
            case DASHBOARD -> "/dashboards/all/" + id;
            case ROLE -> "/security-settings/roles/" + id;
            case EDGE -> "/edgeManagement/edges/all/" + id;
            default -> null;
        };
    }

}
