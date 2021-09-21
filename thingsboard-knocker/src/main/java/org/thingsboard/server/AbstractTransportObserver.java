package org.thingsboard.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.telemetry.cmd.LatestValueCmd;
import org.thingsboard.server.common.data.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.common.data.telemetry.wrapper.TelemetryPluginCmdsWrapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.UUID;

public abstract class AbstractTransportObserver implements TransportObserver {

    protected final ObjectMapper mapper = new ObjectMapper();

    private final String WS_URL = "ws://localhost:8080";

    private final String token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzb21lQGdtYWlsLmNvbSIsInNjb3BlcyI6WyJURU5BTlRfQURNSU4iXSwidXNlcklkIjoiYjMzZmU2ZDAtMTIzYy0xMWVjLWFjZTctZjdhMzBkNjEzMGE1IiwiZW5hYmxlZCI6dHJ1ZSwiaXNQdWJsaWMiOmZhbHNlLCJ0ZW5hbnRJZCI6ImFiMDAxNWQwLTEyM2MtMTFlYy1hY2U3LWY3YTMwZDYxMzBhNSIsImN1c3RvbWVySWQiOiIxMzgxNDAwMC0xZGQyLTExYjItODA4MC04MDgwODA4MDgwODAiLCJpc3MiOiJ0aGluZ3Nib2FyZC5pbyIsImlhdCI6MTYzMjIzNDI5NiwiZXhwIjoxNjMyMjQzMjk2fQ.ns1ttCQus2WMHnHMVTfUgWMRaTdgjImleF0jbl2ZRyh8MhrQgk6d1AEqvo75X9rc_mHD6yd9XBxEIKKYzrEYDQ"; // FIXME: 21.09.21 token will expire

    private static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\"}";


    @Value("${websocket.wait_time}")
    protected int websocketWaitTime;

    protected TelemetryPluginCmdsWrapper getTelemetryCmdsWrapper(UUID uuid) {
        DeviceId deviceId = new DeviceId(uuid);
        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(deviceId);
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, TransportsMonitoringScheduler.PAYLOAD_KEY_STR)));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));
        return wrapper;
    }

    protected WebSocketClientImpl buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        WebSocketClientImpl webSocketClient = new WebSocketClientImpl(new URI(WS_URL + "/api/ws/plugins/telemetry?token=" + token));
        System.out.println(webSocketClient.connectBlocking());
        return webSocketClient;
    }

}
