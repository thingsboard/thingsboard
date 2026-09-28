// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class AiDeviceDashboardControllerOnPremiseTest extends AbstractOnPremiseAiControllerTest {

    private Device device;

    @Before
    public void setup() throws Exception {
        loginTenantAdmin();

        Device d = new Device();
        d.setName("AI Dashboard On-Premise Test Device");
        d.setType("default");
        device = doPost("/api/device", d, Device.class);
    }

    @Test
    public void shouldGenerateDashboardWithOnPremiseToken_whenTenantAdmin() {
        // GIVEN
        givenSubscriptionProvidesAiToken();
        ObjectNode request = JacksonUtil.newObjectNode();
        given(tbAiClient.generateDashboard(eq(device.getUuidId()), eq(request), eq("Bearer " + token), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(JacksonUtil.newObjectNode().put("dashboardId", UUID.randomUUID().toString())));

        // WHEN
        doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", request, JsonNode.class);

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).generateDashboard(eq(device.getUuidId()), eq(request), eq("Bearer " + token), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

}
