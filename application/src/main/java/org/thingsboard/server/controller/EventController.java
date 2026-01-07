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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_DEBUG_CALCULATED_FIELD_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_DEBUG_RULE_CHAIN_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_DEBUG_RULE_NODE_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_END_TIME_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_ERROR_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_LC_EVENT_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_START_TIME_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_STATS_FILTER_OBJ;
import static org.thingsboard.server.controller.ControllerConstants.EVENT_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EventController extends BaseController {

    private static final String EVENT_FILTER_DEFINITION = "# Event Filter Definition" + NEW_LINE +
            "6 different eventFilter objects could be set for different event types. " +
            "The eventType field is required. Others are optional. If some of them are set, the filtering will be applied according to them. " +
            "See the examples below for all the fields used for each event type filtering. " + NEW_LINE +
            "Note," + NEW_LINE +
            " * 'server' - string value representing the server name, identifier or ip address where the platform is running;\n" +
            " * 'errorStr' - the case insensitive 'contains' filter based on error message." + NEW_LINE +
            "## Error Event Filter" + NEW_LINE +
            EVENT_ERROR_FILTER_OBJ + NEW_LINE +
            " * 'method' - string value representing the method name when the error happened." + NEW_LINE +
            "## Lifecycle Event Filter" + NEW_LINE +
            EVENT_LC_EVENT_FILTER_OBJ + NEW_LINE +
            " * 'event' - string value representing the lifecycle event type;\n" +
            " * 'status' - string value representing status of the lifecycle event." + NEW_LINE +
            "## Statistics Event Filter" + NEW_LINE +
            EVENT_STATS_FILTER_OBJ + NEW_LINE +
            " * 'messagesProcessed' - the minimum number of successfully processed messages;\n" +
            " * 'errorsOccurred' - the minimum number of errors occurred during messages processing." + NEW_LINE +
            "## Debug Rule Node Event Filter" + NEW_LINE +
            EVENT_DEBUG_RULE_NODE_FILTER_OBJ + NEW_LINE +
            "## Debug Rule Chain Event Filter" + NEW_LINE +
            EVENT_DEBUG_RULE_CHAIN_FILTER_OBJ + NEW_LINE +
            " * 'msgDirectionType' - string value representing msg direction type (incoming to entity or outcoming from entity);\n" +
            " * 'dataSearch' - the case insensitive 'contains' filter based on data (key and value) for the message;\n" +
            " * 'metadataSearch' - the case insensitive 'contains' filter based on metadata (key and value) for the message;\n" +
            " * 'entityName' - string value representing the entity type;\n" +
            " * 'relationType' - string value representing the type of message routing;\n" +
            " * 'entityId' - string value representing the entity id in the event body (originator of the message);\n" +
            " * 'msgType' - string value representing the message type;\n" +
            " * 'isError' - boolean value to filter the errors." + NEW_LINE +
            "## Debug Calculated Field Event Filter" + NEW_LINE +
            EVENT_DEBUG_CALCULATED_FIELD_FILTER_OBJ + NEW_LINE +
            " * 'entityId' - string value representing the entity id in the event body;\n" +
            " * 'entityType' - string value representing the entity type;\n" +
            " * 'msgId' - string value representing the message id in the rule engine;\n" +
            " * 'msgType' - string value representing the message type;\n" +
            " * 'arguments' - string value representing the arguments that were used in the calculation performed;\n" +
            " * 'result' - string value representing the result of a calculation;\n" +
            " * 'isError' - boolean value to filter the errors." + NEW_LINE;

    @Autowired
    private EventService eventService;

    @ApiOperation(value = "Get Events by type (getEvents)",
            notes = "Returns a page of events for specified entity by specifying event type. " +
                    PAGE_DATA_PARAMETERS)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}/{eventType}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<EventInfo> getEvents(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @Parameter(description = "A string value representing event type", example = "STATS", required = true)
            @PathVariable("eventType") String eventType,
            @Parameter(description = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @RequestParam(TENANT_ID) String strTenantId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = EVENT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"ts", "id"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = EVENT_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = EVENT_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));

        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        return checkNotNull(eventService.findEvents(tenantId, entityId, resolveEventType(eventType), pageLink));
    }

    @ApiOperation(value = "Get Events (Deprecated)",
            notes = "Returns a page of events for specified entity. Deprecated and will be removed in next minor release. " +
                    "The call was deprecated to improve the performance of the system. " +
                    "Current implementation will return 'Lifecycle' events only. " +
                    "Use 'Get events by type' or 'Get events by filter' instead. " +
                    PAGE_DATA_PARAMETERS)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<EventInfo> getEvents(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @Parameter(description = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @RequestParam("tenantId") String strTenantId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = EVENT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"ts", "id"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = EVENT_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = EVENT_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));

        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);

        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        return checkNotNull(eventService.findEvents(tenantId, entityId, EventType.LC_EVENT, pageLink));
    }

    @ApiOperation(value = "Get Events by event filter (getEvents)",
            notes = "Returns a page of events for the chosen entity by specifying the event filter. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE +
                    EVENT_FILTER_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}", method = RequestMethod.POST)
    @ResponseBody
    public PageData<EventInfo> getEvents(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @Parameter(description = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @RequestParam(TENANT_ID) String strTenantId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "A JSON value representing the event filter.", required = true)
            @RequestBody EventFilter eventFilter,
            @Parameter(description = EVENT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"ts", "id"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = EVENT_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = EVENT_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));

        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);

        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        return checkNotNull(eventService.findEventsByFilter(tenantId, entityId, eventFilter, pageLink));
    }

    @ApiOperation(value = "Clear Events (clearEvents)", notes = "Clears events by filter for specified entity.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}/clear", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void clearEvents(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
                            @PathVariable(ENTITY_TYPE) String strEntityType,
                            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
                            @PathVariable(ENTITY_ID) String strEntityId,
                            @Parameter(description = EVENT_START_TIME_DESCRIPTION)
                            @RequestParam(required = false) Long startTime,
                            @Parameter(description = EVENT_END_TIME_DESCRIPTION)
                            @RequestParam(required = false) Long endTime,
                            @Parameter(description = EVENT_FILTER_DEFINITION)
                            @RequestBody EventFilter eventFilter) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.WRITE);

        eventService.removeEvents(getTenantId(), entityId, eventFilter, startTime, endTime);
    }

    private static EventType resolveEventType(String eventType) throws ThingsboardException {
        for (var et : EventType.values()) {
            if (et.name().equalsIgnoreCase(eventType) || et.getOldName().equalsIgnoreCase(eventType)) {
                return et;
            }
        }
        throw new ThingsboardException("Event type: '" + eventType + "' is not supported!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    }

}
