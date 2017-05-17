/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardException;

@RestController
@RequestMapping("/api")
public class EventController extends BaseController {

    @Autowired
    private EventService eventService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}/{eventType}", method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<Event> getEvents(
            @PathVariable("entityType") String strEntityType,
            @PathVariable("entityId") String strEntityId,
            @PathVariable("eventType") String eventType,
            @RequestParam("tenantId") String strTenantId,
            @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset
    ) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            if (!tenantId.getId().equals(ModelConstants.NULL_UUID) &&
                    !tenantId.equals(getCurrentUser().getTenantId())) {
                throw new ThingsboardException("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            return checkNotNull(eventService.findEvents(tenantId, EntityIdFactory.getByTypeAndId(strEntityType, strEntityId), eventType, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<Event> getEvents(
            @PathVariable("entityType") String strEntityType,
            @PathVariable("entityId") String strEntityId,
            @RequestParam("tenantId") String strTenantId,
            @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset
    ) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            if (!tenantId.getId().equals(ModelConstants.NULL_UUID) &&
                    !tenantId.equals(getCurrentUser().getTenantId())) {
                throw new ThingsboardException("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            return checkNotNull(eventService.findEvents(tenantId, EntityIdFactory.getByTypeAndId(strEntityType, strEntityId), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
