/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.entity;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class DefaultEntityServiceRegistry implements EntityServiceRegistry {

    private final ApplicationContext applicationContext;
    private final Map<EntityType, EntityDaoService> entityDaoServicesMap;

    public DefaultEntityServiceRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.entityDaoServicesMap = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        applicationContext.getBeansOfType(EntityDaoService.class).values().forEach(entityDaoService -> {
            EntityType entityType = entityDaoService.getEntityType();
            entityDaoServicesMap.put(entityType, entityDaoService);
            if (EntityType.RULE_CHAIN.equals(entityType)) {
                entityDaoServicesMap.put(EntityType.RULE_NODE, entityDaoService);
            }
        });
    }

    @Override
    public EntityDaoService getServiceByEntityType(EntityType entityType) {
        return entityDaoServicesMap.get(entityType);
    }

}
