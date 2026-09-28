// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationStatistics;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Locks in the downlink contract: the payload published to RabbitMQ is the downlink converter output,
 * and both the converter and the integration report the processed message via debug events.
 */
class RabbitMQIntegrationTest {

    private static final String EXCHANGE_NAME = "test-exchange";
    private static final String DOWNLINK_TOPIC = "test-downlink";

    private RabbitMQIntegration integration;
    private Channel channel;
    private IntegrationContext context;
    private TBDownlinkDataConverter downlinkConverter;
    private IntegrationStatistics statistics;

    @BeforeEach
    void setUp() {
        integration = new RabbitMQIntegration();
        channel = mock(Channel.class);
        context = mock(IntegrationContext.class);
        downlinkConverter = mock(TBDownlinkDataConverter.class);
        statistics = new IntegrationStatistics(context);

        // init() opens a real AMQP connection, so the state it builds is injected directly
        integration.setConfiguration(buildIntegrationConfig());
        ReflectionTestUtils.setField(integration, "context", context);
        ReflectionTestUtils.setField(integration, "downlinkConverter", downlinkConverter);
        ReflectionTestUtils.setField(integration, "metadataTemplate", new UplinkMetaData<>(ContentType.JSON, Map.of("integrationName", "test-rabbitmq")));
        ReflectionTestUtils.setField(integration, "integrationStatistics", statistics);
        ReflectionTestUtils.setField(integration, "channel", channel);
        ReflectionTestUtils.setField(integration, "rabbitMQConsumerConfiguration", buildClientConfiguration());
    }

    @Test
    void publishesConverterOutputAndReportsProcessedMessage() throws Exception {
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenReturn(List.of(downlinkData("{\"cmd\":\"reboot\"}")));

        integration.onDownlinkMsg(downlinkMsg());

        ArgumentCaptor<List<TbMsg>> convertedMsgs = ArgumentCaptor.captor();
        ArgumentCaptor<IntegrationMetaData> convertedMetaData = ArgumentCaptor.captor();
        verify(downlinkConverter).convertDownLink(any(), convertedMsgs.capture(), convertedMetaData.capture());
        assertThat(convertedMsgs.getValue()).singleElement().extracting(TbMsg::getData).isEqualTo("{\"temperature\":42}");
        assertThat(convertedMetaData.getValue().getKvMap()).containsEntry("integrationName", "test-rabbitmq");
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(DOWNLINK_TOPIC), any(AMQP.BasicProperties.class),
                eq("{\"cmd\":\"reboot\"}".getBytes(StandardCharsets.UTF_8)));
        verify(context).onDownlinkMessageProcessed(true);
        assertThat(statistics.getMessagesProcessed()).isEqualTo(1);
        assertThat(statistics.getErrorsOccurred()).isZero();

