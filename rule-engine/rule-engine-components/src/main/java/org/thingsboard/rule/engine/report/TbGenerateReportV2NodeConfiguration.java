// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.report;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.report.ReportConfig;

@Data
public class TbGenerateReportV2NodeConfiguration implements NodeConfiguration<TbGenerateReportV2NodeConfiguration> {

    private boolean useConfigFromMessage;
    private ReportConfig config;

    @Override
    public TbGenerateReportV2NodeConfiguration defaultConfiguration() {
        return new TbGenerateReportV2NodeConfiguration();
    }

}
