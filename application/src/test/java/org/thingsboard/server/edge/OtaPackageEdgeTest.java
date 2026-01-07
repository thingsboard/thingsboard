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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.nio.ByteBuffer;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

@DaoSqlTest
public class OtaPackageEdgeTest extends AbstractEdgeTest {

    @Test
    public void testOtaPackages_usesUrl() throws Exception {
        // create ota package
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(thermostatDeviceProfile.getId());
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("My firmware #1");
        firmwareInfo.setVersion("v1.0");
        firmwareInfo.setTag("My firmware #1 v1.0");
        firmwareInfo.setUsesUrl(true);
        firmwareInfo.setUrl("http://localhost:8080/v1/package");
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        OtaPackageUpdateMsg otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        OtaPackage otaPackage = JacksonUtil.fromString(otaPackageUpdateMsg.getEntity(), OtaPackage.class, true);
        Assert.assertNotNull(otaPackage);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getId(), otaPackage.getId());
        Assert.assertEquals(thermostatDeviceProfile.getId(), otaPackage.getDeviceProfileId());
        Assert.assertEquals(FIRMWARE, otaPackage.getType());
        Assert.assertEquals("My firmware #1", otaPackage.getTitle());
        Assert.assertEquals("v1.0", otaPackage.getVersion());
        Assert.assertEquals("My firmware #1 v1.0", otaPackage.getTag());
        Assert.assertEquals("http://localhost:8080/v1/package", otaPackage.getUrl());
        Assert.assertNull(otaPackage.getData());
        Assert.assertNull(otaPackage.getFileName());
        Assert.assertNull(otaPackage.getContentType());
        Assert.assertNull(otaPackage.getChecksumAlgorithm());
        Assert.assertNull(otaPackage.getChecksum());
        Assert.assertNull(otaPackage.getDataSize());

        // delete ota package
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/otaPackage/" + savedFirmwareInfo.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getMostSignificantBits(), otaPackageUpdateMsg.getIdMSB());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getLeastSignificantBits(), otaPackageUpdateMsg.getIdLSB());
    }

    @Test
    public void testOtaPackages_hasData() throws Exception {
        // create ota package
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(thermostatDeviceProfile.getId());
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("My firmware #2");
        firmwareInfo.setVersion("v2.0");
        firmwareInfo.setTag("My firmware #2 v2.0");
        firmwareInfo.setUsesUrl(false);
        firmwareInfo.setHasData(false);
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        edgeImitator.expectMessageAmount(1);

        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
        MockMultipartFile testData = new MockMultipartFile("file", "firmware.bin", "image/png", ByteBuffer.wrap(new byte[]{1, 3, 5}).array());
        savedFirmwareInfo = saveData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksumAlgorithm={checksumAlgorithm}", testData, ChecksumAlgorithm.SHA256.name());

        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        OtaPackageUpdateMsg otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        OtaPackage otaPackage = JacksonUtil.fromString(otaPackageUpdateMsg.getEntity(), OtaPackage.class, true);
        Assert.assertNotNull(otaPackage);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getId(), otaPackage.getId());
        Assert.assertEquals(thermostatDeviceProfile.getId(), otaPackage.getDeviceProfileId());
        Assert.assertEquals(FIRMWARE, otaPackage.getType());
        Assert.assertEquals("My firmware #2", otaPackage.getTitle());
        Assert.assertEquals("v2.0", otaPackage.getVersion());
        Assert.assertEquals("My firmware #2 v2.0", otaPackage.getTag());
        Assert.assertFalse(otaPackage.hasUrl());
        Assert.assertEquals("firmware.bin", otaPackage.getFileName());
        Assert.assertEquals("image/png", otaPackage.getContentType());
        Assert.assertEquals(ChecksumAlgorithm.SHA256, otaPackage.getChecksumAlgorithm());
        Assert.assertEquals("62467691cf583d4fa78b18fafaf9801f505e0ef03baf0603fd4b0cd004cd1e75", otaPackage.getChecksum());
        Assert.assertEquals(3L, otaPackage.getDataSize().longValue());
        Assert.assertEquals(ByteBuffer.wrap(new byte[]{1, 3, 5}), otaPackage.getData());

        // delete ota package
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/otaPackage/" + savedFirmwareInfo.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getMostSignificantBits(), otaPackageUpdateMsg.getIdMSB());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getLeastSignificantBits(), otaPackageUpdateMsg.getIdLSB());
    }

    private OtaPackageInfo saveData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackageInfo.class);
    }

}
