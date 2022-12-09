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

import org.apache.commons.text.CaseUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;

@Service
public class EntityServiceBeanFactory {

    private static final String SERVICE_SUFFIX = "DaoService";

    private final BeanFactory beanFactory;

    public EntityServiceBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public SimpleEntityService getServiceByEntityType(EntityType entityType) {
        String beanName = EntityType.RULE_NODE.equals(entityType) ? getBeanName(EntityType.RULE_CHAIN) : getBeanName(entityType);
        return beanFactory.getBean(beanName, SimpleEntityService.class);
    }

    private String getBeanName(EntityType entityType) {
        return CaseUtils.toCamelCase(entityType.name(), true, '_') + SERVICE_SUFFIX;
    }

}
