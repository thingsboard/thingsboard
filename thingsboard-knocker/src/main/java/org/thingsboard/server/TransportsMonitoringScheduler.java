package org.thingsboard.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.channels.NotificationChannel;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.telemetry.cmd.LatestValueCmd;
import org.thingsboard.server.common.data.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.common.data.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.common.data.telemetry.wrapper.TelemetryPluginCmdsWrapper;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransportsMonitoringScheduler {

    private final ObjectMapper mapper = new ObjectMapper();

    private final List<TransportObserver> transports;

    private final List<NotificationChannel> channels;

    public static final String PAYLOAD_KEY_STR = "key1";

    @PostConstruct
    public void startMonitoringTransports() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(transports.size());

        for (TransportObserver observer : transports) {
            executorService.scheduleAtFixedRate(() -> {
                String expectedValue = String.valueOf(System.currentTimeMillis());
                try {
                    String msg = observer.pingTransport(getPayload(expectedValue));
                    EntityDataUpdate update = mapper.readValue(msg, EntityDataUpdate.class); // FIXME: 21.09.21 msg might be null
                    List<EntityData> eData = update.getUpdate();
                    String responseValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get(PAYLOAD_KEY_STR).getValue();
                    if (!responseValue.equals(expectedValue))
                        onMonitoringFailure(observer.getTransportType("Violation of message validation"));
                    else
                        log.info(observer.getTransportType("Success").toString());

                } catch (Exception e) {
                    onMonitoringFailure(observer.getTransportType(e.getMessage()));
                }
            }, 0, observer.getMonitoringRate(), TimeUnit.MILLISECONDS);

        }
    }

    private void onMonitoringFailure(TransportType type) {
        for (NotificationChannel channel : channels) {
            channel.onTransportUnavailable(type);
        }
    }

    private String getPayload(String value) {
        return "{"+ "\"" + PAYLOAD_KEY_STR + "\"" + ":" + "\"" + value + "\"" + "}";
    }


}
