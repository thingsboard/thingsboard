/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AssetProfileEdgeTest extends AbstractEdgeTest {

    @Test
    public void testAssetProfiles() throws Exception {
        RuleChainId buildingsRuleChainId = createEdgeRuleChainAndAssignToEdge("Buildings Rule Chain");

        // create asset profile
        AssetProfile assetProfile = this.createAssetProfile("Building");
        assetProfile.setDefaultEdgeRuleChainId(buildingsRuleChainId);
        edgeImitator.expectMessageAmount(1);
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        AssetProfileUpdateMsg assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(assetProfile.getUuidId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(assetProfile.getUuidId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());
        Assert.assertEquals("Building", assetProfileUpdateMsg.getName());
        Assert.assertEquals(buildingsRuleChainId.getId().getMostSignificantBits(), assetProfileUpdateMsg.getDefaultRuleChainIdMSB());
        Assert.assertEquals(buildingsRuleChainId.getId().getLeastSignificantBits(), assetProfileUpdateMsg.getDefaultRuleChainIdLSB());

        // update asset profile
        assetProfile.setImage("IMAGE");
        edgeImitator.expectMessageAmount(1);
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(ByteString.copyFrom("IMAGE".getBytes(StandardCharsets.UTF_8)), assetProfileUpdateMsg.getImage());

        // delete profile
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/assetProfile/" + assetProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(assetProfile.getUuidId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(assetProfile.getUuidId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());

        unAssignFromEdgeAndDeleteRuleChain(buildingsRuleChainId);
    }

    @Test
    public void testSendAssetProfileToCloud() throws Exception {
        RuleChainId edgeRuleChainId = createEdgeRuleChainAndAssignToEdge("Asset Profile Rule Chain");
        DashboardId dashboardId = createDashboardAndAssignToEdge("Asset Profile Dashboard");

        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AssetProfileUpdateMsg.Builder assetProfileUpdateMsgBuilder = AssetProfileUpdateMsg.newBuilder();
        assetProfileUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        assetProfileUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        assetProfileUpdateMsgBuilder.setName("Asset Profile On Edge");
        assetProfileUpdateMsgBuilder.setDefault(false);
        assetProfileUpdateMsgBuilder.setDefaultRuleChainIdMSB(edgeRuleChainId.getId().getMostSignificantBits());
        assetProfileUpdateMsgBuilder.setDefaultRuleChainIdLSB(edgeRuleChainId.getId().getLeastSignificantBits());
        assetProfileUpdateMsgBuilder.setDefaultDashboardIdMSB(dashboardId.getId().getMostSignificantBits());
        assetProfileUpdateMsgBuilder.setDefaultDashboardIdLSB(dashboardId.getId().getLeastSignificantBits());
        assetProfileUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(assetProfileUpdateMsgBuilder);
        uplinkMsgBuilder.addAssetProfileUpdateMsg(assetProfileUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        AssetProfile assetProfile = doGet("/api/assetProfile/" + uuid, AssetProfile.class);
        Assert.assertNotNull(assetProfile);
        Assert.assertEquals("Asset Profile On Edge", assetProfile.getName());
        Assert.assertEquals(dashboardId, assetProfile.getDefaultDashboardId());
        Assert.assertNull(assetProfile.getDefaultRuleChainId());
        Assert.assertEquals(edgeRuleChainId, assetProfile.getDefaultEdgeRuleChainId());

        // delete profile
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/assetProfile/" + assetProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        AssetProfileUpdateMsg assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(assetProfile.getUuidId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(assetProfile.getUuidId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());

        // cleanup
        unAssignFromEdgeAndDeleteDashboard(dashboardId);
        unAssignFromEdgeAndDeleteRuleChain(edgeRuleChainId);
    }

    @Test
    public void testSendAssetProfileToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
        String assetProfileOnCloudName = StringUtils.randomAlphanumeric(15);

        edgeImitator.expectMessageAmount(1);
        AssetProfile assetProfileOnCloud = this.createAssetProfile(assetProfileOnCloudName);
        assetProfileOnCloud = doPost("/api/assetProfile", assetProfileOnCloud, AssetProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AssetProfileUpdateMsg.Builder assetProfileUpdateMsgBuilder = AssetProfileUpdateMsg.newBuilder();
        assetProfileUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        assetProfileUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        assetProfileUpdateMsgBuilder.setName(assetProfileOnCloudName);
        assetProfileUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addAssetProfileUpdateMsg(assetProfileUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<AssetProfileUpdateMsg> assetProfileUpdateMsgOpt = edgeImitator.findMessageByType(AssetProfileUpdateMsg.class);
        Assert.assertTrue(assetProfileUpdateMsgOpt.isPresent());
        AssetProfileUpdateMsg latestAssetProfileUpdateMsg = assetProfileUpdateMsgOpt.get();
        Assert.assertNotEquals(assetProfileOnCloudName, latestAssetProfileUpdateMsg.getName());

        Assert.assertNotEquals(assetProfileOnCloud.getUuidId(), uuid);

        AssetProfile assetProfile = doGet("/api/assetProfile/" + uuid, AssetProfile.class);
        Assert.assertNotNull(assetProfile);
        Assert.assertNotEquals(assetProfileOnCloudName, assetProfile.getName());
    }
}
