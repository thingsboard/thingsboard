/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslHandler;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.quota.host.HostRequestsQuotaService;
import org.thingsboard.server.kafka.TbNodeIdProvider;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ashvayka on 04.10.18.
 */
@Slf4j
@ConditionalOnProperty(prefix = "transport.mqtt", value = "enabled", havingValue = "true", matchIfMissing = true)
@Component
@Data
public class MqttTransportContext {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TransportService transportService;

    @Autowired(required = false)
    private MqttSslHandlerProvider sslHandlerProvider;

    @Autowired(required = false)
    private HostRequestsQuotaService quotaService;

    @Autowired
    private MqttTransportAdaptor adaptor;

    @Autowired
    private TbNodeIdProvider nodeIdProvider;

    @Value("${transport.mqtt.netty.max_payload_size}")
    private Integer maxPayloadSize;


    private SslHandler sslHandler;

    @Getter
    private ExecutorService executor;

    @PostConstruct
    public void init() {
        executor = Executors.newCachedThreadPool();
    }

    @PreDestroy
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public String getNodeId() {
        return nodeIdProvider.getNodeId();
    }
}
