/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.rule.engine.flow;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TbRuleChainInputNodeTest {

    TenantId tenantId;
    RuleChainId nodeRuleChainId;
    RuleChainId rootRuleChainId;
    RuleChainId deviceProfileRuleChainId;
    RuleChainId assetProfileRuleChainId;
    Device device;
    Device deviceWithoutRuleChain;
    Asset asset;
    TbContext ctx;
    RuleEngineDeviceProfileCache ruleEngineDeviceProfileCache;
    RuleEngineAssetProfileCache ruleEngineAssetProfileCache;
    RuleChainService ruleChainService;
    TbRuleChainInputNode node;
    TbRuleChainInputNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbMsgCallback callback;

    @BeforeEach
    void setUp() throws TbNodeException {
        nodeRuleChainId = new RuleChainId(UUID.randomUUID());
        rootRuleChainId = new RuleChainId(UUID.randomUUID());
        deviceProfileRuleChainId = new RuleChainId(UUID.randomUUID());
        assetProfileRuleChainId = new RuleChainId(UUID.randomUUID());
        tenantId = new TenantId(UUID.randomUUID());
        RuleChain rootRuleChain = new RuleChain(rootRuleChainId);

        //device
        DeviceProfile deviceProfile = new DeviceProfile(new DeviceProfileId(UUID.randomUUID()));
        deviceProfile.setDefaultRuleChainId(deviceProfileRuleChainId);
        device = new Device(new DeviceId(UUID.randomUUID()));
        device.setDeviceProfileId(deviceProfile.getId());

        DeviceProfile deviceProfileWithoutRuleChain = new DeviceProfile(new DeviceProfileId(UUID.randomUUID()));
        deviceWithoutRuleChain = new Device(new DeviceId(UUID.randomUUID()));
        deviceWithoutRuleChain.setDeviceProfileId(deviceProfileWithoutRuleChain.getId());

        //asset
        AssetProfile assetProfile = new AssetProfile(new AssetProfileId(UUID.randomUUID()));
        assetProfile.setDefaultRuleChainId(assetProfileRuleChainId);
        asset = new Asset(new AssetId(UUID.randomUUID()));
        asset.setAssetProfileId(assetProfile.getId());

        //node
        config = new TbRuleChainInputNodeConfiguration().defaultConfiguration();
        config.setRuleChainId(nodeRuleChainId.toString());
        config.setForwardMsgToRootRuleChain(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node = spy(new TbRuleChainInputNode());
        node.init(ctx, nodeConfiguration);

        //init mock
        ctx = mock(TbContext.class);
        ruleEngineDeviceProfileCache = mock(RuleEngineDeviceProfileCache.class);
        ruleEngineAssetProfileCache = mock(RuleEngineAssetProfileCache.class);
        ruleChainService = mock(RuleChainService.class);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleChainService()).thenReturn(ruleChainService);
        when(ctx.getDeviceProfileCache()).thenReturn(ruleEngineDeviceProfileCache);
        when(ctx.getAssetProfileCache()).thenReturn(ruleEngineAssetProfileCache);

        doReturn(deviceProfile).when(ruleEngineDeviceProfileCache).get(tenantId, device.getId());
        doReturn(deviceProfileWithoutRuleChain).when(ruleEngineDeviceProfileCache).get(tenantId, deviceWithoutRuleChain.getId());
        doReturn(assetProfile).when(ruleEngineAssetProfileCache).get(tenantId, asset.getId());
        doReturn(rootRuleChain).when(ruleChainService).getRootTenantRuleChain(tenantId);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void getRuleChainId_when_device_transferMsgToOriginatorRootRuleChain() {
        assertThat(getRuleChainIdArgumentCaptor(device.getId()).getValue()).isEqualTo(deviceProfileRuleChainId);
    }

    @Test
    void getRuleChainId_when_asset_transferMsgToOriginatorRootRuleChain() {
        assertThat(getRuleChainIdArgumentCaptor(asset.getId()).getValue()).isEqualTo(assetProfileRuleChainId);
    }

    @Test
    void getRuleChainId_when_device_transferMsgToOriginatorRootRuleChain_without_rulechain() {
        assertThat(getRuleChainIdArgumentCaptor(deviceWithoutRuleChain.getId()).getValue()).isEqualTo(rootRuleChainId);
    }

    @Test
    void getRuleChainId_when_customer_transferMsgToOriginatorRootRuleChain_without_rulechain() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        assertThat(getRuleChainIdArgumentCaptor(customerId).getValue()).isEqualTo(rootRuleChainId);
    }

    @Test
    void getRuleChainId_when_transferMsgToOriginatorRootRuleChain_false() throws TbNodeException {
        config.setForwardMsgToRootRuleChain(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        assertThat(getRuleChainIdArgumentCaptor(device.getId()).getValue()).isEqualTo(nodeRuleChainId);
    }

    @NotNull
    private ArgumentCaptor<RuleChainId> getRuleChainIdArgumentCaptor(EntityId entityId) {
        String data = "{}";
        node.onMsg(ctx, getTbMsg(entityId, data));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<RuleChainId> ruleChainArgumentCaptor = ArgumentCaptor.forClass(RuleChainId.class);
        verify(ctx, times(1)).input(newMsgCaptor.capture(), ruleChainArgumentCaptor.capture());

        return ruleChainArgumentCaptor;
    }

    private TbMsg getTbMsg(EntityId entityId, String data) {
        final Map<String, String> mdMap = Map.of(
                "TestKey_1", "Test",
                "country", "US",
                "voltageDataValue", "220",
                "city", "NY"
        );
        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, new TbMsgMetaData(mdMap), data, callback);
    }

}