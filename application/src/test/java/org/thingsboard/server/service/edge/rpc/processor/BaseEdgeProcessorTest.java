/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
