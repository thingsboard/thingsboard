// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class SplitViewComponent extends AbstractLayoutReportComponent {

    private ReportComponent leftView;
    private ReportComponent rightView;
    private Float splitPosition;
    private Integer splitGap;
    private VerticalAlignment leftVerticalAlignment;
    private VerticalAlignment rightVerticalAlignment;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.SPLIT_VIEW;
    }
}
