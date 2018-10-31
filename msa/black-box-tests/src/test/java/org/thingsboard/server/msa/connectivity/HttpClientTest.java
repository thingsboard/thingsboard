/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.msa.connectivity;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

public class HttpClientTest extends AbstractContainerTest {

    @Test
    public void telemetryUpload() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");

        Device device = createDevice("http_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        ResponseEntity deviceTelemetryResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/v1/{credentialsId}/telemetry",
                        mapper.readTree(createPayload().toString()),
                        ResponseEntity.class,
                        deviceCredentials.getCredentialsId());
        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        Assert.assertEquals(Sets.newHashSet("booleanKey", "stringKey", "doubleKey", "longKey"),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, "booleanKey", Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "stringKey", "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "doubleKey", Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "longKey", Long.toString(73)));

        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + device.getId());
    }
}
