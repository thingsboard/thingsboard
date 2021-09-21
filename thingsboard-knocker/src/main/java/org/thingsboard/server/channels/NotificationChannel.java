package org.thingsboard.server.channels;

import org.thingsboard.server.TransportType;

public interface NotificationChannel {
    void onTransportUnavailable(TransportType transportType);
}
