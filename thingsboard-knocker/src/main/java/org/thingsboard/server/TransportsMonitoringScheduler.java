package org.thingsboard.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.channels.NotificationChannel;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.telemetry.cmd.v2.EntityDataUpdate;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
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
                    String msg = observer.pingTransport(toJsonPayload(expectedValue));
                    String receivedValue = getWebsocketUpdatedValue(msg).orElse(null);
                    if (!expectedValue.equals(receivedValue)) {
                        onMonitoringFailure(new TransportInfo(observer.getTransportType(), "Transport didn't send websocket update or wrong message was received"));
                    }
                    else {
                        log.warn(observer.getTransportType().toString() + " | Successfully");
                    }

                } catch (Exception e) {
                    onMonitoringFailure(new TransportInfo(observer.getTransportType(), e.toString()));
                }
            }, 0, observer.getMonitoringRate(), TimeUnit.MILLISECONDS);

        }
    }

    private void onMonitoringFailure(TransportInfo transportInfo) {
        for (NotificationChannel channel : channels) {
            channel.onTransportUnavailable(transportInfo);
        }
    }

    private String toJsonPayload(String value) {
        return "{"+ "\"" + PAYLOAD_KEY_STR + "\"" + ":" + "\"" + value + "\"" + "}";
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
