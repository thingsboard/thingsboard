package org.thingsboard.server.channels;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TransportInfo;
import org.thingsboard.server.TransportType;

@Component
@Slf4j
@ConditionalOnProperty(
        value="notifications.logging.enabled",
        havingValue = "true")
public class ConsoleNotificationChannel implements NotificationChannel {

    @Override
    public void onTransportUnavailable(TransportInfo transportInfo) {
        log.error(String.valueOf(transportInfo));
    }
}
