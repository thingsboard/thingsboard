// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.report;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.report.ReportTemplateDao;
import org.thingsboard.server.dao.report.ReportTemplateInfoDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.UUID;

public class JpaReportTemplateInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private ReportTemplateInfoDao reportTemplateInfoDao;

    @Autowired
    private ReportTemplateDao reportTemplateDao;

    @Autowired
    private CustomerDao customerDao;

    @Test
    public void testFindReportTemplatesByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            createReportTemplate(tenantId1, null, i);
            createReportTemplate(tenantId2, null, i * 2);
        }

        PageLink pageLink = new PageLink(15, 0, "REPORT_TEMPLATE");
        PageData<ReportTemplateInfo> reportTemplateInfos1 = reportTemplateInfoDao.findReportTemplates(tenantId1, ReportTemplateQuery.builder().pageLink(pageLink).build());
        Assert.assertEquals(15, reportTemplateInfos1.getData().size());

        PageData<ReportTemplateInfo> reportTemplateInfos2 = reportTemplateInfoDao.findReportTemplates(tenantId1, ReportTemplateQuery.builder().pageLink(pageLink.nextPageLink()).build());
        Assert.assertEquals(5, reportTemplateInfos2.getData().size());
    }

    @Test
    public void testFindReportTemplatesByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(), 1);

        for (int i = 0; i < 20; i++) {
            createReportTemplate(tenantId1, customer1.getUuidId(), i);
            createReportTemplate(tenantId1, subCustomer2.getUuidId(), i * 2);
        }

        PageLink pageLink = new PageLink(30, 0, "REPORT_TEMPLATE", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<ReportTemplateInfo> reportTemplateInfos1 = reportTemplateInfoDao.findCustomerReportTemplates(tenantId1, customer1.getUuidId(),
                ReportTemplateQuery.builder().includeCustomers(true).pageLink(pageLink).build());
        Assert.assertEquals(30, reportTemplateInfos1.getData().size());
        reportTemplateInfos1.getData().forEach(reportTemplateInfo -> Assert.assertNotEquals("CUSTOMER_0", reportTemplateInfo.getOwnerName()));

        PageData<ReportTemplateInfo> reportTemplateInfos2 = reportTemplateInfoDao.findCustomerReportTemplates(tenantId1, customer1.getUuidId(),
                ReportTemplateQuery.builder().includeCustomers(true).pageLink(pageLink.nextPageLink()).build());
        Assert.assertEquals(10, reportTemplateInfos2.getData().size());

        PageData<ReportTemplateInfo> reportTemplateInfos3 = reportTemplateInfoDao.findCustomerReportTemplates(tenantId1, subCustomer2.getUuidId(),
                ReportTemplateQuery.builder().includeCustomers(true).pageLink(pageLink).build());
        Assert.assertEquals(20, reportTemplateInfos3.getData().size());
    }

    private void createReportTemplate(UUID tenantId, UUID customerId, int index) {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setId(new ReportTemplateId(Uuids.timeBased()));
        reportTemplate.setTenantId(TenantId.fromUUID(tenantId));
        reportTemplate.setCustomerId(new CustomerId(customerId));
        reportTemplate.setName("REPORT_TEMPLATE_" + index);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());
        reportTemplateDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, reportTemplate);
    }

    private Customer createCustomer(UUID tenantId, UUID parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        return customerDao.save(TenantId.fromUUID(tenantId), customer);
    }
}
