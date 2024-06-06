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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.dao.nosql.TbResultSet;
import org.thingsboard.server.dao.nosql.TbResultSetFuture;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbSaveToCustomCassandraTableNodeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("ac4ca02e-2ae6-404a-8f7e-c4ae31c56aa7"));
    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("64ad971e-9cfa-49e4-9f59-faa1a2350c6e"));

    private final ListeningExecutor dbCallbackExecutor = new TestDbCallbackExecutor();

    private TbSaveToCustomCassandraTableNode node;
    private TbSaveToCustomCassandraTableNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private CassandraCluster cassandraClusterMock;
    @Mock
    private GuavaSession sessionMock;
    @Mock
    private PreparedStatement preparedStatementMock;
    @Mock
    private BoundStatement boundStatementMock;
    @Mock
    private BoundStatementBuilder boundStatementBuilderMock;
    @Mock
    private ColumnDefinitions columnDefinitionsMock;
    @Mock
    private CodecRegistry codecRegistryMock;
    @Mock
    private ProtocolVersion protocolVersionMock;
    @Mock
    private Node nodeMock;

    @BeforeEach
    void setUp() {
        node = spy(new TbSaveToCustomCassandraTableNode());
        config = new TbSaveToCustomCassandraTableNodeConfiguration().defaultConfiguration();
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenCassandraClusterIsMissing_whenInit_thenThrowsException() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatThrownBy(() -> node.init(ctxMock, configuration))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unable to connect to Cassandra database");
    }

    @Test
    void givenFieldsMapIsEmpty_whenInit_thenThrowsException() {
        config.setFieldsMapping(emptyMap());
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctxMock.getCassandraCluster()).thenReturn(cassandraClusterMock);

        assertThatThrownBy(() -> node.init(ctxMock, configuration))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Fields(key,value) map is empty!");
    }

    @Test
    void givenInvalidMessageStructure_whenOnMsg_thenThrowsException() throws TbNodeException {
        config.setTableName("temperature_sensor");
        config.setFieldsMapping(Map.of("temp", "temperature"));

        mockOnInit();

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid message structure, it is not a JSON Object:" + null);
    }

    @Test
    void givenDataKeyIsMissingInMsg_whenOnMsg_thenThrowsException() throws TbNodeException {
        config.setTableName("temperature_sensor");
        config.setFieldsMapping(Map.of("temp", "temperature"));

        mockOnInit();
        mockBoundStatement();

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        String data = """
                {
                  "humidity": 77
                }
                """;
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, data);
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Message data doesn't contain key: 'temp'!");
    }

    @Test
    void givenUnsupportedData_whenOnMsg_thenThrowsException() throws TbNodeException {
        config.setTableName("temperature_sensor");
        config.setFieldsMapping(Map.of("temp", "temperature"));

        mockOnInit();
        mockBoundStatement();

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        String data = """
                {
                "temp": [value]
                }
                """;
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, data);
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Message data key: 'temp' with value: '[\"value\"]' is not a JSON Object or JSON Primitive!");
    }

    @Test
    void givenValidMsgStructureAndKeyIsEntityId_whenOnMsg_thenOk() throws Exception {
        config.setTableName("readings");
        config.setFieldsMapping(Map.of("$entityId", "entityId"));

        mockOnInit();
        when(boundStatementBuilderMock.setUuid(anyInt(), any(UUID.class))).thenReturn(boundStatementBuilderMock);
        when(boundStatementMock.getConsistencyLevel()).thenReturn(DefaultConsistencyLevel.ONE);
        mockBoundStatementBuilder();
        mockSubmittingCassandraTask();

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        verify(sessionMock).prepare("INSERT INTO cs_tb_readings(entityId) VALUES(?)");
        verify(boundStatementBuilderMock).setUuid(anyInt(), eq(DEVICE_ID.getId()));

        ArgumentCaptor<CassandraStatementTask> taskCaptor = ArgumentCaptor.forClass(CassandraStatementTask.class);
        verify(ctxMock).submitCassandraWriteTask(taskCaptor.capture());
        CassandraStatementTask task = taskCaptor.getValue();
        assertThat(task.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(task.getSession()).isEqualTo(sessionMock);
        assertThat(task.getStatement()).isEqualTo(boundStatementMock);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(
                () -> verify(ctxMock).tellSuccess(msg)
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenValidMsgStructure_whenOnMsg_thenSaveToCustomCassandraTable(
            String valueFromData,
            Consumer<BoundStatementBuilder> mockBuilderConsumer,
            Consumer<BoundStatementBuilder> verifyBuilderConsumer) throws TbNodeException {
        config.setTableName("sensor_readings");
        config.setFieldsMapping(Map.of("msgField1", "tableColumn1", "msgField2", "tableColumn2"));

        mockOnInit();
        mockBuilderConsumer.accept(boundStatementBuilderMock);
        mockBoundStatementBuilder();
        mockSubmittingCassandraTask();

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        String data = String.format("""
                {
                    "msgField1": %s,
                    "msgField2": "value2",
                    "anotherField": 22
                }
                """, valueFromData);
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, data);
        node.onMsg(ctxMock, msg);

        verifyBuilderConsumer.accept(boundStatementBuilderMock);

        ArgumentCaptor<CassandraStatementTask> taskCaptor = ArgumentCaptor.forClass(CassandraStatementTask.class);
        verify(ctxMock).submitCassandraWriteTask(taskCaptor.capture());
        CassandraStatementTask task = taskCaptor.getValue();
        assertThat(task.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(task.getSession()).isEqualTo(sessionMock);
        assertThat(task.getStatement()).isEqualTo(boundStatementMock);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(
                () -> verify(ctxMock).tellSuccess(msg)
        );
    }

    private static Stream<Arguments> givenValidMsgStructure_whenOnMsg_thenSaveToCustomCassandraTable() {
        return Stream.of(
                Arguments.of("1.0",
                        (Consumer<BoundStatementBuilder>) mockBuilder -> when(mockBuilder.setDouble(anyInt(), anyDouble())).thenReturn(mockBuilder),
                        (Consumer<BoundStatementBuilder>) verifyBuilder -> verify(verifyBuilder).setDouble(anyInt(), eq(1.0))),
                Arguments.of("2",
                        (Consumer<BoundStatementBuilder>) mockBuilder -> when(mockBuilder.setLong(anyInt(), anyLong())).thenReturn(mockBuilder),
                        (Consumer<BoundStatementBuilder>) verifyBuilder -> verify(verifyBuilder).setLong(anyInt(), eq(2L))),
                Arguments.of("true",
                        (Consumer<BoundStatementBuilder>) mockBuilder -> when(mockBuilder.setBoolean(anyInt(), anyBoolean())).thenReturn(mockBuilder),
                        (Consumer<BoundStatementBuilder>) verifyBuilder -> verify(verifyBuilder).setBoolean(anyInt(), eq(true))),
                Arguments.of("\"string value\"",
                        (Consumer<BoundStatementBuilder>) mockBuilder -> when(mockBuilder.setString(anyInt(), anyString())).thenReturn(mockBuilder),
                        (Consumer<BoundStatementBuilder>) verifyBuilder -> verify(verifyBuilder).setString(anyInt(), eq("string value"))),
                Arguments.of("{\"key\":\"value\"}",
                        (Consumer<BoundStatementBuilder>) mockBuilder -> when(mockBuilder.setString(anyInt(), anyString())).thenReturn(mockBuilder),
                        (Consumer<BoundStatementBuilder>) verifyBuilder -> verify(verifyBuilder).setString(anyInt(), eq("{\"key\":\"value\"}")))
        );
    }

    private void mockOnInit() {
        when(ctxMock.getCassandraCluster()).thenReturn(cassandraClusterMock);
        when(cassandraClusterMock.getSession()).thenReturn(sessionMock);
        when(cassandraClusterMock.getDefaultWriteConsistencyLevel()).thenReturn(DefaultConsistencyLevel.ONE);
        when(sessionMock.prepare(anyString())).thenReturn(preparedStatementMock);
    }

    private void mockBoundStatement() {
        when(preparedStatementMock.bind()).thenReturn(boundStatementMock);
        when(boundStatementMock.getPreparedStatement()).thenReturn(preparedStatementMock);
        when(preparedStatementMock.getVariableDefinitions()).thenReturn(columnDefinitionsMock);
        when(boundStatementMock.codecRegistry()).thenReturn(codecRegistryMock);
        when(boundStatementMock.protocolVersion()).thenReturn(protocolVersionMock);
        when(boundStatementMock.getNode()).thenReturn(nodeMock);
    }

    private void mockBoundStatementBuilder() {
        doAnswer(invocation -> boundStatementBuilderMock).when(node).getStmtBuilder();
        when(boundStatementBuilderMock.build()).thenReturn(boundStatementMock);
    }

    private void mockSubmittingCassandraTask() {
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        doAnswer(invocation -> {
            SettableFuture<TbResultSet> mainFuture = SettableFuture.create();
            mainFuture.set(new TbResultSet(null, null, null));
            return new TbResultSetFuture(mainFuture);
        }).when(ctxMock).submitCassandraWriteTask(any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
    }

}
