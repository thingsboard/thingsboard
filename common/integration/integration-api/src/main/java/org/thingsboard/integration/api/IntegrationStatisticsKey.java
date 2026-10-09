// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;


import lombok.Data;
import org.thingsboard.server.common.data.integration.IntegrationType;

@Data
public class IntegrationStatisticsKey {

    private final IntegrationStatisticsMetricName integrationStatisticsMetricName;
    private final boolean success;
    private final IntegrationType integrationType;

    public String [] getTags() {
        return new String[]{
                "name", this.integrationStatisticsMetricName.getName(),
                "state", this.success ? "success" : "failed",
                "type", this.integrationType.name()};
    }
}
