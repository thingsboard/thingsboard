/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.ota;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;

@DaoSqlTest
public abstract class AbstractOtaLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    private final  String[] RESOURCES_OTA = new String[]{"3.xml", "5.xml", "9.xml"};
    protected static final String CLIENT_ENDPOINT_WITHOUT_FW_INFO = "WithoutFirmwareInfoDevice";
    protected static final String CLIENT_ENDPOINT_OTA5 = "Ota5_Device";
    protected static final String CLIENT_ENDPOINT_OTA9 = "Ota9_Device";


    protected final String OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA =

            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/5_1.2/0/3\": \"state\",\n" +
                    "      \"/5_1.2/0/5\": \"updateResult\",\n" +
                    "      \"/5_1.2/0/6\": \"pkgname\",\n" +
                    "      \"/5_1.2/0/7\": \"pkgversion\",\n" +
                    "      \"/5_1.2/0/9\": \"firmwareUpdateDeliveryMethod\",\n" +
                    "      \"/9_1.1/0/0\": \"pkgname\",\n" +
                    "      \"/9_1.1/0/1\": \"pkgversion\",\n" +
                    "      \"/9_1.1/0/7\": \"updateState\",\n" +
                    "      \"/9_1.1/0/9\": \"updateResult\"\n" +
                    "    },\n" +
                    "    \"observe\": [\n" +
                    "      \"/5_1.2/0/3\",\n" +
                    "      \"/5_1.2/0/5\",\n" +
                    "      \"/5_1.2/0/6\",\n" +
                    "      \"/5_1.2/0/7\",\n" +
                    "      \"/5_1.2/0/9\",\n" +
                    "      \"/9_1.1/0/0\",\n" +
                    "      \"/9_1.1/0/1\",\n" +
                    "      \"/9_1.1/0/7\",\n" +
                    "      \"/9_1.1/0/9\"\n" +
                    "    ],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/5_1.2/0/3\",\n" +
                    "      \"/5_1.2/0/5\",\n" +
                    "      \"/5_1.2/0/6\",\n" +
                    "      \"/5_1.2/0/7\",\n" +
                    "      \"/5_1.2/0/9\",\n" +
                    "      \"/9_1.1/0/0\",\n" +
                    "      \"/9_1.1/0/1\",\n" +
                    "      \"/9_1.1/0/7\",\n" +
                    "      \"/9_1.1/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    public AbstractOtaLwM2MIntegrationTest() {
        setResources(this.RESOURCES_OTA);
    }

    protected OtaPackageInfo createFirmware() throws Exception {
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

    protected OtaPackageInfo createSoftware() throws Exception {
        String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";

        OtaPackageInfo swInfo = new OtaPackageInfo();
        swInfo.setDeviceProfileId(deviceProfile.getId());
        swInfo.setType(SOFTWARE);
        swInfo.setTitle("My sw");
        swInfo.setVersion("v1.0");

        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", swInfo, OtaPackageInfo.class);

        MockMultipartFile testData = new MockMultipartFile("file", "filename.txt", "text/plain", new byte[]{1});

        return savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, "SHA256");
    }

    protected OtaPackageInfo savaData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackageInfo.class);
    }
}
