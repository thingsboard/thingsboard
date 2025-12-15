/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.transport.lwm2m.ota.AbstractOtaLwM2MIntegrationTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.rest.client.utils.RestJsonConverter.toTimeseries;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.INITIATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.QUEUED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.thingsboard.server.dao.service.OtaPackageServiceTest.TARGET_FW_VERSION;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

@Slf4j
public class Ota5LwM2MIntegrationTest extends AbstractOtaLwM2MIntegrationTest {

    @Test
    public void testFirmwareUpdateWithClientWithoutFirmwareOtaInfoFromProfile_IsNotSupported() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(TELEMETRY_WITHOUT_OBSERVE, getBootstrapServerCredentialsNoSec(NONE));
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + this.CLIENT_ENDPOINT_WITHOUT_FW_INFO, transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(this.CLIENT_ENDPOINT_WITHOUT_FW_INFO));
        final Device device = createLwm2mDevice(deviceCredentials, this.CLIENT_ENDPOINT_WITHOUT_FW_INFO, deviceProfile.getId());
        createNewClient(SECURITY_NO_SEC, null, false, this.CLIENT_ENDPOINT_WITHOUT_FW_INFO, device.getId().getId().toString());
        awaitObserveReadAll(0, device.getId().getId().toString());

        device.setFirmwareId(createFirmware("5.1", deviceProfile.getId()).getId());
        final Device savedDevice = doPost("/api/device", device, Device.class);

        Thread.sleep(1000);

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        List<TsKvEntry> ts = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" +
                savedDevice.getId().getId() + "/values/timeseries?keys=fw_state", new TypeReference<>() {}));
        List<OtaPackageUpdateStatus> statuses = ts.stream().map(KvEntry::getValueAsString).map(OtaPackageUpdateStatus::valueOf).collect(Collectors.toList());
        List<OtaPackageUpdateStatus> expectedStatuses = Collections.singletonList(FAILED);

        Assert.assertEquals(expectedStatuses, statuses);
    }

    /**
     * /5/0/5 -> Update Result (Res); 5/0/3 -> State;
     * => ((Res>=0 && Res<=9) &&  State=0)
     * => Write to Package/Write to Package URI -> DOWNLOADING ((Res>=0 && Res<=9) && State=1)
     * => Download Finished -> DOWNLOADED ((Res==0 || Res=8) && State=2)
     * => Executable resource Update is triggered / Initiate Firmware Update -> UPDATING (Res=0 && State=3)
     * => Update Successful  [Res==1]
     * => Start / Res=0 -> "IDLE" ....
     * @throws Exception
     */
    @Test
    public void testFirmwareUpdateByObject5_Ok() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA5, getBootstrapServerCredentialsNoSec(NONE));
        DeviceProfile deviceProfile =  createLwm2mDeviceProfile("profileFor" + this.CLIENT_ENDPOINT_OTA5 + "Ok", transportConfiguration);
        String endpoint = this.CLIENT_ENDPOINT_OTA5 + "Ok";
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(endpoint));
        final Device device = createLwm2mDevice(deviceCredentials, endpoint, deviceProfile.getId());
        createNewClient(SECURITY_NO_SEC, null, false, endpoint, device.getId().getId().toString());
        awaitObserveReadAll(5, device.getId().getId().toString());

        device.setFirmwareId(createFirmware(TARGET_FW_VERSION, deviceProfile.getId()).getId());
        final Device savedDevice = doPost("/api/device", device, Device.class);

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        expectedStatuses = Arrays.asList(QUEUED, INITIATED, DOWNLOADING, DOWNLOADED, UPDATING, UPDATED);
        List<TsKvEntry> ts = await("await on timeseries for FW")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getFwSwStateTelemetryFromAPI(device.getId().getId(), "fw_state"), this::predicateForStatuses);
        log.warn("Object5: Got the ts: {}", ts);
    }
}
