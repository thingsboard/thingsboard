// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.ComponentData;

import java.util.Base64;

@Component
public class DashboardRenderer extends AbstractImageRenderer<DashboardComponent> {

    @Override
    protected String getImageUrl(DashboardComponent component, ComponentData reportDataSource) {
        return encodeImage(reportDataSource.getImage(), "image/png");
    }

    public String encodeImage(byte[] imageBytes, String mimeType) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:" + mimeType + ";base64," + base64;
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.DASHBOARD;
    }

}
