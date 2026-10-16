// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class ApiUsageStateServiceTest extends AbstractServiceTest {

    @Autowired
    ApiUsageStateService apiUsageStateService;

    @Test
    public void testFindTenantApiUsageState() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);
        assertNotNull(state);
    }

    @Test
    public void testDefaultStateIsEnabled() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);
        assertNotNull(state);
        assertTrue(state.isTransportEnabled());
        assertTrue(state.isReExecEnabled());
        assertTrue(state.isDbStorageEnabled());
        assertTrue(state.isJsExecEnabled());
        assertTrue(state.isTbelExecEnabled());
        assertTrue(state.isEmailSendEnabled());
        assertTrue(state.isSmsSendEnabled());
        assertTrue(state.isAlarmCreationEnabled());
        assertTrue(state.isEdgeEnabled());
        assertTrue(state.isReportCreationEnabled());
        assertTrue(state.isIntegrationEnabled());
    }

    @Test
    public void testUpdate() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);

        state.setTransportState(ApiUsageStateValue.DISABLED);
        ApiUsageState updated = apiUsageStateService.update(state);
        assertEquals(ApiUsageStateValue.DISABLED, updated.getTransportState());
    }

    @Test
    public void testUpdateWithNullId() {
        ApiUsageState newState = new ApiUsageState();
        newState.setTenantId(tenantId);
        newState.setTransportState(ApiUsageStateValue.ENABLED);
        Assert.assertThrows(IncorrectParameterException.class, () -> apiUsageStateService.update(newState));
    }

    @Test
    public void testTransportStateUpdate() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);

        state.setTransportState(ApiUsageStateValue.WARNING);
        ApiUsageState updated = apiUsageStateService.update(state);
        assertEquals(ApiUsageStateValue.WARNING, updated.getTransportState());
        assertTrue(updated.isTransportEnabled());

        updated.setTransportState(ApiUsageStateValue.DISABLED);
        updated = apiUsageStateService.update(updated);
        assertEquals(ApiUsageStateValue.DISABLED, updated.getTransportState());
        Assert.assertFalse(updated.isTransportEnabled());

        updated.setTransportState(ApiUsageStateValue.ENABLED);
        updated = apiUsageStateService.update(updated);
        assertEquals(ApiUsageStateValue.ENABLED, updated.getTransportState());
        assertTrue(updated.isTransportEnabled());
    }

    @Test
    public void testEdgeStateUpdate() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);

        state.setEdgeState(ApiUsageStateValue.WARNING);
        ApiUsageState updated = apiUsageStateService.update(state);
        assertEquals(ApiUsageStateValue.WARNING, updated.getEdgeState());
        assertTrue(updated.isEdgeEnabled());

        updated.setEdgeState(ApiUsageStateValue.DISABLED);
        updated = apiUsageStateService.update(updated);
        assertEquals(ApiUsageStateValue.DISABLED, updated.getEdgeState());
        Assert.assertFalse(updated.isEdgeEnabled());

        ApiUsageState fetched = apiUsageStateService.findTenantApiUsageState(tenantId);
        assertEquals(ApiUsageStateValue.DISABLED, fetched.getEdgeState());
    }

    @Test
    public void testIntegrationStateUpdate() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);

        state.setIntegrationState(ApiUsageStateValue.WARNING);
        ApiUsageState updated = apiUsageStateService.update(state);
        assertEquals(ApiUsageStateValue.WARNING, updated.getIntegrationState());
        assertTrue(updated.isIntegrationEnabled());

        updated.setIntegrationState(ApiUsageStateValue.DISABLED);
        updated = apiUsageStateService.update(updated);
        assertEquals(ApiUsageStateValue.DISABLED, updated.getIntegrationState());
        Assert.assertFalse(updated.isIntegrationEnabled());

        ApiUsageState fetched = apiUsageStateService.findTenantApiUsageState(tenantId);
        assertEquals(ApiUsageStateValue.DISABLED, fetched.getIntegrationState());
    }

    @Test
    public void testMultipleStatesIndependent() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);

        state.setEdgeState(ApiUsageStateValue.DISABLED);
        state.setTransportState(ApiUsageStateValue.WARNING);
        state.setReExecState(ApiUsageStateValue.ENABLED);
        state.setIntegrationState(ApiUsageStateValue.WARNING);
        ApiUsageState updated = apiUsageStateService.update(state);

        assertEquals(ApiUsageStateValue.DISABLED, updated.getEdgeState());
        assertEquals(ApiUsageStateValue.WARNING, updated.getTransportState());
        assertEquals(ApiUsageStateValue.ENABLED, updated.getReExecState());
        assertEquals(ApiUsageStateValue.WARNING, updated.getIntegrationState());

        Assert.assertFalse(updated.isEdgeEnabled());
        assertTrue(updated.isTransportEnabled());
        assertTrue(updated.isReExecEnabled());
        assertTrue(updated.isIntegrationEnabled());
    }

    @Test
    public void testFindApiUsageStateByEntityId() {
        ApiUsageState state = apiUsageStateService.findApiUsageStateByEntityId(tenantId);
        assertNotNull(state);
    }

    @Test
    public void testDeleteByTenantId() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);
        assertNotNull(state);

        apiUsageStateService.deleteByTenantId(tenantId);
        state = apiUsageStateService.findTenantApiUsageState(tenantId);
        assertNull(state);
    }

}
