package org.thingsboard.server.channels;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TransportInfo;
import org.thingsboard.server.TransportType;

@Component
@Slf4j
public class ConsoleNotificationChannel implements NotificationChannel {

    @Override
    public void onTransportUnavailable(TransportInfo transportInfo) {
        log.error(String.valueOf(transportInfo));
    }
}
