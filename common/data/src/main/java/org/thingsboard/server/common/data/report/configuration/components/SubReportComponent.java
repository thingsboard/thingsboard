// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.ReportTemplateId;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class SubReportComponent extends AbstractDataReportComponent {

    private ReportTemplateId templateId;
    private boolean avoidPageBreakInside;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.SUB_REPORT;
    }

}
