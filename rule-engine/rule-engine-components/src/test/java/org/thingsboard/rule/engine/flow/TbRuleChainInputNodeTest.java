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
package org.thingsboard.rule.engine.flow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
public class TbRuleChainInputNodeTest extends AbstractRuleNodeUpgradeTest {

    private final String EXISTING_RULE_CHAIN_ID = "a5b09dbb-e7c4-4cb2-8037-39de145a2cd6";
    private final String INVALID_RULE_CHAIN_ID = "91acbce0-079fdb";
    private final String DEFAULT_RULE_CHAIN_ID = "79acbce0-e789-11ee-9cf0-33d8b6079fba";
    private final String EXISTING_TENANT_ID = "83bcece0-e709-11ee-9cf0-15d8b6079acb";
    private final String DEVICE_ID = "14bbaba0-e709-11ee-9cf0-25d8b6079aff";
    private final String DEVICE_PROFILE_ID = "26aaaba0-e611-11ee-9cf0-83d8b6079acc";

    private final RuleChainId ruleChainId = new RuleChainId(UUID.fromString(EXISTING_RULE_CHAIN_ID));
    private final RuleChainId defaultRuleChainId = new RuleChainId(UUID.fromString(DEFAULT_RULE_CHAIN_ID));
    private final TenantId tenantId = new TenantId(UUID.fromString(EXISTING_TENANT_ID));
    private final DeviceId deviceId = new DeviceId(UUID.fromString(DEVICE_ID));
    private final DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.fromString(DEVICE_PROFILE_ID));

    private final RuleChain ruleChain = new RuleChain(ruleChainId);
    private final RuleChain defaultRuleChain = new RuleChain(defaultRuleChainId);
    private final Device device = new Device(deviceId);
    private final DeviceProfile deviceProfile = new DeviceProfile(deviceProfileId);
    private TbRuleChainInputNode node;
    private TbRuleChainInputNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;
    private TbMsg msg = getMsg(deviceId);
    @Mock
    private TbContext ctx;
    @Mock
    private RuleChainService ruleChainService;
    @Mock
    private TbMsgCallback callback;
    @Mock
    private RuleEngineDeviceProfileCache deviceProfileCache;

    @BeforeEach
    public void setUp() {
        node = spy(new TbRuleChainInputNode());
        config = new TbRuleChainInputNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
    }

    @Test
    public void givenValidConfigWithRuleChainId_whenInit_thenOk() throws TbNodeException {
        //GIVEN
        config.setRuleChainId(EXISTING_RULE_CHAIN_ID);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        //WHEN
        assertThatCode(() -> node.init(ctx, nodeConfiguration))
                .doesNotThrowAnyException();

        //THEN
        verify(ctx).checkTenantEntity(ruleChainId);
    }

    @Test
    public void givenInvalidRuleChainId_whenInit_thenThrowException() {
        //GIVEN
        config.setRuleChainId(INVALID_RULE_CHAIN_ID);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        //WHEN
        Assertions.assertThatThrownBy(() -> node.init(ctx, nodeConfiguration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Failed to parse rule chain id: " + INVALID_RULE_CHAIN_ID);

        //THEN
        verify(ctx, never()).tellSuccess(msg);
    }

    @Test
    public void givenForwardMsgToRootIsTrue_whenInit_thenOk() {
        //GIVEN
        config.setForwardMsgToRootRuleChain(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleChainService()).thenReturn(ruleChainService);
        when(ruleChainService.getRootTenantRuleChain(any())).thenReturn(ruleChain);

        //WHEN
        assertThatCode(() -> node.init(ctx, nodeConfiguration))
                .doesNotThrowAnyException();

        //THEN
        verify(ctx).getTenantId();
        verify(ruleChainService, never()).findRuleChainById(eq(tenantId), eq(ruleChainId));
        verify(ruleChainService).getRootTenantRuleChain(eq(tenantId));
        verifyNoMoreInteractions(ruleChainService);
    }

    @Test
    public void givenForwardMsgToRootIsTrueAndNoTenantRootRuleChain_whenInit_thenThrowException() {
        //GIVEN
        config.setForwardMsgToRootRuleChain(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleChainService()).thenReturn(ruleChainService);
        when(ruleChainService.getRootTenantRuleChain(any())).thenReturn(null);

        //WHEN
        Assertions.assertThatThrownBy(() -> node.init(ctx, nodeConfiguration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Failed to find root rule chain for tenant with id: " + tenantId.getId());

        //THEN
        verify(ctx, never()).tellSuccess(msg);
        verify(ctx).getTenantId();
        verify(ruleChainService).getRootTenantRuleChain(eq(tenantId));
        verifyNoMoreInteractions(ruleChainService);
    }

    @Test
    public void givenForwardMsgToRootIsTrue_whenOnMsg_thenShouldTransferToDefaultRuleChain() throws TbNodeException {
        //GIVEN
        config.setForwardMsgToRootRuleChain(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        deviceProfile.setDefaultRuleChainId(defaultRuleChainId);
        device.setDeviceProfileId(deviceProfileId);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleChainService()).thenReturn(ruleChainService);
        when(ruleChainService.getRootTenantRuleChain(any())).thenReturn(ruleChain);
        when(ctx.getDeviceProfileCache()).thenReturn(deviceProfileCache);
        when(deviceProfileCache.get(any(), (DeviceId) any())).thenReturn(deviceProfile);

        node.init(ctx, nodeConfiguration);

        //WHEN
        node.onMsg(ctx, msg);

        //THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<RuleChainId> ruleChainArgumentCaptor = ArgumentCaptor.forClass(RuleChainId.class);
        verify(ctx).input(newMsgCaptor.capture(), ruleChainArgumentCaptor.capture());
        RuleChainId expectedRuleChainId = ruleChainArgumentCaptor.getValue();
        assertThat(expectedRuleChainId).isNotEqualTo(ruleChain.getId());
        assertThat(expectedRuleChainId).isEqualTo(defaultRuleChain.getId());
    }

    @Test
    public void givenForwardMsgToRootIsTrueWithoutDeviceProfile_whenOnMsg_thenShouldTransferToRootRuleChain() throws TbNodeException {
        //GIVEN
        config.setForwardMsgToRootRuleChain(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        device.setDeviceProfileId(deviceProfileId);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleChainService()).thenReturn(ruleChainService);
        when(ruleChainService.getRootTenantRuleChain(any())).thenReturn(ruleChain);
        when(ctx.getDeviceProfileCache()).thenReturn(deviceProfileCache);
        when(deviceProfileCache.get(any(), (DeviceId) any())).thenReturn(deviceProfile);

        node.init(ctx, nodeConfiguration);

        //WHEN
        node.onMsg(ctx, msg);

        //THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<RuleChainId> ruleChainArgumentCaptor = ArgumentCaptor.forClass(RuleChainId.class);
        verify(ctx).input(newMsgCaptor.capture(), ruleChainArgumentCaptor.capture());
        assertThat(ruleChainArgumentCaptor.getValue()).isEqualTo(ruleChain.getId());
    }

    @Test
    public void givenRuleChainId_whenOnMsg_thenShouldTransferToRuleChainById() throws TbNodeException {
        //GIVEN
        config.setRuleChainId(EXISTING_RULE_CHAIN_ID);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        node.init(ctx, nodeConfiguration);

        //WHEN
        node.onMsg(ctx, msg);

        //THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<RuleChainId> ruleChainArgumentCaptor = ArgumentCaptor.forClass(RuleChainId.class);
        verify(ctx).input(newMsgCaptor.capture(), ruleChainArgumentCaptor.capture());
        assertThat(ruleChainArgumentCaptor.getValue()).isEqualTo(ruleChain.getId());
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                //config for version 0
                Arguments.of(0,
                        "{\"ruleChainId\": null}",
                        true,
                        "{\"ruleChainId\": null, \"forwardMsgToRootRuleChain\": false}"
                ),
                //config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"ruleChainId\": null, \"forwardMsgToRootRuleChain\": false}",
                        false,
                        "{\"ruleChainId\": null, \"forwardMsgToRootRuleChain\": false}"
                )
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

    private TbMsg getMsg(EntityId entityId) {
        final Map<String, String> metaDataMap = Map.of(
                "data", "40"
        );
        final TbMsgMetaData metaData = new TbMsgMetaData(metaDataMap);
        final String data = "{ temp: 42, humidity: 77 }";
        return TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, entityId, metaData, data, callback);
    }
}
