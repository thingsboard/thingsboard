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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;

@ExtendWith(MockitoExtension.class)
public class TbSaveToCustomCassandraTableNodeTest extends AbstractRuleNodeUpgradeTest {

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
    @Mock
    private Metadata metadataMock;
    @Mock
    private KeyspaceMetadata keyspaceMetadataMock;
    @Mock
    private TableMetadata tableMetadataMock;

    @BeforeEach
    public void setUp() {
        node = spy(new TbSaveToCustomCassandraTableNode());
        config = new TbSaveToCustomCassandraTableNodeConfiguration().defaultConfiguration();
    }

    @AfterEach
    public void tearDown() {
        node.destroy();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getTableName()).isEqualTo("");
        assertThat(config.getFieldsMapping()).isEqualTo(Map.of("", ""));
        assertThat(config.getDefaultTtl()).isEqualTo(0);
    }

    @Test
    public void givenCassandraClusterIsMissing_whenInit_thenThrowsException() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatThrownBy(() -> node.init(ctxMock, configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Unable to connect to Cassandra database")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    @Test
    public void givenTableDoesNotExist_whenInit_thenThrowsException() {
        config.setTableName("test_table");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        mockCassandraCluster();
        given(keyspaceMetadataMock.getTable(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> node.init(ctxMock, configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Table 'cs_tb_test_table' does not exist in Cassandra cluster.")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(false);
    }

    @Test
    public void givenFieldsMapIsEmpty_whenInit_thenThrowsException() {
        config.setTableName("test_table");
        config.setFieldsMapping(emptyMap());
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        mockCassandraCluster();

        assertThatThrownBy(() -> node.init(ctxMock, configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Fields(key,value) map is empty!");
    }

    @Test
    public void givenInvalidMessageStructure_whenOnMsg_thenThrowsException() throws TbNodeException {
        config.setTableName("temperature_sensor");
        config.setFieldsMapping(Map.of("temp", "temperature"));

        mockOnInit();

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid message structure, it is not a JSON Object: " + null);
    }

    @Test
    public void givenDataKeyIsMissingInMsg_whenOnMsg_thenThrowsException() throws TbNodeException {
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
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(data)
                .build();
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Message data doesn't contain key: 'temp'!");
    }

    @Test
    public void givenUnsupportedData_whenOnMsg_thenThrowsException() throws TbNodeException {
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
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(data)
                .build();
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Message data key: 'temp' with value: '[\"value\"]' is not a JSON Object or JSON Primitive!");
    }

    @ParameterizedTest
    @MethodSource
    public void givenTtl_whenOnMsg_thenVerifyStatement(int ttlFromConfig,
                                                       String expectedQuery,
                                                       Consumer<BoundStatementBuilder> verifyBuilder) throws TbNodeException {
        config.setTableName("readings");
        config.setFieldsMapping(Map.of("$entityId", "entityIdTableColumn"));
        config.setDefaultTtl(ttlFromConfig);

        mockOnInit();
        willAnswer(invocation -> boundStatementBuilderMock).given(node).getStmtBuilder();
        given(boundStatementBuilderMock.setUuid(anyInt(), any(UUID.class))).willReturn(boundStatementBuilderMock);
        given(boundStatementBuilderMock.build()).willReturn(boundStatementMock);
        mockSubmittingCassandraTask();

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        then(sessionMock).should().prepare(expectedQuery);
        verifyBuilder.accept(boundStatementBuilderMock);
    }

    private static Stream<Arguments> givenTtl_whenOnMsg_thenVerifyStatement() {
        return Stream.of(
                Arguments.of(0, "INSERT INTO cs_tb_readings(entityIdTableColumn) VALUES(?)",
                        (Consumer<BoundStatementBuilder>) builder -> {
                            then(builder).should(never()).setInt(anyInt(), anyInt());
                        }),
                Arguments.of(20, "INSERT INTO cs_tb_readings(entityIdTableColumn) VALUES(?) USING TTL ?",
                        (Consumer<BoundStatementBuilder>) builder -> {
                            then(builder).should().setInt(1, 20);
                        })
        );
    }

    @Test
    public void givenValidMsgStructure_whenOnMsg_thenVerifyMatchOfValuesInsertionOrderIntoStatementAndSaveToCustomCassandraTable() throws TbNodeException {
        config.setDefaultTtl(25);
        config.setTableName("readings");
        Map<String, String> mappings = Map.of(
                "$entityId", "entityIdTableColumn",
                "doubleField", "doubleTableColumn",
                "longField", "longTableColumn",
                "booleanField", "booleanTableColumn",
                "stringField", "stringTableColumn",
                "jsonField", "jsonTableColumn"
        );
        config.setFieldsMapping(mappings);

        mockOnInit();
        mockBoundStatementBuilder();
        mockSubmittingCassandraTask();

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        String data = """
                {
                    "doubleField": 22.5,
                    "longField": 56,
                    "booleanField": true,
                    "stringField": "some string",
                    "jsonField": {
                        "key": "value"
                    }
                }
                """;
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(data)
                .build();
        node.onMsg(ctxMock, msg);

        verifySettingStatementBuilder();
        ArgumentCaptor<CassandraStatementTask> taskCaptor = ArgumentCaptor.forClass(CassandraStatementTask.class);
        then(ctxMock).should().submitCassandraWriteTask(taskCaptor.capture());
        CassandraStatementTask task = taskCaptor.getValue();
        assertThat(task.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(task.getSession()).isEqualTo(sessionMock);
        assertThat(task.getStatement()).isEqualTo(boundStatementMock);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(
                () -> then(ctxMock).should().tellSuccess(msg)
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"tableName\":\"\",\"fieldsMapping\":{\"\":\"\"}}",
                        true,
                        "{\"tableName\":\"\",\"fieldsMapping\":{\"\":\"\"},\"defaultTtl\":0}"
                ),
                // default config for version 1 with upgrade from version 1
                Arguments.of(1,
                        "{\"tableName\":\"\",\"fieldsMapping\":{\"\":\"\"},\"defaultTtl\":0}",
                        false,
                        "{\"tableName\":\"\",\"fieldsMapping\":{\"\":\"\"},\"defaultTtl\":0}"
                )
        );
    }

    private void mockOnInit() {
        mockCassandraCluster();
        given(cassandraClusterMock.getDefaultWriteConsistencyLevel()).willReturn(DefaultConsistencyLevel.ONE);
        given(sessionMock.prepare(anyString())).willReturn(preparedStatementMock);
    }

    private void mockCassandraCluster() {
        given(ctxMock.getCassandraCluster()).willReturn(cassandraClusterMock);
        given(cassandraClusterMock.getSession()).willReturn(sessionMock);
        given(sessionMock.getMetadata()).willReturn(metadataMock);
        given(cassandraClusterMock.getKeyspaceName()).willReturn("test_keyspace");
        given(metadataMock.getKeyspace(anyString())).willReturn(Optional.of(keyspaceMetadataMock));
        given(keyspaceMetadataMock.getTable(anyString())).willReturn(Optional.of(tableMetadataMock));
    }

    private void mockBoundStatement() {
        given(preparedStatementMock.bind()).willReturn(boundStatementMock);
        given(boundStatementMock.getPreparedStatement()).willReturn(preparedStatementMock);
        given(preparedStatementMock.getVariableDefinitions()).willReturn(columnDefinitionsMock);
        given(boundStatementMock.codecRegistry()).willReturn(codecRegistryMock);
        given(boundStatementMock.protocolVersion()).willReturn(protocolVersionMock);
        given(boundStatementMock.getNode()).willReturn(nodeMock);
    }

    private void mockBoundStatementBuilder() {
        willAnswer(invocation -> boundStatementBuilderMock).given(node).getStmtBuilder();
        given(boundStatementBuilderMock.setUuid(anyInt(), any(UUID.class))).willReturn(boundStatementBuilderMock);
        given(boundStatementBuilderMock.setDouble(anyInt(), anyDouble())).willReturn(boundStatementBuilderMock);
        given(boundStatementBuilderMock.setLong(anyInt(), anyLong())).willReturn(boundStatementBuilderMock);
        given(boundStatementBuilderMock.setBoolean(anyInt(), anyBoolean())).willReturn(boundStatementBuilderMock);
        given(boundStatementBuilderMock.setString(anyInt(), anyString())).willReturn(boundStatementBuilderMock);
        given(boundStatementBuilderMock.setInt(anyInt(), anyInt())).willReturn(boundStatementBuilderMock);
        given(boundStatementBuilderMock.build()).willReturn(boundStatementMock);
    }

    private void mockSubmittingCassandraTask() {
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        willAnswer(invocation -> {
            SettableFuture<TbResultSet> mainFuture = SettableFuture.create();
            mainFuture.set(new TbResultSet(null, null, null));
            return new TbResultSetFuture(mainFuture);
        }).given(ctxMock).submitCassandraWriteTask(any());
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbCallbackExecutor);
    }

    private void verifySettingStatementBuilder() {
        Map<String, String> fieldsMap = (Map<String, String>) ReflectionTestUtils.getField(node, "fieldsMap");
        List<String> values = new ArrayList<>(fieldsMap.values());
        then(boundStatementBuilderMock).should().setUuid(values.indexOf("entityIdTableColumn"), DEVICE_ID.getId());
        then(boundStatementBuilderMock).should().setDouble(values.indexOf("doubleTableColumn"), 22.5);
        then(boundStatementBuilderMock).should().setLong(values.indexOf("longTableColumn"), 56L);
        then(boundStatementBuilderMock).should().setBoolean(values.indexOf("booleanTableColumn"), true);
        then(boundStatementBuilderMock).should().setString(values.indexOf("stringTableColumn"), "some string");
        then(boundStatementBuilderMock).should().setString(values.indexOf("jsonTableColumn"), "{\"key\":\"value\"}");
        then(boundStatementBuilderMock).should().setInt(values.size(), 25);
    }

}
