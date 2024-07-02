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
package org.thingsboard.rule.engine.transform;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TbChangeOriginatorNodeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("79830b6d-4f93-49bd-9b5b-d31ce51da77b"));
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
    private DeviceService deviceServiceMock;
    @Mock
    private RelationService relationServiceMock;
    @Mock
    private RuleEngineAlarmService alarmServiceMock;

    @BeforeEach
    public void before() throws TbNodeException {
        node = new TbChangeOriginatorNode();
        config = new TbChangeOriginatorNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void newChainCanBeStarted() throws TbNodeException {
        init();
        AssetId assetId = new AssetId(Uuids.timeBased());
        CustomerId customerId = new CustomerId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setCustomerId(customerId);

        RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, assetId, TbMsgMetaData.EMPTY, TbMsgDataType.JSON,TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getAssetService()).willReturn(assetServiceMock);
        given(assetServiceMock.findAssetByIdAsync(any(), eq(assetId))).willReturn(Futures.immediateFuture(asset));

        node.onMsg(ctxMock, msg);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        then(ctxMock).should().transformMsgOriginator(msgCaptor.capture(), originatorCaptor.capture());

        assertThat(originatorCaptor.getValue()).isEqualTo(customerId);
    }

    public void init() throws TbNodeException {
        TbChangeOriginatorNodeConfiguration config = new TbChangeOriginatorNodeConfiguration();
        config.setOriginatorSource("CUSTOMER");
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);

        node = new TbChangeOriginatorNode();
        node.init(null, nodeConfiguration);
    }

    @Test
    public void verifyDefaultConfig() {
        var config = new TbChangeOriginatorNodeConfiguration().defaultConfiguration();
        assertThat(config.getOriginatorSource()).isEqualTo("CUSTOMER");

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
    public void givenUnsupportedSource_whenInit_thenThrowsException() {
        config.setOriginatorSource("UNSUPPORTED_SOURCE");

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source 'UNSUPPORTED_SOURCE' is unsupported.");
    }

    @Test
    public void givenRelatedSourceAndRelatedQueryIsNull_whenInit_thenThrowsException() {
        config.setOriginatorSource("RELATED");
        config.setRelationsQuery(null);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relations query should be specified for the related entity.");
    }

    @Test
    public void givenEntitySourceAndEntityTypeIsNull_whenInit_thenThrowsException() {
        config.setOriginatorSource("ENTITY");
        config.setEntityType(null);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Entity type should be specified for the entity.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void givenEntitySourceAndEntityNamePatternIsEmpty_whenInit_thenThrowsException(String entityName) {
        config.setOriginatorSource("ENTITY");
        config.setEntityType("DEVICE");
        config.setEntityNamePattern(entityName);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Entity name pattern should be specified for the entity.");
    }

    @Test
    public void givenEntitySourceAndUnexpectedEntityType_whenInit_thenThrowsException() {
        config.setOriginatorSource("ENTITY");
        config.setEntityType("TENANT");
        config.setEntityNamePattern("tenant-A");

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unexpected entity type TENANT");
    }

    @Test
    public void givenOriginatorSourceIsCustomer_whenOnMsg_thenTellSuccess() throws TbNodeException {
        config.setOriginatorSource("CUSTOMER");

        CustomerId customerId = new CustomerId(UUID.fromString("d2746ed1-2c45-41d7-b34c-94f947f2aa03"));
        Device device = new Device(DEVICE_ID);
        device.setCustomerId(customerId);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        TbMsg expectedMsg = TbMsg.transformMsgOriginator(msg, customerId);

        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getDeviceService()).willReturn(deviceServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(deviceServiceMock.findDeviceById(any(TenantId.class), any(DeviceId.class))).willReturn(device);
        given(ctxMock.transformMsgOriginator(any(TbMsg.class), any(EntityId.class))).willReturn(expectedMsg);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        then(deviceServiceMock).should().findDeviceById(TENANT_ID, DEVICE_ID);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @Test
    public void givenOriginatorSourceIsTenant_whenOnMsg_thenTellSuccess() throws TbNodeException {
        config.setOriginatorSource("TENANT");

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, ASSET_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        TbMsg expectedMsg = TbMsg.transformMsgOriginator(msg, TENANT_ID);

        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.transformMsgOriginator(any(TbMsg.class), any(EntityId.class))).willReturn(expectedMsg);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @Test
    public void givenOriginatorSourceIsRelatedAndNewOriginatorIsNull_whenOnMsg_thenTellFailure() throws TbNodeException {
        config.setOriginatorSource("RELATED");

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, ASSET_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getRelationService()).willReturn(relationServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
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
        config.setOriginatorSource("ALARM_ORIGINATOR");

        AlarmId alarmId = new AlarmId(UUID.fromString("6b43f694-cb5f-4199-9023-e9e40eeb82dd"));
        Alarm alarm = new Alarm(alarmId);
        alarm.setOriginator(DEVICE_ID);

        TbMsg msg = TbMsg.newMsg(TbMsgType.ALARM, alarmId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        TbMsg expectedMsg = TbMsg.transformMsgOriginator(msg, DEVICE_ID);

        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(alarmServiceMock.findAlarmByIdAsync(any(TenantId.class), any(AlarmId.class))).willReturn(Futures.immediateFuture(alarm));
        given(ctxMock.transformMsgOriginator(any(TbMsg.class), any(EntityId.class))).willReturn(expectedMsg);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        then(alarmServiceMock).should().findAlarmByIdAsync(TENANT_ID, alarmId);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @Test
    public void givenOriginatorSourceIsEntity_whenOnMsg_thenTellSuccess() throws TbNodeException {
        config.setOriginatorSource("ENTITY");
        config.setEntityType("ASSET");
        config.setEntityNamePattern("${md-name-pattern}");

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("md-name-pattern", "test-asset");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, TbMsg.EMPTY_JSON_OBJECT);
        TbMsg expectedMsg = TbMsg.transformMsgOriginator(msg, ASSET_ID);

        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getAssetService()).willReturn(assetServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(assetServiceMock.findAssetByTenantIdAndName(any(TenantId.class), any(String.class))).willReturn(new Asset(ASSET_ID));
        given(ctxMock.transformMsgOriginator(any(TbMsg.class), any(EntityId.class))).willReturn(expectedMsg);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        then(assetServiceMock).should().findAssetByTenantIdAndName(TENANT_ID, "test-asset");
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @Test
    public void givenOriginatorSourceIsEntityAndEntityCouldNotFound_whenOnMsg_thenTellFailure() throws TbNodeException {
        config.setOriginatorSource("ENTITY");
        config.setEntityType("ASSET");
        config.setEntityNamePattern("${md-name-pattern}");

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("md-name-pattern", "test-asset");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, TbMsg.EMPTY_JSON_OBJECT);

        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getAssetService()).willReturn(assetServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(assetServiceMock.findAssetByTenantIdAndName(any(TenantId.class), any(String.class))).willReturn(null);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), throwable.capture());
        assertThat(throwable.getValue()).isInstanceOf(IllegalStateException.class).hasMessage("Failed to found ASSET  entity by name: 'test-asset'!");
    }

}
