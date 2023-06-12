/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
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
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ASSIGNEE_ID;
import static org.thingsboard.server.controller.ControllerConstants.ASSIGN_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
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

    private static final String ALARM_QUERY_SEARCH_STATUS_ARRAY_DESCRIPTION = "A list of string values separated by comma ',' representing one of the AlarmSearchStatus enumeration value";
    private static final String ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES = "ANY, ACTIVE, CLEARED, ACK, UNACK";
    private static final String ALARM_QUERY_STATUS_DESCRIPTION = "A string value representing one of the AlarmStatus enumeration value";
    private static final String ALARM_QUERY_STATUS_ALLOWABLE_VALUES = "ACTIVE_UNACK, ACTIVE_ACK, CLEARED_UNACK, CLEARED_ACK";
    private static final String ALARM_QUERY_SEVERITY_ARRAY_DESCRIPTION = "A list of string values separated by comma ',' representing one of the AlarmSeverity enumeration value";
    private static final String ALARM_QUERY_SEVERITY_ALLOWABLE_VALUES = "CRITICAL, MAJOR, MINOR, WARNING, INDETERMINATE";

    private static final String ALARM_QUERY_TYPE_ARRAY_DESCRIPTION = "A list of string values separated by comma ',' representing alarm types";
    private static final String ALARM_QUERY_ASSIGNEE_DESCRIPTION = "A string value representing the assignee user id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String ALARM_QUERY_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on of next alarm fields: type, severity or status";
    private static final String ALARM_QUERY_START_TIME_DESCRIPTION = "The start timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    private static final String ALARM_QUERY_END_TIME_DESCRIPTION = "The end timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    private static final String ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION = "A boolean value to specify if the alarm originator name will be " +
            "filled in the AlarmInfo object  field: 'originatorName' or will returns as null.";

    @ApiOperation(value = "Get Alarm (getAlarmById)",
            notes = "Fetch the Alarm object based on the provided Alarm Id. " + ALARM_SECURITY_CHECK, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}", method = RequestMethod.GET)
    @ResponseBody
    public Alarm getAlarmById(@Parameter(description = ALARM_ID_PARAM_DESCRIPTION)
                              @PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        return checkAlarmId(alarmId, Operation.READ);
    }

    @ApiOperation(value = "Get Alarm Info (getAlarmInfoById)",
            notes = "Fetch the Alarm Info object based on the provided Alarm Id. " +
                    ALARM_SECURITY_CHECK + ALARM_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/info/{alarmId}", method = RequestMethod.GET)
    @ResponseBody
    public AlarmInfo getAlarmInfoById(@Parameter(description = ALARM_ID_PARAM_DESCRIPTION)
                                      @PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        return checkAlarmInfoId(alarmId, Operation.READ);
    }

    @ApiOperation(value = "Create or Update Alarm (saveAlarm)",
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
            , responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm", method = RequestMethod.POST)
    @ResponseBody
    public Alarm saveAlarm(@Parameter(description = "A JSON value representing the alarm.") @RequestBody Alarm alarm) throws ThingsboardException {
        alarm.setTenantId(getTenantId());
        checkNotNull(alarm.getOriginator());
        checkEntity(alarm.getId(), alarm, Resource.ALARM);
        checkEntityId(alarm.getOriginator(), Operation.READ);
        if (alarm.getAssigneeId() != null) {
            checkUserId(alarm.getAssigneeId(), Operation.READ);
        }
        return tbAlarmService.save(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Delete Alarm (deleteAlarm)",
            notes = "Deletes the Alarm. Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Boolean deleteAlarm(@Parameter(description = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.DELETE);
        return tbAlarmService.delete(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Acknowledge Alarm (ackAlarm)",
            notes = "Acknowledge the Alarm. " +
                    "Once acknowledged, the 'ack_ts' field will be set to current timestamp and special rule chain event 'ALARM_ACK' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/ack", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public AlarmInfo ackAlarm(@Parameter(description = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        //TODO: return correct error code if the alarm is not found or already cleared
        return tbAlarmService.ack(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Clear Alarm (clearAlarm)",
            notes = "Clear the Alarm. " +
                    "Once cleared, the 'clear_ts' field will be set to current timestamp and special rule chain event 'ALARM_CLEAR' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/clear", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public AlarmInfo clearAlarm(@Parameter(description = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        //TODO: return correct error code if the alarm is not found or already cleared
        return tbAlarmService.clear(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Assign/Reassign Alarm (assignAlarm)",
            notes = "Assign the Alarm. " +
                    "Once assigned, the 'assign_ts' field will be set to current timestamp and special rule chain event 'ALARM_ASSIGNED' " +
                    "(or ALARM_REASSIGNED in case of assigning already assigned alarm) will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/assign/{assigneeId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public Alarm assignAlarm(@Parameter(description = ALARM_ID_PARAM_DESCRIPTION)
                             @PathVariable(ALARM_ID) String strAlarmId,
                             @Parameter(description = ASSIGN_ID_PARAM_DESCRIPTION)
                             @PathVariable(ASSIGNEE_ID) String strAssigneeId
    ) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        checkParameter(ASSIGNEE_ID, strAssigneeId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        UserId assigneeId = new UserId(UUID.fromString(strAssigneeId));
        checkUserId(assigneeId, Operation.READ);
        return tbAlarmService.assign(alarm, assigneeId, System.currentTimeMillis(), getCurrentUser());
    }

    @ApiOperation(value = "Unassign Alarm (unassignAlarm)",
            notes = "Unassign the Alarm. " +
                    "Once unassigned, the 'assign_ts' field will be set to current timestamp and special rule chain event 'ALARM_UNASSIGNED' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/assign", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public Alarm unassignAlarm(@Parameter(description = ALARM_ID_PARAM_DESCRIPTION)
                               @PathVariable(ALARM_ID) String strAlarmId
    ) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        return tbAlarmService.unassign(alarm, System.currentTimeMillis(), getCurrentUser());
    }

    @ApiOperation(value = "Get Alarms (getAlarms)",
            notes = "Returns a page of alarms for the selected entity. Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmInfo> getAlarms(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE"))
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @Parameter(description = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES))
            @RequestParam(required = false) String searchStatus,
            @Parameter(description = ALARM_QUERY_STATUS_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES))
            @RequestParam(required = false) String status,
            @Parameter(description = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "startTs", "endTs", "type", "ackTs", "clearTs", "severity", "status"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @Parameter(description = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION)
            @RequestParam(required = false) Boolean fetchOriginator
    ) throws ThingsboardException, ExecutionException, InterruptedException {
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

        return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(entityId, pageLink, alarmSearchStatus, alarmStatus, assigneeUserId, fetchOriginator)).get());
    }

    @ApiOperation(value = "Get All Alarms (getAllAlarms)",
            notes = "Returns a page of alarms that belongs to the current user owner. " +
                    "If the user has the authority of 'Tenant Administrator', the server returns alarms that belongs to the tenant of current user. " +
                    "If the user has the authority of 'Customer User', the server returns alarms that belongs to the customer of current user. " +
                    "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarms", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmInfo> getAllAlarms(
            @Parameter(description = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES))
            @RequestParam(required = false) String searchStatus,
            @Parameter(description = ALARM_QUERY_STATUS_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES))
            @RequestParam(required = false) String status,
            @Parameter(description = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "startTs", "endTs", "type", "ackTs", "clearTs", "severity", "status"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @Parameter(description = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION)
            @RequestParam(required = false) Boolean fetchOriginator
    ) throws ThingsboardException, ExecutionException, InterruptedException {
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

        if (getCurrentUser().isCustomerUser()) {
            return checkNotNull(alarmService.findCustomerAlarms(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), new AlarmQuery(null, pageLink, alarmSearchStatus, alarmStatus, assigneeUserId, fetchOriginator)).get());
        } else {
            return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(null, pageLink, alarmSearchStatus, alarmStatus, assigneeUserId, fetchOriginator)).get());
        }
    }

    @ApiOperation(value = "Get Alarms (getAlarmsV2)",
            notes = "Returns a page of alarms for the selected entity. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/v2/alarm/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmInfo> getAlarmsV2(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE"))
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @Parameter(description = ALARM_QUERY_SEARCH_STATUS_ARRAY_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES))
            @RequestParam(required = false) String[] statusList,
            @Parameter(description = ALARM_QUERY_SEVERITY_ARRAY_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_SEVERITY_ALLOWABLE_VALUES))
            @RequestParam(required = false) String[] severityList,
            @Parameter(description = ALARM_QUERY_TYPE_ARRAY_DESCRIPTION)
            @RequestParam(required = false) String[] typeList,
            @Parameter(description = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "startTs", "endTs", "type", "ackTs", "clearTs", "severity", "status"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime
    ) throws ThingsboardException, ExecutionException, InterruptedException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);
        List<AlarmSearchStatus> alarmStatusList = new ArrayList<>();
        if (statusList != null) {
            for (String strStatus : statusList) {
                if (!StringUtils.isEmpty(strStatus)) {
                    alarmStatusList.add(AlarmSearchStatus.valueOf(strStatus));
                }
            }
        }
        List<AlarmSeverity> alarmSeverityList = new ArrayList<>();
        if (severityList != null) {
            for (String strSeverity : severityList) {
                if (!StringUtils.isEmpty(strSeverity)) {
                    alarmSeverityList.add(AlarmSeverity.valueOf(strSeverity));
                }
            }
        }
        List<String> alarmTypeList = typeList != null ? Arrays.asList(typeList) : Collections.emptyList();
        UserId assigneeUserId = null;
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
        }
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        return checkNotNull(alarmService.findAlarmsV2(getCurrentUser().getTenantId(), new AlarmQueryV2(entityId, pageLink, alarmTypeList, alarmStatusList, alarmSeverityList, assigneeUserId)).get());
    }

    @ApiOperation(value = "Get All Alarms (getAllAlarmsV2)",
            notes = "Returns a page of alarms that belongs to the current user owner. " +
                    "If the user has the authority of 'Tenant Administrator', the server returns alarms that belongs to the tenant of current user. " +
                    "If the user has the authority of 'Customer User', the server returns alarms that belongs to the customer of current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/v2/alarms", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmInfo> getAllAlarmsV2(
            @Parameter(description = ALARM_QUERY_SEARCH_STATUS_ARRAY_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES))
            @RequestParam(required = false) String[] statusList,
            @Parameter(description = ALARM_QUERY_SEVERITY_ARRAY_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_SEVERITY_ALLOWABLE_VALUES))
            @RequestParam(required = false) String[] severityList,
            @Parameter(description = ALARM_QUERY_TYPE_ARRAY_DESCRIPTION)
            @RequestParam(required = false) String[] typeList,
            @Parameter(description = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
            @RequestParam(required = false) String assigneeId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "startTs", "endTs", "type", "ackTs", "clearTs", "severity", "status"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime
    ) throws ThingsboardException, ExecutionException, InterruptedException {
        List<AlarmSearchStatus> alarmStatusList = new ArrayList<>();
        if (statusList != null) {
            for (String strStatus : statusList) {
                if (!StringUtils.isEmpty(strStatus)) {
                    alarmStatusList.add(AlarmSearchStatus.valueOf(strStatus));
                }
            }
        }
        List<AlarmSeverity> alarmSeverityList = new ArrayList<>();
        if (severityList != null) {
            for (String strSeverity : severityList) {
                if (!StringUtils.isEmpty(strSeverity)) {
                    alarmSeverityList.add(AlarmSeverity.valueOf(strSeverity));
                }
            }
        }
        List<String> alarmTypeList = typeList != null ? Arrays.asList(typeList) : Collections.emptyList();
        UserId assigneeUserId = null;
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
        }
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        if (getCurrentUser().isCustomerUser()) {
            return checkNotNull(alarmService.findCustomerAlarmsV2(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), new AlarmQueryV2(null, pageLink, alarmTypeList, alarmStatusList, alarmSeverityList, assigneeUserId)).get());
        } else {
            return checkNotNull(alarmService.findAlarmsV2(getCurrentUser().getTenantId(), new AlarmQueryV2(null, pageLink, alarmTypeList, alarmStatusList, alarmSeverityList, assigneeUserId)).get());
        }
    }

    @ApiOperation(value = "Get Highest Alarm Severity (getHighestAlarmSeverity)",
            notes = "Search the alarms by originator ('entityType' and entityId') and optional 'status' or 'searchStatus' filters and returns the highest AlarmSeverity(CRITICAL, MAJOR, MINOR, WARNING or INDETERMINATE). " +
                    "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , responses = @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/highestSeverity/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public AlarmSeverity getHighestAlarmSeverity(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE"))
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @Parameter(description = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES))
            @RequestParam(required = false) String searchStatus,
            @Parameter(description = ALARM_QUERY_STATUS_DESCRIPTION, schema = @Schema(allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES))
            @RequestParam(required = false) String status,
            @Parameter(description = ALARM_QUERY_ASSIGNEE_DESCRIPTION)
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
        return alarmService.findHighestAlarmSeverity(getCurrentUser().getTenantId(), entityId, alarmSearchStatus,
                alarmStatus, assigneeId);
    }

}
