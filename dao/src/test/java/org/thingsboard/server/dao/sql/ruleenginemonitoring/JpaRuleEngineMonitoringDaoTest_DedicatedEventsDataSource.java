// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.assertTrue;

@TestPropertySource(properties = {
        "spring.datasource.events.enabled=true",
        "spring.datasource.events.url=${spring.datasource.url}",
        "spring.datasource.events.driverClassName=${spring.datasource.driverClassName}"
})
public class JpaRuleEngineMonitoringDaoTest_DedicatedEventsDataSource extends JpaRuleEngineMonitoringDaoTest {

    @Autowired
    private RuleEngineMonitoringDao ruleEngineMonitoringDao;

    @Test
    public void givenDedicatedEventsDataSource_thenDedicatedDaoIsUsed() {
        assertTrue(ruleEngineMonitoringDao instanceof DedicatedJpaRuleEngineMonitoringDao);
    }

}
