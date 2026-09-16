// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;


@DaoSqlTest
public class ApiUsageStateServiceTest extends AbstractServiceTest {

    @Autowired
    ApiUsageStateService apiUsageStateService;

    @Test
    public void testFindTenantApiUsageState() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);
        Assert.assertNotNull(state);
    }

    @Test
    public void testUpdate() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);

        state.setTransportState(ApiUsageStateValue.DISABLED);
        ApiUsageState updated = apiUsageStateService.update(state);
        Assert.assertEquals(ApiUsageStateValue.DISABLED, updated.getTransportState());
    }

    @Test
    public void testUpdateWithNullId() {
        ApiUsageState newState = new ApiUsageState();
        newState.setTenantId(tenantId);
        newState.setTransportState(ApiUsageStateValue.ENABLED);
        Assert.assertThrows(IncorrectParameterException.class, () -> apiUsageStateService.update(newState));
    }

    @Test
    public void testFindApiUsageStateByEntityId() {
        ApiUsageState state = apiUsageStateService.findApiUsageStateByEntityId(tenantId);
        Assert.assertNotNull(state);
    }

    @Test
    public void testDeleteByTenantId() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);
        Assert.assertNotNull(state);

        apiUsageStateService.deleteByTenantId(tenantId);
        state = apiUsageStateService.findTenantApiUsageState(tenantId);
        Assert.assertNull(state);
    }

}
