// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.components.AbstractImageComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageAlignment;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.Map;

import static org.thingsboard.server.report.util.ImageUtils.EMPTY_IMAGE_URI;

public abstract class AbstractImageRenderer<C extends AbstractImageComponent> extends ReportComponentWithLayoutRenderer<C> {

    @Override
    public String renderContent(C imageComponent, ComponentData reportDataSource) {
        String imageUrl = this.getImageUrl(imageComponent, reportDataSource);
        String layoutWidth = this.layoutWidthPx + "px";

        String imageWidth;
        if (ImageWidthType.ORIGINAL == imageComponent.getWidthType()) {
            imageWidth = "auto";
        } else if (ImageWidthType.CUSTOM == imageComponent.getWidthType()) {
            int customWidth = imageComponent.getCustomWidth() >= 1 ? imageComponent.getCustomWidth() : 100;
            imageWidth = customWidth + "px";
        } else {
            imageWidth = layoutWidth;
        }

        String imageAlign = imageComponent.getAlignment() != null ?
                imageComponent.getAlignment().getValue() :
                ImageAlignment.CENTER.getValue();

        Map<String, Object> componentVariables = Map.of(
                "layoutWidth", layoutWidth,
                "imageUrl", StringUtils.isBlank(imageUrl) ? EMPTY_IMAGE_URI : imageUrl,
                "imageWidth", imageWidth,
                "imageAlign", imageAlign
        );

        return ThymeleafUtil.renderFromHtmlTemplate("html/components/image", componentVariables);
    }

    protected abstract String getImageUrl(C component, ComponentData reportDataSource);
}
