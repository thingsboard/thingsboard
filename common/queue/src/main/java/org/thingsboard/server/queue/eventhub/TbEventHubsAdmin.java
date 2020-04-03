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
package org.thingsboard.server.queue.eventhub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TbEventHubsAdmin implements TbQueueAdmin {

    private final TbEventHubsSettings eventHubsSettings;

    private static final String BODY = "<entry xmlns='http://www.w3.org/2005/Atom'>  \n" +
            "  <content type='application/xml'>  \n" +
            "    <EventHubDescription xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://schemas.microsoft.com/netservices/2010/10/servicebus/connect\">  \n" +
            "      <MessageRetentionInDays>1</MessageRetentionInDays>  \n" +
            "      <PartitionCount>1</PartitionCount>  \n" +
            "    </EventHubDescription>  \n" +
            "  </content>  \n" +
            "</entry>  ";

    private RestTemplate restTemplate;

    private final Set<String> topics = ConcurrentHashMap.newKeySet();

    public TbEventHubsAdmin(TbEventHubsSettings eventHubsSettings) {
        this.eventHubsSettings = eventHubsSettings;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        if (topics.contains(topic)) {
            return;
        }

        String sasToken = eventHubsSettings.getSasToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", sasToken);
        try {
            restTemplate.exchange(
                    "https://{namespaceName}.servicebus.windows.net/{eventHubName}?api-version=2014-01",
                    HttpMethod.PUT, new HttpEntity<>(BODY, headers), String.class, eventHubsSettings.getNamespaceName(), topic);
            topics.add(topic);
            log.info("Create new Event Hub: [{}]", topic);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                topics.add(topic);
                log.info("Event Hub is exist: [{}]", topic);
            } else {
                log.error("Failed to create Event Hub: [{}]", topic, e);
                throw e;
            }
        }
    }

}
