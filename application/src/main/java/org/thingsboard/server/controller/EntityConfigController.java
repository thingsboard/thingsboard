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

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityConfig;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityConfigId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EntityConfigController extends BaseController {

    public static final String ENTITY_CONFIG_ID = "entityConfigId";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityConfig/{entityConfigId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityConfig getEntityConfigById(@PathVariable(ENTITY_CONFIG_ID) String entityConfigId) throws ThingsboardException {
        checkParameter(ENTITY_CONFIG_ID, entityConfigId);
        EntityConfig entityConfig = entityConfigService.getEntityConfigById(getTenantId(), new EntityConfigId(toUUID(entityConfigId)));
        if (entityConfig != null) {
            checkEntityId(entityConfig.getEntityId(), Operation.READ);
        }
        return entityConfig;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityConfig/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityConfig getLatestEntityConfigByEntityId(@PathVariable("entityType") String strEntityType,
                                                        @PathVariable("entityId") String strEntityId) throws ThingsboardException {
        checkParameter("entityType", strEntityType);
        checkParameter("entityId", strEntityId);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);
        EntityConfig entityConfig = entityConfigService.getLatestEntityConfigByEntityId(getTenantId(), entityId);
        return entityConfig;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityConfigs/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityConfig> getEntityConfigsByEntityId(
            @PathVariable("entityType") String strEntityType,
            @PathVariable("entityId") String strEntityId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("entityType", strEntityType);
        checkParameter("entityId", strEntityId);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        checkEntityId(entityId, Operation.READ);
        if (StringUtils.isEmpty(sortOrder)) {
            sortOrder = "DESC";
        }
        PageLink pageLink = createPageLink(pageSize, page, null, "createdTime", sortOrder);
        return entityConfigService.getEntityConfigsByEntityId(getTenantId(), entityId, pageLink);
    }
}
