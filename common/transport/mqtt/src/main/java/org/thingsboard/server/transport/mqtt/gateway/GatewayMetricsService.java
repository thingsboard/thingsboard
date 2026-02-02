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
import org.thingsboard.server.common.msg.gateway.metrics.GatewayMetadata;
import org.thingsboard.server.transport.mqtt.gateway.metrics.GatewayMetricsState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbMqttTransportComponent
public class GatewayMetricsService {

    public static final String GATEWAY_METRICS = "gatewayMetrics";

    @Value("${transport.mqtt.gateway_metrics_report_interval_sec:60}")
    private int metricsReportIntervalSec;

    @Autowired
    private SchedulerComponent scheduler;

    @Autowired
    private TransportService transportService;

    private Map<DeviceId, GatewayMetricsState> states = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        scheduler.scheduleAtFixedRate(this::reportMetrics, metricsReportIntervalSec, metricsReportIntervalSec, TimeUnit.SECONDS);
    }

    public void process(TransportProtos.SessionInfoProto sessionInfo, DeviceId gatewayId, List<GatewayMetadata> data, long serverReceiveTs) {
        states.computeIfAbsent(gatewayId, k -> new GatewayMetricsState(sessionInfo)).update(data, serverReceiveTs);
    }

    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceId gatewayId) {
        var state = states.get(gatewayId);
        if (state != null) {
            state.updateSessionInfo(sessionInfo);
        }
    }

    public void onDeviceDelete(DeviceId deviceId) {
        states.remove(deviceId);
    }

    public void reportMetrics() {
        if (states.isEmpty()) {
            return;
        }
        Map<DeviceId, GatewayMetricsState> statesToReport = states;
        states = new ConcurrentHashMap<>();

        long ts = System.currentTimeMillis();

        statesToReport.forEach((gatewayId, state) -> {
            reportMetrics(state, ts);
        });
    }

    private void reportMetrics(GatewayMetricsState state, long ts) {
        if (state.isEmpty()) {
            return;
        }
        var result = state.getStateResult();
        var kvProto = TransportProtos.KeyValueProto.newBuilder()
                .setKey(GATEWAY_METRICS)
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
