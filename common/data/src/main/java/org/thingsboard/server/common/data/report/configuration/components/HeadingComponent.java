// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class HeadingComponent extends AbstractDataWithLayoutReportComponent {

    private String value;
    private Font font;
    private String color;
    private TextAlignment textAlignment;
    private VerticalAlignment verticalAlignment;
    private Integer height;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.HEADING;
    }
}
