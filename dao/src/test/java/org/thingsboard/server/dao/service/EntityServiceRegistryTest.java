// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.dao.rule.RuleChainService;

@Slf4j
@DaoSqlTest
public class EntityServiceRegistryTest extends AbstractServiceTest {

    @Autowired
    EntityServiceRegistry entityServiceRegistry;

    @Test
    public void givenAllEntityTypes_whenGetServiceByEntityTypeCalled_thenAllBeansExists() {
        for (EntityType entityType : EntityType.values()) {
            EntityDaoService entityDaoService = entityServiceRegistry.getServiceByEntityType(entityType);
            Assert.assertNotNull("entityDaoService bean is missed for type: " + entityType.name(), entityDaoService);
        }
    }

    @Test
    public void givenRuleNodeEntityType_whenGetServiceByEntityTypeCalled_thenReturnedRuleChainService() {
        Assert.assertTrue(entityServiceRegistry.getServiceByEntityType(EntityType.RULE_NODE) instanceof RuleChainService);
    }

    @Test
    public void givenCalculatedFieldLinkEntityType_whenGetServiceByEntityTypeCalled_thenReturnedCalculatedFieldService() {
        Assert.assertTrue(entityServiceRegistry.getServiceByEntityType(EntityType.CALCULATED_FIELD_LINK) instanceof CalculatedFieldService);
    }
}
