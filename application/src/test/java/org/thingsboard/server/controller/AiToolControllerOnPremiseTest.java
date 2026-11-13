// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class AiToolControllerOnPremiseTest extends AbstractOnPremiseAiControllerTest {

    @Test
    public void shouldResolveToolApprovalWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();

        ObjectNode request = decision(true);
        ObjectNode response = approvedResponse();
        givenOperation(ChannelProtocol.TOOL_APPROVAL_RESOLVE, request, success(response));

        // WHEN
        JsonNode result = doPost("/api/ai/tools/resolve-approval", request, JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(response);

        TbAiTurnContext context = verifyOperation(ChannelProtocol.TOOL_APPROVAL_RESOLVE, request);
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    private static ObjectNode decision(boolean approved) {
        return JacksonUtil.newObjectNode()
                .put("executionId", UUID.randomUUID().toString())
                .put("approved", approved);
    }

    private static ObjectNode approvedResponse() {
        return JacksonUtil.newObjectNode()
                .put("status", "APPROVED");
    }

}
