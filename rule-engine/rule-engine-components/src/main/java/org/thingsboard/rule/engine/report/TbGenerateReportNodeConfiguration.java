// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.report;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;

@Data
public class TbGenerateReportNodeConfiguration implements NodeConfiguration<TbGenerateReportNodeConfiguration> {

    private boolean useSystemReportsServer;
    private String reportsServerEndpointUrl;
    private boolean useReportConfigFromMessage;
    private DashboardReportConfig reportConfig;

    @Override
    public TbGenerateReportNodeConfiguration defaultConfiguration() {
        TbGenerateReportNodeConfiguration configuration = new TbGenerateReportNodeConfiguration();
        configuration.setUseSystemReportsServer(true);
        configuration.setUseReportConfigFromMessage(true);
        return configuration;
    }
}
