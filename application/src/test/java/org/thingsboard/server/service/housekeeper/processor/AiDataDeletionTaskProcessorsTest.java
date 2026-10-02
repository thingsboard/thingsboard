// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.ai.TbAiTokenProvider;
import org.thingsboard.server.service.ai.transport.TbAiClientRequest;
import org.thingsboard.server.service.ai.transport.TbAiClientRequestFactory;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.ai.transport.TbAiTurnContext;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AiDataDeletionTaskProcessorsTest {

    @Mock
    TbAiTokenProvider tokenProvider;
    @Mock
    TbAiOperations operations;
    @Mock
    TbAiClientRequestFactory clientRequestFactory;

    TenantId tenantId;
    UserId userId;

    AiUserDataDeletionTaskProcessor userDataProcessor;
    AiTenantDataDeletionTaskProcessor tenantDataProcessor;

    @BeforeEach
    void setUp() {
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        userId = new UserId(UUID.randomUUID());
        userDataProcessor = new AiUserDataDeletionTaskProcessor(Optional.of(tokenProvider), operations, clientRequestFactory);
        tenantDataProcessor = new AiTenantDataDeletionTaskProcessor(Optional.of(tokenProvider), operations, clientRequestFactory);
        lenient().when(clientRequestFactory.forTenant(any())).thenReturn(TbAiClientRequest.withDefaultOrigin("https://tb.example.com"));
        lenient().when(operations.execute(any(TbAiOperation.class), any(TbAiTurnContext.class)))
                .thenReturn(TbAiResponse.builder().success(true).build());
    }

    @Test
    void shouldReportMatchingTaskTypes() {
        assertThat(userDataProcessor.getTaskType()).isEqualTo(HousekeeperTaskType.DELETE_AI_USER_DATA);
        assertThat(tenantDataProcessor.getTaskType()).isEqualTo(HousekeeperTaskType.DELETE_AI_TENANT_DATA);
    }

    @Test
    void shouldCallUserDataDeletionWithIdScopedToken_whenProcessingUserDataTask() throws Exception {
        // GIVEN
        given(tokenProvider.isTokenAvailable()).willReturn(true);
        given(tokenProvider.getToken(tenantId, userId)).willReturn("id-scoped-token");
        given(tokenProvider.getAdditionalInfo(tenantId, userId)).willReturn(Map.of("X-User-Id", userId.toString()));

        // WHEN
        userDataProcessor.process(HousekeeperTask.deleteAiUserData(tenantId, userId));

        // THEN — one background operation, with a token provider that resolves identity from the task's raw ids.
        ArgumentCaptor<TbAiOperation> operation = ArgumentCaptor.forClass(TbAiOperation.class);
        ArgumentCaptor<TbAiTurnContext> context = ArgumentCaptor.forClass(TbAiTurnContext.class);
        then(operations).should().execute(operation.capture(), context.capture());
        assertThat(context.getValue().tokenProvider().getToken()).isEqualTo("id-scoped-token");
        assertThat(context.getValue().tokenProvider().getAdditionalInfo()).isEqualTo(Map.of("X-User-Id", userId.toString()));
        assertThat(operation.getValue().type()).isEqualTo(ChannelProtocol.USER_DATA_DELETE);
        assertThat(context.getValue().isBackground()).isTrue();
        assertThat(context.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(context.getValue().userId()).isEqualTo(userId);
        assertThat(context.getValue().tbAccessToken()).isNull();
    }

    @Test
    void shouldCallTenantDataDeletionWithIdScopedToken_whenProcessingTenantDataTask() throws Exception {
        // GIVEN — for a tenant task the tenant id doubles as the token's user identity.
        given(tokenProvider.isTokenAvailable()).willReturn(true);
        given(tokenProvider.getToken(tenantId, new UserId(tenantId.getId()))).willReturn("id-scoped-token");

        // WHEN
        tenantDataProcessor.process(HousekeeperTask.deleteAiTenantData(tenantId));

        // THEN
        ArgumentCaptor<TbAiOperation> operation = ArgumentCaptor.forClass(TbAiOperation.class);
        ArgumentCaptor<TbAiTurnContext> context = ArgumentCaptor.forClass(TbAiTurnContext.class);
        then(operations).should().execute(operation.capture(), context.capture());
        assertThat(operation.getValue().type()).isEqualTo(ChannelProtocol.TENANT_DATA_DELETE);
        assertThat(context.getValue().tokenProvider().getToken()).isEqualTo("id-scoped-token");
    }

    @Test
    void shouldThrow_whenAiServiceReportsFailure() {
        // GIVEN
        given(tokenProvider.isTokenAvailable()).willReturn(true);
        given(operations.execute(any(TbAiOperation.class), any(TbAiTurnContext.class)))
                .willReturn(TbAiResponse.builder().success(false).error("Service unavailable").build());

        // WHEN
        Throwable thrown = catchThrowable(() -> userDataProcessor.process(HousekeeperTask.deleteAiUserData(tenantId, userId)));

        // THEN — the processor must fail loudly so Housekeeper reprocessing kicks in.
        assertThat(thrown).isInstanceOf(RuntimeException.class).hasMessage("Service unavailable");
    }

    @Test
    void shouldSkipWithoutCallingAiService_whenAiTokenIsNotAvailable() throws Exception {
        // GIVEN
        given(tokenProvider.isTokenAvailable()).willReturn(false);

        // WHEN
        userDataProcessor.process(HousekeeperTask.deleteAiUserData(tenantId, userId));
        tenantDataProcessor.process(HousekeeperTask.deleteAiTenantData(tenantId));

        // THEN
        then(operations).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowWithoutCallingAiService_whenNoTokenProviderIsConfigured() {
        // GIVEN
        userDataProcessor = new AiUserDataDeletionTaskProcessor(Optional.empty(), operations, clientRequestFactory);
        tenantDataProcessor = new AiTenantDataDeletionTaskProcessor(Optional.empty(), operations, clientRequestFactory);

        // WHEN
        Throwable userTaskThrown = catchThrowable(() -> userDataProcessor.process(HousekeeperTask.deleteAiUserData(tenantId, userId)));
        Throwable tenantTaskThrown = catchThrowable(() -> tenantDataProcessor.process(HousekeeperTask.deleteAiTenantData(tenantId)));

        // THEN — a missing provider is a misconfiguration, not a no-op: fail so Housekeeper reprocessing kicks in.
        assertThat(userTaskThrown).isInstanceOf(IllegalStateException.class).hasMessage("AI token provider is not configured");
        assertThat(tenantTaskThrown).isInstanceOf(IllegalStateException.class).hasMessage("AI token provider is not configured");
        then(operations).shouldHaveNoInteractions();
    }

}
