/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.query.EntityQueryService;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EntityQueryController extends BaseController {

    @Autowired
    private EntityQueryService entityQueryService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService timeseriesService;


    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/count", method = RequestMethod.POST)
    @ResponseBody
    public long countEntitiesByQuery(@RequestBody EntityCountQuery query) throws ThingsboardException {
        checkNotNull(query);
        try {
            return this.entityQueryService.countEntitiesByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<EntityData> findEntityDataByQuery(@RequestBody EntityDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        try {
            return this.entityQueryService.findEntityDataByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarmsQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<AlarmData> findAlarmDataByQuery(@RequestBody AlarmDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        try {
            return this.entityQueryService.findAlarmDataByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find/keys/timeseries", method = RequestMethod.POST)
    @ResponseBody
    public List<String> findEntityTimeseriesKeysByQuery(@RequestBody EntityDataQuery query) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        return getKeys(query, entityIds ->  timeseriesService.findAllKeysByEntityIds(tenantId, entityIds));
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find/keys/attributes", method = RequestMethod.POST)
    @ResponseBody
    public List<String> findEntityAttributesKeysByQuery(@RequestBody EntityDataQuery query) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        return getKeys(query, entityIds ->  attributesService.findAllKeysByEntityIds(tenantId, entityIds.get(0).getEntityType(), entityIds));
    }

    private List<String> getKeys(EntityDataQuery query, Function<List<EntityId>, List<String>> function) throws ThingsboardException {
        checkNotNull(query);
        try {
            EntityDataPageLink pageLink = query.getPageLink();
            if (pageLink.getPageSize() > 100) {
                pageLink.setPageSize(100);
            }
            List<EntityId> ids = this.entityQueryService.findEntityDataByQuery(getCurrentUser(), query).getData().stream()
                    .map(EntityData::getEntityId)
                    .collect(Collectors.toList());
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            return function.apply(ids);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
