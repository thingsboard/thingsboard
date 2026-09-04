// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TbRuleChainOutputNodeTest {

    private TbRuleChainOutputNode node;
    private EmptyNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;

    @Mock
    private TbContext ctxMock;

    @BeforeEach
    public void setUp() {
        node = spy(new TbRuleChainOutputNode());
        config = new EmptyNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getVersion()).isEqualTo(0);
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, nodeConfiguration));
    }

    @Test
    public void givenRuleNodeName_whenOnMsg_thenForwardMsgToTheCallerRuleChainWithRelationTypeMatchesWithRuleNodeName() throws TbNodeException {
        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("test");
        given(ctxMock.getSelf()).willReturn(ruleNode);

        node.init(ctxMock, nodeConfiguration);
        DeviceId deviceId = new DeviceId(UUID.fromString("f514da88-79b3-46da-9f02-1747c5e84f44"));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().output(msg, "test");
    }

}
