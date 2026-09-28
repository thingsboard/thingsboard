// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.EntityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.JobManager;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportInfoQuery;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.report.service.TbReportService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_TEMPLATE_ID_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_USER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/v2")
public class ReportController extends BaseController {

    @Value("${reports.generation_timeout_ms:120000}")
    private int timeoutMs;

    private static final String REPORT_DESCRIPTION = "The platform uses Report to store generated reports information.";
    private static final String INVALID_REPORT_ID = "Referencing non-existing Report Id will cause 'Not Found' error.";

    public static final String REPORT_ID = "reportId";

    private final JobManager jobManager;
    private final TbReportService tbReportService;
    private final SystemSecurityService systemSecurityService;

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/report")
    public Report createReport(@RequestPart MultipartFile file,
                               @RequestPart String info) throws Exception {
        // Deliberately unguarded: POST /api/v2/report is how a generated report is stored, and a scheduled
        // report produced by an already-persisted scheduler event has to keep being stored while the reporting
        // feature is withheld. Note that it is not restricted to the report microservice - any user token with
        // REPORT/CREATE reaches it. What the guards stop is user-initiated generation, upstream in
        // requestReport and testReportAndDownload.
        Report report = JacksonUtil.fromString(info, Report.class);
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT, Operation.CREATE, null, report);
        return reportService.createReport(report, file.getBytes());
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/report/{reportId}/download")
    public ResponseEntity<ByteArrayResource> downloadReport(@PathVariable(REPORT_ID) UUID id) throws ThingsboardException {
        ReportId reportId = new ReportId(id);
        checkReportId(reportId, Operation.READ);
        ReportData reportData = reportService.getReportDataById(getTenantId(), reportId);
        return buildDownloadResponse(reportData.getData(), reportData.getName(), reportData.getContentType());
    }

    @ApiOperation(value = "Update Report public status (updateReportPublicStatus)",
            notes = "Updates the public status of the report. When public, the report can be downloaded without authentication via the public link. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = "/report/{reportId}/public/{isPublic}")
    public Report updateReportPublicStatus(
            @Parameter(description = REPORT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(REPORT_ID) UUID id,
            @Parameter(description = "Whether the report should be publicly downloadable without authentication")
            @PathVariable("isPublic") boolean isPublic) throws ThingsboardException {
        if (isPublic) {
            // Guarded in the publishing direction only: the public download link stays live while the feature
            // is withheld, so refusing to un-publish would leave a tenant unable to close an open share link.
            checkFeatureAllowed(PlatformFeature.REPORTING);
        }
        ReportId reportId = new ReportId(id);
        Report report = checkReportId(reportId, Operation.WRITE);
        report.setPublic(isPublic);
        try {
            Report savedReport = reportService.updateReport(report);
            logEntityActionService.logEntityAction(getTenantId(), reportId, savedReport,
                    ActionType.UPDATED, getCurrentUser());
            return savedReport;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.REPORT), report,
                    ActionType.UPDATED, getCurrentUser(), e);
            throw e;
        }
    }

