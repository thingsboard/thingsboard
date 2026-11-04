// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.report.ReportTemplateDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@AllArgsConstructor
public class ReportTemplateDataValidator extends DataValidator<ReportTemplate> {

    private final TenantService tenantService;
    private final CustomerDao customerDao;
    private final ReportTemplateDao reportTemplateDao;

    @Override
    protected ReportTemplate validateUpdate(TenantId tenantId, ReportTemplate reportTemplate) {
        ReportTemplate old = reportTemplateDao.findById(reportTemplate.getTenantId(), reportTemplate.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing report template!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, ReportTemplate reportTemplate) {
        validateString("Report template name", reportTemplate.getName());
        if (reportTemplate.getFormat() == null) {
            throw new DataValidationException("Report template format should be specified!");
        }
        if (reportTemplate.getType() == null) {
            throw new DataValidationException("Report template type should be specified!");
        }
        if (reportTemplate.getTenantId() == null) {
            throw new DataValidationException("Report template should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(reportTemplate.getTenantId())) {
                throw new DataValidationException("Report template is referencing to non-existent tenant!");
            }
        }
        if (reportTemplate.getCustomerId() == null) {
            reportTemplate.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!reportTemplate.getCustomerId().isNullUid()) {
            Customer customer = customerDao.findById(reportTemplate.getTenantId(), reportTemplate.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign report template to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(reportTemplate.getTenantId().getId())) {
                throw new DataValidationException("Can't assign report template to customer from different tenant!");
            }
        }
    }
}


