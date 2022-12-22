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
package org.thingsboard.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.entity.EntityServiceBeanFactory;
import org.thingsboard.server.dao.rule.BaseRuleChainService;

@Slf4j
public abstract class BaseEntityServiceBeanFactoryTest extends AbstractServiceTest {

    @Autowired
    private EntityServiceBeanFactory entityServiceBeanFactory;

    @Test
    public void givenAllEntityTypes_whenGetServiceByEntityTypeCalled_thenNoExceptionsThrows() {
        for (EntityType entityType : EntityType.values()) {
            Assertions.assertThatCode(() -> entityServiceBeanFactory.getServiceByEntityType(entityType))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    public void givenRuleNodeEntityType_whenGetServiceByEntityTypeCalled_thenReturnedRuleChainDaoService() {
        Assert.assertTrue(entityServiceBeanFactory.getServiceByEntityType(EntityType.RULE_NODE) instanceof BaseRuleChainService);
    }

}
