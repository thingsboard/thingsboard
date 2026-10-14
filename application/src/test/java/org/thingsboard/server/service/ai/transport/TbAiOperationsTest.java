// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TbAiOperationsTest {

    TbAiTurnContext context = new TbAiTurnContext(mock(SecurityUser.class), "Bearer jwt", null, () -> "token",
            "https://tb.example.com");
    TbAiResponse channelResponse = TbAiResponse.builder().success(true).build();
    TbAiOperation operation = new TbAiOperation(ChannelProtocol.SOLUTION_INSTALL, null);
    ChannelTbAiOperations channel = mock(ChannelTbAiOperations.class);
    SystemSecurityService systemSecurityService = mock(SystemSecurityService.class);
    TbAiOperations operations = new TbAiOperations(channel, systemSecurityService);

    @Test
    void shouldReturnChannelResponse_whenChannelAnswers() {
        // GIVEN
        given(channel.execute(operation, context)).willReturn(channelResponse);

        // WHEN-THEN
        assertThat(operations.execute(operation, context)).isSameAs(channelResponse);
    }

    @Test
    void shouldReturnServiceUnavailable_whenChannelCannotBeOpened() {
        // GIVEN
        given(channel.execute(operation, context)).willThrow(new TbAiChannelUnavailableException("404", null));

        // WHEN
        TbAiResponse response = operations.execute(operation, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Service unavailable");
    }

    @Test
    void shouldReturnRejection_whenChannelHandshakeIsRefused() {
        // GIVEN
        given(channel.execute(operation, context)).willThrow(new TbAiChannelRejectedException("Unauthorized"));

        // WHEN
        TbAiResponse response = operations.execute(operation, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Unauthorized");
    }

    @Test
    void shouldUseSettingsBaseUrlAsOrigin_whenCalledForUser() {
        // GIVEN
        SecurityUser user = mock(SecurityUser.class);
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        given(user.getAuthority()).willReturn(Authority.TENANT_ADMIN);
        given(user.getTenantId()).willReturn(tenantId);
        given(user.getCustomerId()).willReturn(customerId);
        given(systemSecurityService.getBaseUrl(Authority.TENANT_ADMIN, tenantId, customerId, null)).willReturn("https://tb.example.com");
        given(channel.execute(any(), any())).willReturn(channelResponse);

        // WHEN
        operations.execute(operation, user, () -> "token");

        // THEN
        ArgumentCaptor<TbAiTurnContext> captured = ArgumentCaptor.forClass(TbAiTurnContext.class);
        verify(channel).execute(same(operation), captured.capture());
        assertThat(captured.getValue().clientOrigin()).isEqualTo("https://tb.example.com");
        assertThat(captured.getValue().tbAccessToken()).isNull();
    }

}
