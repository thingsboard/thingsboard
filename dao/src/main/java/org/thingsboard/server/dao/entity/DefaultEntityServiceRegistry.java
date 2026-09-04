// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entity;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultEntityServiceRegistry implements EntityServiceRegistry {

    private final List<EntityDaoService> entityDaoServices;
    private final Map<EntityType, EntityDaoService> entityDaoServicesMap = new HashMap<>();

    @PostConstruct
    public void init() {
        log.debug("Initializing EntityServiceRegistry on ContextRefreshedEvent");
        entityDaoServices.forEach(entityDaoService -> {
            EntityType entityType = entityDaoService.getEntityType();
            entityDaoServicesMap.put(entityType, entityDaoService);
            if (EntityType.RULE_CHAIN.equals(entityType)) {
                entityDaoServicesMap.put(EntityType.RULE_NODE, entityDaoService);
            }
            if (EntityType.CALCULATED_FIELD.equals(entityType)) {
                entityDaoServicesMap.put(EntityType.CALCULATED_FIELD_LINK, entityDaoService);
            }
        });
        log.debug("Initialized EntityServiceRegistry total [{}] entries", entityDaoServicesMap.size());
    }

    @Override
    public EntityDaoService getServiceByEntityType(EntityType entityType) {
        return Optional.ofNullable(entityDaoServicesMap.get(entityType))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported entity type " + entityType));
    }

}
