// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class ReportTemplateControllerTest extends AbstractControllerTest {

    private final IdComparator<ReportTemplateInfo> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveReportTemplate() throws Exception {
        ReportTemplate reportTemplate = buildTestReportTemplate();

        Mockito.reset(tbClusterService, auditLogService);

        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        testNotifyEntityEntityGroupNullAllOneTime(savedReportTemplate, savedReportTemplate.getId(), savedReportTemplate.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED);

        Assert.assertNotNull(savedReportTemplate);
        Assert.assertNotNull(savedReportTemplate.getId());
        Assert.assertTrue(savedReportTemplate.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedReportTemplate.getTenantId());
        Assert.assertNotNull(savedReportTemplate.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedReportTemplate.getCustomerId().getId());
        Assert.assertEquals(reportTemplate.getName(), savedReportTemplate.getName());
        Assert.assertEquals(reportTemplate.getDescription(), savedReportTemplate.getDescription());
        Assert.assertEquals(reportTemplate.getConfiguration(), savedReportTemplate.getConfiguration());

        Mockito.reset(tbClusterService, auditLogService);

        savedReportTemplate.setName("My new report");
        doPost("/api/reportTemplate", savedReportTemplate, ReportTemplate.class);

        testNotifyEntityEntityGroupNullAllOneTime(savedReportTemplate, savedReportTemplate.getId(), savedReportTemplate.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);

        ReportTemplate foundReportTemplate = doGet("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString(), ReportTemplate.class);
        Assert.assertEquals(foundReportTemplate.getName(), savedReportTemplate.getName());
    }

    @Test
    public void testSaveReportTemplateWithViolationOfLengthValidation() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName(StringUtils.randomAlphabetic(300));
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("name");
        doPost("/api/reportTemplate", reportTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(reportTemplate, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        reportTemplate.setName("Normal name");
        reportTemplate.setDescription(StringUtils.randomAlphabetic(2000));
        msgError = "description length must be equal or less than 1024";
        doPost("/api/reportTemplate", reportTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(reportTemplate, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testUpdateReportTemplateFromDifferentTenant() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "REPORT_TEMPLATE '" + savedReportTemplate.getName() + "'!";
        doPost("/api/reportTemplate", savedReportTemplate)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + msgError)));

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionDelete + msgError)));

        testNotifyEntityNever(savedReportTemplate.getId(), savedReportTemplate);

        deleteDifferentTenant();
    }

    @Test
    public void testFindReportTemplateById() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        PdfReportTemplateConfig pdfReportTemplateConfig = PdfReportTemplateConfig.builder().components(Collections.emptyList()).build();
        reportTemplate.setConfiguration(pdfReportTemplateConfig);
        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);
        ReportTemplate foundReportTemplate = doGet("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString(), ReportTemplate.class);
        Assert.assertNotNull(foundReportTemplate);
        Assert.assertEquals(savedReportTemplate, foundReportTemplate);
    }

    @Test
    public void testDeleteReportTemplate() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityEntityGroupNullAllOneTime(savedReportTemplate, savedReportTemplate.getId(), savedReportTemplate.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedReportTemplate.getId().getId().toString());

        String reportTemplateIdStr = savedReportTemplate.getId().getId().toString();
        doGet("/api/reportTemplate/" + reportTemplateIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Report template", reportTemplateIdStr))));
    }

    @Test
    public void testSaveReportTemplateWithEmptyName() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        PdfReportTemplateConfig pdfReportTemplateConfig = PdfReportTemplateConfig.builder().components(Collections.emptyList()).build();
        reportTemplate.setConfiguration(pdfReportTemplateConfig);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Report template name " + msgErrorShouldBeSpecified;
        doPost("/api/reportTemplate", reportTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(reportTemplate, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveReportTemplateWithEmptyFormat() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Report template format " + msgErrorShouldBeSpecified;
        doPost("/api/reportTemplate", reportTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(reportTemplate, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveReportTemplateWithEmptyType() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        PdfReportTemplateConfig pdfReportTemplateConfig = PdfReportTemplateConfig.builder().components(Collections.emptyList()).build();
        reportTemplate.setConfiguration(pdfReportTemplateConfig);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Report template type " + msgErrorShouldBeSpecified;
        doPost("/api/reportTemplate", reportTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(reportTemplate, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindReportTemplates() throws Exception {
        List<ReportTemplateInfo> reportTemplates = new ArrayList<>();
        int cntEntity = 178;

        Mockito.reset(tbClusterService, auditLogService);

        for (int i = 0; i < cntEntity; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setName("ReportTemplate" + i);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            reportTemplates.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }
        List<ReportTemplateInfo> loadedReportTemplates = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedReportTemplates.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new ReportTemplate(), new ReportTemplate(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        reportTemplates.sort(idComparator);
        loadedReportTemplates.sort(idComparator);

        Assert.assertEquals(reportTemplates, loadedReportTemplates);
    }

    @Test
    public void testFindReportTemplatesByName() throws Exception {
        String title1 = "Report template title 1";
        List<ReportTemplateInfo> reportTemplatesTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            reportTemplatesTitle1.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }
        String title2 = "Report template title 2";
        List<ReportTemplateInfo> reportTemplatesTitle2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            reportTemplatesTitle2.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }

        List<ReportTemplateInfo> loadedReportTemplatesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedReportTemplatesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        reportTemplatesTitle1.sort(idComparator);
        loadedReportTemplatesTitle1.sort(idComparator);

        Assert.assertEquals(reportTemplatesTitle1, loadedReportTemplatesTitle1);

        List<ReportTemplateInfo> loadedReportTemplatesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedReportTemplatesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        reportTemplatesTitle2.sort(idComparator);
        loadedReportTemplatesTitle2.sort(idComparator);

        Assert.assertEquals(reportTemplatesTitle2, loadedReportTemplatesTitle2);

        for (ReportTemplateInfo reportTemplate : loadedReportTemplatesTitle1) {
            doDelete("/api/reportTemplate/" + reportTemplate.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (ReportTemplateInfo reportTemplate : loadedReportTemplatesTitle2) {
            doDelete("/api/reportTemplate/" + reportTemplate.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindReportTemplatesByQuery() throws Exception {
        List<ReportTemplateInfo> pdfReportTemplates = new ArrayList<>();
        for (int i = 0; i < 37; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = "PDF report template " + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            pdfReportTemplates.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }
        pdfReportTemplates.sort(idComparator);

        List<ReportTemplateInfo> csvReportTemplates = new ArrayList<>();
        for (int i = 0; i < 56; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = "CSV report template " + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setFormat(TbReportFormat.CSV);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            csvReportTemplates.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }
        csvReportTemplates.sort(idComparator);

        List<ReportTemplateInfo> pdfSubReports = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = "PDF subreport " + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.SUB_REPORT);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            pdfSubReports.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }
        pdfSubReports.sort(idComparator);

        List<ReportTemplateInfo> csvSubReports = new ArrayList<>();
        for (int i = 0; i < 33; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = "CSV subreport " + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setFormat(TbReportFormat.CSV);
            reportTemplate.setType(ReportTemplateType.SUB_REPORT);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            csvSubReports.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }
        csvSubReports.sort(idComparator);

        List<ReportTemplateInfo> allPdfReportTemplates = new ArrayList<>();
        allPdfReportTemplates.addAll(pdfReportTemplates);
        allPdfReportTemplates.addAll(pdfSubReports);
        allPdfReportTemplates.sort(idComparator);

        List<ReportTemplateInfo> allCsvReportTemplates = new ArrayList<>();
        allCsvReportTemplates.addAll(csvReportTemplates);
        allCsvReportTemplates.addAll(csvSubReports);
        allPdfReportTemplates.sort(idComparator);

        List<ReportTemplateInfo> allReports = new ArrayList<>();
        allReports.addAll(pdfReportTemplates);
        allReports.addAll(csvReportTemplates);
        allReports.sort(idComparator);

        List<ReportTemplateInfo> allSubReports = new ArrayList<>();
        allSubReports.addAll(pdfSubReports);
        allSubReports.addAll(csvSubReports);
        allSubReports.sort(idComparator);

        List<ReportTemplateInfo> allReportTemplates = new ArrayList<>();
        allReportTemplates.addAll(allReports);
        allReportTemplates.addAll(allSubReports);
        allReportTemplates.sort(idComparator);

        ReportTemplateQuery pdfReportTemplatesQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .typeList(List.of(ReportTemplateType.REPORT))
                .formatList(List.of(TbReportFormat.PDF))
                .build();

        List<ReportTemplateInfo> loadedPdfReportTemplates = this.loadReportTemplatesByQuery(pdfReportTemplatesQuery);

        Assert.assertEquals(pdfReportTemplates, loadedPdfReportTemplates);

        ReportTemplateQuery csvReportTemplatesQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .typeList(List.of(ReportTemplateType.REPORT))
                .formatList(List.of(TbReportFormat.CSV))
                .build();

        List<ReportTemplateInfo> loadedCsvReportTemplates = this.loadReportTemplatesByQuery(csvReportTemplatesQuery);

        Assert.assertEquals(csvReportTemplates, loadedCsvReportTemplates);

        ReportTemplateQuery pdfSubReportsQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .typeList(List.of(ReportTemplateType.SUB_REPORT))
                .formatList(List.of(TbReportFormat.PDF))
                .build();

        List<ReportTemplateInfo> loadedPdfSubReports = this.loadReportTemplatesByQuery(pdfSubReportsQuery);

        Assert.assertEquals(pdfSubReports, loadedPdfSubReports);

        ReportTemplateQuery csvSubReportsQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .typeList(List.of(ReportTemplateType.SUB_REPORT))
                .formatList(List.of(TbReportFormat.CSV))
                .build();

        List<ReportTemplateInfo> loadedCsvSubReports = this.loadReportTemplatesByQuery(csvSubReportsQuery);

        Assert.assertEquals(csvSubReports, loadedCsvSubReports);

        ReportTemplateQuery allPdfReportTemplatesQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .formatList(List.of(TbReportFormat.PDF))
                .build();

        List<ReportTemplateInfo> loadedAllPdfReportTemplates = this.loadReportTemplatesByQuery(allPdfReportTemplatesQuery);

        Assert.assertEquals(allPdfReportTemplates, loadedAllPdfReportTemplates);

        ReportTemplateQuery allCsvReportTemplatesQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .formatList(List.of(TbReportFormat.CSV))
                .build();

        List<ReportTemplateInfo> loadedAllCsvReportTemplates = this.loadReportTemplatesByQuery(allCsvReportTemplatesQuery);

        Assert.assertEquals(allCsvReportTemplates, loadedAllCsvReportTemplates);

        ReportTemplateQuery allReportsQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .typeList(List.of(ReportTemplateType.REPORT))
                .build();

        List<ReportTemplateInfo> loadedAllReports = this.loadReportTemplatesByQuery(allReportsQuery);

        Assert.assertEquals(allReports, loadedAllReports);

        ReportTemplateQuery allSubReportsQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .typeList(List.of(ReportTemplateType.SUB_REPORT))
                .build();

        List<ReportTemplateInfo> loadedAllSubReports = this.loadReportTemplatesByQuery(allSubReportsQuery);

        Assert.assertEquals(allSubReports, loadedAllSubReports);

        ReportTemplateQuery allReportTemplatesQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .build();

        List<ReportTemplateInfo> loadedAllReportTemplates = this.loadReportTemplatesByQuery(allReportTemplatesQuery);

        Assert.assertEquals(allReportTemplates, loadedAllReportTemplates);

        for (ReportTemplateInfo reportTemplate : loadedAllReportTemplates) {
            doDelete("/api/reportTemplate/" + reportTemplate.getId().getId().toString())
                    .andExpect(status().isOk());
        }
        allReportTemplatesQuery = ReportTemplateQuery.builder()
                .pageLink(new PageLink(10, 0))
                .build();
        loadedAllReportTemplates = this.loadReportTemplatesByQuery(allReportTemplatesQuery);

        Assert.assertTrue(loadedAllReportTemplates.isEmpty());
    }

    @Test
    public void testSaveCustomerReportTemplate() throws Exception {
        loginTenantAdmin();
        ReportTemplate tenantReportTemplate = saveRandomReportTemplate();

        loginCustomerAdminUser();
        ReportTemplate savedReportTemplate = saveRandomReportTemplate();

        Assert.assertNotNull(savedReportTemplate);
        Assert.assertNotNull(savedReportTemplate.getId());
        Assert.assertTrue(savedReportTemplate.getCreatedTime() > 0);
        Assert.assertEquals(tenantId, savedReportTemplate.getTenantId());
        Assert.assertEquals(customerId, savedReportTemplate.getCustomerId());

        savedReportTemplate.setName("My new report");
        doPost("/api/reportTemplate", savedReportTemplate, ReportTemplate.class);

        ReportTemplate foundReportTemplate = doGet("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString(), ReportTemplate.class);
        Assert.assertEquals(foundReportTemplate.getName(), savedReportTemplate.getName());

        // check tenant template info is accessible for customer
        ReportTemplateInfo fetchedTenantTemplateInfo = doGet("/api/reportTemplate/info/" + tenantReportTemplate.getId().getId().toString(), ReportTemplateInfo.class);
        Assert.assertEquals(fetchedTenantTemplateInfo.getName(), tenantReportTemplate.getName());

        // check other customer cannot access the report template
        loginDifferentCustomerAdmin();
        doGet("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString()).andExpect(status().isForbidden());
    }

    @Test
    public void testCustomerUserCanReadButNotEditTenantReportTemplate() throws Exception {
        loginTenantAdmin();
        ReportTemplate tenantReportTemplate = saveRandomReportTemplate();
        Assert.assertEquals(NULL_UUID, tenantReportTemplate.getCustomerId().getId());

        loginCustomerAdminUser();

        // Customer admin can READ a tenant-level report template (owned above them in the ownership hierarchy)
        ReportTemplate foundReportTemplate = doGet("/api/reportTemplate/" + tenantReportTemplate.getId().getId().toString(), ReportTemplate.class);
        Assert.assertEquals(tenantReportTemplate.getId(), foundReportTemplate.getId());
        Assert.assertEquals(tenantReportTemplate.getName(), foundReportTemplate.getName());

        // ... but cannot EDIT it
        Mockito.reset(tbClusterService, auditLogService);

        foundReportTemplate.setName("Edited by customer");
        String msgError = "REPORT_TEMPLATE '" + tenantReportTemplate.getName() + "'!";
        doPost("/api/reportTemplate", foundReportTemplate)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + msgError)));

        testNotifyEntityNever(tenantReportTemplate.getId(), tenantReportTemplate);
    }

    private ReportTemplate saveRandomReportTemplate() {
        ReportTemplate reportTemplate = buildTestReportTemplate();
        return doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);
    }

    private ReportTemplate buildTestReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("Test_template_" + StringUtils.randomAlphabetic(5));
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setDescription("My report");
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        return reportTemplate;
    }

    private List<ReportTemplateInfo> loadReportTemplatesByQuery(ReportTemplateQuery query) throws Exception {
        List<ReportTemplateInfo> loadedReportTemplates = new ArrayList<>();
        PageLink pageLink = query.getPageLink();
        PageData<ReportTemplateInfo> pageData;
        String urlTemplate = "/api/reportTemplateInfos/all?typeList={typeList}&formatList={formatList}&includeCustomers={includeCustomers}&";
        String typeList = query.getTypeList() != null ? query.getTypeList().stream().map(Enum::name).collect(Collectors.joining(",")) : "";
        String formatList = query.getFormatList() != null ? query.getFormatList().stream().map(Enum::name).collect(Collectors.joining(",")) : "";
        String includeCustomers = query.isIncludeCustomers() ? "true" : "false";
        do {
            pageData = doGetTypedWithPageLink(urlTemplate,
                    new TypeReference<>() {
                    }, pageLink, typeList, formatList, includeCustomers);
            loadedReportTemplates.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        loadedReportTemplates.sort(idComparator);
        return loadedReportTemplates;
    }

}
