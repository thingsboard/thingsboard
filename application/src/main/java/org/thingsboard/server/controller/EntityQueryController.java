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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.data.search.EntitiesSearchRequest;
import org.thingsboard.server.data.search.EntitySearchResult;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.query.EntitiesSearchService;
import org.thingsboard.server.service.query.EntityQueryService;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_DATA_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_COUNT_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_DATA_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class EntityQueryController extends BaseController {

    private final EntityQueryService entityQueryService;
    private final EntitiesSearchService entitiesSearchService;

    private static final int MAX_PAGE_SIZE = 100;

    @ApiOperation(value = "Count Entities by Query", notes = ENTITY_COUNT_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/count", method = RequestMethod.POST)
    @ResponseBody
    public long countEntitiesByQuery(
            @ApiParam(value = "A JSON value representing the entity count query. See API call notes above for more details.")
            @RequestBody EntityCountQuery query) throws ThingsboardException {
        checkNotNull(query);
        try {
            return this.entityQueryService.countEntitiesByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find Entity Data by Query", notes = ENTITY_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<EntityData> findEntityDataByQuery(
            @ApiParam(value = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        try {
            return this.entityQueryService.findEntityDataByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find Alarms by Query", notes = ALARM_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarmsQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<AlarmData> findAlarmDataByQuery(
            @ApiParam(value = "A JSON value representing the alarm data query. See API call notes above for more details.")
            @RequestBody AlarmDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        try {
            return this.entityQueryService.findAlarmDataByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find Entity Keys by Query",
            notes = "Uses entity data query (see 'Find Entity Data by Query') to find first 100 entities. Then fetch and return all unique time-series and/or attribute keys. Used mostly for UI hints.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find/keys", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> findEntityTimeseriesAndAttributesKeysByQuery(
            @ApiParam(value = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query,
            @ApiParam(value = "Include all unique time-series keys to the result.")
            @RequestParam("timeseries") boolean isTimeseries,
            @ApiParam(value = "Include all unique attribute keys to the result.")
            @RequestParam("attributes") boolean isAttributes) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        checkNotNull(query);
        try {
            EntityDataPageLink pageLink = query.getPageLink();
            if (pageLink.getPageSize() > MAX_PAGE_SIZE) {
                pageLink.setPageSize(MAX_PAGE_SIZE);
            }
            return entityQueryService.getKeysByQuery(getCurrentUser(), tenantId, query, isTimeseries, isAttributes);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Search entities (searchEntities)", notes = "Search entities with specified entity type by id or name within the whole platform. " +
            "Searchable entity types are: CUSTOMER, USER, DEVICE, DEVICE_PROFILE, ASSET, ENTITY_VIEW, DASHBOARD, " +
            "RULE_CHAIN, EDGE, OTA_PACKAGE, TB_RESOURCE, WIDGETS_BUNDLE, TENANT, TENANT_PROFILE." + NEW_LINE +
            "The platform will search for entities, where a name contains the search text (case-insensitively), " +
            "or if the search query is a valid UUID (e.g. 128e4d40-26b3-11ec-aaeb-c7661c54701e) then " +
            "it will also search for an entity where id fully matches the query. If search query is empty " +
            "then all entities will be returned (according to page number, page size and sorting)." + NEW_LINE +
            "The returned result is a page of EntitySearchResult, which contains: " +
            "entity id, name, type (will be present for USER, DEVICE, ASSET, ENTITY_VIEW, RULE_CHAIN, " +
            "EDGE, OTA_PACKAGE, TB_RESOURCE entity types; in case of USER - the type is its authority), " +
            "createdTime, lastActivityTime (will only be present for DEVICE and USER; for USER it is its last login time), " +
            "tenant info (contains tenant's id and title) and owner info (contains owner's id and title " +
            "(entity's customer, or if it is not a customer's entity - tenant)). " + NEW_LINE +
            "Example response value:\n" +
            "{\n" +
            "    \"data\": [\n" +
            "        {\n" +
            "            \"id\": {\n" +
            "                \"entityType\": \"DEVICE\",\n" +
            "                \"id\": \"48be0670-25c9-11ec-a618-8165eb6b112a\"\n" +
            "            },\n" +
            "            \"name\": \"Thermostat T1\",\n" +
            "            \"type\": \"thermostat\",\n" +
            "            \"createdTime\": 1633430698071,\n" +
            "            \"lastActivityTime\": 1635761085285,\n" +
            "            \"tenantInfo\": {\n" +
            "                \"id\": {\n" +
            "                    \"entityType\": \"TENANT\",\n" +
            "                    \"id\": \"2ddd6120-25c9-11ec-a618-8165eb6b112a\"\n" +
            "                },\n" +
            "                \"name\": \"Tenant\"\n" +
            "            },\n" +
            "            \"ownerInfo\": {\n" +
            "                \"id\": {\n" +
            "                    \"entityType\": \"CUSTOMER\",\n" +
            "                    \"id\": \"26cba800-eee3-11eb-9e2c-fb031bd4619c\"\n" +
            "                },\n" +
            "                \"name\": \"Customer A\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"totalPages\": 1,\n" +
            "    \"totalElements\": 1,\n" +
            "    \"hasNext\": false\n" +
            "}")
    @PostMapping("/entities/search")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    public PageData<EntitySearchResult> searchEntities(@RequestBody EntitiesSearchRequest request,
                                                       @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                       @RequestParam int page,
                                                       @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                       @RequestParam int pageSize,
                                                       @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = "name, type, createdTime, lastActivityTime, tenantId, customerId", required = false)
                                                       @RequestParam(required = false) String sortProperty,
                                                       @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES, required = false)
                                                       @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            return entitiesSearchService.searchEntities(getCurrentUser(), request, createPageLink(pageSize, page, null, sortProperty, sortOrder));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
