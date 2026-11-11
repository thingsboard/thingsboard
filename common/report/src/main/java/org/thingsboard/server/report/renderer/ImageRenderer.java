// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.components.ImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.image.ImageSourceType;
import org.thingsboard.server.report.context.ComponentData;

import java.util.List;

import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;

@Component
public class ImageRenderer extends AbstractImageRenderer<ImageComponent> {

    @Override
    protected String getImageUrl(ImageComponent component, ComponentData reportDataSource) {
        String imageUrl = "";
        if (ImageSourceType.ENTITY_KEY == component.getSourceType()) {
            if (!reportDataSource.getEntityDatas().isEmpty()) {
                var entityData = reportDataSource.getEntityDatas().get(0);
                var dataSource = getSingleDataSource(component);
                if (dataSource.isPresent()) {
                    List<DataKey> dataKeys = dataSource.get().getDataKeys();
                    if (dataKeys != null && !dataKeys.isEmpty()) {
                        var dataKey = dataKeys.get(0);
                        imageUrl = entityData.get(dataKey.getLabel());
                    }
                }
            }
        } else {
            imageUrl = component.getImageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = "/assets/report/components/image-placeholder.svg";
            }
        }
        return imageUrl;
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.IMAGE;
    }

}
