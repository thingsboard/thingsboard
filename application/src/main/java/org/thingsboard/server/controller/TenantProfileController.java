/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.tenant.profile.TbTenantProfileService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_PROFILE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class TenantProfileController extends BaseController {

    private static final String TENANT_PROFILE_INFO_DESCRIPTION = "Tenant Profile Info is a lightweight object that contains only id and name of the profile. ";

    private final TbTenantProfileService tbTenantProfileService;

    @ApiOperation(value = "Get Tenant Profile (getTenantProfileById)",
            notes = "Fetch the Tenant Profile object based on the provided Tenant Profile Id. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public TenantProfile getTenantProfileById(
            @Parameter(description = TENANT_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
        return checkTenantProfileId(tenantProfileId, Operation.READ);
    }

    @ApiOperation(value = "Get Tenant Profile Info (getTenantProfileInfoById)",
            notes = "Fetch the Tenant Profile Info object based on the provided Tenant Profile Id. " + TENANT_PROFILE_INFO_DESCRIPTION + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfo/{tenantProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityInfo getTenantProfileInfoById(
            @Parameter(description = TENANT_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
        return checkNotNull(tenantProfileService.findTenantProfileInfoById(getTenantId(), tenantProfileId));
    }

    @ApiOperation(value = "Get default Tenant Profile Info (getDefaultTenantProfileInfo)",
            notes = "Fetch the default Tenant Profile Info object based. " + TENANT_PROFILE_INFO_DESCRIPTION + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfo/default", method = RequestMethod.GET)
    @ResponseBody
    public EntityInfo getDefaultTenantProfileInfo() throws ThingsboardException {
        return checkNotNull(tenantProfileService.findDefaultTenantProfileInfo(getTenantId()));
    }

    @ApiOperation(value = "Create Or update Tenant Profile (saveTenantProfile)",
            notes = "Create or update the Tenant Profile. When creating tenant profile, platform generates Tenant Profile Id as " + UUID_WIKI_LINK +
                    "The newly created Tenant Profile Id will be present in the response. " +
                    "Specify existing Tenant Profile Id id to update the Tenant Profile. " +
                    "Referencing non-existing Tenant Profile Id will cause 'Not Found' error. " +
                    "\n\nUpdate of the tenant profile configuration will cause immediate recalculation of API limits for all affected Tenants. " +
                    "\n\nThe **'profileData'** object is the part of Tenant Profile that defines API limits and Rate limits. " +
                    "\n\nYou have an ability to define maximum number of devices ('maxDevice'), assets ('maxAssets') and other entities. " +
                    "You may also define maximum number of messages to be processed per month ('maxTransportMessages', 'maxREExecutions', etc). " +
                    "The '*RateLimit' defines the rate limits using simple syntax. For example, '1000:1,20000:60' means up to 1000 events per second but no more than 20000 event per minute. " +
                    "Let's review the example of tenant profile data below: " +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"name\": \"Your name\",\n" +
                    "  \"description\": \"Your description\",\n" +
                    "  \"isolatedTbRuleEngine\": false,\n" +
                    "  \"profileData\": {\n" +
                    "    \"configuration\": {\n" +
                    "      \"type\": \"DEFAULT\",\n" +
                    "      \"maxDevices\": 0,\n" +
                    "      \"maxAssets\": 0,\n" +
                    "      \"maxCustomers\": 0,\n" +
                    "      \"maxUsers\": 0,\n" +
                    "      \"maxDashboards\": 0,\n" +
                    "      \"maxRuleChains\": 0,\n" +
                    "      \"maxResourcesInBytes\": 0,\n" +
                    "      \"maxOtaPackagesInBytes\": 0,\n" +
                    "      \"maxResourceSize\": 0,\n" +
                    "      \"transportTenantMsgRateLimit\": \"1000:1,20000:60\",\n" +
                    "      \"transportTenantTelemetryMsgRateLimit\": \"1000:1,20000:60\",\n" +
                    "      \"transportTenantTelemetryDataPointsRateLimit\": \"1000:1,20000:60\",\n" +
                    "      \"transportDeviceMsgRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportDeviceTelemetryMsgRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportDeviceTelemetryDataPointsRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportGatewayMsgRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportGatewayTelemetryMsgRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportGatewayTelemetryDataPointsRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportGatewayDeviceMsgRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportGatewayDeviceTelemetryMsgRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportGatewayDeviceTelemetryDataPointsRateLimit\": \"20:1,600:60\",\n" +
                    "      \"maxTransportMessages\": 10000000,\n" +
                    "      \"maxTransportDataPoints\": 10000000,\n" +
                    "      \"maxREExecutions\": 4000000,\n" +
                    "      \"maxJSExecutions\": 5000000,\n" +
                    "      \"maxDPStorageDays\": 0,\n" +
                    "      \"maxRuleNodeExecutionsPerMessage\": 50,\n" +
                    "      \"maxDebugModeDurationMinutes\": 15,\n" +
                    "      \"maxEmails\": 0,\n" +
                    "      \"maxSms\": 0,\n" +
                    "      \"maxCreatedAlarms\": 1000,\n" +
                    "      \"defaultStorageTtlDays\": 0,\n" +
                    "      \"alarmsTtlDays\": 0,\n" +
                    "      \"rpcTtlDays\": 0,\n" +
                    "      \"queueStatsTtlDays\": 0,\n" +
                    "      \"ruleEngineExceptionsTtlDays\": 0,\n" +
                    "      \"warnThreshold\": 0,\n" +
                    "      \"maxCalculatedFieldsPerEntity\": 5,\n" +
                    "      \"maxArgumentsPerCF\": 10,\n" +
                    "      \"maxDataPointsPerRollingArg\": 1000,\n" +
                    "      \"maxStateSizeInKBytes\": 32,\n" +
                    "      \"maxSingleValueArgumentSizeInKBytes\": 2" +
                    "    }\n" +
                    "  },\n" +
                    "  \"default\": false\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "Remove 'id', from the request body example (below) to create new Tenant Profile entity." +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile", method = RequestMethod.POST)
    @ResponseBody
    public TenantProfile saveTenantProfile(@Parameter(description = "A JSON value representing the tenant profile.")
                                           @Valid @RequestBody TenantProfile tenantProfile) throws ThingsboardException {
        TenantProfile oldProfile;
        if (tenantProfile.getId() == null) {
            accessControlService.checkPermission(getCurrentUser(), Resource.TENANT_PROFILE, Operation.CREATE);
            oldProfile = null;
        } else {
            oldProfile = checkTenantProfileId(tenantProfile.getId(), Operation.WRITE);
        }

        return tbTenantProfileService.save(getTenantId(), tenantProfile, oldProfile);
    }

    @ApiOperation(value = "Delete Tenant Profile (deleteTenantProfile)",
            notes = "Deletes the tenant profile. Referencing non-existing tenant profile Id will cause an error. Referencing profile that is used by the tenants will cause an error. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTenantProfile(@Parameter(description = TENANT_PROFILE_ID_PARAM_DESCRIPTION)
                                    @PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
        TenantProfile profile = checkTenantProfileId(tenantProfileId, Operation.DELETE);
        tbTenantProfileService.delete(getTenantId(), profile);
    }

    @ApiOperation(value = "Make tenant profile default (setDefaultTenantProfile)",
            notes = "Makes specified tenant profile to be default. Referencing non-existing tenant profile Id will cause an error. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    public TenantProfile setDefaultTenantProfile(
            @Parameter(description = TENANT_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
        TenantProfile tenantProfile = checkTenantProfileId(tenantProfileId, Operation.WRITE);
        tenantProfileService.setDefaultTenantProfile(getTenantId(), tenantProfileId);
        return tenantProfile;
    }

    @ApiOperation(value = "Get Tenant Profiles (getTenantProfiles)", notes = "Returns a page of tenant profiles registered in the platform. " + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<TenantProfile> getTenantProfiles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = TENANT_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "description", "isDefault"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tenantProfileService.findTenantProfiles(getTenantId(), pageLink));
    }

    @ApiOperation(value = "Get Tenant Profiles Info (getTenantProfileInfos)", notes = "Returns a page of tenant profile info objects registered in the platform. "
            + TENANT_PROFILE_INFO_DESCRIPTION + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityInfo> getTenantProfileInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = TENANT_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"id", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tenantProfileService.findTenantProfileInfos(getTenantId(), pageLink));
    }

    @GetMapping(value = "/tenantProfiles", params = {"ids"})
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public List<TenantProfile> getTenantProfilesByIds(@Parameter(description = "Comma-separated list of tenant profile ids", array = @ArraySchema(schema = @Schema(type = "string")))
                                                      @RequestParam("ids") UUID[] ids) {
        return tenantProfileService.findTenantProfilesByIds(TenantId.SYS_TENANT_ID, ids);
    }

}
