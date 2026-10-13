// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class AiToolControllerOnPremiseTest extends AbstractOnPremiseAiControllerTest {

    @Test
    public void shouldResolveToolApprovalWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();

        ObjectNode request = decision(true);
        ObjectNode response = approvedResponse();
        given(tbAiClient.resolveToolApproval(eq(request), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(response));

        // WHEN
        JsonNode result = doPost("/api/ai/tools/resolve-approval", request, JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(response);

        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).resolveToolApproval(eq(request), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
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
