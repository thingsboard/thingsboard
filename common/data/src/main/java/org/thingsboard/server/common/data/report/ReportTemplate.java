// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.common.data.util.DataUtils.getChildObjects;

@Schema
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ReportTemplate extends BaseReportTemplate {

    @Serial
    private static final long serialVersionUID = 1729877416392618039L;

    @Schema(description = "a JSON value with report template configuration")
    @Valid
    @NotNull
    private ReportTemplateConfig configuration;

    public ReportTemplate() {
        super();
    }

    public ReportTemplate(ReportTemplateId id) {
        super(id);
    }

    public ReportTemplate(BaseReportTemplate reportTemplate) {
        super(reportTemplate);
    }

    public ReportTemplate(ReportTemplate reportTemplate) {
        super(reportTemplate);
        if (reportTemplate.getConfiguration() != null) {
            this.configuration = mapper.convertValue(
                    mapper.valueToTree(reportTemplate.getConfiguration()),
                    ReportTemplateConfig.class
            );
        }
    }

    @JsonIgnore
    public List<ObjectNode> getEntityAliasesConfig() {
        return getChildObjects("entityAliases", mapper.valueToTree(configuration));
    }

    @JsonIgnore
    public List<ObjectNode> getComponentDataSources() {
        List<ReportComponent> components = configuration.getComponents();
        if (components == null || components.isEmpty()) {
            return Collections.emptyList();
        }
        return components.stream()
                .filter(component -> component instanceof DataReportComponent)
                .flatMap(component -> ((DataReportComponent) component).getDataSources().stream())
                .map(fromValue -> (ObjectNode) mapper.valueToTree(fromValue))
                .toList();
    }

}
