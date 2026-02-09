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
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;

import java.util.Collections;
import java.util.List;

import static org.eclipse.leshan.core.LwM2m.Version.V1_0;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.SINGLE;

@Data
@Schema
public class Lwm2mDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    private static final long serialVersionUID = 6257277825459600068L;

    @Schema(description = "Configuration for mapping LwM2M resources to telemetry and attributes")
    private TelemetryMappingConfiguration observeAttr;
    @Schema(description = "Flag indicating whether LwM2M bootstrap server update is enabled")
    private boolean bootstrapServerUpdateEnable;
    @ArraySchema(schema = @Schema(implementation = LwM2MBootstrapServerCredential.class))
    private List<LwM2MBootstrapServerCredential> bootstrap;
    @Schema(description = "Other LwM2M client settings")
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
