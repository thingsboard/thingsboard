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
package org.thingsboard.server.transport.lwm2m;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.client.object.Security;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredentials;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.service.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MCredentials;

import java.util.Collections;
import java.util.List;

import static org.eclipse.leshan.client.object.Security.noSec;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

public class NoSecLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    private final int PORT = 5685;
    private final Security SECURITY = noSec("coap://localhost:" + PORT, 123);
    private final NetworkConfig COAP_CONFIG = new NetworkConfig().setString("COAP_PORT", Integer.toString(PORT));
    private final String ENDPOINT = "noSecEndpoint";

    private Device createDevice() throws Exception {
        Device device = new Device();
        device.setName("Device A");
        device.setDeviceProfileId(deviceProfile.getId());
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);

        LwM2MCredentials noSecCredentials = new LwM2MCredentials();
        NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint(ENDPOINT);
        noSecCredentials.setClient(clientCredentials);
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(noSecCredentials));
        doPost("/api/device/credentials", deviceCredentials).andExpect(status().isOk());
        return device;
    }

    private OtaPackageInfo createFirmware() throws Exception {
        String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";

        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setDeviceProfileId(deviceProfile.getId());
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("My firmware");
        firmwareInfo.setVersion("v1.0");

        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);

        MockMultipartFile testData = new MockMultipartFile("file", "filename.txt", "text/plain", new byte[]{1});

        return savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, "SHA256");
    }

    protected OtaPackageInfo savaData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackageInfo.class);
    }

    @Test
    public void testConnectAndObserveTelemetry() throws Exception {
        createDeviceProfile(TRANSPORT_CONFIGURATION);

        Device device = createDevice();

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));

        wsClient.send(mapper.writeValueAsString(wrapper));
        wsClient.waitForReply();

        wsClient.registerWaitForUpdate();
        LwM2MTestClient client = new LwM2MTestClient(executor, ENDPOINT);
        client.init(SECURITY, COAP_CONFIG);
        String msg = wsClient.waitForUpdate();

        EntityDataUpdate update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        Assert.assertEquals(42, Long.parseLong(tsValue.getValue()));
        client.destroy();
    }

    @Test
    public void testFirmwareUpdateWithClientWithoutFirmwareInfo() throws Exception {
        createDeviceProfile(TRANSPORT_CONFIGURATION);

        Device device = createDevice();

        OtaPackageInfo firmware = createFirmware();

        LwM2MTestClient client = new LwM2MTestClient(executor, ENDPOINT);
        client.init(SECURITY, COAP_CONFIG);

        Thread.sleep(1000);

        device.setFirmwareId(firmware.getId());

        device = doPost("/api/device", device, Device.class);

        Thread.sleep(1000);

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "fw_state")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));

        wsClient.send(mapper.writeValueAsString(wrapper));
        wsClient.waitForReply();

        wsClient.registerWaitForUpdate();

        String msg = wsClient.waitForUpdate();

        EntityDataUpdate update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("fw_state");
        Assert.assertEquals("FAILED", tsValue.getValue());
        client.destroy();
    }

}
