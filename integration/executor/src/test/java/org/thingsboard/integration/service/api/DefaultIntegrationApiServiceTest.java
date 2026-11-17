// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.api;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.AssetProfileCacheInfo;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvListProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.TbRuleEngineProducerService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.cache.IntegrationExecutorAssetCache;
import org.thingsboard.server.service.cache.IntegrationExecutorAssetProfileCache;
import org.thingsboard.server.service.cache.IntegrationExecutorDeviceCache;
import org.thingsboard.server.service.cache.IntegrationExecutorDeviceProfileCache;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultIntegrationApiServiceTest {

    private static final long TIMEOUT_MS = 5000;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final Integration integration = new Integration(new IntegrationId(UUID.randomUUID()));

    private TbRuleEngineProducerService ruleEngineProducerService;
    private IntegrationExecutorDeviceProfileCache deviceProfileCache;
    private IntegrationExecutorAssetProfileCache assetProfileCache;
    private IntegrationExecutorDeviceCache deviceCache;
    private IntegrationExecutorAssetCache assetCache;
    private DefaultIntegrationApiService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        integration.setTenantId(tenantId);

        StatsFactory statsFactory = mock(StatsFactory.class);
        when(statsFactory.createMessagesStats(any())).thenReturn(mock(MessagesStats.class));
        PartitionService partitionService = mock(PartitionService.class);
        when(partitionService.resolve(any(), any(TenantId.class), any())).thenReturn(new TopicPartitionInfo("tb_core", tenantId, 0, true));
        TbQueueProducerProvider producerProvider = mock(TbQueueProducerProvider.class);
        TbQueueProducer<TbProtoQueueMsg<ToCoreIntegrationMsg>> coreProducer = mock(TbQueueProducer.class);
        when(producerProvider.getTbCoreIntegrationMsgProducer()).thenReturn(coreProducer);
        when(producerProvider.getRuleEngineMsgProducer()).thenReturn(mock(TbQueueProducer.class));

        ruleEngineProducerService = mock(TbRuleEngineProducerService.class);
        doAnswer(invocation -> {
            TbQueueCallback callback = invocation.getArgument(3);
            callback.onSuccess(null);
            return null;
        }).when(ruleEngineProducerService).sendToRuleEngine(any(), any(), any(), any());

        deviceProfileCache = mock(IntegrationExecutorDeviceProfileCache.class);
        assetProfileCache = mock(IntegrationExecutorAssetProfileCache.class);
        deviceCache = mock(IntegrationExecutorDeviceCache.class);
        assetCache = mock(IntegrationExecutorAssetCache.class);

        service = new DefaultIntegrationApiService(statsFactory, partitionService, producerProvider, ruleEngineProducerService,
                mock(TbApiUsageReportClient.class), deviceProfileCache, assetProfileCache, deviceCache, assetCache,
                (TbQueueRequestTemplate<TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>>) mock(TbQueueRequestTemplate.class));
        ReflectionTestUtils.setField(service, "maxCallbackThreads", 1);
        service.init();
    }

    @AfterEach
    public void tearDown() {
        service.destroy();
    }

    @Test
    public void deviceUplinkPassesTelemetryAndAttributeTypeCastDisabledKeysToTheirOwnMessages() {
        DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());
        when(deviceCache.getEntity(any(), any())).thenReturn(Futures.immediateFuture(
                new DeviceCacheInfo(UUID.randomUUID(), tenantId, null, "Device A", "default", deviceProfileId)));
        when(deviceProfileCache.getProfile(eq(deviceProfileId.getId()), any())).thenReturn(Futures.immediateFuture(
                new DeviceProfileCacheInfo(deviceProfileId.getId(), tenantId, "default", null, "Main")));

        DeviceUplinkDataProto data = DeviceUplinkDataProto.newBuilder()
                .setDeviceName("Device A")
                .setDeviceType("default")
                .setPostTelemetryMsg(telemetry())
                .setPostAttributesMsg(attributes())
                .addAllTelemetryTypeCastDisabledKeys(List.of("devAddr"))
                .addAllAttributesTypeCastDisabledKeys(List.of("joinEui", "firmwareVersion"))
                .build();
        IntegrationCallback<Void> callback = mock(IntegrationCallback.class);

        service.sendUplinkData(integration, IntegrationInfoProto.getDefaultInstance(), data, callback);

        Map<TbMsgType, TbMsg> msgs = captureRuleEngineMsgs();
        assertThat(msgs.get(TbMsgType.POST_TELEMETRY_REQUEST).getTypeCastDisabledKeys()).isEqualTo(Set.of("devAddr"));
        assertThat(msgs.get(TbMsgType.POST_ATTRIBUTES_REQUEST).getTypeCastDisabledKeys()).isEqualTo(Set.of("joinEui", "firmwareVersion"));
        verify(callback, timeout(TIMEOUT_MS)).onSuccess(null);
    }

    @Test
    public void assetUplinkPassesTelemetryAndAttributeTypeCastDisabledKeysToTheirOwnMessages() {
        AssetProfileId assetProfileId = new AssetProfileId(UUID.randomUUID());
        when(assetCache.getEntity(any(), any())).thenReturn(Futures.immediateFuture(
                new AssetCacheInfo(UUID.randomUUID(), tenantId, null, "Asset A", "default", assetProfileId)));
        when(assetProfileCache.getProfile(eq(assetProfileId.getId()), any())).thenReturn(Futures.immediateFuture(
                new AssetProfileCacheInfo(assetProfileId.getId(), tenantId, "default", null, "Main")));

        AssetUplinkDataProto data = AssetUplinkDataProto.newBuilder()
                .setAssetName("Asset A")
                .setAssetType("default")
                .setPostTelemetryMsg(telemetry())
                .setPostAttributesMsg(attributes())
                .addAllTelemetryTypeCastDisabledKeys(List.of("devAddr"))
                .addAllAttributesTypeCastDisabledKeys(List.of("joinEui", "firmwareVersion"))
                .build();
        IntegrationCallback<Void> callback = mock(IntegrationCallback.class);

        service.sendUplinkData(integration, IntegrationInfoProto.getDefaultInstance(), data, callback);

        Map<TbMsgType, TbMsg> msgs = captureRuleEngineMsgs();
        assertThat(msgs.get(TbMsgType.POST_TELEMETRY_REQUEST).getTypeCastDisabledKeys()).isEqualTo(Set.of("devAddr"));
        assertThat(msgs.get(TbMsgType.POST_ATTRIBUTES_REQUEST).getTypeCastDisabledKeys()).isEqualTo(Set.of("joinEui", "firmwareVersion"));
        verify(callback, timeout(TIMEOUT_MS)).onSuccess(null);
    }

    @Test
    public void uplinkWithoutTypeCastDisabledKeysAddsNoMetadata() {
        DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());
        when(deviceCache.getEntity(any(), any())).thenReturn(Futures.immediateFuture(
                new DeviceCacheInfo(UUID.randomUUID(), tenantId, null, "Device B", "default", deviceProfileId)));
        when(deviceProfileCache.getProfile(eq(deviceProfileId.getId()), any())).thenReturn(Futures.immediateFuture(
                new DeviceProfileCacheInfo(deviceProfileId.getId(), tenantId, "default", null, "Main")));

        DeviceUplinkDataProto data = DeviceUplinkDataProto.newBuilder()
                .setDeviceName("Device B")
                .setDeviceType("default")
                .setPostTelemetryMsg(telemetry())
                .setPostAttributesMsg(attributes())
                .build();

        service.sendUplinkData(integration, IntegrationInfoProto.getDefaultInstance(), data, mock(IntegrationCallback.class));

        Map<TbMsgType, TbMsg> msgs = captureRuleEngineMsgs();
        assertThat(msgs.get(TbMsgType.POST_TELEMETRY_REQUEST).getMetaData().getValue(TbMsgMetaData.TYPE_CAST_DISABLED_KEYS)).isNull();
        assertThat(msgs.get(TbMsgType.POST_ATTRIBUTES_REQUEST).getMetaData().getValue(TbMsgMetaData.TYPE_CAST_DISABLED_KEYS)).isNull();
    }

    private Map<TbMsgType, TbMsg> captureRuleEngineMsgs() {
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineProducerService, timeout(TIMEOUT_MS).times(2)).sendToRuleEngine(any(), eq(tenantId), captor.capture(), any());
        return captor.getAllValues().stream().collect(Collectors.toMap(TbMsg::getInternalType, Function.identity()));
    }

    private static PostTelemetryMsg telemetry() {
        return PostTelemetryMsg.newBuilder()
                .addTsKvList(TsKvListProto.newBuilder()
                        .setTs(1000L)
                        .addKv(stringKv("devAddr", "01020304")))
                .build();
    }

    private static PostAttributeMsg attributes() {
        return PostAttributeMsg.newBuilder()
                .addKv(stringKv("joinEui", "0000000000000000"))
                .addKv(stringKv("firmwareVersion", "1.0"))
                .build();
    }

    private static KeyValueProto stringKv(String key, String value) {
        return KeyValueProto.newBuilder().setKey(key).setType(KeyValueType.STRING_V).setStringV(value).build();
    }

}
