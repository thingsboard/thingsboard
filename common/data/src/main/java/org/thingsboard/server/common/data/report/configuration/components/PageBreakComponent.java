// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.DataSource;

import java.util.List;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class PageBreakComponent extends AbstractReportComponent implements ReportComponent {

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.PAGE_BREAK;
    }
}
