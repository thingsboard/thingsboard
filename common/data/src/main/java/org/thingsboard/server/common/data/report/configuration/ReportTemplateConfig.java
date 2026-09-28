// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

import java.util.List;

@JsonPropertyOrder({"namePattern", "timeDataPattern", "format", "entityAliases", "filters", "components"})
@Schema(
        discriminatorProperty = "format",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "PDF", schema = PdfReportTemplateConfig.class),
                @DiscriminatorMapping(value = "CSV", schema = CsvReportTemplateConfig.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "format")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PdfReportTemplateConfig.class, name = "PDF"),
        @JsonSubTypes.Type(value = CsvReportTemplateConfig.class, name = "CSV")
})
public interface ReportTemplateConfig {

    String getNamePattern();

    String getTimeDataPattern();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Report format")
    TbReportFormat getFormat();

    List<EntityAlias> getEntityAliases();

    List<Filter> getFilters();

    @ArraySchema(schema = @Schema(implementation = ReportComponent.class))
    List<ReportComponent> getComponents();

}
