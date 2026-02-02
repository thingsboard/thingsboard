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
package org.thingsboard.rule.engine.flow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbRuleChainInputNodeTest extends AbstractRuleNodeUpgradeTest {

    private final TenantId TENANT_ID = new TenantId(UUID.fromString("4ba69ea5-6b27-42df-ab66-e7a727a67027"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("97731954-2147-4176-8f1a-d14f1b73e4e6"));
    private final AssetId ASSET_ID = new AssetId(UUID.fromString("841a47bd-4e8e-4ea5-88e6-420da0d70e51"));

    private TbRuleChainInputNode node;
    private TbRuleChainInputNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;

    @Mock
    private TbContext ctxMock;
    @Mock
    private RuleEngineDeviceProfileCache deviceProfileCacheMock;
    @Mock
    private RuleEngineAssetProfileCache assetProfileCacheMock;

    @BeforeEach
    public void setUp() {
        node = spy(new TbRuleChainInputNode());
        config = new TbRuleChainInputNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getRuleChainId()).isNull();
        assertThat(config.isForwardMsgToDefaultRuleChain()).isFalse();
    }

    @ParameterizedTest
    @MethodSource
    public void givenValidConfig_whenInit_thenOk(String ruleChainIdStr, boolean forwardMsgToDefaultRuleChain) throws TbNodeException {
        //GIVEN
        config.setRuleChainId(ruleChainIdStr);
        config.setForwardMsgToDefaultRuleChain(forwardMsgToDefaultRuleChain);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        //WHEN
        assertThatCode(() -> node.init(ctxMock, nodeConfiguration))
                .doesNotThrowAnyException();

        //THEN
        verify(ctxMock).checkTenantEntity(new RuleChainId(UUID.fromString(ruleChainIdStr)));
    }

    private static Stream<Arguments> givenValidConfig_whenInit_thenOk() {
        return Stream.of(
                Arguments.of("45bba7c4-04bf-419b-ae03-6ceb9724f10e", false),
                Arguments.of("52d57e1b-70bb-480e-bcc4-6710e1dcc9d8", true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"91acbce0-079fdb", "", "  ", "my test string"})
    public void givenInvalidRuleChainId_whenInit_thenThrowsException(String ruleChainIdStr) {
        //GIVEN
        config.setRuleChainId(ruleChainIdStr);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        //WHEN-THEN
        Assertions.assertThatThrownBy(() -> node.init(ctxMock, nodeConfiguration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Failed to parse rule chain id: " + ruleChainIdStr);
    }

    @Test
    public void givenRuleChainIdIsNotSet_whenInit_thenThrowsException() {
        assertThatThrownBy(() -> node.init(ctxMock, nodeConfiguration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Rule chain must be set!");
    }

    @Test
    public void givenForwardMsgToDefaultIsTrue_whenOnMsg_thenShouldTransferToDeviceDefaultRuleChain() throws TbNodeException {
        //GIVEN
        DeviceProfile deviceProfile = new DeviceProfile();
        RuleChainId defaultRuleChainId = new RuleChainId(UUID.fromString("196e3cd5-68b8-421e-a0cf-1d44fa377cdf"));
        deviceProfile.setDefaultRuleChainId(defaultRuleChainId);

        TbMsg msg = getMsg(DEVICE_ID);

        String ruleChainIdFromConfigStr = "acbc924f-7f95-4a9b-a854-e4822deb74c7";
        config.setRuleChainId(ruleChainIdFromConfigStr);
        config.setForwardMsgToDefaultRuleChain(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDeviceProfileCache()).thenReturn(deviceProfileCacheMock);
        when(deviceProfileCacheMock.get(any(TenantId.class), any(DeviceId.class))).thenReturn(deviceProfile);

        node.init(ctxMock, nodeConfiguration);

        //WHEN
        node.onMsg(ctxMock, msg);

        //THEN
        ArgumentCaptor<RuleChainId> ruleChainArgumentCaptor = ArgumentCaptor.forClass(RuleChainId.class);
        verify(ctxMock).input(eq(msg), ruleChainArgumentCaptor.capture());
        RuleChainId expectedRuleChainId = ruleChainArgumentCaptor.getValue();
        assertThat(expectedRuleChainId).isEqualTo(defaultRuleChainId);

        RuleChainId ruleChainId = (RuleChainId) ReflectionTestUtils.getField(node, "ruleChainId");
        assertThat(ruleChainId).isEqualTo(new RuleChainId(UUID.fromString(ruleChainIdFromConfigStr)));
    }

    @Test
    public void givenForwardMsgToDefaultIsTrue_whenOnMsg_thenShouldTransferToAssetDefaultRuleChain() throws TbNodeException {
        //GIVEN
        AssetProfile assetProfile = new AssetProfile();
        RuleChainId defaultRuleChainId = new RuleChainId(UUID.fromString("f0a3cd58-980c-4730-a40c-8f59064d2065"));
        assetProfile.setDefaultRuleChainId(defaultRuleChainId);

        TbMsg msg = getMsg(ASSET_ID);

        String ruleChainIdFromConfigStr = "56f1c0b8-1a00-4ce0-b3ab-a1416d7cc429";
        config.setRuleChainId(ruleChainIdFromConfigStr);
        config.setForwardMsgToDefaultRuleChain(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getAssetProfileCache()).thenReturn(assetProfileCacheMock);
        when(assetProfileCacheMock.get(any(TenantId.class), any(AssetId.class))).thenReturn(assetProfile);

        node.init(ctxMock, nodeConfiguration);

        //WHEN
        node.onMsg(ctxMock, msg);

        //THEN
        ArgumentCaptor<RuleChainId> ruleChainArgumentCaptor = ArgumentCaptor.forClass(RuleChainId.class);
        verify(ctxMock).input(eq(msg), ruleChainArgumentCaptor.capture());
        RuleChainId expectedRuleChainId = ruleChainArgumentCaptor.getValue();
        assertThat(expectedRuleChainId).isEqualTo(defaultRuleChainId);

        RuleChainId ruleChainId = (RuleChainId) ReflectionTestUtils.getField(node, "ruleChainId");
        assertThat(ruleChainId).isEqualTo(new RuleChainId(UUID.fromString(ruleChainIdFromConfigStr)));
    }

    @Test
    public void givenForwardMsgToDefaultIsTrueWithoutDeviceDefaultRuleChain_whenOnMsg_thenShouldTransferToRuleChainFromConfig() throws TbNodeException {
        //GIVEN
        DeviceProfile deviceProfile = new DeviceProfile();

        TbMsg msg = getMsg(DEVICE_ID);

        String ruleChainIdFromConfigStr = "357c2785-e7cc-46a8-9797-957180dabdeb";
        RuleChainId ruleChainIdFromConfig = new RuleChainId(UUID.fromString(ruleChainIdFromConfigStr));
        config.setRuleChainId(ruleChainIdFromConfigStr);
        config.setForwardMsgToDefaultRuleChain(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDeviceProfileCache()).thenReturn(deviceProfileCacheMock);
        when(deviceProfileCacheMock.get(any(TenantId.class), any(DeviceId.class))).thenReturn(deviceProfile);

        node.init(ctxMock, nodeConfiguration);

        //WHEN
        node.onMsg(ctxMock, msg);

        //THEN
        ArgumentCaptor<RuleChainId> ruleChainArgumentCaptor = ArgumentCaptor.forClass(RuleChainId.class);
        verify(ctxMock).input(eq(msg), ruleChainArgumentCaptor.capture());
        assertThat(ruleChainArgumentCaptor.getValue()).isEqualTo(ruleChainIdFromConfig);
    }

    @Test
    public void givenForwardMsgToDefaultIsTrueWithoutAssetDefaultRuleChain_whenOnMsg_thenShouldTransferToRuleChainFromConfig() throws TbNodeException {
        //GIVEN
        AssetProfile assetProfile = new AssetProfile();

        TbMsg msg = getMsg(ASSET_ID);

        String ruleChainIdFromConfigStr = "12883c3d-c10b-4d5b-b606-a59385a920bc";
        RuleChainId ruleChainIdFromConfig = new RuleChainId(UUID.fromString(ruleChainIdFromConfigStr));
        config.setRuleChainId(ruleChainIdFromConfigStr);
        config.setForwardMsgToDefaultRuleChain(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getAssetProfileCache()).thenReturn(assetProfileCacheMock);
        when(assetProfileCacheMock.get(any(TenantId.class), any(AssetId.class))).thenReturn(assetProfile);

        node.init(ctxMock, nodeConfiguration);

        //WHEN
        node.onMsg(ctxMock, msg);

        //THEN
        ArgumentCaptor<RuleChainId> ruleChainArgumentCaptor = ArgumentCaptor.forClass(RuleChainId.class);
        verify(ctxMock).input(eq(msg), ruleChainArgumentCaptor.capture());
        assertThat(ruleChainArgumentCaptor.getValue()).isEqualTo(ruleChainIdFromConfig);
    }

    @Test
    public void givenRuleChainInConfig_whenOnMsg_thenShouldTransferToRuleChainFromConfig() throws TbNodeException {
        //GIVEN
        String ruleChainIdFromConfigStr = "3c02c8b3-645c-4e67-aac5-f984f59471d1";
        RuleChainId ruleChainIdFromConfig = new RuleChainId(UUID.fromString(ruleChainIdFromConfigStr));

        TbMsg msg = getMsg(DEVICE_ID);

        config.setRuleChainId(ruleChainIdFromConfigStr);
        config.setForwardMsgToDefaultRuleChain(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        node.init(ctxMock, nodeConfiguration);

        //WHEN
        node.onMsg(ctxMock, msg);

        //THEN
        ArgumentCaptor<RuleChainId> ruleChainArgumentCaptor = ArgumentCaptor.forClass(RuleChainId.class);
        verify(ctxMock).input(eq(msg), ruleChainArgumentCaptor.capture());
        assertThat(ruleChainArgumentCaptor.getValue()).isEqualTo(ruleChainIdFromConfig);
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                //config for version 0
                Arguments.of(0,
                        "{\"ruleChainId\": null}",
                        true,
                        "{\"ruleChainId\": null, \"forwardMsgToDefaultRuleChain\": false}"
                ),
                //config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"ruleChainId\": null, \"forwardMsgToDefaultRuleChain\": false}",
                        false,
                        "{\"ruleChainId\": null, \"forwardMsgToDefaultRuleChain\": false}"
                )
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

    private TbMsg getMsg(EntityId entityId) {
        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(entityId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
    }
}
