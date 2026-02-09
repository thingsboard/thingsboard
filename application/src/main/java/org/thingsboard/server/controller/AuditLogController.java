/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.AUDIT_LOG_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.USER_ID_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class AuditLogController extends BaseController {

    private static final String AUDIT_LOG_QUERY_START_TIME_DESCRIPTION = "The start timestamp in milliseconds of the search time range over the AuditLog class field: 'createdTime'.";
    private static final String AUDIT_LOG_QUERY_END_TIME_DESCRIPTION = "The end timestamp in milliseconds of the search time range over the AuditLog class field: 'createdTime'.";
    private static final String AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION = "A String value representing comma-separated list of action types. " +
            "This parameter is optional, but it can be used to filter results to fetch only audit logs of specific action types. " +
            "For example, 'LOGIN', 'LOGOUT'. See the 'Model' tab of the Response Class for more details.";
    private static final String AUDIT_LOG_SORT_PROPERTY_DESCRIPTION = "Property of audit log to sort by. " +
            "See the 'Model' tab of the Response Class for more details. " +
            "Note: entityType sort property is not defined in the AuditLog class, however, it can be used to sort audit logs by types of entities that were logged.";


    @ApiOperation(value = "Get audit logs by customer id (getAuditLogsByCustomerId)",
            notes = "Returns a page of audit logs related to the targeted customer entities (devices, assets, etc.), " +
                    "and users actions (login, logout, etc.) that belong to this customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs/customer/{customerId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByCustomerId(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @Parameter(description = AUDIT_LOG_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = AUDIT_LOG_SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "entityType", "entityName", "userName", "actionType", "actionStatus"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = AUDIT_LOG_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = AUDIT_LOG_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @Parameter(description = AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        checkParameter("CustomerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, getStartTime(startTime), getEndTime(endTime));
        List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
        return checkNotNull(auditLogService.findAuditLogsByTenantIdAndCustomerId(tenantId, new CustomerId(UUID.fromString(strCustomerId)), actionTypes, pageLink));
    }

    @ApiOperation(value = "Get audit logs by user id (getAuditLogsByUserId)",
            notes = "Returns a page of audit logs related to the actions of targeted user. " +
                    "For example, RPC call to a particular device, or alarm acknowledgment for a specific device, etc. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs/user/{userId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByUserId(
            @Parameter(description = USER_ID_PARAM_DESCRIPTION)
            @PathVariable("userId") String strUserId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @Parameter(description = AUDIT_LOG_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = AUDIT_LOG_SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "entityType", "entityName", "userName", "actionType", "actionStatus"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = AUDIT_LOG_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = AUDIT_LOG_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @Parameter(description = AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        checkParameter("UserId", strUserId);
        TenantId tenantId = getCurrentUser().getTenantId();
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, getStartTime(startTime), getEndTime(endTime));
        List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
        return checkNotNull(auditLogService.findAuditLogsByTenantIdAndUserId(tenantId, new UserId(UUID.fromString(strUserId)), actionTypes, pageLink));
    }

    @ApiOperation(value = "Get audit logs by entity id (getAuditLogsByEntityId)",
            notes = "Returns a page of audit logs related to the actions on the targeted entity. " +
                    "Basically, this API call is used to get the full lifecycle of some specific entity. " +
                    "For example to see when a device was created, updated, assigned to some customer, or even deleted from the system. " +
                    PAGE_DATA_PARAMETERS + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs/entity/{entityType}/{entityId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByEntityId(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE"))
            @PathVariable("entityType") String strEntityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityId") String strEntityId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = AUDIT_LOG_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = AUDIT_LOG_SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "entityType", "entityName", "userName", "actionType", "actionStatus"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = AUDIT_LOG_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = AUDIT_LOG_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @Parameter(description = AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        TenantId tenantId = getCurrentUser().getTenantId();
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, getStartTime(startTime), getEndTime(endTime));
        List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
        return checkNotNull(auditLogService.findAuditLogsByTenantIdAndEntityId(tenantId, EntityIdFactory.getByTypeAndId(strEntityType, strEntityId), actionTypes, pageLink));
    }

    @ApiOperation(value = "Get all audit logs (getAuditLogs)",
            notes = "Returns a page of audit logs related to all entities in the scope of the current user's Tenant. " +
                    PAGE_DATA_PARAMETERS + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogs(
            @Parameter(description = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @Parameter(description = AUDIT_LOG_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = AUDIT_LOG_SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "entityType", "entityName", "userName", "actionType", "actionStatus"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = AUDIT_LOG_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = AUDIT_LOG_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @Parameter(description = AUDIT_LOG_QUERY_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, getStartTime(startTime), getEndTime(endTime));
        return checkNotNull(auditLogService.findAuditLogsByTenantId(tenantId, actionTypes, pageLink));
    }

    private List<ActionType> parseActionTypesStr(String actionTypesStr) {
        List<ActionType> result = null;
        if (StringUtils.isNoneBlank(actionTypesStr)) {
            String[] tmp = actionTypesStr.split(",");
            result = Arrays.stream(tmp).map(at -> ActionType.valueOf(at.toUpperCase())).collect(Collectors.toList());
        }
        return result;
    }

    private Long getStartTime(Long startTime) {
        if (startTime == null) {
            return 1L;
        }
        return startTime;
    }

    private Long getEndTime(Long endTime) {
        if (endTime == null) {
            return System.currentTimeMillis();
        }
        return endTime;
    }
}