    @ApiOperation(value = "Get Report (getReportById)",
            notes = "Fetch the Report object based on the provided report Id. " +
                    REPORT_DESCRIPTION + INVALID_REPORT_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/report/{reportId}")
    public Report getReportById(@Parameter(description = REPORT_ID_PARAM_DESCRIPTION, required = true)
                                @PathVariable(REPORT_ID) String strReportId) throws ThingsboardException {
        checkParameter(REPORT_ID, strReportId);
        ReportId reportId = new ReportId(toUUID(strReportId));
        return checkReportId(reportId, Operation.READ);
    }

    @ApiOperation(value = "Delete Report (deleteReport)",
            notes = "Deletes the report. " + INVALID_REPORT_ID + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/report/{reportId}")
    public void deleteReport(
            @Parameter(description = REPORT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(REPORT_ID) String strReportId) throws Exception {
        // Deliberately unguarded: removal is the one write direction that reduces use of a withheld feature,
        // and it is also the other way to close a public download link that stays live while it is withheld.
        checkParameter(REPORT_ID, strReportId);
        ReportId reportId = new ReportId(toUUID(strReportId));
        checkReportId(reportId, Operation.DELETE);
        reportService.deleteReport(getTenantId(), reportId);
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<Report> getReports(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                       @RequestParam int pageSize,
                                       @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                       @RequestParam int page,
                                       @Parameter(description = "Case-insensitive 'substring' filter based on report's name or customer title")
                                       @RequestParam(required = false) String textSearch,
                                       @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                       @RequestParam(required = false) String sortProperty,
                                       @Parameter(description = SORT_ORDER_DESCRIPTION)
                                       @RequestParam(required = false) String sortOrder,
                                       @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
                                       @RequestParam(required = false) Boolean includeCustomers,
                                       @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.REPORT, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return reportService.findReports(user.getTenantId(), user.getCustomerId(), includeCustomers != null && includeCustomers, pageLink);
    }

    @GetMapping("/reportInfos")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<ReportInfo> getReportInfosByIds(
            @Parameter(description = "A list of report ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("strReportIds") String[] strReportIds) throws ThingsboardException {
        checkArrayParameter("strReportIds", strReportIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<ReportId> reportIds = new ArrayList<>();
        for (String strReportTemplateId : strReportIds) {
            reportIds.add(new ReportId(toUUID(strReportTemplateId)));
        }
        List<ReportInfo> reportInfos = checkNotNull(reportService.findReportInfoByIds(tenantId, reportIds));
        return filterReportTemplatesByReadPermission(reportInfos);
    }

    @GetMapping("/reportInfos/all")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<ReportInfo> getReportInfos(
            @Parameter(description = REPORT_TEMPLATE_ID_DESCRIPTION)
            @RequestParam(required = false) UUID reportTemplateId,
            @Parameter(description = REPORT_USER_DESCRIPTION)
            @RequestParam(required = false) UUID userId,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on report's name or customer title")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION)
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION)
            @RequestParam(required = false) String sortOrder,
            @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.REPORT, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        ReportInfoQuery query = ReportInfoQuery.builder()
                .reportTemplateId(reportTemplateId)
                .userId(userId)
                .includeCustomers(includeCustomers != null && includeCustomers)
                .pageLink(pageLink)
                .build();
        return checkNotNull(reportService.findReportInfos(getTenantId(), user.getCustomerId(), query));
    }

    @ApiOperation(value = "Download test report (testReportAndDownload)",
            notes = "Generate and download test report." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/report/test")
    public ResponseEntity<ByteArrayResource> testReportAndDownload(@RequestBody ReportRequest reportRequest) throws Exception {
        checkFeatureAllowed(PlatformFeature.REPORTING);
        SecurityUser currentUser = getCurrentUser();
        UserId userId = StringUtils.isNotEmpty(reportRequest.getUserId()) ? new UserId(UUID.fromString(reportRequest.getUserId())) : getCurrentUser().getId();
        EntityId userOwnerId = ownersCacheService.getOwner(currentUser.getTenantId(), userId);
        checkUserOwnerPermission(currentUser, userId);
        AccessJwtToken accessToken = systemSecurityService.createUserAccessToken(currentUser.getTenantId(), userId);
        ReportTemplateConfig configuration = reportRequest.getReportTemplateConfig();
        if (configuration == null) {
            configuration = checkReportTemplateId(reportRequest.getReportTemplateId(), Operation.READ).getConfiguration();
        }

        ReportTask reportTask = ReportTask.builder()
                .tenantId(currentUser.getTenantId())
                .reportTemplateConfig(configuration)
                .timezone(reportRequest.getTimezone())
                .userId(userId)
                .userOwnerId(userOwnerId)
                .originator(reportRequest.getOriginator())
                .accessToken(accessToken.getToken())
                .accessTokenExpirationTs(accessToken.getClaims().getExpiration().getTime())
                .nonProduction(subscriptionService.isDevelopment(currentUser.getTenantId()))
                .build();

        Future<ReportData> future = tbReportService.generateTestReport(reportTask);
        try {
            ReportData reportData = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            ByteArrayResource resource = new ByteArrayResource(reportData.getData());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + reportData.getName() + "\"")
                    .header("x-filename", reportData.getName())
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.parseMediaType(reportData.getContentType()))
                    .body(resource);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ThingsboardException("Timeout for test report generation. Generation took more than " + timeoutMs + " milliseconds!", ThingsboardErrorCode.GENERAL);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/report/request")
    public Job requestReport(@RequestBody ReportRequest reportRequest) throws Exception {
        checkFeatureAllowed(PlatformFeature.REPORTING);
        ReportTemplateId reportTemplateId = reportRequest.getReportTemplateId();
        if (reportTemplateId == null) {
            /*
             * we can't use template configuration instead of id in the job config, because job can execute much later,
             * and job processor needs the up-to-date config when the job gets to be executed
             * */
            throw new IllegalArgumentException("Report template id must be specified");
        }
        checkReportTemplateId(reportTemplateId, Operation.READ);
        SecurityUser currentUser = getCurrentUser();
        UserId userId;
        if (StringUtils.isNotEmpty(reportRequest.getUserId())) {
            userId = new UserId(UUID.fromString(reportRequest.getUserId()));
            checkUserOwnerPermission(currentUser, userId);
        } else {
            userId = currentUser.getId();
        }
        TenantId tenantId = getTenantId();
        return jobManager.submitJob(Job.newReportJob()
                .tenantId(tenantId)
                .customerId(currentUser.getCustomerId())
                .reportTemplateId(reportTemplateId)
                .userId(userId)
                .timezone(reportRequest.getTimezone())
                .makePublic(reportRequest.isMakePublic())
                .originator(reportRequest.getOriginator())
                .targets(reportRequest.getTargets())
                .notificationTemplateId(reportRequest.getNotificationTemplateId())
                .build()).get();
    }

    @ApiOperation(value = "Download report by public key (downloadReportByPublicKey)",
            notes = "Downloads the report identified by its public key. " +
                    "This endpoint is intentionally public: it requires no authentication and is accessible to anyone " +
                    "in possession of the report's random public key. It only returns data for reports that have been " +
                    "explicitly made public (see updateReportPublicStatus).")
    @GetMapping(value = "/report/public/{publicKey}/download")
    public ResponseEntity<ByteArrayResource> downloadReportByPublicKey(
            @Parameter(description = "The random public key of the report", required = true)
            @PathVariable("publicKey") String publicKey) throws ThingsboardException {
        ReportData reportData = reportService.getReportDataByPublicKey(publicKey);
        if (reportData == null) {
            throw new ThingsboardException("Report data not found", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return buildDownloadResponse(reportData.getData(), reportData.getName(), reportData.getContentType());
    }

    private ResponseEntity<ByteArrayResource> buildDownloadResponse(byte[] data, String name, String contentType) {
        ByteArrayResource resource = new ByteArrayResource(data);
        ContentDisposition cd = ContentDisposition.attachment()
                .filename(name, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("x-filename", name)
                .contentLength(resource.contentLength())
                .header("Content-Type", contentType)
                .body(resource);
    }

    private List<ReportInfo> filterReportTemplatesByReadPermission(List<ReportInfo> reportInfos) {
        return reportInfos.stream().filter(report -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.REPORT, Operation.READ, report.getId(), report);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

}
