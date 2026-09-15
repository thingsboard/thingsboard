// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.iot_hub;

import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@Data
public class SolutionTemplateInstalledItemDescriptor implements IotHubInstalledItemDescriptor {

    private List<EntityId> createdEntityIds;
    private List<String> tenantTelemetryKeys;
    private List<String> tenantAttributeKeys;
    private DashboardId dashboardId;
    private CustomerId publicId;
    private boolean mainDashboardPublic;
    private String details;

}
