// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.assertTrue;

@TestPropertySource(properties = {
        "spring.datasource.events.enabled=true",
        "spring.datasource.events.url=${spring.datasource.url}",
        "spring.datasource.events.driverClassName=${spring.datasource.driverClassName}"
})
public class JpaTraceDaoTest_DedicatedEventsDataSource extends JpaTraceDaoTest {

    @Autowired
    private TraceDao traceDao;

    @Test
    public void givenDedicatedEventsDataSource_thenDedicatedTraceDaoIsUsed() {
        assertTrue(traceDao instanceof DedicatedJpaTraceDao);
    }

}
