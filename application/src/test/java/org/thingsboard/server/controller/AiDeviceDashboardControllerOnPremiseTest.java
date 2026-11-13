// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.DashboardOperationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
        givenOperation(ChannelProtocol.DASHBOARD_GENERATE, new DashboardOperationRequest(device.getUuidId(), request), success(JacksonUtil.newObjectNode().put("dashboardId", UUID.randomUUID().toString())));

        // WHEN
        doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", request, JsonNode.class);

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.DASHBOARD_GENERATE, new DashboardOperationRequest(device.getUuidId(), request));
        assertThat(context.tbAccessToken()).isEqualTo("Bearer " + token);
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

}
