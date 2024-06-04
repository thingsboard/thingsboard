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
package org.thingsboard.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
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

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class TbMsgCountNodeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("773ccb0e-e822-44b7-97aa-10de83ad081d"));
    private final ThingsBoardThreadFactory factory = ThingsBoardThreadFactory.forName("msg-count-node-test");
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(factory);


    private TbMsgCountNode node;
    private TbMsgCountNodeConfiguration config;
    private CountDownLatch awaitTellSelfLatch;

    @Mock
    private TbContext ctxMock;

    @BeforeEach
    public void setUp() {
        node = new TbMsgCountNode();
        config = new TbMsgCountNodeConfiguration().defaultConfiguration();
    }

    @AfterEach
    public void tearDown() {
        executorService.shutdownNow();
        node.destroy();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getInterval()).isEqualTo(1);
        assertThat(config.getTelemetryPrefix()).isEqualTo("messageCount");
    }

    @Test
    public void givenIncomingMsgs_whenOnMsg_thenSendsMsgWithMsgCount() throws TbNodeException, InterruptedException {
        int msgCountInterval = 1;
        config.setInterval(msgCountInterval);
        config.setTelemetryPrefix("count");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(1);
        AtomicInteger currentMsgNumber = new AtomicInteger(0);
        AtomicBoolean isMsgWithCounterSent = new AtomicBoolean(false);

        invokeTellSelf(isMsgWithCounterSent, config.getInterval());
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getServiceId()).thenReturn("tb-rule-engine");
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.fromString("4ac734ea-2434-4402-b61b-eaa66b650711"));
        when(ctxMock.getSelfId()).thenReturn(ruleNodeId);
        TbMsg tickMsg = TbMsg.newMsg(TbMsgType.MSG_COUNT_SELF_MSG, ruleNodeId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        when(ctxMock.newMsg(null, TbMsgType.MSG_COUNT_SELF_MSG, ruleNodeId, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING)).thenReturn(tickMsg);

        node.init(ctxMock, configuration);

        DeviceId deviceId = new DeviceId(UUID.fromString("e7e2392a-16fe-4b2b-81db-ec99a0edd54f"));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        for (int i = 0; i < msgCount; i++) {
            if (isMsgWithCounterSent.get()) {
                break;
            }
            node.onMsg(ctxMock, msg);
            currentMsgNumber.getAndIncrement();
        }

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(currentMsgNumber.get())).ack(msgCaptor.capture());
        msgCaptor.getAllValues().forEach(captor -> assertThat(captor).isEqualTo(msg));
        ArgumentCaptor<TbMsg> msgWithCounterCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock).enqueueForTellNext(msgWithCounterCaptor.capture(), eq(TbNodeConnectionType.SUCCESS));
        TbMsg resultedMsg = msgWithCounterCaptor.getValue();
        String expectedData = "{\"count_tb-rule-engine\":" + currentMsgNumber + "}";
        TbMsg expectedMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TENANT_ID, TbMsgMetaData.EMPTY, expectedData);
        assertThat(resultedMsg).usingRecursiveComparison()
                .ignoringFields("id", "ts", "ctx", "metaData")
                .isEqualTo(expectedMsg);
        assertThat(resultedMsg.getMetaData().getData()).hasFieldOrProperty("delta");
    }

    private void invokeTellSelf(AtomicBoolean outgoingMsgSent, int interval) {
        doAnswer((Answer<Void>) invocationOnMock -> {
            executorService.schedule(() -> {
                TbMsg tickMsg = invocationOnMock.getArgument(0);
                outgoingMsgSent.set(true);
                node.onMsg(ctxMock, tickMsg);
                awaitTellSelfLatch.countDown();
            }, interval, TimeUnit.SECONDS);
            return null;
        }).when(ctxMock).tellSelf(ArgumentMatchers.any(TbMsg.class), ArgumentMatchers.anyLong());
    }

}