        IntegrationDebugEvent event = lastDownlinkEvent();
        assertThat(event.getStatus()).isEqualTo("OK");
        assertThat(JacksonUtil.toJsonNode(event.getMessage()).get("payload").get("cmd").asText()).isEqualTo("reboot");
    }

    @Test
    void publishesEveryNonEmptyItemOfConverterResult() throws Exception {
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenReturn(List.of(
                downlinkData("{\"cmd\":\"first\"}"),
                downlinkData(""),
                downlinkData("{\"cmd\":\"second\"}")));

        integration.onDownlinkMsg(downlinkMsg());

        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(DOWNLINK_TOPIC), any(AMQP.BasicProperties.class),
                eq("{\"cmd\":\"first\"}".getBytes(StandardCharsets.UTF_8)));
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(DOWNLINK_TOPIC), any(AMQP.BasicProperties.class),
                eq("{\"cmd\":\"second\"}".getBytes(StandardCharsets.UTF_8)));
        verify(channel, times(2)).basicPublish(any(), any(), any(), any());
        verify(context, times(2)).onDownlinkMessageProcessed(true);
        assertThat(statistics.getMessagesProcessed()).isEqualTo(2);
        assertThat(statistics.getErrorsOccurred()).isZero();
        assertThat(downlinkEvents()).hasSize(2).allMatch(event -> "OK".equals(event.getStatus()));
    }

    @Test
    void keepsPublishingRemainingItemsWhenOneFails() throws Exception {
        when(context.isExceptionStackTraceEnabled()).thenReturn(true);
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenReturn(List.of(
                downlinkData("{\"cmd\":\"first\"}"),
                downlinkData("{\"cmd\":\"second\"}")));
        doThrow(new IOException("Failed to publish downlink"))
                .when(channel).basicPublish(any(), any(), any(), eq("{\"cmd\":\"first\"}".getBytes(StandardCharsets.UTF_8)));

        integration.onDownlinkMsg(downlinkMsg());

        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(DOWNLINK_TOPIC), any(AMQP.BasicProperties.class),
                eq("{\"cmd\":\"second\"}".getBytes(StandardCharsets.UTF_8)));
        verify(context).onDownlinkMessageProcessed(false);
        verify(context).onDownlinkMessageProcessed(true);
        assertThat(statistics.getMessagesProcessed()).isEqualTo(1);
        assertThat(statistics.getErrorsOccurred()).isEqualTo(1);

        List<IntegrationDebugEvent> events = downlinkEvents();
        assertThat(events).hasSize(2);
        assertThat(events.getFirst().getStatus()).isEqualTo("ERROR");
        assertThat(events.getFirst().getError()).contains("Failed to publish downlink");
        assertThat(events.getLast().getStatus()).isEqualTo("OK");
    }

    @Test
    void skipsPublishWhenConverterReturnsEmptyData() throws Exception {
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenReturn(List.of(downlinkData("")));

        integration.onDownlinkMsg(downlinkMsg());

        verify(channel, never()).basicPublish(any(), any(), any(), any());
        verify(context, never()).onDownlinkMessageProcessed(true);
        assertThat(statistics.getMessagesProcessed()).isZero();
        assertThat(statistics.getErrorsOccurred()).isZero();
    }

    @Test
    void reportsErrorWhenConverterFails() throws Exception {
        when(context.isExceptionStackTraceEnabled()).thenReturn(true);
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenThrow(new RuntimeException("Failed to convert downlink"));

        integration.onDownlinkMsg(downlinkMsg());

        verify(channel, never()).basicPublish(any(), any(), any(), any());
        verify(context).onDownlinkMessageProcessed(false);
        assertThat(statistics.getMessagesProcessed()).isZero();
        assertThat(statistics.getErrorsOccurred()).isEqualTo(1);

        IntegrationDebugEvent event = lastDownlinkEvent();
        assertThat(event.getStatus()).isEqualTo("ERROR");
        assertThat(event.getError()).contains("Failed to convert downlink");
    }

    private IntegrationDebugEvent lastDownlinkEvent() {
        List<IntegrationDebugEvent> events = downlinkEvents();
        assertThat(events).as("saved [Downlink] debug events").isNotEmpty();
        return events.getLast();
    }

    // the pre-converter event is saved as "Downlink: <msg type>", so only the exact type is collected here
    private List<IntegrationDebugEvent> downlinkEvents() {
        ArgumentCaptor<IntegrationDebugEvent> captor = ArgumentCaptor.captor();
        verify(context, atLeastOnce()).saveEvent(captor.capture(), any());
        return captor.getAllValues().stream()
                .filter(event -> "Downlink".equals(event.getEventType()))
                .toList();
    }

    private static DownlinkData downlinkData(String payload) {
        return DownlinkData.builder()
                .contentType("JSON")
                .data(payload.getBytes(StandardCharsets.UTF_8))
                .metadata(Map.of("deviceName", "Device A"))
                .build();
    }

    private static IntegrationDownlinkMsg downlinkMsg() {
        IntegrationDownlinkMsg downlink = mock(IntegrationDownlinkMsg.class);
        when(downlink.getTbMsg()).thenReturn(tbMsg());
        return downlink;
    }

    private static TbMsg tbMsg() {
        return TbMsg.newMsg()
                .id(UUID.fromString("1a4a7b4a-8bd1-4ee6-9e1e-3f8f8f6a0e11"))
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new IntegrationId(UUID.fromString("2b5b8c5b-9ce2-4ff7-8f2f-4a9a9a7b1f22")))
                .copyMetaData(new TbMsgMetaData(Map.of("deviceName", "Device A")))
                .data("{\"temperature\":42}")
                .build();
    }

    private static Integration buildIntegrationConfig() {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(UUID.randomUUID()));
        integration.setTenantId(TenantId.SYS_TENANT_ID);
        integration.setName("test-rabbitmq");
        integration.setType(IntegrationType.RABBITMQ);
        integration.setDebugSettings(DebugSettings.until(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1)));
        return integration;
    }

    private static RabbitMQConsumerConfiguration buildClientConfiguration() {
        RabbitMQConsumerConfiguration configuration = new RabbitMQConsumerConfiguration();
        configuration.setExchangeName(EXCHANGE_NAME);
        configuration.setDownlinkTopic(DOWNLINK_TOPIC);
        return configuration;
    }

}
