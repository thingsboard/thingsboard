// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.aws.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationStatistics;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AwsSqsIntegrationTest {

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/000000000000/test-queue";

    private AwsSqsIntegration integration;
    private AmazonSQS sqs;
    private IntegrationContext context;
    private ExecutorService executor;
    private ScheduledExecutorService scheduler;
    private IntegrationStatistics statistics;

    @BeforeEach
    void setUp() {
        integration = spy(new AwsSqsIntegration());
        sqs = mock(AmazonSQS.class);
        context = mock(IntegrationContext.class);
        executor = mock(ExecutorService.class);
        scheduler = mock(ScheduledExecutorService.class);
        statistics = new IntegrationStatistics(context);
        when(context.getExecutorService()).thenReturn(executor);
        when(context.getScheduledExecutorService()).thenReturn(scheduler);
        when(context.isExceptionStackTraceEnabled()).thenReturn(true);

        // init() builds a real AWS client, so the state it builds is injected directly
        integration.setConfiguration(buildIntegrationConfig());
        // AwsSqsIntegration keeps its own context field that shadows the one AbstractIntegration reads for debug events
        ReflectionTestUtils.setField(integration, "context", context);
        ReflectionTestUtils.setField(integration, AbstractIntegration.class, "context", context, null);
        ReflectionTestUtils.setField(integration, "metadataTemplate", new UplinkMetaData<>(ContentType.JSON, Map.of("integrationName", "test-sqs")));
        ReflectionTestUtils.setField(integration, "integrationStatistics", statistics);
        ReflectionTestUtils.setField(integration, "sqs", sqs);
        ReflectionTestUtils.setField(integration, "sqsConfiguration", buildSqsConfiguration());
    }

    @Test
    void invalidJsonBodyDoesNotStopProcessingOfTheRestOfTheBatch() {
        doNothing().when(integration).process(any());
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(new ReceiveMessageResult().withMessages(
                message("id-1", "rh-1", "hello"),
                message("id-2", "rh-2", "{\"deviceName\":\"Batch A\",\"deviceType\":\"thermostat\",\"temperature\":21}"),
                message("id-3", "rh-3", "{\"deviceName\":\"Batch B\",\"deviceType\":\"thermostat\",\"temperature\":22}")
        ));

        ReflectionTestUtils.invokeMethod(integration, "pollMessages");

        ArgumentCaptor<SqsIntegrationMsg> processed = ArgumentCaptor.captor();
        verify(integration, times(2)).process(processed.capture());
        assertThat(processed.getAllValues())
                .extracting(msg -> msg.getJson().get("deviceName").asText())
                .containsExactly("Batch A", "Batch B");

        verify(sqs).deleteMessage(QUEUE_URL, "rh-1");
        verify(sqs).deleteMessage(QUEUE_URL, "rh-2");
        verify(sqs).deleteMessage(QUEUE_URL, "rh-3");

        verify(executor).execute(any());
        verify(scheduler, never()).schedule(any(Runnable.class), anyLong(), any());

        assertThat(statistics.getErrorsOccurred()).isEqualTo(1);

        List<IntegrationDebugEvent> events = uplinkEvents();
        assertThat(events).hasSize(1);
        IntegrationDebugEvent event = events.get(0);
        assertThat(event.getStatus()).isEqualTo("ERROR");
        assertThat(event.getMessageType()).isEqualTo("JSON");
        assertThat(event.getError()).contains("Unrecognized token 'hello'");
        JsonNode debugMessage = JacksonUtil.toJsonNode(event.getMessage());
        assertThat(debugMessage.get("messageId").asText()).isEqualTo("id-1");
        assertThat(debugMessage.get("payload").asText()).isEqualTo("hello");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validJsonBodies")
    void validJsonBodyIsParsedAsIs(String name, String body, String expectedJson) {
        SqsIntegrationMsg msg = ReflectionTestUtils.invokeMethod(integration, "toSqsIntegrationMsg", message("id-1", "rh-1", body));

        assertThat(msg.getJson()).isEqualTo(JacksonUtil.toJsonNode(expectedJson));
    }

    private static Stream<Arguments> validJsonBodies() {
        String plain = "{\"deviceName\":\"Plain\",\"temperature\":20}";
        String quote = "{\"deviceName\":\"Quote test\",\"note\":\"say \\\"hi\\\"\",\"temperature\":20}";
        String newline = "{\"deviceName\":\"Newline test\",\"note\":\"line1\\nline2\",\"temperature\":20}";
        String backslash = "{\"deviceName\":\"Backslash test\",\"path\":\"C:\\\\data\",\"temperature\":20}";
        String unicode = "{\"deviceName\":\"Unicode test\",\"note\":\"\\u00b0C\",\"temperature\":20}";
        String encodedString = "\"{\\\"deviceName\\\":\\\"Escaped string\\\",\\\"temperature\\\":20}\"";
        return Stream.of(
                Arguments.of("plain object", plain, plain),
                Arguments.of("escaped quote in a string value", quote, quote),
                Arguments.of("escaped line break in a string value", newline, newline),
                Arguments.of("escaped backslash in a string value", backslash, backslash),
                Arguments.of("unicode escape in a string value", unicode, unicode),
                Arguments.of("JSON-encoded string", encodedString, "{\"deviceName\":\"Escaped string\",\"temperature\":20}")
        );
    }

    private List<IntegrationDebugEvent> uplinkEvents() {
        ArgumentCaptor<IntegrationDebugEvent> captor = ArgumentCaptor.captor();
        verify(context).saveEvent(captor.capture(), any());
        return captor.getAllValues().stream()
                .filter(event -> "Uplink".equals(event.getEventType()))
                .toList();
    }

    private static Message message(String messageId, String receiptHandle, String body) {
        return new Message().withMessageId(messageId).withReceiptHandle(receiptHandle).withBody(body);
    }

    private static Integration buildIntegrationConfig() {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(UUID.randomUUID()));
        integration.setTenantId(TenantId.SYS_TENANT_ID);
        integration.setName("test-sqs");
        integration.setType(IntegrationType.AWS_SQS);
        integration.setDebugSettings(DebugSettings.until(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1)));
        return integration;
    }

    private static SqsIntegrationConfiguration buildSqsConfiguration() {
        SqsIntegrationConfiguration configuration = new SqsIntegrationConfiguration();
        configuration.setQueueUrl(QUEUE_URL);
        configuration.setPollingPeriodSeconds(5L);
        return configuration;
    }

}
