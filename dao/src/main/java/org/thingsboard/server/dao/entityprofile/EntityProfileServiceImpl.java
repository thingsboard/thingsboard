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
package org.thingsboard.server.dao.entityprofile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityProfileServiceImpl implements EntityProfileService {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_ENTITY_PROFILE_ID = "Incorrect entityProfileId ";

    private final EntityProfileDao dao;

    @Override
    public PageData<EntityProfile> findEntityProfilesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEntityProfilesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return dao.findEntityProfilesByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<EntityProfile> findEntityProfilesByTenantIdAndType(TenantId tenantId, EntityType entityType, PageLink pageLink) {
        log.trace("Executing findEntityProfilesByTenantId, tenantId [{}], entityType [{}], pageLink [{}]", tenantId, entityType, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateEntityType(entityType);
        validatePageLink(pageLink);
        return dao.findEntityProfilesByTenantIdAndType(tenantId, entityType, pageLink);
    }

    @Override
    public EntityProfile findById(TenantId tenantId, EntityProfileId entityProfileId) {
        log.trace("Executing findById, tenantId [{}], entityProfileId [{}]", tenantId, entityProfileId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityProfileId, INCORRECT_ENTITY_PROFILE_ID + tenantId);
        return dao.findById(tenantId, entityProfileId);
    }

    @Override
    public EntityProfile save(EntityProfile entityProfile) {
        log.trace("Executing save, entityProfile [{}]", entityProfile);
        validateId(entityProfile.getTenantId(), INCORRECT_TENANT_ID + entityProfile.getTenantId());
        return dao.save(entityProfile.getTenantId(), entityProfile);
    }

    @Override
    public void delete(TenantId tenantId, EntityProfileId entityProfileId) {
        log.trace("Executing delete, tenantId [{}], entityProfileId [{}]", tenantId, entityProfileId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityProfileId, INCORRECT_ENTITY_PROFILE_ID + tenantId);
        dao.removeById(tenantId, entityProfileId);
    }

    private void validateEntityType(EntityType entityType) {
        if (entityType == null) {
            throw new IncorrectParameterException("Entity type must be specified.");
        }
    }
}
