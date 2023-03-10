/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.google.common.collect.MapMaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultEntityServiceRegistry implements EntityServiceRegistry {

    private final ApplicationContext applicationContext;
    private final Map<EntityType, EntityDaoService> entityDaoServicesMap = new MapMaker().weakValues().makeMap();

    @EventListener(ContextRefreshedEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void init() {
        log.debug("Initializing EntityServiceRegistry on ContextRefreshedEvent");
        applicationContext.getBeansOfType(EntityDaoService.class).values().forEach(entityDaoService -> {
            EntityType entityType = entityDaoService.getEntityType();
            entityDaoServicesMap.put(entityType, entityDaoService);
            if (EntityType.RULE_CHAIN.equals(entityType)) {
                entityDaoServicesMap.put(EntityType.RULE_NODE, entityDaoService);
            }
        });
        log.debug("Initialized EntityServiceRegistry total [{}] entries", entityDaoServicesMap.size());
    }

    @Override
    public EntityDaoService getServiceByEntityType(EntityType entityType) {
        return entityDaoServicesMap.get(entityType);
    }

}
