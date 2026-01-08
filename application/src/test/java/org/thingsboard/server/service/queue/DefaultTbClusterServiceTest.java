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
package org.thingsboard.server.service.queue;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsgHeaders;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.TbRuleEngineProducerService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbClusterService.class)
public class DefaultTbClusterServiceTest {

    public static final String MONOLITH = "monolith";

    public static final String CORE = "core";

    public static final String RULE_ENGINE = "rule_engine";

    public static final String TRANSPORT = "transport";

    @MockBean
    protected TbDeviceProfileCache deviceProfileCache;
    @MockBean
    protected TbAssetProfileCache assetProfileCache;
    @MockBean
    protected GatewayNotificationsService gatewayNotificationsService;
    @MockBean
    protected EdgeService edgeService;
    @MockBean
    protected PartitionService partitionService;
    @MockBean
    protected TbQueueProducerProvider producerProvider;
    @MockBean
    protected TbRuleEngineProducerService ruleEngineProducerService;
    @MockBean
    protected TbTransactionalCache<EdgeId, String> edgeCache;
    @MockBean
    protected CalculatedFieldService calculatedFieldService;

    @SpyBean
    protected TopicService topicService;
    @SpyBean
    protected TbClusterService clusterService;

    @Test
    public void testOnQueueChangeSingleMonolith() {
        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(MONOLITH));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbQueueProducer);

        clusterService.onQueuesUpdate(List.of(createTestQueue()));

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH);
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_CORE), any());
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_TRANSPORT), any());

        verify(tbQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(producerProvider, never()).getTbCoreNotificationsMsgProducer();
        verify(producerProvider, never()).getTransportNotificationsMsgProducer();
    }

    @Test
    public void testOnQueueChangeMultipleMonoliths() {
        String monolith1 = MONOLITH + 1;
        String monolith2 = MONOLITH + 2;
        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(monolith1, monolith2));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(monolith1, monolith2));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(monolith1, monolith2));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbQueueProducer);

        clusterService.onQueuesUpdate(List.of(createTestQueue()));

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2);
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_CORE), any());
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_TRANSPORT), any());

        verify(tbQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2)), any(TbProtoQueueMsg.class), isNull());

        verify(producerProvider, never()).getTbCoreNotificationsMsgProducer();
        verify(producerProvider, never()).getTransportNotificationsMsgProducer();
    }

    @Test
    public void testOnQueueChangeSingleMonolithAndSingleRemoteTransport() {
        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(MONOLITH, TRANSPORT));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbREQueueProducer = mock(TbQueueProducer.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> tbTransportQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbREQueueProducer);
        when(producerProvider.getTransportNotificationsMsgProducer()).thenReturn(tbTransportQueueProducer);

        clusterService.onQueuesUpdate(List.of(createTestQueue()));

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_TRANSPORT, TRANSPORT);
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_CORE), any());

        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(tbTransportQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, TRANSPORT)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(tbTransportQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(producerProvider, never()).getTbCoreNotificationsMsgProducer();
    }

    @Test
    public void testOnQueueChangeMultipleMicroservices() {
        String monolith1 = MONOLITH + 1;
        String monolith2 = MONOLITH + 2;

        String core1 = CORE + 1;
        String core2 = CORE + 2;

        String ruleEngine1 = RULE_ENGINE + 1;
        String ruleEngine2 = RULE_ENGINE + 2;

        String transport1 = TRANSPORT + 1;
        String transport2 = TRANSPORT + 2;

        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(monolith1, monolith2, ruleEngine1, ruleEngine2));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(monolith1, monolith2, core1, core2));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(monolith1, monolith2, transport1, transport2));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbREQueueProducer = mock(TbQueueProducer.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> tbCoreQueueProducer = mock(TbQueueProducer.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> tbTransportQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbREQueueProducer);
        when(producerProvider.getTbCoreNotificationsMsgProducer()).thenReturn(tbCoreQueueProducer);
        when(producerProvider.getTransportNotificationsMsgProducer()).thenReturn(tbTransportQueueProducer);

        clusterService.onQueuesUpdate(List.of(createTestQueue()));

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine2);

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_CORE, core1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_CORE, core2);
        verify(topicService, never()).getNotificationsTopic(ServiceType.TB_CORE, monolith1);
        verify(topicService, never()).getNotificationsTopic(ServiceType.TB_CORE, monolith2);

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_TRANSPORT, transport1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_TRANSPORT, transport2);
        verify(topicService, never()).getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith1);
        verify(topicService, never()).getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith2);

        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2)), any(TbProtoQueueMsg.class), isNull());
        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine2)), any(TbProtoQueueMsg.class), isNull());

        verify(tbCoreQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, core1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbCoreQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, core2)), any(TbProtoQueueMsg.class), isNull());
        verify(tbCoreQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbCoreQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, monolith2)), any(TbProtoQueueMsg.class), isNull());

        verify(tbTransportQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transport1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transport2)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith2)), any(TbProtoQueueMsg.class), isNull());
    }

    @Test
    public void testPushNotificationToCoreWithRestApiCallResponseMsgProto() {
        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);
        TbQueueCallback callbackMock = mock(TbQueueCallback.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> tbCoreQueueProducer = mock(TbQueueProducer.class);

        doReturn(tpi).when(topicService).getNotificationsTopic(any(ServiceType.class), any(String.class));
        when(producerProvider.getTbCoreNotificationsMsgProducer()).thenReturn(tbCoreQueueProducer);
        TransportProtos.RestApiCallResponseMsgProto responseMsgProto = TransportProtos.RestApiCallResponseMsgProto.getDefaultInstance();
        TransportProtos.ToCoreNotificationMsg toCoreNotificationMsg = TransportProtos.ToCoreNotificationMsg.newBuilder().setRestApiCallResponseMsg(responseMsgProto).build();

        clusterService.pushNotificationToCore(CORE, responseMsgProto, callbackMock);

        verify(topicService).getNotificationsTopic(ServiceType.TB_CORE, CORE);
        verify(producerProvider).getTbCoreNotificationsMsgProducer();
        ArgumentCaptor<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> protoQueueMsgArgumentCaptor = ArgumentCaptor.forClass(TbProtoQueueMsg.class);
        verify(tbCoreQueueProducer).send(eq(tpi), protoQueueMsgArgumentCaptor.capture(), eq(callbackMock));
        TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg> protoQueueMsgArgumentCaptorValue = protoQueueMsgArgumentCaptor.getValue();
        assertThat(protoQueueMsgArgumentCaptorValue.getKey()).isNotNull();
        assertThat(protoQueueMsgArgumentCaptorValue.getValue()).isEqualTo(toCoreNotificationMsg);
        assertThat(protoQueueMsgArgumentCaptorValue.getHeaders().getData()).isEqualTo(new DefaultTbQueueMsgHeaders().getData());
    }

    @Test
    public void testPushMsgToRuleEngineWithTenantIdIsNullUuidAndEntityIsTenantUseQueueFromMsgIsTrue() {
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> tbREQueueProducer = mock(TbQueueProducer.class);
        TbQueueCallback callback = mock(TbQueueCallback.class);

        TenantId tenantId = TenantId.fromUUID(UUID.fromString("3c8bd350-1239-4a3b-b9c3-4dd76f8e20f1"));
        TbMsg requestMsg = TbMsg.newMsg()
                .queueName(DataConstants.HP_QUEUE_NAME)
                .type(TbMsgType.REST_API_REQUEST)
                .originator(tenantId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        when(producerProvider.getRuleEngineMsgProducer()).thenReturn(tbREQueueProducer);

        clusterService.pushMsgToRuleEngine(TenantId.SYS_TENANT_ID, tenantId, requestMsg, true, callback);

        verify(producerProvider).getRuleEngineMsgProducer();
        verify(ruleEngineProducerService).sendToRuleEngine(tbREQueueProducer, tenantId, requestMsg, callback);
    }

    @Test
    public void testPushMsgToRuleEngineWithTenantIdIsNullUuidAndEntityIsDevice() {
        TenantId tenantId = TenantId.SYS_TENANT_ID;
        DeviceId deviceId = new DeviceId(UUID.fromString("aa6d112d-2914-4a22-a9e3-bee33edbdb14"));
        TbMsg requestMsg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        TbQueueCallback callback = mock(TbQueueCallback.class);

        clusterService.pushMsgToRuleEngine(tenantId, deviceId, requestMsg, false, callback);

        verifyNoMoreInteractions(partitionService, producerProvider);
    }

    @Test
    public void testPushMsgToRuleEngineWithTenantIdIsNotNullUuidUseQueueFromMsgIsTrue() {
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> tbREQueueProducer = mock(TbQueueProducer.class);
        TbQueueCallback callback = mock(TbQueueCallback.class);

        TenantId tenantId = TenantId.fromUUID(UUID.fromString("3c8bd350-1239-4a3b-b9c3-4dd76f8e20f1"));
        DeviceId deviceId = new DeviceId(UUID.fromString("adbb9d41-3367-40fd-9e74-7dd7cc5d30cf"));
        DeviceProfile deviceProfile = new DeviceProfile(new DeviceProfileId(UUID.fromString("552f5d6d-0b2b-43e1-a7d2-a51cb2a96927")));
        TbMsg requestMsg = TbMsg.newMsg()
                .queueName(DataConstants.HP_QUEUE_NAME)
                .type(TbMsgType.REST_API_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        when(deviceProfileCache.get(any(TenantId.class), any(DeviceId.class))).thenReturn(deviceProfile);
        when(producerProvider.getRuleEngineMsgProducer()).thenReturn(tbREQueueProducer);

        clusterService.pushMsgToRuleEngine(tenantId, deviceId, requestMsg, true, callback);

        verify(producerProvider).getRuleEngineMsgProducer();
        verify(ruleEngineProducerService).sendToRuleEngine(tbREQueueProducer, tenantId, requestMsg, callback);
    }

    @Test
    public void testPushMsgToRuleEngineUseQueueFromMsgIsFalse() {
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> tbREQueueProducer = mock(TbQueueProducer.class);
        TbQueueCallback callback = mock(TbQueueCallback.class);

        TenantId tenantId = TenantId.fromUUID(UUID.fromString("5377c8d0-26e5-4d81-84c6-4344043973c8"));
        DeviceId deviceId = new DeviceId(UUID.fromString("016c2abb-f46f-49f9-a83d-4d28b803cfe6"));
        DeviceProfile deviceProfile = new DeviceProfile(new DeviceProfileId(UUID.fromString("dc5766e2-1a32-4022-859b-743050097ab7")));
        deviceProfile.setDefaultQueueName(DataConstants.MAIN_QUEUE_NAME);
        TbMsg requestMsg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        when(deviceProfileCache.get(any(TenantId.class), any(DeviceId.class))).thenReturn(deviceProfile);
        when(producerProvider.getRuleEngineMsgProducer()).thenReturn(tbREQueueProducer);

        clusterService.pushMsgToRuleEngine(tenantId, deviceId, requestMsg, false, callback);

        verify(producerProvider).getRuleEngineMsgProducer();
        TbMsg expectedMsg = requestMsg.transform(DataConstants.MAIN_QUEUE_NAME);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineProducerService).sendToRuleEngine(eq(tbREQueueProducer), eq(tenantId), actualMsg.capture(), eq(callback));
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    protected Queue createTestQueue() {
        TenantId tenantId = TenantId.SYS_TENANT_ID;
        Queue queue = new Queue(new QueueId(UUID.randomUUID()));
        queue.setTenantId(tenantId);
        queue.setName(DataConstants.MAIN_QUEUE_NAME);
        queue.setTopic("main");
        queue.setPartitions(10);
        return queue;
    }

    @Test
    public void testGetRuleEngineProfileForUpdatedAndDeletedDevice() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TenantId tenantId = new TenantId(UUID.randomUUID());
        DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());

        Device device = new Device(deviceId);
        device.setDeviceProfileId(deviceProfileId);

        // device updated
        TbMsg tbMsg = TbMsg.newMsg().type(TbMsgType.ENTITY_UPDATED).build();
        ((DefaultTbClusterService) clusterService).getRuleEngineProfileForEntityOrElseNull(tenantId, deviceId, tbMsg);
        verify(deviceProfileCache, times(1)).get(tenantId, deviceId);

        // device deleted
        tbMsg = TbMsg.newMsg().type(TbMsgType.ENTITY_DELETED).data(JacksonUtil.toString(device)).build();
        ((DefaultTbClusterService) clusterService).getRuleEngineProfileForEntityOrElseNull(tenantId, deviceId, tbMsg);
        verify(deviceProfileCache, times(1)).get(tenantId, deviceProfileId);
    }

    @Test
    public void testGetRuleEngineProfileForUpdatedAndDeletedAsset() {
        AssetId assetId = new AssetId(UUID.randomUUID());
        TenantId tenantId = new TenantId(UUID.randomUUID());
        AssetProfileId assetProfileId = new AssetProfileId(UUID.randomUUID());

        Asset asset = new Asset(assetId);
        asset.setAssetProfileId(assetProfileId);

        // asset updated
        TbMsg tbMsg = TbMsg.newMsg().type(TbMsgType.ENTITY_UPDATED).build();
        ((DefaultTbClusterService) clusterService).getRuleEngineProfileForEntityOrElseNull(tenantId, assetId, tbMsg);
        verify(assetProfileCache, times(1)).get(tenantId, assetId);

        // asset deleted
        tbMsg = TbMsg.newMsg().type(TbMsgType.ENTITY_DELETED).data(JacksonUtil.toString(asset)).build();
        ((DefaultTbClusterService) clusterService).getRuleEngineProfileForEntityOrElseNull(tenantId, assetId, tbMsg);
        verify(assetProfileCache, times(1)).get(tenantId, assetProfileId);
    }

}
