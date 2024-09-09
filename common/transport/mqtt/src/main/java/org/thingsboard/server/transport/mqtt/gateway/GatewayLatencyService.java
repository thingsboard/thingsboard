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
package org.thingsboard.server.transport.mqtt.gateway;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.transport.mqtt.TbMqttTransportComponent;
import org.thingsboard.server.transport.mqtt.gateway.latency.GatewayLatencyData;
import org.thingsboard.server.transport.mqtt.gateway.latency.GatewayLatencyState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbMqttTransportComponent
public class GatewayLatencyService {

    @Value("${transport.mqtt.gateway_latency_report_interval_sec:3600}")
    private int latencyReportIntervalSec;

    @Autowired
    private SchedulerComponent scheduler;

    @Autowired
    private TransportService transportService;

    private Map<DeviceId, GatewayLatencyState> states = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        scheduler.scheduleAtFixedRate(this::reportLatency, latencyReportIntervalSec, latencyReportIntervalSec, TimeUnit.SECONDS);
    }

    public void process(TransportProtos.SessionInfoProto sessionInfo, DeviceId gatewayId, Map<String, GatewayLatencyData> data, long ts) {
        states.computeIfAbsent(gatewayId, k -> new GatewayLatencyState(sessionInfo)).update(ts, data);
    }

    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceId gatewayId) {
        var state = states.get(gatewayId);
        if (state != null) {
            state.updateSessionInfo(sessionInfo);
        }
    }

    public void onDeviceDelete(DeviceId deviceId) {
        var state = states.remove(deviceId);
        if (state != null) {
            state.clear();
        }
    }

    public void onDeviceDisconnect(DeviceId deviceId) {
        GatewayLatencyState state = states.remove(deviceId);
        if (state != null) {
            reportLatency(state, System.currentTimeMillis());
        }
    }

    public void reportLatency() {
        if (states.isEmpty()) {
            return;
        }
        Map<DeviceId, GatewayLatencyState> oldStates = states;
        states = new ConcurrentHashMap<>();

        long ts = System.currentTimeMillis();

        oldStates.forEach((gatewayId, state) -> {
            reportLatency(state, ts);
        });
        oldStates.clear();
    }

    private void reportLatency(GatewayLatencyState state, long ts) {
        if (state.isEmpty()) {
            return;
        }
        var result = state.getLatencyStateResult();
        state.clear();
        var kvProto = TransportProtos.KeyValueProto.newBuilder()
                .setKey("latencyCheck")
                .setType(TransportProtos.KeyValueType.JSON_V)
                .setJsonV(JacksonUtil.toString(result))
                .build();

        TransportProtos.TsKvListProto tsKvList = TransportProtos.TsKvListProto.newBuilder()
                .setTs(ts)
                .addKv(kvProto)
                .build();

        TransportProtos.PostTelemetryMsg telemetryMsg = TransportProtos.PostTelemetryMsg.newBuilder()
                .addTsKvList(tsKvList)
                .build();

        transportService.process(state.getSessionInfo(), telemetryMsg, TransportServiceCallback.EMPTY);
    }

}
