// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;
import java.util.Map;

@Data
public class DeviceInstalledItemDescriptor implements IotHubInstalledItemDescriptor {

    private List<EntityId> createdEntityIds;
    private DashboardId dashboardId;
    private String selectedInstallMethod;
    private Map<String, JsonNode> installState;

}
