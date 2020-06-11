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
package org.thingsboard.server.service.entityprofile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.common.data.entityprofile.HasEntityProfileId;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entityprofile.EntityProfileService;
import org.thingsboard.server.service.entityprofile.processor.EntityProfilePostProcessor;
import org.thingsboard.server.service.entityprofile.profile.BaseProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.ENTITY_PROFILES_CACHE;

@Service
@RequiredArgsConstructor
public class TbEntityProfileServiceImpl implements TbEntityProfileService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EntityProfileService entityProfileService;
    private Map<Class<?>, EntityProfilePostProcessor<?>> processors = Collections.emptyMap();
    private TbEntityProfileService self;

    @Autowired
    public void setProcessors(List<EntityProfilePostProcessor<?>> processors) {
        this.processors = processors.stream()
                .collect(Collectors.toMap(EntityProfilePostProcessor::getProfileClass, Function.identity()));
    }

    @Autowired
    public void setSelf(TbEntityProfileService self) {
        this.self = self;
    }

    @Override
    public PageData<EntityProfile> findEntityProfilesByTenantId(TenantId tenantId, PageLink pageLink) {
        return entityProfileService.findEntityProfilesByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<EntityProfile> findEntityProfilesByTenantIdAndType(TenantId tenantId, EntityType entityType,
                                                                       PageLink pageLink) {
        return entityProfileService.findEntityProfilesByTenantIdAndType(tenantId, entityType, pageLink);
    }

    @Override
    @Cacheable(cacheNames = ENTITY_PROFILES_CACHE, key = "{#id}")
    public EntityProfile findById(TenantId tenantId, EntityProfileId id) {
        return entityProfileService.findById(tenantId, id);
    }

    @Override
    @CacheEvict(cacheNames = ENTITY_PROFILES_CACHE, key = "{#entityProfile.id}")
    public EntityProfile save(EntityProfile entityProfile) {
        return entityProfileService.save(entityProfile);
    }

    @Override
    @CacheEvict(cacheNames = ENTITY_PROFILES_CACHE, key = "{#id}")
    public void delete(TenantId tenantId, EntityProfileId id) {
        entityProfileService.delete(tenantId, id);
    }

    @Override
    @SneakyThrows
    public <P extends BaseProfile> EntityProfile createDefault(TenantId tenantId, String name, Class<P> clazz) {
        P profile = clazz.getConstructor().newInstance();
        EntityProfilePostProcessor<P> processor = getProcessor(clazz);
        processor.initProfile(profile);
        EntityProfile entityProfile = EntityProfile.builder()
                .tenantId(tenantId)
                .name(name)
                .entityType(processor.getEntityType())
                .profile(objectMapper.valueToTree(profile))
                .build();
        return self.save(entityProfile);
    }

    @Override
    @SneakyThrows
    public <T extends HasEntityProfileId & HasTenantId, P extends BaseProfile> P findProfile(T obj, Class<P> clazz) {
        TenantId tenantId = obj.getTenantId();
        EntityProfileId entityProfileId = obj.getEntityProfileId();
        JsonNode jsonProfile = objectMapper.createObjectNode();
        if (entityProfileId != null) {
            EntityProfile entityProfile = self.findById(tenantId, entityProfileId);
            if (entityProfile != null) {
                jsonProfile = entityProfile.getProfile();
            }
        }
        P profile = objectMapper.treeToValue(jsonProfile, clazz);
        getProcessor(clazz).setDefaultValues(profile);
        return profile;
    }

    @SuppressWarnings("unchecked")
    private <P extends BaseProfile> EntityProfilePostProcessor<P> getProcessor(Class<P> clazz) {
        EntityProfilePostProcessor<P> processor = (EntityProfilePostProcessor<P>) processors.get(clazz);
        if (processor == null) {
            throw new IllegalStateException(clazz.getSimpleName() + " processor not found");
        }
        return processor;
    }
}
