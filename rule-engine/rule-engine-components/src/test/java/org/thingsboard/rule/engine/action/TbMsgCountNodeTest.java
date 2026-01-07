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
package org.thingsboard.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.times;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class TbMsgCountNodeTest {

    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("ee682a85-7f5a-4182-91bc-46e555138fe2"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("1b21c7cc-0c9e-4ab1-b867-99451599e146"));
    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("04dfbd38-10e5-47b7-925f-11e795db89e1"));

    private final TbMsg tickMsg = TbMsg.newMsg()
            .type(TbMsgType.MSG_COUNT_SELF_MSG)
            .originator(RULE_NODE_ID)
            .copyMetaData(TbMsgMetaData.EMPTY)
            .data(TbMsg.EMPTY_STRING)
            .build();

    private ScheduledExecutorService executorService;
    private TbMsgCountNode node;
    private TbMsgCountNodeConfiguration config;

    @Mock
    private TbContext ctxMock;

    @BeforeEach
    public void setUp() {
        node = new TbMsgCountNode();
        config = new TbMsgCountNodeConfiguration().defaultConfiguration();
        executorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("msg-count-node-test");
    }

    @AfterEach
    public void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        node.destroy();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getInterval()).isEqualTo(1);
        assertThat(config.getTelemetryPrefix()).isEqualTo("messageCount");
    }

    @Test
    public void givenIncomingMsgs_whenOnMsg_thenSendsMsgWithMsgCount() throws TbNodeException, InterruptedException {
        // GIVEN
        int msgCount = 100;
        var awaitTellSelfLatch = new CountDownLatch(1);
        var currentMsgNumber = new AtomicInteger(0);
        var msgWithCounterSent = new AtomicBoolean(false);

        willAnswer((Answer<Void>) invocationOnMock -> {
            executorService.schedule(() -> {
                TbMsg tickMsg = invocationOnMock.getArgument(0);
                msgWithCounterSent.set(true);
                node.onMsg(ctxMock, tickMsg);
                awaitTellSelfLatch.countDown();
            }, config.getInterval(), TimeUnit.SECONDS);
            return null;
        }).given(ctxMock).tellSelf(any(TbMsg.class), anyLong());
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getServiceId()).willReturn("tb-rule-engine");
        given(ctxMock.getSelfId()).willReturn(RULE_NODE_ID);
        given(ctxMock.newMsg(null, TbMsgType.MSG_COUNT_SELF_MSG, RULE_NODE_ID, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING)).willReturn(tickMsg);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var expectedProcessedMsgs = new ArrayList<TbMsg>();
        for (int i = 0; i < msgCount; i++) {
            var msg = TbMsg.newMsg()
                    .type(TbMsgType.POST_TELEMETRY_REQUEST)
                    .originator(DEVICE_ID)
                    .copyMetaData(TbMsgMetaData.EMPTY)
                    .data(TbMsg.EMPTY_STRING)
                    .build();
            if (msgWithCounterSent.get()) {
                break;
            }
            node.onMsg(ctxMock, msg);
            expectedProcessedMsgs.add(msg);
            currentMsgNumber.getAndIncrement();
        }

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should(times(currentMsgNumber.get())).ack(msgCaptor.capture());
        var actualProcessedMsgs = msgCaptor.getAllValues();
        assertThat(actualProcessedMsgs).hasSize(expectedProcessedMsgs.size());
        assertThat(actualProcessedMsgs).isNotEmpty();
        assertThat(actualProcessedMsgs).containsExactlyInAnyOrderElementsOf(expectedProcessedMsgs);

        ArgumentCaptor<TbMsg> msgWithCounterCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(msgWithCounterCaptor.capture(), eq(TbNodeConnectionType.SUCCESS));
        TbMsg resultedMsg = msgWithCounterCaptor.getValue();
        String expectedData = "{\"messageCount_tb-rule-engine\":" + currentMsgNumber + "}";
        TbMsg expectedMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TENANT_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(expectedData)
                .build();
        assertThat(resultedMsg).usingRecursiveComparison()
                .ignoringFields("id", "ts", "ctx", "metaData")
                .isEqualTo(expectedMsg);
        Map<String, String> actualMetadata = resultedMsg.getMetaData().getData();
        assertThat(actualMetadata).hasFieldOrProperty("delta");
    }

}
