// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.session;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Optional;
import java.util.UUID;

@Data
public abstract class DeviceAwareSessionContext implements SessionContext {

    @Getter
    protected final UUID sessionId;
    @Getter
    private volatile DeviceId deviceId;
    @Getter
    private volatile TenantId tenantId;
    @Getter
    protected volatile TransportDeviceInfo deviceInfo;
    @Getter
    @Setter
    protected volatile DeviceProfile deviceProfile;
    @Getter
    @Setter
    protected volatile TransportProtos.SessionInfoProto sessionInfo;

    @Setter
    private volatile boolean connected;

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void setDeviceInfo(TransportDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        this.deviceId = deviceInfo.getDeviceId();
        this.tenantId = deviceInfo.getTenantId();
    }

    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        this.sessionInfo = sessionInfo;
        this.deviceProfile = deviceProfile;
        this.deviceInfo.setDeviceType(deviceProfile.getName());
    }

    @Override
    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        this.sessionInfo = sessionInfo;
        this.deviceInfo.setDeviceProfileId(device.getDeviceProfileId());
        this.deviceInfo.setDeviceType(device.getType());
        deviceProfileOpt.ifPresent(profile -> this.deviceProfile = profile);
    }

    public boolean isConnected() {
        return connected;
    }

    public void setDisconnected() {
        this.connected = false;
    }

    public boolean isSparkplug() {
        DeviceProfileTransportConfiguration transportConfiguration = this.deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
            return ((MqttDeviceProfileTransportConfiguration) transportConfiguration).isSparkplug();
        } else {
            return false;
        }
    }

}
