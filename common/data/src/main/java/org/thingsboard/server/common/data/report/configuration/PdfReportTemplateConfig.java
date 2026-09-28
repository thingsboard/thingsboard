// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.style.Insets;
import org.thingsboard.server.common.data.report.configuration.style.PageOrientation;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;

@Data
@SuperBuilder
@NoArgsConstructor
public class PdfReportTemplateConfig extends AbstractReportTemplateConfig {

    private PageSize pageSize;
    private PageOrientation pageOrientation;
    private Insets pageMargins;
    private String pageBackground;
    private HeaderFooter header;
    private HeaderFooter footer;

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.PDF;
    }

}
