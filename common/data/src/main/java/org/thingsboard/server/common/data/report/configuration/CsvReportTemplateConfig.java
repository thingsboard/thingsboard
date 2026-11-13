// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.report.TbReportFormat;

@Data
@NoArgsConstructor
@SuperBuilder
public class CsvReportTemplateConfig extends AbstractReportTemplateConfig {

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.CSV;
    }

}
