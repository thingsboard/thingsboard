// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.AbstractIntegration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceActivityProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.TbRuleEngineProducerService;
import org.thingsboard.server.service.profile.DefaultTbAssetProfileCache;
import org.thingsboard.server.service.profile.DefaultTbDeviceProfileCache;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultPlatformIntegrationServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final DeviceId DEVICE_ID = DeviceId.fromString("1d288a06-9b26-11ee-b9d1-0242ac120002");
    private static final AssetId ASSET_ID = AssetId.fromString("2d288a06-9b26-11ee-b9d1-0242ac120002");
    private static final CustomerId CUSTOMER_ID = new CustomerId(UUID.fromString("3d288a06-9b26-11ee-b9d1-0242ac120002"));

    @Mock
    private DeviceService deviceService;

    @Mock
    private AssetService assetService;

    @Mock
    private CustomerService customerService;

    @Mock
    private RelationService relationService;

    @Mock
    private DefaultTbDeviceProfileCache deviceProfileCache;

    @Mock
    private DefaultTbAssetProfileCache assetProfileCache;

    @Mock
    private TbRuleEngineProducerService ruleEngineProducerService;

    @Mock
    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> integrationRuleEngineMsgProducer;

    @Mock
    private ExecutorService callbackExecutor;

    private DefaultPlatformIntegrationService service;

    @BeforeEach
    public void setUp() {
        service = spy(new DefaultPlatformIntegrationService());

        ReflectionTestUtils.setField(service, "deviceService", deviceService);
        ReflectionTestUtils.setField(service, "assetService", assetService);
        ReflectionTestUtils.setField(service, "customerService", customerService);
        ReflectionTestUtils.setField(service, "relationService", relationService);
        ReflectionTestUtils.setField(service, "deviceProfileCache", deviceProfileCache);
        ReflectionTestUtils.setField(service, "assetProfileCache", assetProfileCache);
        ReflectionTestUtils.setField(service, "ruleEngineProducerService", ruleEngineProducerService);
        ReflectionTestUtils.setField(service, "integrationRuleEngineMsgProducer", integrationRuleEngineMsgProducer);
        ReflectionTestUtils.setField(service, "callbackExecutor", callbackExecutor);
    }

    @Test
    public void whenProcessingDeviceActivityData_thenCallOnActivity() {
        DeviceActivityProto data = DeviceActivityProto.newBuilder()
                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                .build();

        TbCallback tbCallback = mock(TbCallback.class);
        IntegrationApiCallback callback = new IntegrationApiCallback(tbCallback);

        doAnswer(_ -> {
            tbCallback.onSuccess();
            return null;
        }).when(service).onActivity(any(), any(), anyLong());

        service.processDeviceActivityData(data, callback);
        ArgumentCaptor<IntegrationActivityKey> keyCaptor = ArgumentCaptor.forClass(IntegrationActivityKey.class);
        verify(service).onActivity(keyCaptor.capture(), any(), any(Long.class));

        IntegrationActivityKey capturedKey = keyCaptor.getValue();
        assertThat(capturedKey.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(capturedKey.getDeviceId()).isEqualTo(DEVICE_ID);
    }

    @Test
    public void whenDeviceExists_thenReturnItWithoutCreation() {
        AbstractIntegration integration = mock(AbstractIntegration.class);
        when(integration.getTenantId()).thenReturn(TENANT_ID);

        String deviceName = "testDevice";
        String deviceType = "default";
        String label = "Test Device";
        String customerName = "Test Customer";
        String groupName = "Test Group";

        Device existingDevice = new Device();
        existingDevice.setId(DEVICE_ID);
        existingDevice.setName(deviceName);
        existingDevice.setType(deviceType);
        existingDevice.setTenantId(TENANT_ID);

        when(deviceService.findDeviceByTenantIdAndName(TENANT_ID, deviceName)).thenReturn(existingDevice);

        Device result = service.processGetOrCreateDevice(integration, deviceName, deviceType, label, customerName, groupName);
        assertThat(result).isSameAs(existingDevice);
        verify(deviceService, times(1)).findDeviceByTenantIdAndName(TENANT_ID, deviceName);
        verify(deviceService, never()).saveDevice(any(Device.class));
    }

    @Test
    public void whenDeviceDoesNotExist_thenCreateItAndProcessMsgToRuleEngine() {
        AbstractIntegration integration = mock(AbstractIntegration.class);
        when(integration.getTenantId()).thenReturn(TENANT_ID);
        when(integration.isAllowCreateDevicesOrAssets()).thenReturn(true);
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        when(integration.getId()).thenReturn(integrationId);
        when(integration.getName()).thenReturn("Test Integration");

        String deviceName = "testDevice";
        String deviceType = "default";
        String label = "Test Device";
        String customerName = "Test Customer";
        String groupName = null;

        when(deviceService.findDeviceByTenantIdAndName(TENANT_ID, deviceName)).thenReturn(null);

        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setTitle(customerName);
        customer.setTenantId(TENANT_ID);

        when(customerService.findCustomerByTenantIdAndTitle(TENANT_ID, customerName)).thenReturn(Optional.of(customer));

        Device savedDevice = new Device();
        savedDevice.setId(DEVICE_ID);
        savedDevice.setName(deviceName);
        savedDevice.setType(deviceType);
        savedDevice.setLabel(label);
        savedDevice.setTenantId(TENANT_ID);
        savedDevice.setCustomerId(CUSTOMER_ID);

        when(deviceService.saveDevice(any(Device.class))).thenReturn(savedDevice);

        doAnswer(_ -> null).when(service).onActivity(any(), any(), anyLong());
        when(deviceProfileCache.find(any())).thenReturn(null);
        doAnswer(invocation -> {
            TbQueueCallback callback = invocation.getArgument(3);
            callback.onSuccess(null);
            return null;
        }).when(ruleEngineProducerService).sendToRuleEngine(eq(integrationRuleEngineMsgProducer), any(TenantId.class), any(TbMsg.class), any(TbQueueCallback.class));

        Device result = service.processGetOrCreateDevice(integration, deviceName, deviceType, label, customerName, groupName);
        assertThat(result).isEqualTo(savedDevice);

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceService).saveDevice(deviceCaptor.capture());

        Device capturedDevice = deviceCaptor.getValue();
        assertThat(capturedDevice.getName()).isEqualTo(deviceName);
        assertThat(capturedDevice.getType()).isEqualTo(deviceType);
        assertThat(capturedDevice.getLabel()).isEqualTo(label);
        assertThat(capturedDevice.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(capturedDevice.getCustomerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    public void whenDeviceIsCreatedByIntegration_thenManagedByIntegrationRelationExists() {
        AbstractIntegration integration = mock(AbstractIntegration.class);
        when(integration.getTenantId()).thenReturn(TENANT_ID);
        when(integration.isAllowCreateDevicesOrAssets()).thenReturn(true);
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        when(integration.getId()).thenReturn(integrationId);
        when(integration.getName()).thenReturn("Test Integration");

        String deviceName = "testDevice";
        String deviceType = "default";
        String label = "Test Device";
        String customerName = null;
        String groupName = null;

        when(deviceService.findDeviceByTenantIdAndName(TENANT_ID, deviceName)).thenReturn(null);

        Device savedDevice = new Device();
        savedDevice.setId(DEVICE_ID);
        savedDevice.setName(deviceName);
        savedDevice.setType(deviceType);
        savedDevice.setLabel(label);
        savedDevice.setTenantId(TENANT_ID);

        when(deviceService.saveDevice(any(Device.class))).thenReturn(savedDevice);

        doAnswer(_ -> null).when(service).onActivity(any(), any(), anyLong());
        when(deviceProfileCache.find(any())).thenReturn(null);
        doAnswer(invocation -> {
            TbQueueCallback callback = invocation.getArgument(3);
            callback.onSuccess(null);
            return null;
        }).when(ruleEngineProducerService).sendToRuleEngine(eq(integrationRuleEngineMsgProducer), any(TenantId.class), any(TbMsg.class), any(TbQueueCallback.class));

        Device result = service.processGetOrCreateDevice(integration, deviceName, deviceType, label, customerName, groupName);
        assertThat(result).isEqualTo(savedDevice);

        ArgumentCaptor<EntityRelation> relationCaptor = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationService, times(1)).saveRelation(eq(TENANT_ID), relationCaptor.capture());

        EntityRelation capturedRelation = relationCaptor.getValue();
        assertThat(capturedRelation.getFrom()).isEqualTo(integrationId);
        assertThat(capturedRelation.getTo()).isEqualTo(DEVICE_ID);
        assertThat(capturedRelation.getType()).isEqualTo("ManagedByIntegration");
    }

    @Test
    public void whenAssetExists_thenReturnItWithoutCreation() {
        AbstractIntegration integration = mock(AbstractIntegration.class);
        when(integration.getTenantId()).thenReturn(TENANT_ID);

        String assetName = "testAsset";
        String assetType = "default";
        String label = "Test Asset";
        String customerName = "Test Customer";
        String groupName = "Test Group";

        Asset existingAsset = new Asset();
        existingAsset.setId(ASSET_ID);
        existingAsset.setName(assetName);
        existingAsset.setType(assetType);
        existingAsset.setTenantId(TENANT_ID);

        when(assetService.findAssetByTenantIdAndName(TENANT_ID, assetName)).thenReturn(existingAsset);

        Asset result = service.processGetOrCreateAsset(integration, assetName, assetType, label, customerName, groupName);
        assertThat(result).isSameAs(existingAsset);
        verify(assetService, times(1)).findAssetByTenantIdAndName(TENANT_ID, assetName);
        verify(assetService, never()).saveAsset(any(Asset.class));
    }

    @Test
    public void whenAssetDoesNotExist_thenCreateItAndReturnIt() {
        AbstractIntegration integration = mock(AbstractIntegration.class);
        when(integration.getTenantId()).thenReturn(TENANT_ID);
        when(integration.isAllowCreateDevicesOrAssets()).thenReturn(true);
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        when(integration.getId()).thenReturn(integrationId);
        when(integration.getName()).thenReturn("Test Integration");

        String assetName = "testAsset";
        String assetType = "default";
        String label = "Test Asset";
        String customerName = "Test Customer";
        String groupName = null;

        when(assetService.findAssetByTenantIdAndName(TENANT_ID, assetName)).thenReturn(null);

        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setTitle(customerName);
        customer.setTenantId(TENANT_ID);

        when(customerService.findCustomerByTenantIdAndTitle(TENANT_ID, customerName)).thenReturn(Optional.of(customer));

        Asset savedAsset = new Asset();
        savedAsset.setId(ASSET_ID);
        savedAsset.setName(assetName);
        savedAsset.setType(assetType);
        savedAsset.setLabel(label);
        savedAsset.setTenantId(TENANT_ID);
        savedAsset.setCustomerId(CUSTOMER_ID);

        when(assetService.saveAsset(any(Asset.class))).thenReturn(savedAsset);

        doAnswer(_ -> null).when(service).onActivity(any(), any(), anyLong());
        when(assetProfileCache.find(any())).thenReturn(null);
        doAnswer(invocation -> {
            TbQueueCallback callback = invocation.getArgument(3);
            callback.onSuccess(null);
            return null;
        }).when(ruleEngineProducerService).sendToRuleEngine(eq(integrationRuleEngineMsgProducer), any(TenantId.class), any(TbMsg.class), any(TbQueueCallback.class));

        Asset result = service.processGetOrCreateAsset(integration, assetName, assetType, label, customerName, groupName);
        assertThat(result).isEqualTo(savedAsset);

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetService).saveAsset(assetCaptor.capture());

        Asset capturedAsset = assetCaptor.getValue();
        assertThat(capturedAsset.getName()).isEqualTo(assetName);
        assertThat(capturedAsset.getType()).isEqualTo(assetType);
        assertThat(capturedAsset.getLabel()).isEqualTo(label);
        assertThat(capturedAsset.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(capturedAsset.getCustomerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    public void whenAssetIsCreatedByIntegration_thenManagedByIntegrationRelationExists() {
        AbstractIntegration integration = mock(AbstractIntegration.class);
        when(integration.getTenantId()).thenReturn(TENANT_ID);
        when(integration.isAllowCreateDevicesOrAssets()).thenReturn(true);
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        when(integration.getId()).thenReturn(integrationId);
        when(integration.getName()).thenReturn("Test Integration");

        String assetName = "testAsset";
        String assetType = "default";
        String label = "Test Asset";
        String customerName = null;
        String groupName = null;

        when(assetService.findAssetByTenantIdAndName(TENANT_ID, assetName)).thenReturn(null);

        Asset savedAsset = new Asset();
        savedAsset.setId(ASSET_ID);
        savedAsset.setName(assetName);
        savedAsset.setType(assetType);
        savedAsset.setLabel(label);
        savedAsset.setTenantId(TENANT_ID);

        when(assetService.saveAsset(any(Asset.class))).thenReturn(savedAsset);

        doAnswer(_ -> null).when(service).onActivity(any(), any(), anyLong());
        when(assetProfileCache.find(any())).thenReturn(null);
        doAnswer(invocation -> {
            TbQueueCallback callback = invocation.getArgument(3);
            callback.onSuccess(null);
            return null;
        }).when(ruleEngineProducerService).sendToRuleEngine(eq(integrationRuleEngineMsgProducer), any(TenantId.class), any(TbMsg.class), any(TbQueueCallback.class));

        Asset result = service.processGetOrCreateAsset(integration, assetName, assetType, label, customerName, groupName);
        assertThat(result).isEqualTo(savedAsset);

        ArgumentCaptor<EntityRelation> relationCaptor = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationService, times(1)).saveRelation(eq(TENANT_ID), relationCaptor.capture());

        EntityRelation capturedRelation = relationCaptor.getValue();
        assertThat(capturedRelation.getFrom()).isEqualTo(integrationId);
        assertThat(capturedRelation.getTo()).isEqualTo(ASSET_ID);
        assertThat(capturedRelation.getType()).isEqualTo("ManagedByIntegration");
    }

    @Test
    public void whenDeviceCreationForbidden_thenThrowPermissionDenied() {
        AbstractIntegration integration = mock(AbstractIntegration.class);
        when(integration.getTenantId()).thenReturn(TENANT_ID);
        when(integration.isAllowCreateDevicesOrAssets()).thenReturn(false);

        when(deviceService.findDeviceByTenantIdAndName(TENANT_ID, "newDevice")).thenReturn(null);

        assertThatThrownBy(() -> service.processGetOrCreateDevice(integration, "newDevice", "default", null, null, null))
                .isInstanceOf(ThingsboardRuntimeException.class)
                .hasFieldOrPropertyWithValue("errorCode", ThingsboardErrorCode.PERMISSION_DENIED);

        verify(deviceService, never()).saveDevice(any(Device.class));
    }

    @Test
    public void whenAssetCreationForbidden_thenThrowPermissionDenied() {
        AbstractIntegration integration = mock(AbstractIntegration.class);
        when(integration.getTenantId()).thenReturn(TENANT_ID);
        when(integration.isAllowCreateDevicesOrAssets()).thenReturn(false);

        when(assetService.findAssetByTenantIdAndName(TENANT_ID, "newAsset")).thenReturn(null);

        assertThatThrownBy(() -> service.processGetOrCreateAsset(integration, "newAsset", "default", null, null, null))
                .isInstanceOf(ThingsboardRuntimeException.class)
                .hasFieldOrPropertyWithValue("errorCode", ThingsboardErrorCode.PERMISSION_DENIED);

        verify(assetService, never()).saveAsset(any(Asset.class));
    }

    @Test
    public void whenProcessDeviceActivityDataThrows_thenCallbackOnError() {
        DeviceActivityProto data = DeviceActivityProto.newBuilder()
                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                .build();

        TbCallback tbCallback = mock(TbCallback.class);
        IntegrationApiCallback callback = new IntegrationApiCallback(tbCallback);

        RuntimeException exception = new RuntimeException("activity error");
        doThrow(exception).when(service).onActivity(any(), any(), anyLong());

        service.processDeviceActivityData(data, callback);

        verify(tbCallback).onFailure(exception);
        verify(tbCallback, never()).onSuccess();
    }

    @Test
    public void whenCustomerNotFound_thenCreateNewCustomer() {
        AbstractIntegration integration = mock(AbstractIntegration.class);
        when(integration.getTenantId()).thenReturn(TENANT_ID);
        when(integration.isAllowCreateDevicesOrAssets()).thenReturn(true);
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        when(integration.getId()).thenReturn(integrationId);
        when(integration.getName()).thenReturn("Test Integration");

        String deviceName = "testDevice";
        String deviceType = "default";
        String customerName = "New Customer";

        when(deviceService.findDeviceByTenantIdAndName(TENANT_ID, deviceName)).thenReturn(null);
        when(customerService.findCustomerByTenantIdAndTitle(TENANT_ID, customerName)).thenReturn(Optional.empty());

        Customer newCustomer = new Customer();
        newCustomer.setId(CUSTOMER_ID);
        newCustomer.setTitle(customerName);
        newCustomer.setTenantId(TENANT_ID);
        when(customerService.saveCustomer(any(Customer.class))).thenReturn(newCustomer);

        Device savedDevice = new Device();
        savedDevice.setId(DEVICE_ID);
        savedDevice.setName(deviceName);
        savedDevice.setType(deviceType);
        savedDevice.setTenantId(TENANT_ID);
        savedDevice.setCustomerId(CUSTOMER_ID);

        when(deviceService.saveDevice(any(Device.class))).thenReturn(savedDevice);
        doAnswer(_ -> null).when(service).onActivity(any(), any(), anyLong());
        when(deviceProfileCache.find(any())).thenReturn(null);
        doAnswer(invocation -> {
            TbQueueCallback cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(ruleEngineProducerService).sendToRuleEngine(eq(integrationRuleEngineMsgProducer), any(TenantId.class), any(TbMsg.class), any(TbQueueCallback.class));

        Device result = service.processGetOrCreateDevice(integration, deviceName, deviceType, null, customerName, null);

        assertThat(result).isEqualTo(savedDevice);
        verify(customerService).saveCustomer(any(Customer.class));
    }

}
