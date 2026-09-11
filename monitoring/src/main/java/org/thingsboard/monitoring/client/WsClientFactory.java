// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.client;

import lombok.RequiredArgsConstructor;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class WsClientFactory {

    private final MonitoringReporter monitoringReporter;
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
        monitoringReporter.reportLatency(Latencies.WS_CONNECT, stopWatch.getTime());
        return wsClient;
    }

}
