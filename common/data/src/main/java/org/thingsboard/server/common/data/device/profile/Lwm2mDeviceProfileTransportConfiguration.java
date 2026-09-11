// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;

import static org.eclipse.leshan.core.LwM2m.Version.V1_0;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.SINGLE;

import java.util.Collections;
import java.util.List;

@Data
public class Lwm2mDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    private static final long serialVersionUID = 6257277825459600068L;

    private TelemetryMappingConfiguration observeAttr;
    private boolean bootstrapServerUpdateEnable;
    private List<LwM2MBootstrapServerCredential> bootstrap;
    private OtherConfiguration clientLwM2mSettings;

    public Lwm2mDeviceProfileTransportConfiguration() {
        updateDefault();
    }

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.LWM2M;
    }

    private void updateDefault(){
        this.setBootstrap(Collections.emptyList());
        this.setClientLwM2mSettings(new OtherConfiguration(false,1, 1, 1, PowerMode.DRX, null, null, null, null, null, V1_0.toString()));
        this.setObserveAttr(new TelemetryMappingConfiguration(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyMap(), SINGLE));
    }
}
