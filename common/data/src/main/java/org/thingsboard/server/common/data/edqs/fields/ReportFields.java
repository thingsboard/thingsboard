// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.report.TbReportFormat;

import java.util.UUID;

@Data
@NoArgsConstructor
@SuperBuilder
public class ReportFields extends AbstractEntityFields {

    private String format;

    public ReportFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, TbReportFormat format) {
        super(id, createdTime, tenantId, customerId, name, null);
        this.format = format.name();
    }
}
