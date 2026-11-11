// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.style.BorderLength;
import org.thingsboard.server.common.data.report.configuration.style.BorderType;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class DividerComponent extends AbstractLayoutReportComponent {

    private BorderLength length;
    private BorderType borderType;
    private Integer widthPx;
    private String color;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.DIVIDER;
    }
}
