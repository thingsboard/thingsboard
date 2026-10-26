// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edge;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;

import java.util.UUID;

@DaoSqlTest
public class RequestMsgEdgeTest extends AbstractEdgeTest {

    @Test
    public void testSendRequestsForEntitiesOfAnotherTenantToCloud() throws Exception {
        loginDifferentTenant();
        try {
            Device device = new Device();
            device.setName("Different Tenant Device");
            device.setType("default");
            device = doPost("/api/device", device, Device.class);
            doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                    JacksonUtil.toJsonNode("{\"key1\":\"value1\"}"));

            Asset asset = new Asset();
            asset.setName("Different Tenant Asset");
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);

            EntityRelation relation = new EntityRelation();
            relation.setType("test");
            relation.setFrom(device.getId());
            relation.setTo(asset.getId());
            relation.setTypeGroup(RelationTypeGroup.COMMON);
            doPost("/api/v2/relation", relation, EntityRelation.class);

            RuleChain ruleChain = new RuleChain();
            ruleChain.setName("Different Tenant Rule Chain");
            ruleChain.setType(RuleChainType.CORE);
            ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

            UUID deviceUuid = device.getUuidId();
            UUID userUuid = savedDifferentTenantUser.getUuidId();
            UUID ruleChainUuid = ruleChain.getUuidId();
            loginTenantAdmin();

            UplinkMsg uplinkMsg = UplinkMsg.newBuilder()
                    .addUserCredentialsRequestMsg(UserCredentialsRequestMsg.newBuilder()
                            .setUserIdMSB(userUuid.getMostSignificantBits())
                            .setUserIdLSB(userUuid.getLeastSignificantBits()).build())
                    .addDeviceCredentialsRequestMsg(DeviceCredentialsRequestMsg.newBuilder()
                            .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                            .setDeviceIdLSB(deviceUuid.getLeastSignificantBits()).build())
                    .addAttributesRequestMsg(AttributesRequestMsg.newBuilder()
                            .setEntityType(EntityType.DEVICE.name())
                            .setEntityIdMSB(deviceUuid.getMostSignificantBits())
                            .setEntityIdLSB(deviceUuid.getLeastSignificantBits())
                            .setScope(DataConstants.SERVER_SCOPE).build())
                    .addRelationRequestMsg(RelationRequestMsg.newBuilder()
                            .setEntityType(EntityType.DEVICE.name())
                            .setEntityIdMSB(deviceUuid.getMostSignificantBits())
                            .setEntityIdLSB(deviceUuid.getLeastSignificantBits()).build())
                    .addRuleChainMetadataRequestMsg(RuleChainMetadataRequestMsg.newBuilder()
                            .setRuleChainIdMSB(ruleChainUuid.getMostSignificantBits())
                            .setRuleChainIdLSB(ruleChainUuid.getLeastSignificantBits()).build())
                    .build();

            edgeImitator.expectResponsesAmount(1);
            edgeImitator.expectMessageAmount(1);
            edgeImitator.sendUplinkMsg(uplinkMsg);
            Assert.assertTrue(edgeImitator.waitForResponses());
            Assert.assertFalse(edgeImitator.waitForMessages(5));
        } finally {
            deleteDifferentTenant();
            loginTenantAdmin();
        }
    }

}
