// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.RichTextComponent;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ThymeleafUtil;

@Component
public class RichTextRenderer extends ReportComponentWithLayoutRenderer<RichTextComponent> {

    @Override
    protected String renderContent(RichTextComponent richTextComponent, ComponentData reportDataSource) {
        return ThymeleafUtil.renderFromTextString(richTextComponent.getValue(), reportDataSource.getVariables());
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.RICH_TEXT;
    }

}
