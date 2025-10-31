/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.thingsboard.rule.engine.transform.OriginatorSource.ALARM_ORIGINATOR;
import static org.thingsboard.rule.engine.transform.OriginatorSource.CUSTOMER;
import static org.thingsboard.rule.engine.transform.OriginatorSource.ENTITY;
import static org.thingsboard.rule.engine.transform.OriginatorSource.RELATED;
import static org.thingsboard.rule.engine.transform.OriginatorSource.TENANT;

@ExtendWith(MockitoExtension.class)
public class TbChangeOriginatorNodeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("79830b6d-4f93-49bd-9b5b-d31ce51da77b"));
    private final CustomerId CUSTOMER_ID = new CustomerId(UUID.fromString("c6b2c94b-5517-4f20-bf8e-ae9407eb8a7a"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("990605a4-db46-4ed4-942f-e18200453571"));
    private final AssetId ASSET_ID = new AssetId(UUID.fromString("55de3f10-1b55-4950-b711-ed132896b260"));

    private final ListeningExecutor dbExecutor = new TestDbCallbackExecutor();

    private TbChangeOriginatorNode node;
    private TbChangeOriginatorNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private AssetService assetServiceMock;
    @Mock
    private RelationService relationServiceMock;
    @Mock
    private RuleEngineAlarmService alarmServiceMock;
    @Mock
    private EntityService entityServiceMock;

    @BeforeEach
    public void setup() {
        node = new TbChangeOriginatorNode();
        config = new TbChangeOriginatorNodeConfiguration().defaultConfiguration();

        lenient().when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);
        lenient().when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
        lenient().when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        lenient().when(ctxMock.getAlarmService()).thenReturn(alarmServiceMock);
        lenient().when(ctxMock.getEntityService()).thenReturn(entityServiceMock);
    }

    @Test
    public void verifyDefaultConfig() {
        var config = new TbChangeOriginatorNodeConfiguration().defaultConfiguration();
        assertThat(config.getOriginatorSource()).isEqualTo(CUSTOMER);
        RelationsQuery relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        RelationEntityTypeFilter relationEntityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(relationEntityTypeFilter));
        assertThat(config.getRelationsQuery()).isEqualTo(relationsQuery);
        assertThat(config.getEntityType()).isNull();
        assertThat(config.getEntityNamePattern()).isNull();
    }

    @Test
    public void givenRelatedSourceIsNull_whenInit_thenThrowsException() {
        config.setOriginatorSource(null);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Originator source should be specified.");
    }

    @Test
    public void givenRelatedSourceAndRelatedQueryIsNull_whenInit_thenThrowsException() {
        config.setOriginatorSource(RELATED);
        config.setRelationsQuery(null);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relations query should be specified if 'Related entity' source is selected.");
    }

    @Test
    public void givenEntitySourceAndEntityTypeIsNull_whenInit_thenThrowsException() {
        config.setOriginatorSource(ENTITY);
        config.setEntityType(null);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Entity type should be specified if 'Entity by name pattern' source is selected.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void givenEntitySourceAndEntityNamePatternIsEmpty_whenInit_thenThrowsException(String entityName) {
        config.setOriginatorSource(ENTITY);
        config.setEntityType(EntityType.DEVICE.name());
        config.setEntityNamePattern(entityName);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name pattern should be specified if 'Entity by name pattern' source is selected.");
    }

    @Test
    public void givenEntitySourceAndUnexpectedEntityType_whenInit_thenThrowsException() {
        config.setOriginatorSource(ENTITY);
        config.setEntityType(EntityType.TENANT.name());
        config.setEntityNamePattern("tenant-A");

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unexpected entity type TENANT");
    }

    @Test
    @DisplayName("""
            Given a device assigned to a customer and node configured to change originator to customer,
            when processing the message,
            then should change message originator from device to customer""")
    public void givenDeviceAssignedToCustomer_whenProcessingMessage_thenChangesOriginatorToCustomer() throws TbNodeException {
        // GIVEN
        var device = new Device(DEVICE_ID);
        device.setCustomerId(CUSTOMER_ID);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        TbMsg expectedMsg = msg.transform()
                .originator(CUSTOMER_ID)
                .build();

        given(entityServiceMock.fetchEntityCustomerIdAsync(TENANT_ID, device.getId())).willReturn(
                FluentFuture.from(immediateFuture(Optional.of(CUSTOMER_ID)))
        );

        given(ctxMock.transformMsgOriginator(any(TbMsg.class), any(EntityId.class))).willReturn(expectedMsg);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().transformMsgOriginator(msg, CUSTOMER_ID);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @Test
    @DisplayName("""
            Given a customer as message originator and node configured to change originator to customer,
            when processing the message,
            then should keep the customer as originator""")
    public void givenCustomerAsOriginator_whenProcessingMessage_thenKeepsCustomerAsOriginator() throws TbNodeException {
        // GIVEN
        var customer = new Customer(CUSTOMER_ID);

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(customer.getId())
                .metaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        var expectedMsg = msg.transform()
                .originator(CUSTOMER_ID)
                .build();

        given(ctxMock.transformMsgOriginator(any(TbMsg.class), any(EntityId.class))).willReturn(expectedMsg);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().transformMsgOriginator(msg, CUSTOMER_ID);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @Test
    public void givenOriginatorSourceIsTenant_whenOnMsg_thenTellSuccess() throws TbNodeException {
        config.setOriginatorSource(TENANT);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(ASSET_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        TbMsg expectedMsg = msg.transform()
                .originator(TENANT_ID)
                .build();

        given(ctxMock.transformMsgOriginator(any(TbMsg.class), any(EntityId.class))).willReturn(expectedMsg);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().transformMsgOriginator(msg, TENANT_ID);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @Test
    public void givenOriginatorSourceIsRelatedAndNewOriginatorIsNull_whenOnMsg_thenTellFailure() throws TbNodeException {
        config.setOriginatorSource(RELATED);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(ASSET_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        given(relationServiceMock.findByQuery(any(TenantId.class), any(EntityRelationsQuery.class))).willReturn(Futures.immediateFuture(Collections.emptyList()));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        var query = new EntityRelationsQuery();
        var relationsQuery = config.getRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ASSET_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        then(relationServiceMock).should().findByQuery(TENANT_ID, query);
        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), throwable.capture());
        assertThat(throwable.getValue()).isInstanceOf(NoSuchElementException.class).hasMessage("Failed to find new originator!");
    }

    @Test
    public void givenOriginatorSourceIsAlarmOriginator_whenOnMsg_thenTellSuccess() throws TbNodeException {
        config.setOriginatorSource(ALARM_ORIGINATOR);

        AlarmId alarmId = new AlarmId(UUID.fromString("6b43f694-cb5f-4199-9023-e9e40eeb82dd"));
        Alarm alarm = new Alarm(alarmId);
        alarm.setOriginator(DEVICE_ID);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(alarmId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        TbMsg expectedMsg = msg.transform()
                .originator(DEVICE_ID)
                .build();

        given(alarmServiceMock.findAlarmByIdAsync(any(TenantId.class), any(AlarmId.class))).willReturn(Futures.immediateFuture(alarm));
        given(ctxMock.transformMsgOriginator(any(TbMsg.class), any(EntityId.class))).willReturn(expectedMsg);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        then(alarmServiceMock).should().findAlarmByIdAsync(TENANT_ID, alarmId);
        then(ctxMock).should().transformMsgOriginator(msg, DEVICE_ID);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @ParameterizedTest
    @MethodSource
    public void givenOriginatorSourceIsEntity_whenOnMsg_thenTellSuccess(String entityNamePattern, TbMsgMetaData metaData, String data) throws TbNodeException {
        config.setOriginatorSource(ENTITY);
        config.setEntityType(EntityType.ASSET.name());
        config.setEntityNamePattern(entityNamePattern);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(data)
                .build();
        TbMsg expectedMsg = msg.transform()
                .originator(ASSET_ID)
                .build();

        given(assetServiceMock.findAssetByTenantIdAndName(any(TenantId.class), any(String.class))).willReturn(new Asset(ASSET_ID));
        given(ctxMock.transformMsgOriginator(any(TbMsg.class), any(EntityId.class))).willReturn(expectedMsg);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        String expectedEntityName = TbNodeUtils.processPattern(entityNamePattern, msg);
        then(assetServiceMock).should().findAssetByTenantIdAndName(TENANT_ID, expectedEntityName);
        then(ctxMock).should().transformMsgOriginator(msg, ASSET_ID);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    private static Stream<Arguments> givenOriginatorSourceIsEntity_whenOnMsg_thenTellSuccess() {
        return Stream.of(
                Arguments.of("test-asset", TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("${md-name-pattern}", new TbMsgMetaData(Map.of("md-name-pattern", "md-test-asset")), TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("${msg-name-pattern}", TbMsgMetaData.EMPTY, "{\"msg-name-pattern\":\"msg-test-asset\"}")
        );
    }

    @Test
    public void givenOriginatorSourceIsEntityAndEntityCouldNotFound_whenOnMsg_thenTellFailure() throws TbNodeException {
        config.setOriginatorSource(ENTITY);
        config.setEntityType(EntityType.ASSET.name());
        config.setEntityNamePattern("${md-name-pattern}");

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("md-name-pattern", "test-asset");
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        given(assetServiceMock.findAssetByTenantIdAndName(any(TenantId.class), any(String.class))).willReturn(null);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), throwable.capture());
        assertThat(throwable.getValue()).isInstanceOf(IllegalStateException.class).hasMessage("Failed to find asset with name 'test-asset'!");
    }

}
