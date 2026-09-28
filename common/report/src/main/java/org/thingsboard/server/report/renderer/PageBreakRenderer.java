// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.PageBreakComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.ComponentData;


@Component
public class PageBreakRenderer implements PdfReportComponentRenderer<PageBreakComponent> {

    @Override
    public String render(PageBreakComponent component, ComponentData reportDataSource) {
        return "<div class=\"page-break\"></div>";
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.PAGE_BREAK;
    }

}
