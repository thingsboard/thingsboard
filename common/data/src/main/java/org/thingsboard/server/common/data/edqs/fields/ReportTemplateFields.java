// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;

import java.util.UUID;

@Data
@NoArgsConstructor
@SuperBuilder
public class ReportTemplateFields extends AbstractEntityFields {
    private String type;
    private String format;

    public ReportTemplateFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, ReportTemplateType type, TbReportFormat format, Long version) {
        super(id, createdTime, tenantId, customerId, name, version);
        this.format = format.name();
        this.type = type.name();
    }
}
