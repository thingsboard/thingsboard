/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class TbKafkaNodeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("5f2eac08-bd1f-4635-a6c2-437369f996cf"));
    private final ListeningExecutor executor = new TestDbCallbackExecutor();

    private TbKafkaNode node;
    private TbKafkaNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private Producer<String, String> producerMock;
    @Mock
    private RecordMetadata recordMetadataMock;

    @BeforeEach
    void setUp() {
        node = new TbKafkaNode();
        config = new TbKafkaNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getTopicPattern()).isEqualTo("my-topic");
        assertThat(config.getKeyPattern()).isNull();
        assertThat(config.getBootstrapServers()).isEqualTo("localhost:9092");
        assertThat(config.getRetries()).isEqualTo(0);
        assertThat(config.getBatchSize()).isEqualTo(16384);
        assertThat(config.getLinger()).isEqualTo(0);
        assertThat(config.getBufferMemory()).isEqualTo(33554432);
        assertThat(config.getAcks()).isEqualTo("-1");
        assertThat(config.getKeySerializer()).isEqualTo(StringSerializer.class.getName());
        assertThat(config.getValueSerializer()).isEqualTo(StringSerializer.class.getName());
        assertThat(config.getOtherProperties()).isEmpty();
        assertThat(config.isAddMetadataKeyValuesAsKafkaHeaders()).isFalse();
        assertThat(config.getKafkaHeadersCharset()).isEqualTo("UTF-8");
    }

    @Test
    public void givenAddMetadataKeyValuesAsKafkaHeadersIsTrueAndKafkaHeadersCharsetIsSet_whenInit_thenOk() {
        config.setAddMetadataKeyValuesAsKafkaHeaders(true);
        config.setKafkaHeadersCharset("UTF-16");

        String ruleNodeIdStr = "0d35733c-7661-4797-819e-d9188974e3b2";
        String serviceIdStr = "test-service";

        given(ctxMock.getSelfId()).willReturn(new RuleNodeId(UUID.fromString(ruleNodeIdStr)));
        given(ctxMock.getServiceId()).willReturn(serviceIdStr);

        assertThatNoException().isThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))));

        Boolean addMetadataKeyValuesAsKafkaHeaders = (Boolean) ReflectionTestUtils.getField(node, "addMetadataKeyValuesAsKafkaHeaders");
        Charset toBytesCharset = (Charset) ReflectionTestUtils.getField(node, "toBytesCharset");

        assertThat(addMetadataKeyValuesAsKafkaHeaders).isTrue();
        assertThat(toBytesCharset).isEqualTo(StandardCharsets.UTF_16);
    }

    @Test
    public void verifyGetKafkaPropertiesMethod() {
        String sslKeyStoreCertificateChain = "cbdvch\\nfwrg\nvgwg\\n";
        String sslKeyStoreKey = "nghmh\\nhmmnh\\\\ngreg\nvgwg\\n";
        String sslTruststoreCertificates = "grthrt\fd\\nfwrg\nvgwg\\n";
        config.setOtherProperties(Map.of(
                "ssl.keystore.certificate.chain", sslKeyStoreCertificateChain,
                "ssl.keystore.key", sslKeyStoreKey,
                "ssl.truststore.certificates", sslTruststoreCertificates,
                "ssl.protocol", "TLSv1.2"
        ));
        ReflectionTestUtils.setField(node, "config", config);

        String ruleNodeIdStr = "e646b885-8004-45b4-8bfb-78db21870e0f";
        String serviceIdStr = "test-service";
        given(ctxMock.getSelfId()).willReturn(new RuleNodeId(UUID.fromString(ruleNodeIdStr)));
        given(ctxMock.getServiceId()).willReturn(serviceIdStr);

        Properties expectedProperties = new Properties();
        expectedProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "producer-tb-kafka-node-" + ruleNodeIdStr + "-" + serviceIdStr);
        expectedProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        expectedProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, config.getValueSerializer());
        expectedProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.getKeySerializer());
        expectedProperties.put(ProducerConfig.ACKS_CONFIG, config.getAcks());
        expectedProperties.put(ProducerConfig.RETRIES_CONFIG, config.getRetries());
        expectedProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, config.getBatchSize());
        expectedProperties.put(ProducerConfig.LINGER_MS_CONFIG, config.getLinger());
        expectedProperties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, config.getBufferMemory());
        expectedProperties.put("ssl.keystore.certificate.chain", sslKeyStoreCertificateChain.replace("\\n", "\n"));
        expectedProperties.put("ssl.keystore.key", sslKeyStoreKey.replace("\\n", "\n"));
        expectedProperties.put("ssl.truststore.certificates", sslTruststoreCertificates.replace("\\n", "\n"));
        expectedProperties.put("ssl.protocol", "TLSv1.2");

        Properties actualsProperties = node.getKafkaProperties(ctxMock);
        assertThat(actualsProperties).isEqualTo(expectedProperties);
    }

    @Test
    public void givenInitErrorIsNotNull_whenOnMsg_thenTellFailure() {
        init();
        String errorMsg = "Error during init!";
        ReflectionTestUtils.setField(node, "initError", new RuntimeException(errorMsg));

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), actualError.capture());
        assertThat(actualError.getValue())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to initialize Kafka rule node producer: " + errorMsg);
    }

    @Test
    public void givenForceAckIsTrueAndExceptionWasThrown_whenOnMsg_thenTellFailure() {
        init();
        ReflectionTestUtils.setField(node, "forceAck", true);

        ListeningExecutor executorMock = mock(ListeningExecutor.class);
        given(ctxMock.getExternalCallExecutor()).willReturn(executorMock);
        String errorMsg = "Something went wrong!";
        willThrow(new RuntimeException(errorMsg)).given(executorMock).executeAsync(any(Callable.class));

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().ack(msg);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(actualMsg.capture(), actualError.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(msg);
        assertThat(actualError.getValue()).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
    }

    @ParameterizedTest
    @MethodSource
    public void givenTopicAndKeyPatternsAndAddMetadataKeyValuesAsKafkaHeadersIsFalse_whenOnMsg_thenTellSuccess
            (String topicPattern, String keyPattern, TbMsgMetaData metaData, String data) {
        config.setTopicPattern(topicPattern);
        config.setKeyPattern(keyPattern);
        init();

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, data);
        String topic = TbNodeUtils.processPattern(topicPattern, msg);
        String key = TbNodeUtils.processPattern(keyPattern, msg);
        long offset = 1;
        int partition = 0;
        mockSuccessfulPublishingRequest(topic, offset, partition);

        node.onMsg(ctxMock, msg);

        verifyProducerRecord(topic, key, msg.getData());
        verifyOutboundMsg(offset, partition, topic, msg);
    }

    private static Stream<Arguments> givenTopicAndKeyPatternsAndAddMetadataKeyValuesAsKafkaHeadersIsFalse_whenOnMsg_thenTellSuccess() {
        return Stream.of(
                Arguments.of("test-topic", "test-key", new TbMsgMetaData(), TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("${mdTopicPattern}", "${mdKeyPattern}", new TbMsgMetaData(
                        Map.of(
                                "mdTopicPattern", "md-test-topic",
                                "mdKeyPattern", "md-test-key"
                        )), TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("$[msgTopicPattern]", "$[msgKeyPattern]", new TbMsgMetaData(),
                        "{\"msgTopicPattern\":\"msg-test-topic\",\"msgKeyPattern\":\"msg-test-key\"}")
        );
    }

    @Test
    public void givenForceAckIsFalseAndAddMetadataKeyValuesAsKafkaHeadersIsTrueAndToBytesCharsetIsSet_whenOnMsg_thenAckAndTellSuccess() {
        String topic = "test-topic";
        String key = "test-key";
        config.setTopicPattern(topic);
        config.setKeyPattern(key);
        config.setAddMetadataKeyValuesAsKafkaHeaders(true);
        config.setKafkaHeadersCharset("UTF-16");
        init();
        ReflectionTestUtils.setField(node, "forceAck", false);
        ReflectionTestUtils.setField(node, "addMetadataKeyValuesAsKafkaHeaders", true);
        ReflectionTestUtils.setField(node, "toBytesCharset", StandardCharsets.UTF_16);

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, TbMsg.EMPTY_JSON_OBJECT);

        long offset = 1;
        int partition = 0;

        mockSuccessfulPublishingRequest(topic, offset, partition);

        node.onMsg(ctxMock, msg);

        then(ctxMock).should(never()).ack(msg);
        Headers expectedHeaders = new RecordHeaders();
        msg.getMetaData().values().forEach((k, v) -> expectedHeaders.add(new RecordHeader("tb_msg_md_" + k, v.getBytes(StandardCharsets.UTF_16))));
        verifyProducerRecord(topic, key, msg.getData(), expectedHeaders);
        verifyOutboundMsg(offset, partition, topic, msg);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void givenKeyIsNullOrEmptyAndErrorOccursDuringPublishing_whenOnMsg_thenTellFailure(String key) {
        String topic = "test-topic";
        config.setTopicPattern(topic);
        config.setKeyPattern(key);
        config.setAddMetadataKeyValuesAsKafkaHeaders(false);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        String errorMsg = "Something went wrong!";

        given(ctxMock.getExternalCallExecutor()).willReturn(executor);
        willAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(recordMetadataMock, new RuntimeException(errorMsg));
            return null;
        }).given(producerMock).send(any(), any(Callback.class));

        init();
        node.onMsg(ctxMock, msg);

        verifyProducerRecord(topic, null, msg.getData());

        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(actualMsg.capture(), actualError.capture());
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("error", RuntimeException.class + ": " + errorMsg);
        TbMsg expectedMsg = TbMsg.transformMsgMetadata(msg, metaData);
        assertThat(actualMsg.getValue())
                .usingRecursiveComparison()
                .ignoringFields("ctx")
                .isEqualTo(expectedMsg);
    }

    @Test
    public void givenProducerIsNotNull_whenDestroy_thenShouldClose() {
        ReflectionTestUtils.setField(node, "producer", producerMock);

        node.destroy();

        then(producerMock).should().close();
    }

    @Test
    public void givenProducerIsNull_whenDestroy_thenDoNothing() {
        node.destroy();
        then(producerMock).shouldHaveNoInteractions();
    }

    private void mockSuccessfulPublishingRequest(String topic, long offset, int partition) {
        given(ctxMock.getExternalCallExecutor()).willReturn(executor);
        willAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(recordMetadataMock, null);
            return null;
        }).given(producerMock).send(any(), any(Callback.class));
        given(recordMetadataMock.offset()).willReturn(offset);
        given(recordMetadataMock.partition()).willReturn(partition);
        given(recordMetadataMock.topic()).willReturn(topic);
    }

    private void init() {
        ReflectionTestUtils.setField(node, "config", config);
        ReflectionTestUtils.setField(node, "producer", producerMock);
        ReflectionTestUtils.setField(node, "addMetadataKeyValuesAsKafkaHeaders", false);
    }

    private void verifyProducerRecord(String expectedTopic, String expectedKey, String expectedValue) {
        verifyProducerRecord(expectedTopic, expectedKey, expectedValue, null);
    }

    private void verifyProducerRecord(String expectedTopic, String expectedKey, String expectedValue, Headers expectedHeaders) {
        ArgumentCaptor<ProducerRecord<String, String>> actualRecordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        then(producerMock).should().send(actualRecordCaptor.capture(), any());
        ProducerRecord<String, String> actualRecord = actualRecordCaptor.getValue();
        assertThat(actualRecord.topic()).isEqualTo(expectedTopic);
        assertThat(actualRecord.key()).isEqualTo(expectedKey);
        assertThat(actualRecord.value()).isEqualTo(expectedValue);
        if (expectedHeaders != null) {
            assertThat(actualRecord.headers()).isEqualTo(expectedHeaders);
        }
    }

    private void verifyOutboundMsg(long expectedOffset, long expectedPartition, String expectedTopic, TbMsg originalMsg) {
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        metaData.putValue("offset", String.valueOf(expectedOffset));
        metaData.putValue("partition", String.valueOf(expectedPartition));
        metaData.putValue("topic", expectedTopic);
        TbMsg expectedMsg = TbMsg.transformMsgMetadata(originalMsg, metaData);
        assertThat(actualMsg.getValue())
                .usingRecursiveComparison()
                .ignoringFields("ctx")
                .isEqualTo(expectedMsg);
    }
}
