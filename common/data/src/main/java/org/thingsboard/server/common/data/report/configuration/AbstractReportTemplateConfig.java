// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

import java.util.List;

@Data
@NoArgsConstructor
@SuperBuilder
public abstract class AbstractReportTemplateConfig implements ReportTemplateConfig {

    protected String namePattern;
    protected String timeDataPattern;
    protected List<EntityAlias> entityAliases;
    protected List<Filter> filters;
    @NotNull
    protected List<ReportComponent> components;

}
