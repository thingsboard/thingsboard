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
package org.thingsboard.server.service.edge.rpc.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;

import java.util.Optional;
import java.util.UUID;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class BaseEdgeProcessorTest {

    private final BaseEdgeProcessor processor = new BaseEdgeProcessor() {
        @Override
        public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto msg) {
            return Futures.immediateFuture(null);
        }
    };

    @Test
    public void dashboardSaveRequiredWhenConfigurationChanges() {
        DashboardId dashboardId = new DashboardId(UUID.randomUUID());
        Dashboard current = new Dashboard(dashboardId);
        current.setTitle("Dashboard");
        current.setConfiguration(JacksonUtil.toJsonNode("{\"widgets\":{\"w1\":{\"type\":\"timeseries\"}}}"));

        Dashboard updated = new Dashboard(current);
        updated.setConfiguration(JacksonUtil.toJsonNode("{\"widgets\":{\"w1\":{\"type\":\"latest\"}}}"));

        assertThat(processor.isSaveRequired(current, updated)).isTrue();
    }

    @Test
    public void dashboardSaveNotRequiredWhenOnlyVersionChanges() {
        DashboardId dashboardId = new DashboardId(UUID.randomUUID());
        Dashboard current = new Dashboard(dashboardId);
        current.setTitle("Dashboard");
        current.setConfiguration(JacksonUtil.toJsonNode("{\"widgets\":{\"w1\":{\"type\":\"timeseries\"}}}"));
        current.setVersion(1L);

        Dashboard updated = new Dashboard(current);
        updated.setVersion(2L);

        assertThat(processor.isSaveRequired(current, updated)).isFalse();
    }

    @Test
    public void dashboardSaveNotRequiredWhenIdentical() {
        DashboardId dashboardId = new DashboardId(UUID.randomUUID());
        Dashboard current = new Dashboard(dashboardId);
        current.setTitle("Dashboard");
        current.setConfiguration(JacksonUtil.toJsonNode("{\"widgets\":{\"w1\":{\"type\":\"timeseries\"}}}"));

        Dashboard updated = new Dashboard(current);

        assertThat(processor.isSaveRequired(current, updated)).isFalse();
    }

    @Test
    public void deviceSaveRequiredWhenDeviceDataChanges() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device current = new Device(deviceId);
        current.setName("Device");
        current.setDeviceData(deviceDataWithTransport());

        Device updated = new Device(current);
        DeviceData withoutTransport = new DeviceData();
        withoutTransport.setConfiguration(new DefaultDeviceConfiguration());
        updated.setDeviceData(withoutTransport);

        assertThat(processor.isSaveRequired(current, updated)).isTrue();
    }

    @Test
    public void deviceSaveRequiredWhenAdditionalInfoChanges() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device current = new Device(deviceId);
        current.setName("Device");
        current.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"old\"}"));

        Device updated = new Device(current);
        updated.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"new\"}"));

        assertThat(processor.isSaveRequired(current, updated)).isTrue();
    }

    @Test
    public void deviceSaveNotRequiredWhenOnlyVersionChanges() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device current = new Device(deviceId);
        current.setName("Device");
        current.setDeviceData(deviceDataWithTransport());
        current.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"same\"}"));
        current.setVersion(1L);

        Device updated = new Device(current);
        updated.setVersion(2L);

        assertThat(processor.isSaveRequired(current, updated)).isFalse();
    }

    @Test
    public void assetSaveRequiredWhenAdditionalInfoChanges() {
        AssetId assetId = new AssetId(UUID.randomUUID());
        Asset current = new Asset(assetId);
        current.setName("Asset");
        current.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"old\"}"));

        Asset updated = new Asset(current);
        updated.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"new\"}"));

        assertThat(processor.isSaveRequired(current, updated)).isTrue();
    }

    @Test
    public void assetSaveNotRequiredWhenOnlyVersionChanges() {
        AssetId assetId = new AssetId(UUID.randomUUID());
        Asset current = new Asset(assetId);
        current.setName("Asset");
        current.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"same\"}"));
        current.setVersion(1L);

        Asset updated = new Asset(current);
        updated.setVersion(2L);

        assertThat(processor.isSaveRequired(current, updated)).isFalse();
    }

    @Test
    public void entityViewSaveRequiredWhenAdditionalInfoChanges() {
        EntityViewId entityViewId = new EntityViewId(UUID.randomUUID());
        EntityView current = new EntityView(entityViewId);
        current.setName("EntityView");
        current.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"old\"}"));

        EntityView updated = new EntityView(current);
        updated.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"new\"}"));

        assertThat(processor.isSaveRequired(current, updated)).isTrue();
    }

    @Test
    public void entityViewSaveNotRequiredWhenOnlyVersionChanges() {
        EntityViewId entityViewId = new EntityViewId(UUID.randomUUID());
        EntityView current = new EntityView(entityViewId);
        current.setName("EntityView");
        current.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"same\"}"));
        current.setVersion(1L);

        EntityView updated = new EntityView(current);
        updated.setVersion(2L);

        assertThat(processor.isSaveRequired(current, updated)).isFalse();
    }

    @Test
    public void userSaveRequiredWhenAdditionalInfoChanges() {
        UserId userId = new UserId(UUID.randomUUID());
        User current = new User(userId);
        current.setEmail("user@thingsboard.io");
        current.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"old\"}"));

        User updated = new User(current);
        updated.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"new\"}"));

        assertThat(processor.isSaveRequired(current, updated)).isTrue();
    }

    @Test
    public void userSaveNotRequiredWhenOnlyVersionChanges() {
        UserId userId = new UserId(UUID.randomUUID());
        User current = new User(userId);
        current.setEmail("user@thingsboard.io");
        current.setAdditionalInfo(JacksonUtil.toJsonNode("{\"description\":\"same\"}"));
        current.setVersion(1L);

        User updated = new User(current);
        updated.setVersion(2L);

        assertThat(processor.isSaveRequired(current, updated)).isFalse();
    }

    private static DeviceData deviceDataWithTransport() {
        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        return deviceData;
    }


    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("a00ec470-c6b4-11ef-8c88-63b5533fb5bc"));
    final EdgeId edgeId = new EdgeId(UUID.fromString("d1f0e6a0-53e1-11ee-883e-e56b48fd2088"));
    final DeviceId gatewayDeviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");

    @Mock
    EdgeContextComponent edgeCtx;
    @Mock
    AttributesService attributesService;
    @Mock
    EdgeEventService edgeEventService;
    @Mock
    DbCallbackExecutorService dbCallbackExecutorService;

    @BeforeEach
    void setupEdgeEventMocks() {
        ReflectionTestUtils.setField(processor, "edgeCtx", edgeCtx);
        ReflectionTestUtils.setField(processor, "dbCallbackExecutorService", dbCallbackExecutorService);
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(dbCallbackExecutorService).execute(any());
        lenient().when(edgeCtx.getAttributesService()).thenReturn(attributesService);
        lenient().when(edgeCtx.getEdgeEventService()).thenReturn(edgeEventService);
        lenient().when(edgeEventService.saveAsync(any())).thenReturn(immediateFuture(null));
    }

    void givenEdgeIsOffline() {
        AttributeKvEntry active = new BaseAttributeKvEntry(new BooleanDataEntry(DefaultDeviceStateService.ACTIVITY_STATE, false), 100L);
        given(attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, DefaultDeviceStateService.ACTIVITY_STATE))
                .willReturn(immediateFuture(Optional.of(active)));
    }

    JsonNode attributesBody() {
        return JacksonUtil.toJsonNode("{\"kv\":{\"Modbus\":\"{}\"},\"ts\":100,\"scope\":\"SHARED_SCOPE\"}");
    }

    @ParameterizedTest
    @EnumSource(value = EdgeEventActionType.class, names = {"ATTRIBUTES_UPDATED", "ATTRIBUTES_DELETED"})
    public void shouldSaveDeviceAttributesEventWhileEdgeIsOffline(EdgeEventActionType action) throws Exception {
        // GIVEN
        givenEdgeIsOffline();

        // WHEN
        processor.saveEdgeEvent(tenantId, edgeId, EdgeEventType.DEVICE, action, gatewayDeviceId, attributesBody()).get();

        // THEN
        ArgumentCaptor<EdgeEvent> captor = ArgumentCaptor.forClass(EdgeEvent.class);
        then(edgeEventService).should().saveAsync(captor.capture());

        EdgeEvent edgeEvent = captor.getValue();
        assertThat(edgeEvent.getEdgeId()).isEqualTo(edgeId);
        assertThat(edgeEvent.getType()).isEqualTo(EdgeEventType.DEVICE);
        assertThat(edgeEvent.getAction()).isEqualTo(action);
        assertThat(edgeEvent.getEntityId()).isEqualTo(gatewayDeviceId.getId());
        assertThat(edgeEvent.getBody()).isEqualTo(attributesBody());
    }

    @ParameterizedTest
    @EnumSource(value = EdgeEventActionType.class, names = {"ATTRIBUTES_UPDATED", "ATTRIBUTES_DELETED"})
    public void shouldNotSaveAttributesEventOfOtherEntityTypesWhileEdgeIsOffline(EdgeEventActionType action) throws Exception {
        // GIVEN
        givenEdgeIsOffline();
        var assetId = new AssetId(UUID.randomUUID());

        // WHEN
        processor.saveEdgeEvent(tenantId, edgeId, EdgeEventType.ASSET, action, assetId, attributesBody()).get();

        // THEN
        then(edgeEventService).should(never()).saveAsync(any());
    }

    @Test
    public void shouldNotSaveDeviceUpdatedEventWhileEdgeIsOffline() throws Exception {
        // GIVEN
        givenEdgeIsOffline();

        // WHEN
        processor.saveEdgeEvent(tenantId, edgeId, EdgeEventType.DEVICE, EdgeEventActionType.UPDATED, gatewayDeviceId, null).get();

        // THEN
        then(edgeEventService).should(never()).saveAsync(any());
    }

    @Test
    public void shouldNotSaveDeviceAttributesEventWhenEdgeWasNeverActivated() throws Exception {
        // GIVEN
        given(attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, DefaultDeviceStateService.ACTIVITY_STATE))
                .willReturn(immediateFuture(Optional.empty()));

        // WHEN
        processor.saveEdgeEvent(tenantId, edgeId, EdgeEventType.DEVICE, EdgeEventActionType.ATTRIBUTES_UPDATED, gatewayDeviceId, attributesBody()).get();

        // THEN
        then(edgeEventService).should(never()).saveAsync(any());
    }

    @Test
    public void shouldSaveDeviceAttributesEventWhileEdgeIsActive() throws Exception {
        // GIVEN
        AttributeKvEntry active = new BaseAttributeKvEntry(new BooleanDataEntry(DefaultDeviceStateService.ACTIVITY_STATE, true), 100L);
        given(attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, DefaultDeviceStateService.ACTIVITY_STATE))
                .willReturn(immediateFuture(Optional.of(active)));

        // WHEN
        processor.saveEdgeEvent(tenantId, edgeId, EdgeEventType.DEVICE, EdgeEventActionType.ATTRIBUTES_UPDATED, gatewayDeviceId, attributesBody()).get();

        // THEN
        then(edgeEventService).should().saveAsync(any());
    }

}
