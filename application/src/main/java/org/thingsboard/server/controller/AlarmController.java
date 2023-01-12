/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ASSIGNEE_ID;
import static org.thingsboard.server.controller.ControllerConstants.ASSIGN_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmController extends BaseController {

    private final TbAlarmService tbAlarmService;

    public static final String ALARM_ID = "alarmId";
    private static final String ALARM_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the originator of alarm is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the originator of alarm belongs to the customer. ";
    private static final String ALARM_QUERY_SEARCH_STATUS_DESCRIPTION = "A string value representing one of the AlarmSearchStatus enumeration value";
    private static final String ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES = "ANY, ACTIVE, CLEARED, ACK, UNACK";
    private static final String ALARM_QUERY_STATUS_DESCRIPTION = "A string value representing one of the AlarmStatus enumeration value";
    private static final String ALARM_QUERY_STATUS_ALLOWABLE_VALUES = "ACTIVE_UNACK, ACTIVE_ACK, CLEARED_UNACK, CLEARED_ACK";
    private static final String ALARM_QUERY_ASSIGNEE_DESCRIPTION = "A string value representing the assignee user id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String ALARM_QUERY_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on of next alarm fields: type, severity or status";
    private static final String ALARM_QUERY_START_TIME_DESCRIPTION = "The start timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    private static final String ALARM_QUERY_END_TIME_DESCRIPTION = "The end timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    private static final String ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION = "A boolean value to specify if the alarm originator name will be " +
            "filled in the AlarmInfo object  field: 'originatorName' or will returns as null.";

    @ApiOperation(value = "Get Alarm (getAlarmById)",
            notes = "Fetch the Alarm object based on the provided Alarm Id. " + ALARM_SECURITY_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}", method = RequestMethod.GET)
    @ResponseBody
    public Alarm getAlarmById(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                              @PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        try {
            AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
            return checkAlarmId(alarmId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Alarm Info (getAlarmInfoById)",
            notes = "Fetch the Alarm Info object based on the provided Alarm Id. " +
                    ALARM_SECURITY_CHECK + ALARM_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/info/{alarmId}", method = RequestMethod.GET)
    @ResponseBody
    public AlarmInfo getAlarmInfoById(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                                      @PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        try {
            AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
            return checkAlarmInfoId(alarmId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create or update Alarm (saveAlarm)",
            notes = "Creates or Updates the Alarm. " +
                    "When creating alarm, platform generates Alarm Id as " + UUID_WIKI_LINK +
                    "The newly created Alarm id will be present in the response. Specify existing Alarm id to update the alarm. " +
                    "Referencing non-existing Alarm Id will cause 'Not Found' error. " +
                    "\n\nPlatform also deduplicate the alarms based on the entity id of originator and alarm 'type'. " +
                    "For example, if the user or system component create the alarm with the type 'HighTemperature' for device 'Device A' the new active alarm is created. " +
                    "If the user tries to create 'HighTemperature' alarm for the same device again, the previous alarm will be updated (the 'end_ts' will be set to current timestamp). " +
                    "If the user clears the alarm (see 'Clear Alarm(clearAlarm)'), than new alarm with the same type and same device may be created. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Alarm entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm", method = RequestMethod.POST)
    @ResponseBody
    public Alarm saveAlarm(@ApiParam(value = "A JSON value representing the alarm.") @RequestBody Alarm alarm) throws ThingsboardException {
        alarm.setTenantId(getTenantId());
        checkEntity(alarm.getId(), alarm, Resource.ALARM);
        return tbAlarmService.save(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Delete Alarm (deleteAlarm)",
            notes = "Deletes the Alarm. Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Boolean deleteAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.DELETE);
        return tbAlarmService.delete(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Acknowledge Alarm (ackAlarm)",
            notes = "Acknowledge the Alarm. " +
                    "Once acknowledged, the 'ack_ts' field will be set to current timestamp and special rule chain event 'ALARM_ACK' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/ack", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void ackAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        tbAlarmService.ack(alarm, getCurrentUser()).get();
    }

    @ApiOperation(value = "Clear Alarm (clearAlarm)",
            notes = "Clear the Alarm. " +
                    "Once cleared, the 'clear_ts' field will be set to current timestamp and special rule chain event 'ALARM_CLEAR' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/clear", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void clearAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        tbAlarmService.clear(alarm, getCurrentUser()).get();
    }

    @ApiOperation(value = "Assign/Reassign Alarm (assignAlarm)",
            notes = "Assign the Alarm. " +
                    "Once assigned, the 'assign_ts' field will be set to current timestamp and special rule chain event 'ALARM_ASSIGNED' " +
                    "(or ALARM_REASSIGNED in case of assigning already assigned alarm) will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/assign/{assigneeId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void assignAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                            @PathVariable(ALARM_ID) String strAlarmId,
                            @ApiParam(value = ASSIGN_ID_PARAM_DESCRIPTION)
                            @PathVariable(ASSIGNEE_ID) String strAssigneeId
    ) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        checkParameter(ASSIGNEE_ID, strAssigneeId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        UserId assigneeId = new UserId(UUID.fromString(strAssigneeId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        User assigneeUser = userService.findUserById(getTenantId(), assigneeId);

        SecurityUser assigneeSecurityUser = new SecurityUser(assigneeUser, false, null);
        try {
            checkEntityId(alarm.getOriginator(), Operation.WRITE, assigneeSecurityUser);
        } catch (Exception e) {
            throw new ThingsboardException("Assignee user doesn't have permission for alarm originator", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        tbAlarmService.assign(alarm, getCurrentUser(), assigneeId).get();
    }

    @ApiOperation(value = "Unassign Alarm (unassignAlarm)",
            notes = "Unassign the Alarm. " +
                    "Once unassigned, the 'assign_ts' field will be set to current timestamp and special rule chain event 'ALARM_UNASSIGNED' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/assign", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void assignAlarm(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                            @PathVariable(ALARM_ID) String strAlarmId
    ) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        tbAlarmService.unassign(alarm, getCurrentUser()).get();
    }

    @ApiOperation(value = "Get Alarms (getAlarms)",
            notes = "Returns a page of alarms for the selected entity. Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmInfo> getAlarms(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE")
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String searchStatus,
            @ApiParam(value = ALARM_QUERY_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String status,
            @ApiParam(value = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ALARM_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION)
            @RequestParam(required = false) Boolean fetchOriginator
    ) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
        AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
        if (alarmSearchStatus != null && alarmStatus != null) {
            throw new ThingsboardException("Invalid alarms search query: Both parameters 'searchStatus' " +
                    "and 'status' can't be specified at the same time!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        checkEntityId(entityId, Operation.READ);
        UserId assigneeUserId = null;
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
        }
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        try {
            return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(entityId, pageLink, alarmSearchStatus, alarmStatus, assigneeUserId, fetchOriginator)).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get All Alarms (getAllAlarms)",
            notes = "Returns a page of alarms that belongs to the current user owner. " +
                    "If the user has the authority of 'Tenant Administrator', the server returns alarms that belongs to the tenant of current user. " +
                    "If the user has the authority of 'Customer User', the server returns alarms that belongs to the customer of current user. " +
                    "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarms", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmInfo> getAllAlarms(
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String searchStatus,
            @ApiParam(value = ALARM_QUERY_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String status,
            @ApiParam(value = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ALARM_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION)
            @RequestParam(required = false) Boolean fetchOriginator
    ) throws ThingsboardException {
        AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
        AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
        if (alarmSearchStatus != null && alarmStatus != null) {
            throw new ThingsboardException("Invalid alarms search query: Both parameters 'searchStatus' " +
                    "and 'status' can't be specified at the same time!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        UserId assigneeUserId = null;
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
        }
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        try {
            if (getCurrentUser().isCustomerUser()) {
                return checkNotNull(alarmService.findCustomerAlarms(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), new AlarmQuery(null, pageLink, alarmSearchStatus, alarmStatus, assigneeUserId, fetchOriginator)).get());
            } else {
                return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(null, pageLink, alarmSearchStatus, alarmStatus, assigneeUserId, fetchOriginator)).get());
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Highest Alarm Severity (getHighestAlarmSeverity)",
            notes = "Search the alarms by originator ('entityType' and entityId') and optional 'status' or 'searchStatus' filters and returns the highest AlarmSeverity(CRITICAL, MAJOR, MINOR, WARNING or INDETERMINATE). " +
                    "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/highestSeverity/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public AlarmSeverity getHighestAlarmSeverity(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE")
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String searchStatus,
            @ApiParam(value = ALARM_QUERY_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String status,
            @ApiParam(value = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId
    ) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
        AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
        if (alarmSearchStatus != null && alarmStatus != null) {
            throw new ThingsboardException("Invalid alarms search query: Both parameters 'searchStatus' " +
                    "and 'status' can't be specified at the same time!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        checkEntityId(entityId, Operation.READ);
        try {
            return alarmService.findHighestAlarmSeverity(getCurrentUser().getTenantId(), entityId, alarmSearchStatus, alarmStatus, assigneeId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
