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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.edqs.EdqsState;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.msg.edqs.EdqsApiService;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.security.permission.Operation;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_DATA_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_SCOPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_COUNT_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_DATA_QUERY_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EntityQueryController extends BaseController {

    @Autowired
    private EntityQueryService entityQueryService;
    @Autowired
    private EdqsService edqsService;
    @Autowired
    private EdqsApiService edqsApiService;

    private static final int MAX_PAGE_SIZE = 100;

    @ApiOperation(value = "Count Entities by Query", notes = ENTITY_COUNT_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/count", method = RequestMethod.POST)
    @ResponseBody
    public long countEntitiesByQuery(
            @Parameter(description = "A JSON value representing the entity count query. See API call notes above for more details.")
            @RequestBody EntityCountQuery query) throws ThingsboardException {
        checkNotNull(query);
        resolveQuery(query);
        return this.entityQueryService.countEntitiesByQuery(getCurrentUser(), query);
    }

    @ApiOperation(value = "Find Entity Data by Query", notes = ENTITY_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<EntityData> findEntityDataByQuery(
            @Parameter(description = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        resolveQuery(query);
        return this.entityQueryService.findEntityDataByQuery(getCurrentUser(), query);
    }

    @ApiOperation(value = "Find Alarms by Query", notes = ALARM_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarmsQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<AlarmData> findAlarmDataByQuery(
            @Parameter(description = "A JSON value representing the alarm data query. See API call notes above for more details.")
            @RequestBody AlarmDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getPageLink());
        UserId assigneeId = query.getPageLink().getAssigneeId();
        if (assigneeId != null) {
            checkUserId(assigneeId, Operation.READ);
        }
        resolveQuery(query);
        return this.entityQueryService.findAlarmDataByQuery(getCurrentUser(), query);
    }

    @ApiOperation(value = "Count Alarms by Query (countAlarmsByQuery)", notes = "Returns the number of alarms that match the query definition.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarmsQuery/count", method = RequestMethod.POST)
    @ResponseBody
    public long countAlarmsByQuery(@Parameter(description = "A JSON value representing the alarm count query.")
                                   @RequestBody AlarmCountQuery query) throws ThingsboardException {
        checkNotNull(query);
        UserId assigneeId = query.getAssigneeId();
        if (assigneeId != null) {
            checkUserId(assigneeId, Operation.READ);
        }
        resolveQuery(query);
        return this.entityQueryService.countAlarmsByQuery(getCurrentUser(), query);
    }

    @ApiOperation(value = "Find Entity Keys by Query",
            notes = "Uses entity data query (see 'Find Entity Data by Query') to find first 100 entities. Then fetch and return all unique time-series and/or attribute keys. Used mostly for UI hints.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find/keys", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> findEntityTimeseriesAndAttributesKeysByQuery(
            @Parameter(description = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query,
            @Parameter(description = "Include all unique time-series keys to the result.")
            @RequestParam("timeseries") boolean isTimeseries,
            @Parameter(description = "Include all unique attribute keys to the result.")
            @RequestParam("attributes") boolean isAttributes,
            @Parameter(description = ATTRIBUTES_SCOPE_DESCRIPTION, schema = @Schema(allowableValues = {"SERVER_SCOPE", "SHARED_SCOPE", "CLIENT_SCOPE"}))
            @RequestParam(value = "scope", required = false) String scope) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        checkNotNull(query);
        resolveQuery(query);
        EntityDataPageLink pageLink = query.getPageLink();
        if (pageLink.getPageSize() > MAX_PAGE_SIZE) {
            pageLink.setPageSize(MAX_PAGE_SIZE);
        }
        return entityQueryService.getKeysByQuery(getCurrentUser(), tenantId, query, isTimeseries, isAttributes, scope);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping("/edqs/system/request")
    public void processSystemEdqsRequest(@RequestBody ToCoreEdqsRequest request) {
        edqsService.processSystemRequest(request);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping("/edqs/state")
    public EdqsState getEdqsState() {
        return edqsService.getState();
    }

    private void resolveQuery(EntityCountQuery query) throws ThingsboardException {
        if (query.getEntityFilter() != null) {
            var user = getCurrentUser();
            var customerId = user.getCustomerId();
            var ownerId = customerId != null && !customerId.isNullUid() ? customerId : getTenantId();
            EntityFilter.resolveEntityFilter(query.getEntityFilter(), getTenantId(), user.getId(), ownerId);
        }
    }

}
