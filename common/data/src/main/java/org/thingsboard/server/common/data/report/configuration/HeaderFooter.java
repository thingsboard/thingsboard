// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

import java.util.List;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class HeaderFooter {

    private boolean enabled;
    @ArraySchema(schema = @Schema(implementation = ReportComponent.class))
    @NotNull
    private List<ReportComponent> components;
    private HeaderFooter firstPage;

}
