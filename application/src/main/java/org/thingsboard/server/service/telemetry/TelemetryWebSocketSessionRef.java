package org.thingsboard.server.service.telemetry;

import lombok.Getter;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Created by ashvayka on 27.03.18.
 */
public class TelemetryWebSocketSessionRef {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String sessionId;
    @Getter
    private final SecurityUser securityCtx;
    @Getter
    private final InetSocketAddress localAddress;
    @Getter
    private final InetSocketAddress remoteAddress;

    public TelemetryWebSocketSessionRef(String sessionId, SecurityUser securityCtx, InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        this.sessionId = sessionId;
        this.securityCtx = securityCtx;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelemetryWebSocketSessionRef that = (TelemetryWebSocketSessionRef) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "TelemetryWebSocketSessionRef{" +
                "sessionId='" + sessionId + '\'' +
                ", localAddress=" + localAddress +
                ", remoteAddress=" + remoteAddress +
                '}';
    }
}
