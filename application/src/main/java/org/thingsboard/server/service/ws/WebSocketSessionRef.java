/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.ws;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 27.03.18.
 */
@Builder
@Data
public class WebSocketSessionRef {

    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private volatile SecurityUser securityCtx;
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;
    private final WebSocketSessionType sessionType;
    private final AtomicInteger sessionSubIdSeq = new AtomicInteger();

    public TenantId getTenantId() {
        return securityCtx != null ? securityCtx.getTenantId() : TenantId.SYS_TENANT_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketSessionRef that = (WebSocketSessionRef) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        String info = "";
        if (securityCtx != null) {
            info += "[" + securityCtx.getTenantId() + "]";
            info += "[" + securityCtx.getId() + "]";
        }
        info += "[" + sessionId + "]";
        return info;
    }

}
