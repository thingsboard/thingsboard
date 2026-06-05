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
package org.thingsboard.server.transport.lwm2m.ota.sql;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.transport.lwm2m.ota.AbstractOtaLwM2MIntegrationTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.INITIATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.QUEUED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

@Slf4j
public class Ota9LwM2MIntegrationTest extends AbstractOtaLwM2MIntegrationTest {

    /**
     * => Start -> INITIAL (State=0) -> DOWNLOAD STARTED;
     * => PKG / URI Write -> DOWNLOAD STARTED (Res=1 (Downloading) && State=1) -> DOWNLOADED
     * => PKG Written -> DOWNLOADED (Res=1 Initial && State=2) -> DELIVERED;
     * => PKG integrity verified -> DELIVERED  (Res=3 (Successfully Downloaded and package integrity verified) && State=3) -> INSTALLED;
     * => Install -> INSTALLED (Res=2 SW successfully installed) && State=4) -> Start
     *
     *
     */
    @Test
    public void testSoftwareUpdateByObject9() throws Exception {
        String clientEndpoint = this.CLIENT_ENDPOINT_OTA9;
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA9, getBootstrapServerCredentialsNoSec(NONE));
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + clientEndpoint, transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        final Device device = createLwm2mDevice(deviceCredentials, clientEndpoint, deviceProfile.getId());
        createNewClient(SECURITY_NO_SEC, null, false, clientEndpoint, device.getId().getId().toString());
        awaitObserveReadAll(4, device.getId().getId().toString());

        device.setSoftwareId(createSoftware(deviceProfile.getId(), "v1.0").getId());
        final Device savedDevice = doPost("/api/device", device, Device.class); //sync call

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        expectedStatuses = List.of(
                QUEUED, INITIATED, DOWNLOADING, DOWNLOADING, DOWNLOADING, DOWNLOADED, VERIFIED, UPDATED);
        List<TsKvEntry> ts = await("await on timeseries")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getFwSwStateTelemetryFromAPI(device.getId().getId(), "sw_state"), this::predicateForStatuses);
        log.warn("Object9: Got the ts: {}", ts);
    }
}
