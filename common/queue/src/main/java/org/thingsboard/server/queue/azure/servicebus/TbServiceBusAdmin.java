/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.queue.azure.servicebus;

import com.microsoft.azure.servicebus.management.ManagementClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnExpression("'${queue.type:null}'=='service-bus'")
public class TbServiceBusAdmin implements TbQueueAdmin {

    private final Set<String> queues = ConcurrentHashMap.newKeySet();

    private final ManagementClient client;

    public TbServiceBusAdmin(TbServiceBusSettings serviceBusSettings) {
        ConnectionStringBuilder builder = new ConnectionStringBuilder(
                serviceBusSettings.getNamespaceName(),
                "queues",
                serviceBusSettings.getSasKeyName(),
                serviceBusSettings.getSasKey());

        client = new ManagementClient(builder);

        try {
            client.getQueues().forEach(queueDescription -> queues.add(queueDescription.getPath()));
        } catch (ServiceBusException | InterruptedException e) {
            log.error("Failed to get queues.", e);
            throw new RuntimeException("Failed to get queues.", e);
        }
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        if (queues.contains(topic)) {
            return;
        }

        try {
            client.createQueue(topic);
            queues.add(topic);
        } catch (ServiceBusException | InterruptedException e) {
            log.error("Failed to create queue: [{}]", topic, e);
        }
    }

    public void destroy() {
        try {
            client.close();
        } catch (IOException e) {
            log.error("Failed to close ManagementClient.");
        }
    }
}
