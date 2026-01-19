/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.KafkaThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.exception.ThingsboardKafkaClientError;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
public class TbKafkaNodeTest extends AbstractRuleNodeUpgradeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("5f2eac08-bd1f-4635-a6c2-437369f996cf"));
    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("d46bb666-ecab-4d89-a28f-5abdca23ac29"));
    private final ListeningExecutor executor = new TestDbCallbackExecutor();

    private final long OFFSET = 1;
    private final int PARTITION = 0;

    private final String SERVICE_ID_STR = "test-service-id";
    private final String TEST_TOPIC = "test-topic";
    private final String TEST_KEY = "test-key";

    private TbKafkaNode node;
    private TbKafkaNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private KafkaProducer<String, String> producerMock;
    @Mock
    private KafkaThread ioThreadMock;
    @Mock
    private RecordMetadata recordMetadataMock;

    @BeforeEach
    public void setUp() {
        node = spy(new TbKafkaNode());
        config = new TbKafkaNodeConfiguration().defaultConfiguration();
        config.setTopicPattern(TEST_TOPIC);
        config.setKeyPattern(TEST_KEY);
    }

    @Test
    public void verifyDefaultConfig() {
        config = new TbKafkaNodeConfiguration().defaultConfiguration();
        assertThat(config.getTopicPattern()).isEqualTo("my-topic");
        assertThat(config.getKeyPattern()).isNull();
        assertThat(config.getBootstrapServers()).isEqualTo("localhost:9092");
        assertThat(config.getRetries()).isEqualTo(0);
        assertThat(config.getBatchSize()).isEqualTo(16384);
        assertThat(config.getLinger()).isEqualTo(0);
        assertThat(config.getBufferMemory()).isEqualTo(33554432);
        assertThat(config.getAcks()).isEqualTo("-1");
        assertThat(config.getOtherProperties()).isEmpty();
        assertThat(config.isAddMetadataKeyValuesAsKafkaHeaders()).isFalse();
        assertThat(config.getKafkaHeadersCharset()).isEqualTo("UTF-8");
    }

    @Test
    public void givenExceptionDuringKafkaInitialization_whenInit_thenDestroy() throws TbNodeException {
        // GIVEN
        given(ctxMock.getSelfId()).willReturn(RULE_NODE_ID);
        ReflectionTestUtils.setField(producerMock, "ioThread", ioThreadMock);
        willAnswer(invocationOnMock -> {
            Thread.UncaughtExceptionHandler exceptionHandler = invocationOnMock.getArgument(0);
            exceptionHandler.uncaughtException(ioThreadMock, new ThingsboardKafkaClientError("Error during init"));
            return null;
        }).given(ioThreadMock).setUncaughtExceptionHandler(any());
        willReturn(producerMock).given(node).getKafkaProducer(any());

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // THEN
        then(producerMock).should().close();
        then(producerMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void verifyKafkaProperties() throws TbNodeException {
        String sslKeyStoreCertificateChain = "cbdvch\\nfwrg\nvgwg\\n";
        String sslKeyStoreKey = "nghmh\\nhmmnh\\\\ngreg\nvgwg\\n";
        String sslTruststoreCertificates = "grthrt\fd\\nfwrg\nvgwg\\n";
        config.setOtherProperties(Map.of(
                "ssl.keystore.certificate.chain", sslKeyStoreCertificateChain,
                "ssl.keystore.key", sslKeyStoreKey,
                "ssl.truststore.certificates", sslTruststoreCertificates,
                "ssl.protocol", "TLSv1.2"
        ));

        mockSuccessfulInit();

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        Properties expectedProperties = new Properties();
        expectedProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "producer-tb-kafka-node-" + RULE_NODE_ID.getId() + "-" + SERVICE_ID_STR);
        expectedProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        expectedProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        expectedProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        expectedProperties.put(ProducerConfig.ACKS_CONFIG, config.getAcks());
        expectedProperties.put(ProducerConfig.RETRIES_CONFIG, config.getRetries());
        expectedProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, config.getBatchSize());
        expectedProperties.put(ProducerConfig.LINGER_MS_CONFIG, config.getLinger());
        expectedProperties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, config.getBufferMemory());
        expectedProperties.put("ssl.keystore.certificate.chain", sslKeyStoreCertificateChain.replace("\\n", "\n"));
        expectedProperties.put("ssl.keystore.key", sslKeyStoreKey.replace("\\n", "\n"));
        expectedProperties.put("ssl.truststore.certificates", sslTruststoreCertificates.replace("\\n", "\n"));
        expectedProperties.put("ssl.protocol", "TLSv1.2");

        ArgumentCaptor<Properties> properties = ArgumentCaptor.forClass(Properties.class);
        then(node).should().getKafkaProducer(properties.capture());
        assertThat(properties.getValue()).isEqualTo(expectedProperties);
    }

    @Test
    public void givenInitErrorIsNotNull_whenOnMsg_thenTellFailure() {
        // GIVEN
        String errorMsg = "Error during kafka initialization!";
        ReflectionTestUtils.setField(node, "config", config);
        ReflectionTestUtils.setField(node, "initError", new ThingsboardKafkaClientError(errorMsg));

        // WHEN
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), actualError.capture());
        assertThat(actualError.getValue())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to initialize Kafka rule node producer: " + errorMsg);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void givenForceAckAndExceptionWasThrown_whenOnMsg_thenTellFailure(boolean forceAck) throws TbNodeException {
        // GIVEN
        given(ctxMock.isExternalNodeForceAck()).willReturn(forceAck);
        mockSuccessfulInit();
        ListeningExecutor executorMock = mock(ListeningExecutor.class);
        given(ctxMock.getExternalCallExecutor()).willReturn(executorMock);
        String errorMsg = "Something went wrong!";
        willThrow(new RuntimeException(errorMsg)).given(executorMock).executeAsync(any(Callable.class));

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should(forceAck ? times(1) : never()).ack(msg);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(actualMsg.capture(), actualError.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(msg);
        assertThat(actualError.getValue()).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
    }

    @ParameterizedTest
    @MethodSource
    public void givenForceAckIsTrueTopicAndKeyPatternsAndAddMetadataKeyValuesAsKafkaHeadersIsFalse_whenOnMsg_thenEnqueueForTellNext(
            String topicPattern, String keyPattern, TbMsgMetaData metaData, String data
    ) throws TbNodeException {
        // GIVEN
        config.setTopicPattern(topicPattern);
        config.setKeyPattern(keyPattern);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(data)
                .build();
        String topic = TbNodeUtils.processPattern(topicPattern, msg);
        String key = TbNodeUtils.processPattern(keyPattern, msg);

        given(ctxMock.isExternalNodeForceAck()).willReturn(true);
        mockSuccessfulInit();
        mockSuccessfulPublishingRequest(topic);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().ack(msg);
        verifyProducerRecord(topic, key, msg.getData());
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(actualMsg.capture(), eq(TbNodeConnectionType.SUCCESS));
        verifyOutgoingSuccessMsg(topic, actualMsg.getValue(), msg);
    }

    private static Stream<Arguments> givenForceAckIsTrueTopicAndKeyPatternsAndAddMetadataKeyValuesAsKafkaHeadersIsFalse_whenOnMsg_thenEnqueueForTellNext() {
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

    @ParameterizedTest
    @NullAndEmptySource
    public void givenForceAckIsFalseAndKeyIsNullOrEmptyAndErrorOccursDuringPublishing_whenOnMsg_thenTellFailure(String key) throws TbNodeException {
        // GIVEN
        config.setKeyPattern(key);

        given(ctxMock.isExternalNodeForceAck()).willReturn(false);
        mockSuccessfulInit();
        String errorMsg = "Something went wrong!";
        mockFailedPublishingRequest(new RuntimeException(errorMsg));

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        verifyProducerRecord(TEST_TOPIC, null, msg.getData());
        then(ctxMock).should(never()).ack(msg);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(actualMsg.capture(), actualError.capture());
        verifyOutgoingFailureMsg(errorMsg, actualMsg.getValue(), msg);
    }

    @Test
    public void givenForceAckIsTrueAndAddKafkaHeadersIsTrueAndToBytesCharsetIsNullAndErrorOccursDuringPublishing_whenOnMsg_thenEnqueueForTellFailure() throws TbNodeException {
        // GIVEN
        config.setAddMetadataKeyValuesAsKafkaHeaders(true);
        config.setKafkaHeadersCharset(null);

        given(ctxMock.isExternalNodeForceAck()).willReturn(true);
        mockSuccessfulInit();
        String errorMsg = "Something went wrong!";
        mockFailedPublishingRequest(new RuntimeException(errorMsg));

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().ack(msg);
        Headers expectedHeaders = new RecordHeaders();
        msg.getMetaData().values().forEach((k, v) -> expectedHeaders.add(new RecordHeader("tb_msg_md_" + k, v.getBytes(StandardCharsets.UTF_8))));
        verifyProducerRecord(TEST_TOPIC, TEST_KEY, msg.getData(), expectedHeaders);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().enqueueForTellFailure(actualMsg.capture(), actualError.capture());
        verifyOutgoingFailureMsg(errorMsg, actualMsg.getValue(), msg);
    }

    @Test
    public void givenForceAckIsFalseAndAddMetadataKeyValuesAsKafkaHeadersIsTrueAndToBytesCharsetIsSet_whenOnMsg_thenTellSuccess() throws TbNodeException {
        // GIVEN
        config.setAddMetadataKeyValuesAsKafkaHeaders(true);
        config.setKafkaHeadersCharset("UTF-16");

        given(ctxMock.isExternalNodeForceAck()).willReturn(false);
        mockSuccessfulInit();
        mockSuccessfulPublishingRequest(TEST_TOPIC);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should(never()).ack(msg);
        Headers expectedHeaders = new RecordHeaders();
        msg.getMetaData().values().forEach((k, v) -> expectedHeaders.add(new RecordHeader("tb_msg_md_" + k, v.getBytes(StandardCharsets.UTF_16))));
        verifyProducerRecord(TEST_TOPIC, TEST_KEY, msg.getData(), expectedHeaders);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        verifyOutgoingSuccessMsg(TEST_TOPIC, actualMsg.getValue(), msg);
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

    private void mockSuccessfulInit() {
        given(ctxMock.getSelfId()).willReturn(RULE_NODE_ID);
        given(ctxMock.getServiceId()).willReturn(SERVICE_ID_STR);
        ReflectionTestUtils.setField(producerMock, "ioThread", ioThreadMock);
        willReturn(producerMock).given(node).getKafkaProducer(any());
    }

    private void mockSuccessfulPublishingRequest(String topic) {
        given(ctxMock.getExternalCallExecutor()).willReturn(executor);
        willAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(recordMetadataMock, null);
            return null;
        }).given(producerMock).send(any(), any(Callback.class));
        given(recordMetadataMock.offset()).willReturn(OFFSET);
        given(recordMetadataMock.partition()).willReturn(PARTITION);
        given(recordMetadataMock.topic()).willReturn(topic);
    }

    private void mockFailedPublishingRequest(Exception exception) {
        given(ctxMock.getExternalCallExecutor()).willReturn(executor);
        willAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(recordMetadataMock, exception);
            return null;
        }).given(producerMock).send(any(), any(Callback.class));
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

    private void verifyOutgoingSuccessMsg(String expectedTopic, TbMsg actualMsg, TbMsg originalMsg) {
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        metaData.putValue("offset", String.valueOf(OFFSET));
        metaData.putValue("partition", String.valueOf(PARTITION));
        metaData.putValue("topic", expectedTopic);
        TbMsg expectedMsg = originalMsg.transform()
                .metaData(metaData)
                .build();
        assertThat(actualMsg)
                .usingRecursiveComparison()
                .ignoringFields("ctx")
                .isEqualTo(expectedMsg);
    }

    private void verifyOutgoingFailureMsg(String errorMsg, TbMsg actualMsg, TbMsg originalMsg) {
        TbMsgMetaData metaData = originalMsg.getMetaData();
        metaData.putValue("error", RuntimeException.class + ": " + errorMsg);
        TbMsg expectedMsg = originalMsg.transform()
                .metaData(metaData)
                .build();
        assertThat(actualMsg).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                //config for version 0
                Arguments.of(0,
                        "{\n" +
                                "  \"topicPattern\": \"test-topic\",\n" +
                                "  \"keyPattern\": \"test-key\",\n" +
                                "  \"bootstrapServers\": \"localhost:9092\",\n" +
                                "  \"retries\": 0,\n" +
                                "  \"batchSize\": 16384,\n" +
                                "  \"linger\": 0,\n" +
                                "  \"bufferMemory\": 33554432,\n" +
                                "  \"acks\": \"-1\",\n" +
                                "  \"otherProperties\": {},\n" +
                                "  \"addMetadataKeyValuesAsKafkaHeaders\": false,\n" +
                                "  \"kafkaHeadersCharset\": \"UTF-8\",\n" +
                                "  \"keySerializer\": \"org.apache.kafka.common.serialization.StringSerializer\",\n" +
                                "  \"valueSerializer\": \"org.apache.kafka.common.serialization.StringSerializer\"\n" +
                                "}",
                        true,
                        "{\n" +
                                "  \"topicPattern\": \"test-topic\",\n" +
                                "  \"keyPattern\": \"test-key\",\n" +
                                "  \"bootstrapServers\": \"localhost:9092\",\n" +
                                "  \"retries\": 0,\n" +
                                "  \"batchSize\": 16384,\n" +
                                "  \"linger\": 0,\n" +
                                "  \"bufferMemory\": 33554432,\n" +
                                "  \"acks\": \"-1\",\n" +
                                "  \"otherProperties\": {},\n" +
                                "  \"addMetadataKeyValuesAsKafkaHeaders\": false,\n" +
                                "  \"kafkaHeadersCharset\": \"UTF-8\"\n" +
                                "}"
                ),
                //config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\n" +
                                "  \"topicPattern\": \"test-topic\",\n" +
                                "  \"keyPattern\": \"test-key\",\n" +
                                "  \"bootstrapServers\": \"localhost:9092\",\n" +
                                "  \"retries\": 0,\n" +
                                "  \"batchSize\": 16384,\n" +
                                "  \"linger\": 0,\n" +
                                "  \"bufferMemory\": 33554432,\n" +
                                "  \"acks\": \"-1\",\n" +
                                "  \"otherProperties\": {},\n" +
                                "  \"addMetadataKeyValuesAsKafkaHeaders\": false,\n" +
                                "  \"kafkaHeadersCharset\": \"UTF-8\"\n" +
                                "}",
                        false,
                        "{\n" +
                                "  \"topicPattern\": \"test-topic\",\n" +
                                "  \"keyPattern\": \"test-key\",\n" +
                                "  \"bootstrapServers\": \"localhost:9092\",\n" +
                                "  \"retries\": 0,\n" +
                                "  \"batchSize\": 16384,\n" +
                                "  \"linger\": 0,\n" +
                                "  \"bufferMemory\": 33554432,\n" +
                                "  \"acks\": \"-1\",\n" +
                                "  \"otherProperties\": {},\n" +
                                "  \"addMetadataKeyValuesAsKafkaHeaders\": false,\n" +
                                "  \"kafkaHeadersCharset\": \"UTF-8\"\n" +
                                "}"
                )
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
