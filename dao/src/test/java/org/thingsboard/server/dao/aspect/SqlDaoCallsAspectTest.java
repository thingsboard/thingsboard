// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.aspect;

import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
@TestPropertySource(properties = "sql.log_tenant_stats=true")
public class SqlDaoCallsAspectTest extends AbstractServiceTest {

    @Autowired
    private DeviceDao deviceDao;

    @Test
    public void sqlDaoCallsAspectAdvisesSqlDaos() {
        assertThat(deviceDao).isInstanceOf(Advised.class);
        assertThat(((Advised) deviceDao).getAdvisors()).anyMatch(a -> a.toString().contains("SqlDaoCallsAspect"));
    }

}
