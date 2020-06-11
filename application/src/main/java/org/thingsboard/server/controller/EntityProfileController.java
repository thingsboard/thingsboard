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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class EntityProfileController extends BaseController {
    private static final String ENTITY_PROFILES_ID = "entityProfilesId";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/entityProfiles/{entityProfilesId}")
    public EntityProfile getEntityProfileById(@PathVariable(ENTITY_PROFILES_ID) String strId) throws ThingsboardException {
        checkParameter(ENTITY_PROFILES_ID, strId);
        try {
            EntityProfileId id = new EntityProfileId(toUUID(strId));
            return checkEntityProfileId(id, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping("/entityProfiles")
    public EntityProfile saveEntityProfile(@RequestBody EntityProfile entityProfile) throws ThingsboardException {
        entityProfile = entityProfile.toBuilder()
                .tenantId(getCurrentUser().getTenantId())
                .build();
        ActionType actionType = entityProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            checkEntity(entityProfile.getId(), entityProfile, Resource.ENTITY_PROFILE);
            entityProfile = tbEntityProfileService.save(entityProfile);
            checkNotNull(entityProfile);
            logEntityAction(entityProfile.getId(), entityProfile, null, actionType, null);
            return entityProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_PROFILE), entityProfile, null, actionType, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping("/entityProfiles/{entityProfilesId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteEntityProfile(@PathVariable(ENTITY_PROFILES_ID) String strId) throws ThingsboardException {
        checkParameter(ENTITY_PROFILES_ID, strId);
        EntityProfileId id = new EntityProfileId(toUUID(strId));
        try {
            EntityProfile entityProfile = checkEntityProfileId(new EntityProfileId(toUUID(strId)), Operation.DELETE);
            tbEntityProfileService.delete(getCurrentUser().getTenantId(), id);
            logEntityAction(id, entityProfile, null, ActionType.DELETED, null);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_PROFILE), null, null, ActionType.DELETED, e, id);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(path = "/entityProfiles", params = {"pageSize", "page"})
    public PageData<EntityProfile> getEntityProfiles(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (getCurrentUser().isSystemAdmin()) {
            return checkNotNull(tbEntityProfileService.findTenantProfiles(pageLink));
        }
        TenantId tenantId = getCurrentUser().getTenantId();
        try {
            if (isEmpty(type)) {
                return checkNotNull(tbEntityProfileService.findEntityProfilesByTenantId(tenantId, pageLink));
            } else {
                return checkNotNull(tbEntityProfileService.findEntityProfilesByTenantIdAndType(tenantId, EntityType.valueOf(type), pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
