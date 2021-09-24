package org.thingsboard.server.channels;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.TransportInfo;
import org.thingsboard.server.TransportType;

public interface NotificationChannel {

    ObjectMapper mapper = new ObjectMapper();

    void onTransportUnavailable(TransportInfo transportInfo);
}
