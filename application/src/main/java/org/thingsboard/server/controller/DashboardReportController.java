// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.rule.engine.api.DashboardReportService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.utils.MiscUtils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_PARAMS_EXAMPLE;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class DashboardReportController extends BaseController {

    private SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

    private final DashboardReportService dashboardReportService;
    private final SystemSecurityService systemSecurityService;

    public static final String DASHBOARD_ID = "dashboardId";

    @ApiOperation(value = "Download dashboard report (downloadDashboardReport)",
            notes = "Generate and download a report from the specified dashboard. " +
                    "The request payload is a JSON object with params of report. For example:\n\n"
                    + MARKDOWN_CODE_BLOCK_START + REPORT_PARAMS_EXAMPLE + MARKDOWN_CODE_BLOCK_END + "\n" + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/report/{dashboardId}/download", method = RequestMethod.POST, produces = {"application/pdf", "image/jpeg", "image/png"})
    @ResponseBody
    public DeferredResult<ResponseEntity<Resource>> downloadDashboardReport(@Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION, required = true)
                                                                            @PathVariable(DASHBOARD_ID) String strDashboardId,
                                                                            @Parameter(example = REPORT_PARAMS_EXAMPLE, required = true)
                                                                            @RequestBody JsonNode reportParams,
                                                                            HttpServletRequest request) throws ThingsboardException {
        checkFeatureAllowed(PlatformFeature.REPORTING);
        DeferredResult<ResponseEntity<Resource>> result = new DeferredResult<>();
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            DashboardInfo dashboardInfo = checkDashboardInfoId(dashboardId, Operation.READ);
            String baseUrl = MiscUtils.constructBaseUrl(request);

            String name = dashboardInfo.getTitle();
            name += "-" + defaultDateFormat.format(new Date());

            SecurityUser currentUser = getCurrentUser();
            String publicId = "";
            if (currentUser.getUserPrincipal().getType() == UserPrincipal.Type.PUBLIC_ID) {
                publicId = currentUser.getUserPrincipal().getValue();
            }

            AccessJwtToken accessToken;
            TenantId tenantId = currentUser.getTenantId();
            if (StringUtils.isEmpty(publicId)) {
                accessToken = systemSecurityService.createUserAccessToken(tenantId, currentUser.getId());
            } else {
                accessToken = systemSecurityService.createUserAccessTokenFromPublicId(tenantId, publicId);
                ((ObjectNode) reportParams).put("publicId", publicId);
            }
            dashboardReportService.generateDashboardReport(baseUrl, dashboardId, tenantId, currentUser.getId(), name,
                    reportParams, accessToken.getToken(), accessToken.getClaims().getExpiration().getTime(),
                    onSuccess(result), result::setErrorResult);
        } catch (Exception e) {
            result.setErrorResult(e);
        }
        return result;
    }

    @ApiOperation(value = "Download test report (downloadTestReport)",
            notes = "Generate and download test report." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/report/test", method = RequestMethod.POST, produces = {"application/pdf", "image/jpeg", "image/png"})
    @ResponseBody
    public DeferredResult<ResponseEntity<Resource>> downloadTestReport(@RequestBody DashboardReportConfig reportConfig,
                                                                       @Parameter(description = "A string value representing the report server endpoint.", example = "http://localhost:8383")
                                                                       @RequestParam(required = false) String reportsServerEndpointUrl) throws ThingsboardException {
        checkFeatureAllowed(PlatformFeature.REPORTING);
        DeferredResult<ResponseEntity<Resource>> result = new DeferredResult<>();
        try {
            String strDashboardId = reportConfig.getDashboardId();
            checkParameter(DASHBOARD_ID, strDashboardId);

            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            checkDashboardInfoId(dashboardId, Operation.READ);

            dashboardReportService.generateReport(getTenantId(), reportConfig, reportsServerEndpointUrl, onSuccess(result), result::setErrorResult);
        } catch (Exception e) {
            result.setErrorResult(e);
        }
        return result;
    }

    /**
     * The bytes handed to this consumer already carry the non-production notice where one is due, applied by
     * {@code DefaultDashboardReportService}, and nothing here may mark them again. A report that could not be
     * marked arrives at the error consumer instead, and so fails the download rather than serving an unmarked
     * artifact.
     */
    private Consumer<DashboardReportData> onSuccess(DeferredResult<ResponseEntity<Resource>> result) {
        return reportData -> {
            ByteArrayResource resource = new ByteArrayResource(reportData.getData());
            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(reportData.getName(), StandardCharsets.UTF_8)
                    .build();
            ResponseEntity<Resource> response = ResponseEntity.ok().
                    header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    .header("x-filename", reportData.getName())
                    .contentLength(resource.contentLength())
                    .contentType(parseMediaType(reportData.getContentType()))
                    .body(resource);
            result.setResult(response);
        };
    }

}
