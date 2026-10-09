// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ReportTemplateInfo extends BaseReportTemplate {

    private static final long serialVersionUID = 1729877416392618039L;

    @Schema(description = "Owner name", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;

    public ReportTemplateInfo() {
        super();
    }

    public ReportTemplateInfo(BaseReportTemplate reportTemplate) {
        super(reportTemplate);
    }

    public ReportTemplateInfo(BaseReportTemplate reportTemplate, String ownerName) {
        super(reportTemplate);
        this.ownerName = ownerName;
    }

}
