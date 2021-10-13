/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class AuditLogController extends BaseController {

    protected final String AUDIT_LOG_ACTION_TYPES_DESCRIPTION = "A String value representing action types parameter. The value is not required, but it can be any value of ActionType class. " +
            "For example, 'ADDED,DELETED,UPDATED,LOGIN,LOGOUT'.";
    protected final String SORT_AUDIT_LOG_PROPERTY_DESCRIPTION = "Property of logs to sort by";
    protected final String SORT_AUDIT_LOG_PROPERTY_ALLOWABLE_VALUES = "createdTime, entityName, entityType, user, type, status";

    @ApiOperation(value = "Get audit logs by customer id (getAuditLogsByCustomerId)",
            notes = "Returns a page of audit logs by selected customer. " + PAGE_DATA_PARAMETERS,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs/customer/{customerId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByCustomerId(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = "The case insensitive 'startsWith' filter based on the customer name.")
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_AUDIT_LOG_PROPERTY_DESCRIPTION, allowableValues = SORT_AUDIT_LOG_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "A long value representing the start timestamp(milliseconds) of search time range.")
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = "A long value representing the end timestamp(milliseconds) of search time range.")
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = AUDIT_LOG_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            checkParameter("CustomerId", strCustomerId);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            return checkNotNull(auditLogService.findAuditLogsByTenantIdAndCustomerId(tenantId, new CustomerId(UUID.fromString(strCustomerId)), actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get audit logs by user id (getAuditLogsByUserId)",
            notes = "Returns a page of audit logs by selected user. " + PAGE_DATA_PARAMETERS,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs/user/{userId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByUserId(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable("userId") String strUserId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = "The case insensitive 'startsWith' filter based on the user name.")
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_AUDIT_LOG_PROPERTY_DESCRIPTION, allowableValues = SORT_AUDIT_LOG_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "A long value representing the start timestamp(milliseconds) of search time range.")
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = "A long value representing the end timestamp(milliseconds) of search time range.")
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = AUDIT_LOG_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            checkParameter("UserId", strUserId);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            return checkNotNull(auditLogService.findAuditLogsByTenantIdAndUserId(tenantId, new UserId(UUID.fromString(strUserId)), actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get audit logs by entity id (getAuditLogsByEntityId)",
            notes = "Returns a page of audit logs by selected entity. " + PAGE_DATA_PARAMETERS,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs/entity/{entityType}/{entityId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByEntityId(
            @ApiParam(value = ENTITY_TYPE_DESCRIPTION)
            @PathVariable("entityType") String strEntityType,
            @ApiParam(value = ENTITY_ID_DESCRIPTION)
            @PathVariable("entityId") String strEntityId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = "The case insensitive 'startsWith' filter based on the entity name.")
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_AUDIT_LOG_PROPERTY_DESCRIPTION, allowableValues = SORT_AUDIT_LOG_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "A long value representing the start timestamp(milliseconds) of search time range.")
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = "A long value representing the end timestamp(milliseconds) of search time range.")
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = AUDIT_LOG_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            checkParameter("EntityId", strEntityId);
            checkParameter("EntityType", strEntityType);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            return checkNotNull(auditLogService.findAuditLogsByTenantIdAndEntityId(tenantId, EntityIdFactory.getByTypeAndId(strEntityType, strEntityId), actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get all audit logs (getAuditLogs)",
            notes = "Returns a page of all audit logs. " + PAGE_DATA_PARAMETERS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/audit/logs", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogs(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = "The case insensitive 'startsWith' filter based on any name like 'Device', 'Asset', 'Customer' etc.")
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_AUDIT_LOG_PROPERTY_DESCRIPTION, allowableValues = SORT_AUDIT_LOG_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "A long value representing the start timestamp(milliseconds) of search time range.")
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = "A long value representing the end timestamp(milliseconds) of search time range.")
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = AUDIT_LOG_ACTION_TYPES_DESCRIPTION)
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            return checkNotNull(auditLogService.findAuditLogsByTenantId(tenantId, actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<ActionType> parseActionTypesStr(String actionTypesStr) {
        List<ActionType> result = null;
        if (StringUtils.isNoneBlank(actionTypesStr)) {
            String[] tmp = actionTypesStr.split(",");
            result = Arrays.stream(tmp).map(at -> ActionType.valueOf(at.toUpperCase())).collect(Collectors.toList());
        }
        return result;
    }
}
