// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ErrorComponent extends AbstractReportComponent {

    private String errorMessage;
    private Exception exception;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ERROR;
    }

}
