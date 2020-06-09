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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.entityprofile.BaseProfile;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.common.data.entityprofile.HasEntityProfileId;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityProfileServiceImpl implements EntityProfileService {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_ENTITY_PROFILE_ID = "Incorrect entityProfileId ";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EntityProfileDao dao;
    private Map<Class<?>, EntityProfilePostProcessor<?>> processors = Collections.emptyMap();

    @Autowired(required = false)
    public void setProcessors(List<EntityProfilePostProcessor<?>> processors) {
        this.processors = processors.stream()
                .collect(Collectors.toMap(EntityProfilePostProcessor::getProfileClass, Function.identity()));
    }

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

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T extends HasEntityProfileId & HasTenantId, P extends BaseProfile> P findProfile(T obj, Class<P> clazz) {
        TenantId tenantId = obj.getTenantId();
        EntityProfileId entityProfileId = obj.getEntityProfileId();
        JsonNode jsonProfile = objectMapper.createObjectNode();;
        if (entityProfileId != null) {
            EntityProfile entityProfile = findById(tenantId, entityProfileId);
            if (entityProfile != null) {
                jsonProfile = entityProfile.getProfile();
            }
        }
        P profile = objectMapper.treeToValue(jsonProfile, clazz);
        EntityProfilePostProcessor<P> processor = (EntityProfilePostProcessor<P>) processors.get(clazz);
        if (processor != null) {
            processor.setDefaultValues(profile);
        }
        return profile;
    }

    private void validateEntityType(EntityType entityType) {
        if (entityType == null) {
            throw new IncorrectParameterException("Entity type must be specified.");
        }
    }
}
