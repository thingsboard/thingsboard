// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.HashMap;

@Component
public class SplitViewRenderer extends ReportComponentWithLayoutRenderer<SplitViewComponent> {

    @Override
    protected String renderContent(SplitViewComponent component, ComponentData reportDataSource) {
        HashMap<String, Object> componentVariables = new HashMap<>(reportDataSource.getVariables());
        componentVariables.put("leftVerticalAlignment", resolveVerticalAlignment(component.getLeftVerticalAlignment()));
        componentVariables.put("rightVerticalAlignment", resolveVerticalAlignment(component.getRightVerticalAlignment()));
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/split-view-template", componentVariables);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.SPLIT_VIEW;
    }

    private String resolveVerticalAlignment(VerticalAlignment vertical) {
        return (vertical != null ? vertical : VerticalAlignment.MIDDLE).getValue();
    }

}
