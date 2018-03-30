package org.thingsboard.server.service.telemetry;

import lombok.Data;
import lombok.Getter;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Created by ashvayka on 27.03.18.
 */
@Data
public class TelemetryWebSocketTextMsg {

    private final TelemetryWebSocketSessionRef sessionRef;
    private final String payload;

}
