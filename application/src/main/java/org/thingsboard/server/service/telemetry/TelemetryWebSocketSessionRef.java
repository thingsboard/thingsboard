/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.telemetry;

import lombok.Getter;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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
    @Getter
    private final AtomicInteger sessionSubIdSeq;

    public TelemetryWebSocketSessionRef(String sessionId, SecurityUser securityCtx, InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        this.sessionId = sessionId;
        this.securityCtx = securityCtx;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.sessionSubIdSeq = new AtomicInteger();
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
