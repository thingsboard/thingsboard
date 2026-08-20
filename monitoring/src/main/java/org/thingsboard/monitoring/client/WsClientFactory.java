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
package org.thingsboard.monitoring.client;

import lombok.RequiredArgsConstructor;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.metrics.ProbeMetricsRecorder;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class WsClientFactory {

    private final MonitoringReporter monitoringReporter;
    private final ProbeMetricsRecorder probeMetricsRecorder;
    private final TbStopWatch stopWatch;
    @Value("${monitoring.ws.base_url}")
    private String baseUrl;
    @Value("${monitoring.ws.request_timeout_ms}")
    private int requestTimeoutMs;

    public WsClient createClient(String accessToken) throws Exception {
        URI uri = new URI(baseUrl + "/api/ws/plugins/telemetry?token=" + accessToken);
        stopWatch.start();
        WsClient wsClient = new WsClient(uri, requestTimeoutMs);
        if (baseUrl.startsWith("wss")) {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
            wsClient.setSocketFactory(builder.build().getSocketFactory());
        }
        boolean connected = wsClient.connectBlocking(requestTimeoutMs, TimeUnit.MILLISECONDS);
        if (!connected) {
            throw new IllegalStateException("Failed to establish WS session");
        }
        // TbStopWatch.getTime() stops/resets internally, so it can only be called once here -
        // reuse the same value for both the latency report and the probe metric
        long connectLatencyNanos = stopWatch.getTime();
        monitoringReporter.reportLatency(Latencies.WS_CONNECT, connectLatencyNanos);
        probeMetricsRecorder.recordActionDuration(MonitoredServiceKey.WS, "connect", connectLatencyNanos / 1_000_000);
        return wsClient;
    }

}
