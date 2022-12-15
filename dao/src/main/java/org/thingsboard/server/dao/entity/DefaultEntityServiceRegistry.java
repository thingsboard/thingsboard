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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.CaseUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

@Slf4j
@Service
public class DefaultEntityServiceRegistry implements EntityServiceRegistry {

    private static final String SERVICE_SUFFIX = "DaoService";

    private Map<String, EntityDaoService> entityDaoServicesMap;

    private final ApplicationContext applicationContext;

    public DefaultEntityServiceRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        entityDaoServicesMap = applicationContext.getBeansOfType(EntityDaoService.class);
    }

    @PreDestroy
    public void destroy() {
        if (entityDaoServicesMap != null) {
            entityDaoServicesMap.clear();
        }
    }

    @Override
    public EntityDaoService getServiceByEntityType(EntityType entityType) {
        String beanName = EntityType.RULE_NODE.equals(entityType) ? getBeanName(EntityType.RULE_CHAIN) : getBeanName(entityType);
        return entityDaoServicesMap.get(beanName);
    }

    private String getBeanName(EntityType entityType) {
        return CaseUtils.toCamelCase(entityType.name(), true, '_') + SERVICE_SUFFIX;
    }

}
