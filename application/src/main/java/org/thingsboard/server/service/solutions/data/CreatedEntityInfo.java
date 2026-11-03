// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
            case CUSTOMER -> "/customers/" + id;
            case USER -> "/users/" + id;
            case ASSET -> "/entities/assets/" + id;
            case DEVICE -> "/entities/devices/" + id;
            case DEVICE_PROFILE -> "/profiles/deviceProfiles/" + id;
            case ASSET_PROFILE -> "/profiles/assetProfiles/" + id;
            case DASHBOARD -> "/dashboards/" + id;
            case RULE_CHAIN -> "/ruleChains/" + id;
            case EDGE -> "/edgeManagement/instances/" + id;
            default -> null;
        };
    }

}
