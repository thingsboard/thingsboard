// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.report.TbReportTemplateService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_TEMPLATE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_TEMPLATE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReportTemplateController extends BaseController {

    private static final String REPORT_TEMPLATE_INFO_DESCRIPTION = "Report Templates allows you to create reports according to the report template configuration. " +
            "Report service uses report template configuration to generate report. See the 'Model' tab of the Response Class for more details. ";
    private static final String REPORT_TEMPLATE_DESCRIPTION = "Report Template extends Report Template Info object and adds " +
            "'configuration' - a JSON structure of report template configuration. See the 'Model' tab of the Response Class for more details. ";

    private static final String REPORT_TEMPLATE_QUERY_TYPE_ARRAY_DESCRIPTION = "A list of string values separated by comma ',' representing one of the ReportTemplateType enumeration value.";
    private static final String REPORT_TEMPLATE_QUERY_FORMAT_ARRAY_DESCRIPTION = "A list of string values separated by comma ',' representing one of the TbReportFormat enumeration value.";

    private static final String INVALID_REPORT_TEMPLATE_ID = "Referencing non-existing Report Template Id will cause 'Not Found' error.";

    public static final String REPORT_TEMPLATE_ID = "reportTemplateId";

    private final TbReportTemplateService tbReportTemplateService;

    @ApiOperation(value = "Get Report Template (getReportTemplateById)",
            notes = "Fetch the ReportTemplate object based on the provided report template Id. " +
                    REPORT_TEMPLATE_DESCRIPTION + INVALID_REPORT_TEMPLATE_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/reportTemplate/{reportTemplateId}")
    public ReportTemplate getReportTemplateById(@Parameter(description = REPORT_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
                                                @PathVariable(REPORT_TEMPLATE_ID) String strReportTemplateId) throws ThingsboardException {
        checkParameter(REPORT_TEMPLATE_ID, strReportTemplateId);
        ReportTemplateId reportTemplateId = new ReportTemplateId(toUUID(strReportTemplateId));
        return checkReportTemplateId(reportTemplateId, Operation.READ);
    }

    @ApiOperation(value = "Get Report Template Info (getReportTemplateInfoById)",
            notes = "Fetch the ReportTemplateInfo object based on the provided report template Id. " +
                    REPORT_TEMPLATE_INFO_DESCRIPTION + INVALID_REPORT_TEMPLATE_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/reportTemplate/info/{reportTemplateId}")
    public ReportTemplateInfo getReportTemplateInfoById(@Parameter(description = REPORT_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
                                                        @PathVariable(REPORT_TEMPLATE_ID) String strReportTemplateId) throws ThingsboardException {
        checkParameter(REPORT_TEMPLATE_ID, strReportTemplateId);
        ReportTemplateId reportTemplateId = new ReportTemplateId(toUUID(strReportTemplateId));
        return checkReportTemplateInfoId(reportTemplateId, Operation.READ);
    }

    @ApiOperation(value = "Save Report Template (saveReportTemplate)",
            notes = "Creates or Updates report template. " + REPORT_TEMPLATE_DESCRIPTION +
                    "When creating report template, platform generates report template Id as " + UUID_WIKI_LINK +
                    "The newly created report template id will be present in the response. Specify existing report template id to update the report template. " +
                    "Referencing non-existing report template Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Report Template entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/reportTemplate")
    public ReportTemplate saveReportTemplate(
            @Parameter(description = "A JSON value representing the Report Template.")
            @RequestBody ReportTemplate reportTemplate) throws Exception {
        checkFeatureAllowed(PlatformFeature.REPORTING);
        SecurityUser currentUser = getCurrentUser();
        reportTemplate.setTenantId(currentUser.getTenantId());
        if (Authority.CUSTOMER_USER.equals(currentUser.getAuthority())) {
            reportTemplate.setCustomerId(currentUser.getCustomerId());
        }
        checkEntity(reportTemplate.getId(), reportTemplate, Resource.REPORT_TEMPLATE);
        return tbReportTemplateService.save(reportTemplate, currentUser);
    }

    @ApiOperation(value = "Delete Report Template (deleteReportTemplate)",
            notes = "Deletes the report template. " + INVALID_REPORT_TEMPLATE_ID + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/reportTemplate/{reportTemplateId}")
    public void deleteReportTemplate(
            @Parameter(description = REPORT_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(REPORT_TEMPLATE_ID) String strReportTemplateId) throws Exception {
        // Deliberately unguarded: removal is the one write direction that reduces use of a withheld feature.
        checkParameter(REPORT_TEMPLATE_ID, strReportTemplateId);
        ReportTemplateId reportTemplateId = new ReportTemplateId(toUUID(strReportTemplateId));
        ReportTemplate reportTemplate = checkReportTemplateId(reportTemplateId, Operation.DELETE);
        tbReportTemplateService.delete(reportTemplate, getCurrentUser());
    }

    @ApiOperation(value = "Get All Report Templates for current user (getAllReportTemplateInfos)",
            notes = "Returns a page of report template info objects owned by the tenant or the customer of a current user. "
                    + REPORT_TEMPLATE_INFO_DESCRIPTION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/reportTemplateInfos/all", params = {"pageSize", "page"})
    public PageData<ReportTemplateInfo> getAllReportTemplateInfos(
            @Parameter(description = REPORT_TEMPLATE_QUERY_TYPE_ARRAY_DESCRIPTION, array = @ArraySchema(schema = @Schema(type = "string", allowableValues = {"REPORT", "SUB_REPORT"})))
            @RequestParam(required = false) String[] typeList,
            @Parameter(description = REPORT_TEMPLATE_QUERY_FORMAT_ARRAY_DESCRIPTION, array = @ArraySchema(schema = @Schema(type = "string", allowableValues = {"PDF", "CSV"})))
            @RequestParam(required = false) String[] formatList,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = REPORT_TEMPLATE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "ownerName"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT_TEMPLATE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        List<ReportTemplateType> reportTemplateTypeList = new ArrayList<>();
        if (typeList != null) {
            for (String strType : typeList) {
                if (!StringUtils.isEmpty(strType)) {
                    reportTemplateTypeList.add(ReportTemplateType.valueOf(strType));
                }
            }
        }
        List<TbReportFormat> reportTemplateFormatList = new ArrayList<>();
        if (formatList != null) {
            for (String strFormat : formatList) {
                if (!StringUtils.isEmpty(strFormat)) {
                    reportTemplateFormatList.add(TbReportFormat.valueOf(strFormat));
                }
            }
        }
        ReportTemplateQuery query = ReportTemplateQuery.builder()
                .pageLink(pageLink)
                .includeCustomers(includeCustomers != null && includeCustomers)
                .formatList(reportTemplateFormatList)
                .typeList(reportTemplateTypeList)
                .build();
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            return checkNotNull(reportTemplateService.findReportTemplates(tenantId, query));
        } else {
            CustomerId customerId = getCurrentUser().getCustomerId();
            return checkNotNull(reportTemplateService.findCustomerReportTemplates(tenantId, customerId, query));
        }
    }

    @ApiOperation(value = "Get report templates by Report Template Ids (getReportTemplatesByIds)",
            notes = "Returns a list of ReportTemplateInfo objects based on the provided ids. Filters the list based on the user permissions. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/reportTemplates", params = {"reportTemplateIds"})
    public List<ReportTemplateInfo> getReportTemplatesByIds(
            @Parameter(description = "A list of report template ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("reportTemplateIds") String[] strReportTemplateIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("reportTemplateIds", strReportTemplateIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<ReportTemplateId> reportTemplateIds = new ArrayList<>();
        for (String strReportTemplateId : strReportTemplateIds) {
            reportTemplateIds.add(new ReportTemplateId(toUUID(strReportTemplateId)));
        }
        List<ReportTemplateInfo> reportTemplates = checkNotNull(reportTemplateService.findReportTemplateInfoByIds(tenantId, reportTemplateIds));
        return filterReportTemplatesByReadPermission(reportTemplates);
    }

    private List<ReportTemplateInfo> filterReportTemplatesByReadPermission(List<ReportTemplateInfo> reportTemplates) {
        return reportTemplates.stream().filter(reportTemplate -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.REPORT_TEMPLATE, Operation.READ, reportTemplate.getId(), reportTemplate);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }
}
