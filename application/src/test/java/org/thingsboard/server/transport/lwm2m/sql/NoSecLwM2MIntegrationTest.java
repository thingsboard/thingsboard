/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredentials;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.thingsboard.rest.client.utils.RestJsonConverter.toTimeseries;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.INITIATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.QUEUED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;

@Slf4j
public class NoSecLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    public static final int TIMEOUT = 30;
    private final String OTA_TRANSPORT_CONFIGURATION = "{\n" +
            "  \"observeAttr\": {\n" +
            "    \"keyName\": {\n" +
            "      \"/5_1.0/0/3\": \"state\",\n" +
            "      \"/5_1.0/0/5\": \"updateResult\",\n" +
            "      \"/5_1.0/0/6\": \"pkgname\",\n" +
            "      \"/5_1.0/0/7\": \"pkgversion\",\n" +
            "      \"/5_1.0/0/9\": \"firmwareUpdateDeliveryMethod\",\n" +
            "      \"/9_1.0/0/0\": \"pkgname\",\n" +
            "      \"/9_1.0/0/1\": \"pkgversion\",\n" +
            "      \"/9_1.0/0/7\": \"updateState\",\n" +
            "      \"/9_1.0/0/9\": \"updateResult\"\n" +
            "    },\n" +
            "    \"observe\": [\n" +
            "      \"/5_1.0/0/3\",\n" +
            "      \"/5_1.0/0/5\",\n" +
            "      \"/5_1.0/0/6\",\n" +
            "      \"/5_1.0/0/7\",\n" +
            "      \"/5_1.0/0/9\",\n" +
            "      \"/9_1.0/0/0\",\n" +
            "      \"/9_1.0/0/1\",\n" +
            "      \"/9_1.0/0/7\",\n" +
            "      \"/9_1.0/0/9\"\n" +
            "    ],\n" +
            "    \"attribute\": [],\n" +
            "    \"telemetry\": [\n" +
            "      \"/5_1.0/0/3\",\n" +
            "      \"/5_1.0/0/5\",\n" +
            "      \"/5_1.0/0/6\",\n" +
            "      \"/5_1.0/0/7\",\n" +
            "      \"/5_1.0/0/9\",\n" +
            "      \"/9_1.0/0/0\",\n" +
            "      \"/9_1.0/0/1\",\n" +
            "      \"/9_1.0/0/7\",\n" +
            "      \"/9_1.0/0/9\"\n" +
            "    ],\n" +
            "    \"attributeLwm2m\": {}\n" +
            "  },\n" +
            "  \"bootstrap\": {\n" +
            "    \"servers\": {\n" +
            "      \"binding\": \"UQ\",\n" +
            "      \"shortId\": 123,\n" +
            "      \"lifetime\": 300,\n" +
            "      \"notifIfDisabled\": true,\n" +
            "      \"defaultMinPeriod\": 1\n" +
            "    },\n" +
            "    \"lwm2mServer\": {\n" +
            "      \"host\": \"localhost\",\n" +
            "      \"port\": 5685,\n" +
            "      \"serverId\": 123,\n" +
            "      \"securityMode\": \"NO_SEC\",\n" +
            "      \"serverPublicKey\": \"\",\n" +
            "      \"clientHoldOffTime\": 1,\n" +
            "      \"bootstrapServerAccountTimeout\": 0\n" +
            "    },\n" +
            "    \"bootstrapServer\": {\n" +
            "      \"host\": \"localhost\",\n" +
            "      \"port\": 5687,\n" +
            "      \"serverId\": 111,\n" +
            "      \"securityMode\": \"NO_SEC\",\n" +
            "      \"serverPublicKey\": \"\",\n" +
            "      \"clientHoldOffTime\": 1,\n" +
            "      \"bootstrapServerAccountTimeout\": 0\n" +
            "    }\n" +
            "  },\n" +
            "  \"clientLwM2mSettings\": {\n" +
            "    \"fwUpdateStrategy\": 1,\n" +
            "    \"swUpdateStrategy\": 1,\n" +
            "    \"clientOnlyObserveAfterConnect\": 1,\n" +
            "    \"powerMode\": \"PSM\",\n" +
            "    \"fwUpdateResource\": \"\",\n" +
            "    \"swUpdateResource\": \"\",\n" +
            "    \"compositeOperationsSupport\": false\n" +
            "  },\n" +
            "  \"type\": \"LWM2M\"\n" +
            "}";

    LwM2MTestClient client = null;

    @After
    public void tearDown() {
        if (client != null) {
            client.destroy();
        }
    }

    @Test
    public void testConnectAndObserveTelemetry() throws Exception {
        NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint(ENDPOINT);
        super.basicTestConnectionObserveTelemetry(SECURITY, clientCredentials, COAP_CONFIG, ENDPOINT);
    }

    @Test
    public void testFirmwareUpdateWithClientWithoutFirmwareInfo() throws Exception {
        LwM2MTestClient client = null;
        try {
            createDeviceProfile(TRANSPORT_CONFIGURATION);
            NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
            clientCredentials.setEndpoint(ENDPOINT);
            Device device = createDevice(clientCredentials);

            client = new LwM2MTestClient(executor, ENDPOINT);
            client.init(SECURITY, COAP_CONFIG);

            Thread.sleep(1000);

            device.setFirmwareId(createFirmware().getId());
            device = doPost("/api/device", device, Device.class);

            Thread.sleep(5000);

            List<TsKvEntry> ts = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/values/timeseries?keys=fw_state", new TypeReference<>() {
            }));

            List<OtaPackageUpdateStatus> statuses = ts.stream().map(KvEntry::getValueAsString).map(OtaPackageUpdateStatus::valueOf).collect(Collectors.toList());

            List<OtaPackageUpdateStatus> expectedStatuses = Collections.singletonList(FAILED);

            Assert.assertEquals(expectedStatuses, statuses);
        } finally {
            if (client != null) {
                client.destroy();
            }
        }
    }

    @Test
    public void testFirmwareUpdateByObject5() throws Exception {
        LwM2MTestClient client = null;
        try {
            createDeviceProfile(OTA_TRANSPORT_CONFIGURATION);
            NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
            clientCredentials.setEndpoint("OTA_" + ENDPOINT);
            Device device = createDevice(clientCredentials);

            device.setFirmwareId(createFirmware().getId());
            device = doPost("/api/device", device, Device.class);
            Thread.sleep(1000);

            client = new LwM2MTestClient(executor, "OTA_" + ENDPOINT);
            client.init(SECURITY, COAP_CONFIG);

            Thread.sleep(3000);

            List<TsKvEntry> ts = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/values/timeseries?orderBy=ASC&keys=fw_state&startTs=0&endTs=" + System.currentTimeMillis(), new TypeReference<>() {
            }));

            List<OtaPackageUpdateStatus> statuses = ts.stream().sorted(Comparator.comparingLong(TsKvEntry::getTs)).map(KvEntry::getValueAsString).map(OtaPackageUpdateStatus::valueOf).collect(Collectors.toList());

            List<OtaPackageUpdateStatus> expectedStatuses = Arrays.asList(QUEUED, INITIATED, DOWNLOADING, DOWNLOADED, UPDATING, UPDATED);

            Assert.assertEquals(expectedStatuses, statuses);
        } finally {
            if (client != null) {
                client.destroy();
            }
        }
    }

    /**
     * This is the example how to use the AWAITILITY instead Thread.sleep()
     * Test will finish as fast as possible, but will await until TIMEOUT if a build machine is busy or slow
     * Check the detailed log output to learn how Awaitility polling the API and when exactly expected result appears
     * */
    @Test
    public void testSoftwareUpdateByObject9() throws Exception {
        //given
        final List<OtaPackageUpdateStatus> expectedStatuses = List.of(
                QUEUED, INITIATED, DOWNLOADING, DOWNLOADING, DOWNLOADING, DOWNLOADED, VERIFIED, UPDATED);

        createDeviceProfile(OTA_TRANSPORT_CONFIGURATION);
        NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint("OTA_" + ENDPOINT);
        final Device device = createDevice(clientCredentials);
        device.setSoftwareId(createSoftware().getId());

        final Device savedDevice = doPost("/api/device", device, Device.class); //sync call
        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        //when
        log.warn("Init the client...");
        client = new LwM2MTestClient(executor, "OTA_" + ENDPOINT);
        client.init(SECURITY, COAP_CONFIG);

        log.warn("AWAIT atMost {} SECONDS on timeseries List<TsKvEntry> by API with list size {}...", TIMEOUT, expectedStatuses.size());
        List<TsKvEntry> ts = await("await on timeseries")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getSwStateTelemetryFromAPI(device.getId().getId()), hasSize(expectedStatuses.size()));
        log.warn("Got the ts: {}", ts);

        ts.sort(Comparator.comparingLong(TsKvEntry::getTs));
        log.warn("Ts ordered: {}", ts);
        ts.forEach((x) -> log.warn("ts: {} ", x));

        List<OtaPackageUpdateStatus> statuses = ts.stream().map(KvEntry::getValueAsString).map(OtaPackageUpdateStatus::valueOf).collect(Collectors.toList());
        log.warn("Converted ts to statuses: {}", statuses);

        assertThat(statuses).isEqualTo(expectedStatuses);
    }

    private Device getDeviceFromAPI(UUID deviceId) throws Exception {
        final Device device = doGet("/api/device/" + deviceId, Device.class);
        log.warn("Fetched device by API for deviceId {}, device is {}", deviceId, device);
        return device;
    }

    private List<TsKvEntry> getSwStateTelemetryFromAPI(UUID deviceId) throws Exception {
        final List<TsKvEntry> tsKvEntries = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?orderBy=ASC&keys=sw_state&startTs=0&endTs=" + System.currentTimeMillis(), new TypeReference<>() {
        }));
        log.warn("Fetched telemetry by API for deviceId {}, list size {}, tsKvEntries {}", deviceId, tsKvEntries.size(), tsKvEntries);
        return tsKvEntries;
    }
}
