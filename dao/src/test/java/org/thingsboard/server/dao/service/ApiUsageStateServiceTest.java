/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
    public void testMultipleStatesIndependent() {
        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tenantId);

        state.setEdgeState(ApiUsageStateValue.DISABLED);
        state.setTransportState(ApiUsageStateValue.WARNING);
        state.setReExecState(ApiUsageStateValue.ENABLED);
        ApiUsageState updated = apiUsageStateService.update(state);

        assertEquals(ApiUsageStateValue.DISABLED, updated.getEdgeState());
        assertEquals(ApiUsageStateValue.WARNING, updated.getTransportState());
        assertEquals(ApiUsageStateValue.ENABLED, updated.getReExecState());

        Assert.assertFalse(updated.isEdgeEnabled());
        assertTrue(updated.isTransportEnabled());
        assertTrue(updated.isReExecEnabled());
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
