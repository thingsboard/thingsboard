// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.ai.common.data.usage.ApiUsageInfo;
import org.thingsboard.ai.common.data.usage.Resource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DefaultTbAiServiceTest {

    @Mock
    TbAiOperations operations;

    @Mock
    TbApiUsageStateService apiUsageStateService;
    @Mock
    TbApiUsageReportClient apiUsageReportClient;
    @Mock
    TbAiTokenProvider tbAiTokenProvider;
    @Mock
    TbAiClient tbAiClient;
    @Mock
    TbAiService.TbAiCall<TbAiResponse> call;

    final int sseInactivityTimeoutSeconds = 600;
    final int sseLogMaxDataLength = 150;

    final UUID chatId = UUID.fromString("6231991d-6930-4927-9362-8841ddb16649");
    final UserId userId = new UserId(UUID.fromString("a8a3d6f0-1f5d-4b1a-9c2e-3b7d9e1c4a55"));
    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("2e175812-f800-4434-a0a6-3a28cf51923f"));
    final CustomerId customerId = new CustomerId(UUID.fromString("4b11723a-a6c1-4a30-bd29-410627b906bf"));

    DefaultTbAiService service;
    SecurityUser user;
    Sinks.Many<ServerSentEvent<String>> upstream;
    Flux<ServerSentEvent<String>> result;

    @BeforeEach
    void setUp() {
        service = newService(true, Optional.of(tbAiTokenProvider));
        user = newUser(tenantId);
    }

    // ---------------------------------------------------------------------------------------------
    // process(call, user, checkCredits)
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldCheckCreditsReportUsageAndReturnValue_whenProcessWithCreditCheckSucceeds() {
        // GIVEN
        given(apiUsageStateService.getApiUsageState(tenantId)).willReturn(enabledApiUsageState());
        JsonNode value = JacksonUtil.newObjectNode().put("answer", 42);
        given(call.apply(any(), any())).willReturn(TbAiResponse.builder().success(true).value(value).creditsUsed(150).build());

        // WHEN
        JsonNode returned = service.process(call, user, true);

        // THEN
        assertThat(returned).isSameAs(value);
        then(call).should().apply(same(tbAiClient), notNull()); // invoked with the real client and a user-scoped provider

        then(apiUsageStateService).should().getApiUsageState(tenantId);
        then(apiUsageReportClient).should().report(tenantId, customerId, ApiUsageRecordKey.AI_CREDITS_COUNT, 150L);
        then(apiUsageReportClient).shouldHaveNoMoreInteractions();
        then(tbAiClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldSkipPreRequestCreditCheckButStillReportConsumedCredits_whenProcessWithoutCreditCheck() {
        // GIVEN: checkCredits=false only skips the pre-request availability check; credits actually
        // consumed by the call must still be reported
        JsonNode value = JacksonUtil.newObjectNode().put("ok", true);
        given(call.apply(any(), any())).willReturn(TbAiResponse.builder().success(true).value(value).creditsUsed(75).build());

        // WHEN
        JsonNode returned = service.process(call, user, false);

        // THEN
        assertThat(returned).isSameAs(value);
        then(call).should().apply(same(tbAiClient), notNull());

        then(apiUsageStateService).shouldHaveNoInteractions(); // pre-request credit check skipped
        then(apiUsageReportClient).should().report(tenantId, customerId, ApiUsageRecordKey.AI_CREDITS_COUNT, 75L);
        then(apiUsageReportClient).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldThrowTooManyRequestsAndSkipCall_whenProcessAndOutOfCredits() {
        // GIVEN
        given(apiUsageStateService.getApiUsageState(tenantId)).willReturn(disabledApiUsageState());

        // WHEN
        Throwable thrown = catchThrowable(() -> service.process(call, user, true));

        // THEN
        assertThat(thrown).isInstanceOf(ThingsboardRuntimeException.class).hasMessage("Out of AI credits");
        assertThat(((ThingsboardRuntimeException) thrown).getErrorCode()).isEqualTo(ThingsboardErrorCode.TOO_MANY_REQUESTS);
        then(call).should(never()).apply(any(), any());
        then(apiUsageReportClient).shouldHaveNoInteractions();
        then(tbAiClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowPermissionDeniedAndSkipEverything_whenProcessAndFeatureDisabled() {
        // GIVEN
        DefaultTbAiService disabledService = newService(false, Optional.of(tbAiTokenProvider));

        // WHEN
        Throwable thrown = catchThrowable(() -> disabledService.process(call, user, true));

        // THEN
        assertThat(thrown).isInstanceOf(ThingsboardRuntimeException.class).hasMessage("AI feature is disabled");
        assertThat(((ThingsboardRuntimeException) thrown).getErrorCode()).isEqualTo(ThingsboardErrorCode.PERMISSION_DENIED);
        then(call).should(never()).apply(any(), any());
        then(apiUsageStateService).shouldHaveNoInteractions();
        then(apiUsageReportClient).shouldHaveNoInteractions();
        then(tbAiClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldReportCreditsThenThrowGeneral_whenProcessAndResponseIsFailure() {
        // GIVEN
        given(apiUsageStateService.getApiUsageState(tenantId)).willReturn(enabledApiUsageState());
        given(call.apply(any(), any())).willReturn(TbAiResponse.builder().success(false).error("Upstream rejected").creditsUsed(42).build());

        // WHEN
        Throwable thrown = catchThrowable(() -> service.process(call, user, true));

        // THEN
        assertThat(thrown).isInstanceOf(ThingsboardRuntimeException.class).hasMessage("Upstream rejected");
        assertThat(((ThingsboardRuntimeException) thrown).getErrorCode()).isEqualTo(ThingsboardErrorCode.GENERAL);
        // credits are reported before the success check, so they are billed even on failure
        then(apiUsageReportClient).should().report(tenantId, customerId, ApiUsageRecordKey.AI_CREDITS_COUNT, 42L);
        then(apiUsageReportClient).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldThrowBadRequestAndSkipCall_whenProcessAndTokenProviderUnavailable() {
        // GIVEN
        DefaultTbAiService noProviderService = newService(true, Optional.empty());

        // WHEN (no credit check, so the failure is solely the missing token provider)
        Throwable thrown = catchThrowable(() -> noProviderService.process(call, user, false));

        // THEN
        assertThat(thrown).isInstanceOf(ThingsboardRuntimeException.class).hasMessage("AI feature is not available");
        assertThat(((ThingsboardRuntimeException) thrown).getErrorCode()).isEqualTo(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        then(call).should(never()).apply(any(), any());
        then(apiUsageStateService).shouldHaveNoInteractions();
        then(tbAiClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldDelegateTokenAndAdditionalInfoToUserScopedProvider_whenProcessCalled() {
        // GIVEN
        given(tbAiTokenProvider.getToken(user)).willReturn("jwt-token");
        given(tbAiTokenProvider.getAdditionalInfo(user)).willReturn(Map.of("X-Forwarded-For", "198.51.100.10"));
        given(call.apply(any(), any())).willReturn(TbAiResponse.builder().success(true).value(JacksonUtil.newObjectNode()).build());

        // WHEN
        service.process(call, user, false);

        // THEN the provider handed to the call is scoped to the calling user
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProviderCaptor = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        then(call).should().apply(same(tbAiClient), tokenProviderCaptor.capture());

        TbAiClient.TokenProvider tokenProvider = tokenProviderCaptor.getValue();
        assertThat(tokenProvider.getToken()).isEqualTo("jwt-token");
        assertThat(tokenProvider.getAdditionalInfo()).containsExactlyInAnyOrderEntriesOf(Map.of("X-Forwarded-For", "198.51.100.10"));

        then(tbAiTokenProvider).should().getToken(user);
        then(tbAiTokenProvider).should().getAdditionalInfo(user);
        then(tbAiTokenProvider).shouldHaveNoMoreInteractions();
    }

    // ---------------------------------------------------------------------------------------------
    // isEnabled()
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldReflectSettings_whenIsEnabledCalled() {
        assertThat(service.isEnabled()).isTrue();
        assertThat(newService(false, Optional.of(tbAiTokenProvider)).isEnabled()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------
    // getApiUsageInfo(user)
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldReturnEmptyUsageWithoutCallingClient_whenFeatureDisabled() {
        // GIVEN
        DefaultTbAiService disabledService = newService(false, Optional.of(tbAiTokenProvider));

        // WHEN
        ApiUsageInfo usageInfo = disabledService.getApiUsageInfo(user);

        // THEN
        assertThat(usageInfo.usage()).isEmpty();
        assertThat(usageInfo.periodStartTs()).isZero();
        then(tbAiClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldParseKnownUsageAndTolerateUnknownFieldsAndEnums_whenClientRespondsWithNewerSchema() {
        // GIVEN a response from a newer AI service: an unknown resource type, an unknown nested
        // field and an unknown top-level field that this version does not know about
        ObjectNode usage = JacksonUtil.newObjectNode();
        usage.set("AI_CREDITS", JacksonUtil.newObjectNode()
                .put("used", 250)
                .put("limit", 1000)
                .put("unexpectedNestedField", "ignored"));
        usage.set("FUTURE_RESOURCE", JacksonUtil.newObjectNode()
                .put("used", 5)
                .put("limit", 10));
        ObjectNode response = JacksonUtil.newObjectNode();
        response.set("usage", usage);
        response.put("periodStartTs", 1716460800000L);
        response.put("unexpectedTopLevelField", "ignored");
        given(tbAiClient.getApiUsageInfo(any()))
                .willReturn(TbAiResponse.builder().success(true).value(response).build());

        // WHEN
        ApiUsageInfo usageInfo = service.getApiUsageInfo(user);

        // THEN the known parts are parsed and the unrecognized field/enum do not break parsing
        assertThat(usageInfo.periodStartTs()).isEqualTo(1716460800000L);
        assertThat(usageInfo.usage()).containsEntry(Resource.AI_CREDITS, new ApiUsageInfo.ResourceUsage(250L, 1000));
        then(apiUsageStateService).shouldHaveNoInteractions(); // usage is fetched without a credit check
        then(tbAiClient).should().getApiUsageInfo(any());
        then(operations).should().execute(argThat(operation -> ChannelProtocol.USAGE_GET.equals(operation.type())), same(user), any());
    }

    @Test
    void shouldReturnEmptyUsage_whenClientResponseIsFailure() {
        // GIVEN
        given(tbAiClient.getApiUsageInfo(any()))
                .willReturn(TbAiResponse.builder().success(false).error("AI service down").build());

        // WHEN
        ApiUsageInfo usageInfo = service.getApiUsageInfo(user);

        // THEN
        assertThat(usageInfo.usage()).isEmpty();
        assertThat(usageInfo.periodStartTs()).isZero();
        then(tbAiClient).should().getApiUsageInfo(any());
    }

    // ---------------------------------------------------------------------------------------------
    // processStream(chatId, call, user)
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldThrowPermissionDenied_whenProcessStreamAndFeatureDisabled() {
        // GIVEN
        DefaultTbAiService disabledService = newService(false, Optional.of(tbAiTokenProvider));

        // WHEN
        Throwable thrown = catchThrowable(() ->
                disabledService.processStream(chatId, (client, tokenProvider) -> Flux.empty(), user));

        // THEN
        assertThat(thrown).isInstanceOf(ThingsboardRuntimeException.class).hasMessage("AI feature is disabled");
        assertThat(((ThingsboardRuntimeException) thrown).getErrorCode()).isEqualTo(ThingsboardErrorCode.PERMISSION_DENIED);
        then(apiUsageStateService).shouldHaveNoInteractions();
        then(apiUsageReportClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowTooManyRequests_whenProcessStreamAndOutOfCredits() {
        // GIVEN
        given(apiUsageStateService.getApiUsageState(tenantId)).willReturn(disabledApiUsageState());

        // WHEN
        Throwable thrown = catchThrowable(() ->
                service.processStream(chatId, (client, tokenProvider) -> Flux.empty(), user));

        // THEN
        assertThat(thrown).isInstanceOf(ThingsboardRuntimeException.class).hasMessage("Out of AI credits");
        assertThat(((ThingsboardRuntimeException) thrown).getErrorCode()).isEqualTo(ThingsboardErrorCode.TOO_MANY_REQUESTS);
        then(apiUsageReportClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldReportCredits_whenClientCancelsMidStream() {
        // GIVEN
        result = startStream();
        Disposable client = result.subscribe();
        upstream.tryEmitNext(assistantMessage("hello"));

        // WHEN
        client.dispose();
        upstream.tryEmitNext(creditsUsedEvent(150));
        upstream.tryEmitComplete();

        // THEN
        then(apiUsageReportClient).should().report(tenantId, customerId, ApiUsageRecordKey.AI_CREDITS_COUNT, 150L);
    }

    @Test
    void shouldReportCreditsAndDeliverEvents_whenStreamCompletesNormally() {
        // GIVEN
        result = startStream();
        List<ServerSentEvent<String>> received = new ArrayList<>();
        result.subscribe(received::add);

        // WHEN
        upstream.tryEmitNext(assistantMessage("hello"));
        upstream.tryEmitNext(creditsUsedEvent(150));
        upstream.tryEmitComplete();

        // THEN
        then(apiUsageReportClient).should().report(tenantId, customerId, ApiUsageRecordKey.AI_CREDITS_COUNT, 150L);
        assertThat(received).hasSize(1);
        assertThat(received.get(0).event()).isEqualTo("assistantMessage");
    }

    @Test
    void shouldReportCreditsAndSwallowEvent_whenCreditsEventCarriesExtraFields() {
        // GIVEN
        result = startStream();
        List<ServerSentEvent<String>> received = new ArrayList<>();
        result.subscribe(received::add);

        // WHEN a newer AI service emits a creditsUsed event enriched with extra fields
        ObjectNode data = JacksonUtil.newObjectNode()
                .put("creditsUsed", 150)
                .put("model", "future-model")
                .put("requestId", "req-42");
        data.set("breakdown", JacksonUtil.newObjectNode().put("input", 90).put("output", 60));
        upstream.tryEmitNext(ServerSentEvent.<String>builder().event("creditsUsed").data(JacksonUtil.toString(data)).build());
        upstream.tryEmitComplete();

        // THEN the credits are still billed and the bookkeeping event is not forwarded to the client
        then(apiUsageReportClient).should().report(tenantId, customerId, ApiUsageRecordKey.AI_CREDITS_COUNT, 150L);
        assertThat(received).isEmpty();
    }

    @Test
    void shouldForwardHeartbeatAsIs_whenReceivedFromUpstream() {
        // GIVEN
        result = startStream();
        List<ServerSentEvent<String>> received = new ArrayList<>();
        result.subscribe(received::add);

        // WHEN
        upstream.tryEmitNext(heartbeatEvent());
        upstream.tryEmitComplete();

        // THEN
        assertThat(received).hasSize(1);
        assertThat(received.get(0).comment()).isEqualTo("heartbeat");
        assertThat(received.get(0).event()).isNull();
        assertThat(received.get(0).data()).isNull();
        then(apiUsageReportClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldEmitInternalErrorAndNotReportCredits_whenCreditsEventIsMalformed() {
        // GIVEN
        result = startStream();
        List<ServerSentEvent<String>> received = new ArrayList<>();
        result.subscribe(received::add);

        // WHEN a creditsUsed event carries a non-numeric amount
        upstream.tryEmitNext(ServerSentEvent.<String>builder()
                .event("creditsUsed")
                .data(JacksonUtil.toString(JacksonUtil.newObjectNode().put("creditsUsed", "oops")))
                .build());

        // THEN the client gets a friendly error and nothing is billed
        assertThat(received).hasSize(1);
        assertThat(received.get(0).event()).isEqualTo("error");
        assertThat(errorMessageOf(received.get(0))).isEqualTo("Internal error");
        then(apiUsageReportClient).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @MethodSource("upstreamErrors")
    void shouldEmitMappedErrorEvent_whenUpstreamFails(Throwable error, String expectedMessage) {
        // GIVEN
        result = startStream();
        List<ServerSentEvent<String>> received = new ArrayList<>();
        result.subscribe(received::add);

        // WHEN
        upstream.tryEmitError(error);

        // THEN
        assertThat(received).hasSize(1);
        assertThat(received.get(0).event()).isEqualTo("error");
        assertThat(errorMessageOf(received.get(0))).isEqualTo(expectedMessage);
        then(apiUsageReportClient).shouldHaveNoInteractions();
    }

    static List<Arguments> upstreamErrors() {
        return List.of(
                // PE-side inactivity timeout from the .timeout(...) operator
                arguments(new TimeoutException("inactivity"), "Request timed out"),
                // AI service unreachable (e.g. connection refused / DNS failure)
                arguments(connectError(), "Service unavailable"),
                // auth failures: the response body is ignored and the status reason phrase is used
                arguments(aiError(401, "Unauthorized", "Authentication required"), "Unauthorized"),
                arguments(aiError(403, "Forbidden", "Access denied"), "Forbidden"),
                // the AI service always wraps errors in an {"message": ...} body, which is surfaced verbatim
                arguments(aiError(400, "Bad Request", "message: must not be blank"), "message: must not be blank"),
                arguments(aiError(404, "Not Found", "Chat not found"), "Chat not found"),
                arguments(aiError(429, "Too Many Requests", "Rate limit exceeded. Please try again later."), "Rate limit exceeded. Please try again later."),
                arguments(aiError(500, "Internal Server Error", "Unexpected error"), "Unexpected error"),
                // infrastructure in front of the AI service (proxy/gateway/CDN) returns non-JSON bodies,
                // so the status-class fallback message is used
                arguments(gatewayError(502, "Bad Gateway"), "Service unavailable"),
                arguments(gatewayError(413, "Payload Too Large"), "Invalid request"),
                // non-standard / unresolvable status code -> generic message
                arguments(gatewayError(520, "Web Server Returned an Unknown Error"), "Internal error"),
                // any other unexpected throwable
                arguments(new RuntimeException("boom"), "Internal error")
        );
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private DefaultTbAiService newService(boolean enabled, Optional<TbAiTokenProvider> tokenProvider) {
        lenient().when(operations.execute(any(TbAiOperation.class), any(SecurityUser.class), any(TbAiClient.TokenProvider.class)))
                .thenAnswer(invocation -> invocation.<TbAiOperation>getArgument(0).httpCall().get());
        return new DefaultTbAiService(apiUsageStateService, apiUsageReportClient, tokenProvider,
                tbAiClient, new TbAiSettings(enabled, sseInactivityTimeoutSeconds, sseLogMaxDataLength), operations);
    }

    private SecurityUser newUser(TenantId tenant) {
        SecurityUser u = new SecurityUser();
        u.setId(userId);
        u.setTenantId(tenant);
        u.setCustomerId(customerId);
        return u;
    }

    private Flux<ServerSentEvent<String>> startStream() {
        given(apiUsageStateService.getApiUsageState(tenantId)).willReturn(enabledApiUsageState());
        upstream = Sinks.many().unicast().onBackpressureBuffer();
        return service.processStream(chatId, (client, tokenProvider) -> upstream.asFlux(), user);
    }

    private static ApiUsageState enabledApiUsageState() {
        ApiUsageState state = new ApiUsageState();
        state.setAiState(ApiUsageStateValue.ENABLED);
        return state;
    }

    private static ApiUsageState disabledApiUsageState() {
        ApiUsageState state = new ApiUsageState();
        state.setAiState(ApiUsageStateValue.DISABLED);
        return state;
    }

    private static WebClientRequestException connectError() {
        return new WebClientRequestException(new IOException("Connection refused"), HttpMethod.POST,
                URI.create("https://ai.example.test/api/chats/x/messages"), HttpHeaders.EMPTY);
    }

    // mirrors the AI service ErrorHandler, which always responds with an {"message": ...} body
    private static WebClientResponseException aiError(int status, String reasonPhrase, String message) {
        return wcre(status, reasonPhrase, JacksonUtil.toString(JacksonUtil.newObjectNode().put("message", message)));
    }

    // mirrors an infrastructure error in front of the AI service: a non-JSON (e.g. HTML) body
    private static WebClientResponseException gatewayError(int status, String reasonPhrase) {
        return wcre(status, reasonPhrase, "<html><body><h1>" + status + " " + reasonPhrase + "</h1></body></html>");
    }

    private static WebClientResponseException wcre(int status, String reasonPhrase, String body) {
        return new WebClientResponseException(status, reasonPhrase, HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private static String errorMessageOf(ServerSentEvent<String> event) {
        return JacksonUtil.toJsonNode(event.data()).get("message").asText();
    }

    static ServerSentEvent<String> assistantMessage(String message) {
        return ServerSentEvent.<String>builder()
                .event("assistantMessage")
                .data(JacksonUtil.toString(JacksonUtil.newObjectNode().put("message", message)))
                .build();
    }

    static ServerSentEvent<String> creditsUsedEvent(int credits) {
        return ServerSentEvent.<String>builder()
                .event("creditsUsed")
                .data(JacksonUtil.toString(JacksonUtil.newObjectNode().put("creditsUsed", credits)))
                .build();
    }

    static ServerSentEvent<String> heartbeatEvent() {
        return ServerSentEvent.<String>builder().comment("heartbeat").build();
    }

}
