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

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AssetEdgeTest extends AbstractEdgeTest {

    @Test
    public void testAssets() throws Exception {
        // create asset and assign to edge
        edgeImitator.expectMessageAmount(2);
        Asset savedAsset = saveAsset("Edge Asset 2");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<AssetUpdateMsg> assetUpdateMsgOpt = edgeImitator.findMessageByType(AssetUpdateMsg.class);
        Assert.assertTrue(assetUpdateMsgOpt.isPresent());
        AssetUpdateMsg assetUpdateMsg = assetUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getUuidId().getMostSignificantBits(), assetUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getIdLSB());
        Assert.assertEquals(savedAsset.getName(), assetUpdateMsg.getName());
        Assert.assertEquals(savedAsset.getType(), assetUpdateMsg.getType());
        Optional<AssetProfileUpdateMsg> assetProfileUpdateMsgOpt = edgeImitator.findMessageByType(AssetProfileUpdateMsg.class);
        Assert.assertTrue(assetProfileUpdateMsgOpt.isPresent());
        AssetProfileUpdateMsg assetProfileUpdateMsg = assetProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getAssetProfileId().getId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getAssetProfileId().getId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());

        // update asset
        edgeImitator.expectMessageAmount(1);
        savedAsset.setName("Edge Asset 2 Updated");
        savedAsset = doPost("/api/asset", savedAsset, Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getName(), assetUpdateMsg.getName());

        // unassign asset from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getUuidId().getMostSignificantBits(), assetUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getIdLSB());

        // delete asset - no messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/asset/" + savedAsset.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // create asset #2 and assign to edge
        edgeImitator.expectMessageAmount(2);
        savedAsset = saveAsset("Edge Asset 3");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        assetUpdateMsgOpt = edgeImitator.findMessageByType(AssetUpdateMsg.class);
        Assert.assertTrue(assetUpdateMsgOpt.isPresent());
        assetUpdateMsg = assetUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getUuidId().getMostSignificantBits(), assetUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getIdLSB());
        Assert.assertEquals(savedAsset.getName(), assetUpdateMsg.getName());
        Assert.assertEquals(savedAsset.getType(), assetUpdateMsg.getType());
        assetProfileUpdateMsgOpt = edgeImitator.findMessageByType(AssetProfileUpdateMsg.class);
        Assert.assertTrue(assetProfileUpdateMsgOpt.isPresent());
        assetProfileUpdateMsg = assetProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getAssetProfileId().getId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getAssetProfileId().getId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());

        // assign asset #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        edgeImitator.expectMessageAmount(1);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), assetUpdateMsg.getCustomerIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getCustomerIdLSB());

        // unassign asset #2 from customer
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(
                new CustomerId(EntityId.NULL_UUID),
                new CustomerId(new UUID(assetUpdateMsg.getCustomerIdMSB(), assetUpdateMsg.getCustomerIdLSB())));

        // delete asset #2 - messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/asset/" + savedAsset.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getUuidId().getMostSignificantBits(), assetUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getIdLSB());
    }


}
