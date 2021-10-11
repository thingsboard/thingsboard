/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.channels.NotificationChannel;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.observers.AbstractTransportObserver;
import org.thingsboard.server.transport.TransportInfo;
import org.thingsboard.server.transport.TransportType;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransportsMonitoringScheduler {
    private final ObjectMapper mapper = new ObjectMapper();

    public static final String PAYLOAD_KEY_STR = "key1";

    private final List<AbstractTransportObserver> observers;
    private final List<NotificationChannel> channels;

    @Value("${common.initial_delay}")
    private int initialDelay;

    @Value("${common.failure_threshold}")
    private int failureThreshold;

    @Value("${notifications.on_success_enabled}")
    private boolean onSuccessEnabled;

    private Map<TransportType, AtomicInteger> failuresCountsMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void startMonitoringTransports() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(observers.size());

        for (AbstractTransportObserver observer : observers) {
            failuresCountsMap.put(observer.getTransportType(), new AtomicInteger(0));
            executorService.scheduleAtFixedRate(() -> {
                String expectedValue = String.valueOf(System.currentTimeMillis());
                try {
                    String msg = observer.pingTransport(toJsonPayload(expectedValue));
                    String receivedValue = getWebsocketUpdatedValue(msg).orElse(null);
                    if (!expectedValue.equals(receivedValue)) {
                        onMonitoringFailure(new TransportInfo(
                                observer.getTransportType(),
                                "Websocket update msg is empty or it differ from expected one")
                        );
                    } else {
                        onMonitoringSuccess(new TransportInfo(observer.getTransportType(), "Successfully"));
                    }

                } catch (Exception e) {
                    onMonitoringFailure(new TransportInfo(observer.getTransportType(), e.toString()));
                }
            }, initialDelay, observer.getMonitoringRate(), TimeUnit.MILLISECONDS);

        }
    }

    private void onMonitoringFailure(TransportInfo transportInfo) {
        AtomicInteger failureCount = failuresCountsMap.get(transportInfo.getTransportType());
        if (failureCount.get() >= failureThreshold) {
            for (NotificationChannel channel : channels) {
                channel.sendNotification(transportInfo);
            }
        }
        else {
            failureCount.incrementAndGet();
        }

    }

    private void onMonitoringSuccess(TransportInfo transportInfo) {
        if (onSuccessEnabled) {
            for (NotificationChannel channel : channels) {
                channel.sendNotification(transportInfo);
            }
        }
        failuresCountsMap.get(transportInfo.getTransportType()).set(0);

        log.info(transportInfo.toString());
    }

    private String toJsonPayload(String value) {
        return "{" + "\"" + PAYLOAD_KEY_STR + "\"" + ":" + "\"" + value + "\"" + "}";
    }

    private Optional<String> getWebsocketUpdatedValue(String msg) {
        try {
            if (msg == null)
                return Optional.empty();
            EntityDataUpdate update = mapper.readValue(msg, EntityDataUpdate.class);
            List<EntityData> eData = update.getUpdate();
            return Optional.ofNullable(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get(PAYLOAD_KEY_STR).getValue());
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }


}
