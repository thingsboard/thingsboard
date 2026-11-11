// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.DividerComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.style.BorderType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.HashMap;

import static org.thingsboard.server.common.data.report.configuration.style.BorderLength.SHORT;


@Component
public class DividerRenderer extends ReportComponentWithLayoutRenderer<DividerComponent> {

    @Override
    protected String renderContent(DividerComponent component, ComponentData reportDataSource) {
        HashMap<String, Object> componentVariables = new HashMap<>();
        componentVariables.put("width", component.getLength() != null && component.getLength() == SHORT ? "50%" : "100%");
        String borderWidth = component.getWidthPx() != null ? component.getWidthPx() + "px" : "1px";
        String borderColor = component.getColor() != null ? ColorUtils.normalizeCssColor(component.getColor()) : "#000";
        String borderType = component.getBorderType() != null ? component.getBorderType().getValue() : BorderType.SOLID.getValue();
        componentVariables.put("borderStyle", borderWidth + " " + borderType + " " + borderColor);

        return ThymeleafUtil.renderFromHtmlTemplate("html/components/divider-template", componentVariables);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.DIVIDER;
    }

}
