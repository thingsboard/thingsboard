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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class QueueEdgeTest extends AbstractEdgeTest {

    @Test
    public void testQueues() throws Exception {
        loginSysAdmin();

        // create queue
        Queue queue = new Queue();
        queue.setName("EdgeMain");
        queue.setTopic("tb_rule_engine.EdgeMain");
        queue.setPollInterval(25);
        queue.setPartitions(10);
        queue.setConsumerPerPartition(false);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue.setProcessingStrategy(processingStrategy);

        edgeImitator.expectMessageAmount(1);
        Queue savedQueue = doPost("/api/queues?serviceType=" + ServiceType.TB_RULE_ENGINE.name(), queue, Queue.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof QueueUpdateMsg);
        QueueUpdateMsg queueUpdateMsg = (QueueUpdateMsg) latestMessage;
        Queue queueMsg = JacksonUtil.fromString(queueUpdateMsg.getEntity(), Queue.class, true);
        Assert.assertNotNull(queueMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, queueUpdateMsg.getMsgType());
        Assert.assertEquals(savedQueue, queueMsg);
        Assert.assertEquals("EdgeMain", queueMsg.getName());
        Assert.assertEquals("tb_rule_engine.EdgeMain", queueMsg.getTopic());
        Assert.assertEquals(25, queueMsg.getPollInterval());
        Assert.assertEquals(10, queueMsg.getPartitions());
        Assert.assertEquals(2000, queueMsg.getPackProcessingTimeout());
        Assert.assertEquals(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR, queueMsg.getSubmitStrategy().getType());
        Assert.assertEquals(0, queueMsg.getSubmitStrategy().getBatchSize());
        Assert.assertEquals(ProcessingStrategyType.RETRY_ALL, queueMsg.getProcessingStrategy().getType());
        Assert.assertEquals(3, queueMsg.getProcessingStrategy().getRetries());
        Assert.assertEquals(0.7, queueMsg.getProcessingStrategy().getFailurePercentage(), 1);
        Assert.assertEquals(3, queueMsg.getProcessingStrategy().getPauseBetweenRetries());
        Assert.assertEquals(5, queueMsg.getProcessingStrategy().getMaxPauseBetweenRetries());

        // update queue
        edgeImitator.expectMessageAmount(1);
        savedQueue.setPollInterval(50);
        savedQueue = doPost("/api/queues?serviceType=" + ServiceType.TB_RULE_ENGINE.name(), savedQueue, Queue.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof QueueUpdateMsg);
        queueUpdateMsg = (QueueUpdateMsg) latestMessage;
        queueMsg = JacksonUtil.fromString(queueUpdateMsg.getEntity(), Queue.class, true);
        Assert.assertNotNull(queueMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, queueUpdateMsg.getMsgType());
        Assert.assertEquals(50, queueMsg.getPollInterval());

        // delete queue
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/queues/" + savedQueue.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof QueueUpdateMsg);
        queueUpdateMsg = (QueueUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, queueUpdateMsg.getMsgType());
        Assert.assertEquals(savedQueue.getUuidId().getMostSignificantBits(), queueUpdateMsg.getIdMSB());
        Assert.assertEquals(savedQueue.getUuidId().getLeastSignificantBits(), queueUpdateMsg.getIdLSB());
    }

}
