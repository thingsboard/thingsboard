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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

}
