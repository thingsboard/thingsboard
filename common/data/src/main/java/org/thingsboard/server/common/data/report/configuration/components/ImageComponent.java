// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.image.ImageAlignment;
import org.thingsboard.server.common.data.report.configuration.image.ImageSourceType;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class ImageComponent extends AbstractImageComponent {

    private ImageSourceType sourceType;
    private String imageUrl;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.IMAGE;
    }

}
