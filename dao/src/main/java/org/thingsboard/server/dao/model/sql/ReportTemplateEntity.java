// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.report.BaseReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import static org.thingsboard.server.dao.model.ModelConstants.REPORT_TEMPLATE_CONFIGURATION_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.REPORT_TEMPLATE_TABLE_NAME)
public class ReportTemplateEntity extends AbstractReportTemplateEntity<ReportTemplate> {

    @Convert(converter = JsonConverter.class)
    @Column(name = REPORT_TEMPLATE_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    public ReportTemplateEntity() {
        super();
    }

    public ReportTemplateEntity(ReportTemplate reportTemplate) {
        super(reportTemplate);
        this.configuration = JacksonUtil.valueToTree(reportTemplate.getConfiguration());
    }

    @Override
    public ReportTemplate toData() {
        BaseReportTemplate baseReportTemplate = super.toBaseReportTemplate();
        ReportTemplate reportTemplate = new ReportTemplate(baseReportTemplate);
        reportTemplate.setConfiguration(JacksonUtil.treeToValue(configuration, ReportTemplateConfig.class));
        return reportTemplate;
    }
}
